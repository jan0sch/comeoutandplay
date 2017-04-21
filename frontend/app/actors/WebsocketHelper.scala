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

package actors

import play.twirl.api.Html

/**
  * Helper methods for websockets.
  */
trait WebsocketHelper {

  /**
    * Helper method that removes any characters from the Html
    * representation that would lead to problems when sending via
    * JSON to the client.
    *
    * @param html The Html that must be cleaned.
    * @return The cleaned Html as String.
    */
  def cleanHtmlTemplate(html: Html): String =
    html
      .toString()
      .replaceAll("\"", "'")
      .split('\n')
      .map(e ⇒ e.toString.trim.filter(_ >= ' '))
      .mkString

}
