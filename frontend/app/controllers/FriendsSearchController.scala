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

import javax.inject.Inject

import actors.FriendsSearchWebsocketActor
import akka.actor.ActorSystem
import akka.stream.Materializer
import com.mohiva.play.silhouette.api.{ HandlerResult, Silhouette }
import models.daos.FriendsDAO
import play.api.i18n.MessagesApi
import play.api.libs.json.JsValue
import play.api.libs.streams.ActorFlow
import play.api.mvc.{ AnyContentAsEmpty, Controller, Request, WebSocket }
import utils.auth.DefaultEnv

import scala.concurrent.Future

/**
  * Special controller that defines a WebSocket for the exchange of
  * data between the backend and the client relating to friends.
  *
  * @param system       The actor system.
  * @param silhouette   Silhouette environment.
  * @param materializer The Materializer.
  * @param friendsDAO   The friendsDAO to get data access.
  * @param messagesApi  Messages API for i18n.
  */
class FriendsSearchController @Inject()(implicit system: ActorSystem,
                                        silhouette: Silhouette[DefaultEnv],
                                        materializer: Materializer,
                                        friendsDAO: FriendsDAO,
                                        val messagesApi: MessagesApi)
    extends Controller {

  /**
    * * Specify the WebSocket with the accepted data types.
    *
    * This WebSocket is secured to provide the currently signed in
    * user to the WebSocket.
    *
    * @return WebSocket
    */
  def socket: WebSocket = WebSocket.acceptOrResult[JsValue, JsValue] { implicit request =>
    import play.api.libs.concurrent.Execution.Implicits.defaultContext
    implicit val req = Request(request, AnyContentAsEmpty)
    silhouette
      .SecuredRequestHandler { securedRequest =>
        Future.successful(HandlerResult(Ok, Some(securedRequest.identity)))
      }
      .map {
        case HandlerResult(r, Some(user)) =>
          Right(
            ActorFlow.actorRef(
              out => FriendsSearchWebsocketActor.props(messagesApi, out, user, friendsDAO)
            )
          )
        case HandlerResult(r, None) => Left(Forbidden)
      }
  }
}
