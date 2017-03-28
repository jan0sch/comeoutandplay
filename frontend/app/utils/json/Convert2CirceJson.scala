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

package utils.json
import io.circe.{ Json, ParsingFailure }
import io.circe.parser._
import play.api.libs.json.JsValue

/**
  * Typeclass für die Konvertierung in Circe-JSON.
  *
  * @tparam T Der Eingabedatentyp.
  */
trait Convert2CirceJson[T] {

  /**
    * Konvertiert den übergebenen Typ in einen Circe-JSON Typ.
    *
    * @param t JSON-Eingabedatentyp.
    * @return Entweder ein Fehler oder ein Circe-JSON Typ.
    */
  def toCirceJson(t: T): Either[ParsingFailure, Json]

  /**
    * Konvertiert den übergebenen Typ in einen Circe-JSON Typ.
    *
    * @param t JSON-Eingabedatentyp.
    * @return Ein Circe-JSON Typ.
    */
  def toCirceJsonUnsafe(t: T): Json

}

object Convert2CirceJson {

  implicit val PlayJson2Circe: Convert2CirceJson[JsValue] = new Convert2CirceJson[JsValue] {
    override def toCirceJson(t: JsValue): Either[ParsingFailure, Json] =
      parse(t.toString())

    @SuppressWarnings(Array("org.wartremover.warts.OptionPartial"))
    override def toCirceJsonUnsafe(t: JsValue): Json =
      // Dieser Code dient exemplarisch als schlechtes Beispiel.
      // Operationen wie `.get` oder `.head` führen zu undurchsichtigen Fehlern
      // (None.get Exception), die quasi analog zur berühmt, berüchtigten
      // Null-Pointer-Exception zu sehen sind.
      // Es kann Fälle geben, in denen diese Art der Anwendung in Ordnung ist, d.h.
      // die Eingabedaten _garantieren_ keinen Fehler, aber dies ist eher selten.
      // In diesem Fall wird ein bereits als valid verarbeitetes JSON in "nochmal"
      // verarbeitet, daher sollte die Wahrscheinlichkeit eines Fehlers sehr gering sein.
      parse(t.toString()).toOption.get
  }

  object syntax {
    implicit final class WrapPlayJson2Circe(val t: JsValue) extends AnyVal {
      def toCirceJson(
          implicit ev: Convert2CirceJson[JsValue]
      ): Either[ParsingFailure, Json] =
        ev.toCirceJson(t)

      def toCirceJsonUnsafe(implicit ev: Convert2CirceJson[JsValue]): Json =
        ev.toCirceJsonUnsafe(t)
    }
  }

}
