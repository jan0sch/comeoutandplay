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
import io.circe.Json
import play.api.libs.json.JsValue

/**
  * Typeclass für die Konvertierung in Play Json.
  *
  * @tparam T Der Eingabedatentyp.
  */
trait Convert2PlayJson[T] {

  /**
    * Konvertiert den übergebenen JSON-Typ in einen Play JSON Typ.
    *
    * @param json JSON-Eingabedatentyp.
    * @return Ein Play-JSON-Typ.
    */
  def toJsValue(json: T): play.api.libs.json.JsValue

}

object Convert2PlayJson {

  /**
    * Implementierung für Circe -> Play Json.
    */
  implicit val Circe2PlayJson: Convert2PlayJson[io.circe.Json] = (json: Json) =>
    play.api.libs.json.Json.parse(json.noSpaces)

  object syntax {

    implicit final class WrapCirce2PlayJson(val t: io.circe.Json) extends AnyVal {

      /**
        * Konvertiert den Datentyp in einen Play-JSON-Typ.
        *
        * @param ev Eine implizit verfügbare Klasse, die die notwendigen Funktionen bereitstellt.
        * @return Einen Play-JSON-Typ.
        */
      def toJsValue(implicit ev: Convert2PlayJson[io.circe.Json]): JsValue =
        ev.toJsValue(t)
    }

  }

}
