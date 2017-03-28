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

import com.wegtam.books.comeoutandplay.games.seabattle.adt.Message
import io.circe.syntax._
import play.api.libs.json.JsValue
import utils.json.Convert2CirceJson.syntax._
import utils.json.Convert2PlayJson.syntax._

import scala.concurrent.Future
import scala.language.higherKinds

trait WebsocketAlgebra[F[_]] {

  /**
    * Dekodiert eine empfange Nachricht im JsValue format von Play
    * in eine Nachricht, die vom Spiel verarbeitet werden kann.
    *
    * @param message Eine JsValue-Nachricht, die vom Websocket empfangen wurde.
    * @return Die dekodierte Spielnachricht.
    */
  def decodeRawMessage(message: JsValue): F[Message]

  /**
    * Kodiert eine Spielnachricht in eine JsValue-Nachricht, die vom
    * Websocket verschickt werden kann.
    *
    * @param message Eine Spielnachricht.
    * @return Die kodierte JsValue-Nachricht.
    */
  def encodeGameMessage(message: Message): F[JsValue]

}

class FutureWebsocketAlgebra extends WebsocketAlgebra[Future] {
  override def decodeRawMessage(message: JsValue): Future[Message] = {
    val result = for {
      circe <- message.toCirceJson
      gameM <- circe.as[Message]
    } yield gameM
    result match {
      case Left(e)  => Future.failed(e)
      case Right(m) => Future.successful(m)
    }
  }

  override def encodeGameMessage(message: Message): Future[JsValue] =
    Future.successful(message.asJson.toJsValue)
}
