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

package com.wegtam.books.comeoutandplay.games.seabattle

import cats.instances.string._
import cats.syntax.eq._
import org.scalajs.dom
import org.scalajs.dom.raw.Document

object Sockets {

  /**
    * Return the base uri for a websocket connection.
    *
    * @param doc The current document.
    * @return The base uri for a websocket connection using SSL if available.
    */
  def getBaseUri(doc: Document): String = {
    val p = if (dom.document.location.protocol === "https:") "wss" else "ws"
    s"$p://${dom.document.location.host}"
  }
}
