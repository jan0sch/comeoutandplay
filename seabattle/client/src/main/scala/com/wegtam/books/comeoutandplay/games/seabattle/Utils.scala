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
import com.wegtam.books.comeoutandplay.games.seabattle.adt.{ Board, Position }
import org.scalajs.dom
import org.scalajs.dom.html.{ Canvas, TextArea }

object Utils {

  /**
    * Ein sealed trait für die Logmodi.
    */
  sealed trait LogMode

  object LogMode {

    /**
      * Schreibe die neuesten Nachrichten nach oben.
      */
    case object LatestFirst extends LogMode

    /**
      * Schreibe die neuesten Nachrichten nach hinten (wie normales Log).
      */
    case object LatestLast extends LogMode
  }

  /**
    * Berechnet die Basis-URL für Websocket-Verbindungen.
    *
    * @param doc Ein DOM-Dokument.
    * @return Eine Basis-URL für Websockets (`ws[s]://hostname[:port]`).
    */
  def calcWSBaseUrl(doc: dom.html.Document): String = {
    val scheme =
      if (doc.location.protocol === "https:")
        "wss:"
      else
        "ws:"
    val port = scheme match {
      case "ws:"  => if (doc.location.port === "80") "" else s":${doc.location.port}"
      case "wss:" => if (doc.location.port === "443") "" else s":${doc.location.port}"
      case _      => s":${doc.location.port}"
    }
    s"$scheme//${doc.location.hostname}$port"
  }

  /**
    * Berechne die Feldgröße zum Zeichnen des übergebenen Spielfelds
    * anhand der Größe des umschließenden HTML-Elements.
    *
    * @param b         Ein Spielfeld.
    * @param container Das DOM-Element, welches das Canvas enthält.
    * @return Die Feldgröße, die zum Zeichnen auf einem Canvas benutzt werden soll.
    */
  def calcFieldSize(b: Board)(container: dom.Element): Int = {
    val height = container.clientHeight
    val width  = container.clientWidth
    if (width < height)
      width / b.columns
    else
      height / b.rows
  }

  /**
    * Berechne die korrekte Positionsangabe für das Spielfeld anhand der
    * Koordinaten des Mausklicks auf das Canvas und der verwendeten Feldgröße.
    *
    * @param c         Das Canvas, auf dem das Spielfeld dargestellt wird.
    * @param fieldSize Die verwendete Feldgröße.
    * @param e         Das Klickereignis.
    * @return Eine Positionsangabe für das korrekte Feld auf dem Spielfeld.
    */
  def calcClickPosition(c: Canvas)(fieldSize: Int)(e: dom.MouseEvent): Position = {
    val r   = c.getBoundingClientRect()
    val col = ((e.clientX - r.left) / fieldSize).toInt
    val row = ((e.clientY - r.top) / fieldSize).toInt
    Position(col, row)
  }

  /**
    * Schreibe einen Logeintrag.
    *
    * @param m      Der Logmodus (neueste nach oben oder nach unten).
    * @param target Das Zielelement (TextArea).
    * @param msg    Die Lognachricht.
    */
  def log(m: LogMode)(target: TextArea)(msg: String): Unit =
    m match {
      case LogMode.LatestFirst => target.textContent = msg + "\n" + target.textContent
      case LogMode.LatestLast  => target.textContent += msg + "\n"
    }

  // Helfer für klassisches Logging (append).
  val logClassic: TextArea => String => Unit = log(LogMode.LatestLast)
  // Helfer für Logging, bei dem die neuesten Einträge "oben" landen.
  val logModern: TextArea => String => Unit = log(LogMode.LatestFirst)
}
