/*
 * Copyright (C) 2017  Jens Grassel & André Schütz
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package controllers

import java.util.UUID

import actors.seabattle.{ FutureWebsocketAlgebra, WebsocketActor }
import akka.actor.ActorSystem
import akka.stream.Materializer
import cats.instances.int._
import cats.syntax.eq._
import com.mohiva.play.silhouette.api.{ HandlerResult, Silhouette }
import com.wegtam.books.comeoutandplay.games.seabattle.adt._
import com.wegtam.books.comeoutandplay.games.seabattle.adt.GameStateOps.syntax._
import com.wegtam.books.comeoutandplay.games.seabattle.logic._
import javax.inject.Inject
import models.{ FriendWithInfo, GameStateLight }
import models.daos.{ FriendsDAO, SeabattleDAO }
import play.api.data._
import play.api.data.format.Formats._
import play.api.data.Forms._
import play.api.i18n.{ I18nSupport, Messages }
import play.api.libs.json.JsValue
import play.api.libs.streams.ActorFlow
import play.api.mvc._
import utils.auth.DefaultEnv
import utils.GameStateConverter.syntax._

import scala.collection.immutable._
import scala.concurrent.Future

class SeabattleController @Inject()(components: ControllerComponents)(
    implicit system: ActorSystem,
    silhouette: Silhouette[DefaultEnv],
    materializer: Materializer,
    seabattleDAO: SeabattleDAO,
    friendsDAO: FriendsDAO,
    implicit val webJarsUtil: org.webjars.play.WebJarsUtil,
    implicit val assets: AssetsFinder
) extends AbstractController(components)
    with I18nSupport {
  import system.dispatcher

  val createForm = Form(
    mapping(
      "opponent" -> optional(of[UUID])
    )(CreateSeabattleGame.apply)(CreateSeabattleGame.unapply)
  )

  /**
    * Startseite für "Schiffe versenken"
    *
    * @return Eine HTML-Seite mit diversen Links.
    */
  def index: Action[AnyContent] = silhouette.SecuredAction.async { implicit request =>
    val user = request.identity
    for {
      mine      <- seabattleDAO.getOwn(user.userID)
      running   <- seabattleDAO.getParticipating(finished = false)(user.userID)
      completed <- seabattleDAO.getParticipating(finished = true)(user.userID)
      open      <- seabattleDAO.getOpen(Option(user.userID))
      friends   <- friendsDAO.getFriends(user)
      data = SeabattleStatusPage(
        completed = completed.to[Seq].light,
        friends = friends.to[Seq],
        mine = mine.to[Seq].light,
        open = open.to[Seq].light,
        running = running.to[Seq].light
      )
    } yield Ok(views.html.games.seabattle.index(createForm, data, user))
  }

  /**
    * Lege ein neues Spiel an.
    *
    * @return
    */
  def create: Action[AnyContent] = silhouette.SecuredAction.async { implicit request =>
    val user = request.identity
    createForm.bindFromRequest.fold(
      formWithErrors =>
        for {
          mine      <- seabattleDAO.getOwn(user.userID)
          running   <- seabattleDAO.getParticipating(finished = false)(user.userID)
          completed <- seabattleDAO.getParticipating(finished = true)(user.userID)
          open      <- seabattleDAO.getOpen(Option(user.userID))
          friends   <- friendsDAO.getFriends(user)
          data = SeabattleStatusPage(
            completed = completed.to[Seq].light,
            friends = friends.to[Seq],
            mine = mine.to[Seq].light,
            open = open.to[Seq].light,
            running = running.to[Seq].light
          )
        } yield BadRequest(views.html.games.seabattle.index(formWithErrors, data, user)),
      formData => {
        val gameId  = UUID.randomUUID()
        val ownerId = request.identity.userID
        val gameState = GameState
          .createEmpty(gameId)(ownerId)
          .copy(opponent = formData.op, running = formData.op.nonEmpty)
        seabattleDAO.add(gameState).map { r =>
          if (r > 0)
            Redirect(routes.SeabattleController.game(gameId))
          else
            Redirect(routes.ApplicationController.index())
        }
      }
    )
  }

  /**
    * Löscht den Spielstand mit der angegebenen ID, wenn der Nutzer der
    * Spieleigentümer ist.
    *
    * @param id Die ID des Spiels.
    * @return
    */
  def destroy(id: UUID): Action[AnyContent] = silhouette.SecuredAction.async { implicit request =>
    val gameId  = id
    val user    = request.identity
    val ownerId = user.userID
    for {
      o <- seabattleDAO.get(gameId)
      r <- o match {
        case None => Future.successful(NotFound(views.html.games.seabattle.notFound(user)))
        case Some(GameState(`gameId`, `ownerId`, _, _, _, _, _, _, _)) =>
          seabattleDAO.remove(gameId).map(_ => Redirect(routes.SeabattleController.index()))
        case Some(GameState(`gameId`, _, _, _, _, _, _, _, _)) => Future.successful(Forbidden(""))
        case _                                                 => Future.successful(InternalServerError(""))
      }
    } yield r
  }

  /**
    * Lade das angegebene Spiel und starte es oder zeige eine 404 Seite an.
    *
    * @param id Die ID des Spiels.
    * @return Eine HTML-Seite mit dem Spiel oder ein 404-Fehler.
    */
  def game(id: UUID): Action[AnyContent] = silhouette.SecuredAction.async { implicit request =>
    val gameId = id
    val user   = request.identity
    seabattleDAO.get(gameId).map { o =>
      o.fold(NotFound(views.html.games.seabattle.notFound(user)))(
        gameState =>
          if (gameState.ready.contains(user.userID)) {
            gameState.getOtherPlayerId(user.userID) match {
              case None =>
                Redirect(routes.SeabattleController.index())
                  .flashing("error" -> Messages("games.seabattle.game.no-opponent"))
              case Some(opponentId) =>
                Ok(views.html.games.seabattle.game(gameState, opponentId, user))
            }
          } else
            Redirect(routes.SeabattleController.gamePrepare(gameId))
      )
    }
  }

  /**
    * Lade das angegebene Spiel und starte den Spielvorbereitungsmodus
    * (Präparieren des Spielfelds).
    *
    * @param id Die ID des Spiels.
    * @return Eine HTML-Seite, die das Vorbereiten des Spielfelds erlaubt oder ein 404-Fehler.
    */
  def gamePrepare(id: UUID): Action[AnyContent] = silhouette.SecuredAction.async {
    implicit request =>
      val gameId = id
      val user   = request.identity
      val shipClasses = ShipClass.all.flatMap { c =>
        Seq((c, Orientation.Horizontal), (c, Orientation.Vertical))
      }
      seabattleDAO.get(gameId).map { o =>
        o.fold(NotFound(views.html.games.seabattle.notFound(user)))(
          gameState =>
            if (gameState.ready.contains(user.userID))
              Redirect(routes.SeabattleController.game(id))
            else
              Ok(views.html.games.seabattle.prepare(gameState, shipClasses, user))
        )
      }
  }

  /**
    * Dem Spiel mit der angegebenen ID beitreten.
    *
    * @param id Die ID des Spiels.
    * @return Entweder eine 404 Fehlerseite oder eine Weiterleitung zur Spielseite.
    */
  def join(id: UUID): Action[AnyContent] = silhouette.SecuredAction.async { implicit request =>
    val gameId = id
    val user   = request.identity
    val msg    = Message.RegisterPlayer(gameId, user.userID)
    for {
      o <- seabattleDAO.get(gameId)
      u <- o.fold(Future.successful(0)) { s =>
        MessageHandlerModule.handle(msg).run(s).value match {
          case (gs, Message.PlayerRegistered(`gameId`, _)) => seabattleDAO.update(gs)
          case _                                           => Future.successful(0)
        }
      }
    } yield {
      o.fold(NotFound(views.html.games.seabattle.notFound(user))) { gameState =>
        if (u === 0)
          Redirect(routes.SeabattleController.game(gameState.gameId))
            .flashing("error" -> Messages("games.seabattle.open-games.join.failure"))
        else
          Redirect(routes.SeabattleController.game(gameState.gameId))
            .flashing("success" -> Messages("games.seabattle.open-games.join.success"))
      }
    }
  }

  /**
    * Erstellt ein Websocket für das Spiel mit der angegebenen ID.
    *
    * @param id Die ID des Spiels.
    * @return Ein Websocket oder ein 401-Fehler.
    */
  def socket(id: UUID): WebSocket = WebSocket.acceptOrResult[JsValue, JsValue] { implicit request =>
    implicit val req: Request[AnyContentAsEmpty.type] = Request(request, AnyContentAsEmpty)
    silhouette
      .SecuredRequestHandler { securedRequest =>
        Future.successful(HandlerResult(Ok, Some(securedRequest.identity)))
      }
      .map {
        case HandlerResult(_, Some(_)) =>
          Right(
            ActorFlow.actorRef(
              out => WebsocketActor.props(id, new FutureWebsocketAlgebra, seabattleDAO, out)
            )
          )
        case HandlerResult(_, None) => Left(Forbidden)
      }
  }

}

final case class CreateSeabattleGame(op: Option[UUID])

/**
  * Container für Spieldaten auf der Übersichtsseite.
  *
  * @param completed Eine List mit Spielständen von Spielen, die abgeschlossen sind und an denen der aktuelle Nutzer teilgenommen hat.
  * @param friends   Eine Liste der Freunde des aktullen Nutzers.
  * @param mine      Eine Liste mit Spielständen, deren Eigentümer der aktuelle Nutzer ist.
  * @param open      Eine List mit Spielständen, die offen sind, d.h. auf Mitspieler warten.
  * @param running   Eine List mit Spielständen von Spielen, die laufen und an denen der aktuelle Nutzer teilnimmt.
  */
final case class SeabattleStatusPage(completed: Seq[GameStateLight],
                                     friends: Seq[FriendWithInfo],
                                     mine: Seq[GameStateLight],
                                     open: Seq[GameStateLight],
                                     running: Seq[GameStateLight])
