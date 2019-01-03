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

package actors.seabattle

import java.util.UUID

import akka.actor.{ Actor, ActorLogging, ActorRef, Props }
import com.wegtam.books.comeoutandplay.games.seabattle.adt._
import com.wegtam.books.comeoutandplay.games.seabattle.adt.Message._
import com.wegtam.books.comeoutandplay.games.seabattle.logic._
import com.wegtam.books.comeoutandplay.games.seabattle.repositories.GameStateRepository
import play.api.libs.json.JsValue

import scala.concurrent.Future
import scala.language.higherKinds

/**
  * Aktor, der ein Websocket implementiert, welches die Kommunikation für
  * ein Spiel regelt.
  *
  * @param id     Die ID des Spiels.
  * @param alg    Die Implementierung der Algebra.
  * @param repo   Das Repository für die Spielstände.
  * @param output Die ActorRef, an die Antworten gesendet werden.
  */
class WebsocketActor(id: UUID,
                     alg: WebsocketAlgebra[Future],
                     repo: GameStateRepository[Future],
                     output: ActorRef)
    extends Actor
    with ActorLogging {

  import context.dispatcher

  @SuppressWarnings(Array("org.wartremover.warts.Any"))
  override def receive: Receive = {
    case json: JsValue =>
      log.debug("Received JSON message: {}!", json)
      val response = for {
        input <- alg.decodeRawMessage(json)
        gsO   <- repo.get(id)
        res <- gsO.fold(Future.successful(GameError("Game not found!"): Message)) { gs =>
          MessageHandlerModule.handle(input).run(gs).value match {
            case (_, GameError(s, d)) => Future.successful(GameError(s, d))
            case (gs, msg)            => repo.update(gs).map(_ => msg)
          }
        }
        answer <- alg.encodeGameMessage(res)
      } yield answer
      val _ = response.map(r => output ! r)
    case msg =>
      log.error("Unable to handle received message: {}!", msg)
  }
}

object WebsocketActor {

  /**
    * Helfermethode zum Erstellen des Aktors.
    *
    * @param gameId     Die ID des zugehörigen Spiels.
    * @param algebra    Die zu verwendende Algebra.
    * @param repository Das Repository für den Datenbankzugriff.
    * @param output     Referenz zum Aktor für die Websocketkommunikation.
    * @return Die Props, die zum Erstellen des Aktors notwendig sind.
    */
  def props(gameId: UUID,
            algebra: WebsocketAlgebra[Future],
            repository: GameStateRepository[Future],
            output: ActorRef): Props =
    Props(new WebsocketActor(id = gameId, alg = algebra, repo = repository, output = output))

}
