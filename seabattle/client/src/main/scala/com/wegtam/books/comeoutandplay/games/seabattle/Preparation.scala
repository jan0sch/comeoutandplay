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

import java.util.UUID

import com.wegtam.books.comeoutandplay.games.seabattle.Painter.syntax._
import com.wegtam.books.comeoutandplay.games.seabattle.adt._
import io.circe.parser._
import io.circe.syntax._
import org.scalajs.dom
import org.scalajs.dom.html.{ Button, Canvas, Select, TextArea }
import org.scalajs.dom.raw.HTMLOptionElement

import scala.scalajs.js.annotation._

@JSExportTopLevel("Preparation")
object Preparation {
  private var currentOrientation: Option[Orientation] = None
  private var currentShipClass: Option[ShipClass]     = None
  private var myGameBoard: Board                      = Board.createDefaultBoard

  /**
    * Setzte das Spielfeld.
    * Im Fehlerfall wird ein Standardspielfeld gesetzt.
    *
    * @param json Eine JSON-Zeichenkette, die das Spielfeld beschreibt.
    */
  @JSExport
  def setBoard(json: String): Unit =
    myGameBoard = decode[Board](json).getOrElse(Board.createDefaultBoard)

  /**
    * Löscht das aktuelle Spielfeld und zeichnet den entsprechenden Teil der
    * Seite neu.
    *
    * @param c         Das Canvas, auf dem gezeichnet werden soll.
    * @param fieldSize Die Feldgröße.
    */
  def clearGameBoard(c: Canvas, fieldSize: Int): Unit = {
    myGameBoard = Board.createDefaultBoard
    myGameBoard.paint(fieldSize)(c)
  }

  def parseSelection(oe: HTMLOptionElement): Option[(ShipClass, Orientation)] =
    decode[(ShipClass, Orientation)](oe.value) match {
      case Left(e) =>
        dom.console.error(s"Could not decode selection (${oe.value}): $e")
        None
      case Right((c, o)) => Some((c, o))
    }

  /**
    * Haupteinsprungpunkt für die Spielvorbereitung.
    *
    * @param socketPath Der relative Pfad zum Websocket.
    * @param canvasId   Die ID des Canvas-Elements.
    * @param shipyardId Die ID der Auswahlliste (`select`) mit Schiffen und Ausrichtungen.
    * @param clearId    Die ID des Buttons, der ein Zurücksetzen des Spielfelds auslösen soll.
    * @param readyId    Die ID des Buttons, der die Bereitschaft zum Spiel setzen soll.
    * @param logId      Die ID einer Texteingabe, die als Logdatei genutzt wird.
    */
  @JSExport
  def runUnsafe(socketPath: String,
                canvasId: String,
                shipyardId: String,
                clearId: String,
                readyId: String,
                logId: String): Unit = {
    val canvas = dom.document.getElementById(canvasId).asInstanceOf[Canvas]
    val fieldSize =
      Utils.calcFieldSize(myGameBoard)(canvas.parentElement)
    val messageLog          = dom.document.getElementById(logId).asInstanceOf[TextArea]
    val log: String => Unit = Utils.logModern(messageLog)
    val shipyard            = dom.document.getElementById(shipyardId).asInstanceOf[Select]

    myGameBoard.paint(fieldSize)(canvas)

    val clearButton = dom.document.getElementById(clearId).asInstanceOf[Button]
    clearButton.onclick = (_: dom.Event) => {
      if (dom.window.confirm(clearButton.getAttribute("data-locale"))) {
        clearGameBoard(canvas, fieldSize)
        log("Cleared game board.")
      }
    }

    val readyButton = dom.document.getElementById(readyId).asInstanceOf[Button]
    val gameId      = UUID.fromString(readyButton.getAttribute("data-game"))
    val playerId    = UUID.fromString(readyButton.getAttribute("data-player"))
    val readyUrl    = readyButton.getAttribute("data-url")

    val redirectToGame = (u: String) => dom.window.location.assign(u)

    val socketUrl = Utils.calcWSBaseUrl(dom.document) + socketPath
    val socket    = new dom.WebSocket(socketUrl)
    socket.onmessage = (e: dom.MessageEvent) => {
      dom.console.log("Got message...")
      decode[Message](e.data.toString) match {
        case Left(error) => dom.console.error(error.toString)
        case Right(Message.GameError(summary, details)) =>
          log(summary)
          dom.console.error(s"$summary $details")
          dom.window.alert(summary)
        case Right(Message.BoardSaved(`gameId`, `playerId`, board)) =>
          myGameBoard = board
          val msg: Message = Message.Ready(gameId, playerId)
          socket.send(msg.asJson.noSpaces)
        case Right(Message.WaitForOtherPlayer(`gameId`)) => redirectToGame(readyUrl)
        case Right(Message.MakeMove(`gameId`, _, _))     => redirectToGame(readyUrl)
        case Right(msg)                                  => dom.console.error(s"Received unhandled message! $msg")
      }
    }
    socket.onerror = (_: dom.Event) => {
      dom.console.error("Server communication error!")
    }

    readyButton.onclick = (_: dom.Event) => {
      if (dom.window.confirm(clearButton.getAttribute("data-locale"))) {
        val msg: Message = Message.SaveBoard(
          gameId = gameId,
          playerId = playerId,
          board = myGameBoard
        )
        socket.send(msg.asJson.noSpaces)
      }
    }

    parseSelection(shipyard.options(shipyard.selectedIndex)) match {
      case None =>
        dom.console.error(
          s"Could not parse selected value: ${shipyard.options(shipyard.selectedIndex).value}"
        )
      case Some((c, o)) =>
        currentOrientation = Option(o)
        currentShipClass = Option(c)
    }

    shipyard.onchange = (_: dom.Event) => {
      val ds = parseSelection(shipyard.options(shipyard.selectedIndex))
      val (c, o) = ds match {
        case None           => (None, None)
        case Some((sc, or)) => (Option(sc), Option(or))
      }
      currentShipClass = c
      currentOrientation = o
      log(s"You selected $c ($o).")
    }

    canvas.onmousedown = (e: dom.MouseEvent) => {
      val pos = Utils.calcClickPosition(canvas)(fieldSize)(e)
      log(s"Placing $currentShipClass in $currentOrientation orientation at $pos")
      (currentOrientation, currentShipClass) match {
        case (Some(o), Some(c)) =>
          Board.placeShip(myGameBoard, c, o, pos) match {
            case Left(f) =>
              log(s"Could not place ship: $f")
            case Right(n) =>
              n.paint(fieldSize)(canvas)
              myGameBoard = n
          }
        case _ => log("No shipclass or orientation selected!")
      }
    }
  }

}
