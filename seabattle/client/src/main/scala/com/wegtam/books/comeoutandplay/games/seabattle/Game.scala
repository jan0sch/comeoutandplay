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
import org.scalajs.dom.html.{ Canvas, Div, TextArea }

import scala.scalajs.js.annotation._

@JSExportTopLevel("Game")
object Game {
  private var enemyBoard: Option[EnemyBoard] = None
  private var myGameBoard: Option[Board]     = None
  private var firePos: Option[Position]      = None
  private var pingServerJob: Option[Int]     = None
  private val pingServerJobInterval: Double  = 3000d

  /**
    * Setzte das Spielfeld.
    * Im Fehlerfall wird ein Standardspielfeld gesetzt.
    *
    * @param json Eine JSON-Zeichenkette, die das Spielfeld beschreibt.
    */
  @JSExport
  def setBoard(json: String): Unit =
    myGameBoard = decode[Board](json).toOption

  @JSExport
  def setEnemyBoard(json: String): Unit =
    enemyBoard = decode[EnemyBoard](json).toOption

  /**
    * Erstelle eine "Alert-Box" (Bootstrap).
    *
    * @param kind Der Typ der Box.
    * @param txt Der Text für die Box.
    * @return Das Element mit der Box.
    */
  @JSExport
  def createAlertBox(kind: AlertBoxType)(txt: String): dom.Element = {
    val info = dom.document.createElement("div")
    info.setAttribute("class", s"${kind.cssClass} alert-dismissible")
    info.setAttribute("role", "alert")
    info.textContent = txt

    val btn = dom.document.createElement("button")
    btn.setAttribute("type", "button")
    btn.setAttribute("class", "close")
    btn.setAttribute("data-dismiss", "alert")
    btn.setAttribute("aria-label", "close")

    val spn = dom.document.createElement("span")
    spn.setAttribute("aria-hidden", "true")
    spn.textContent = "X"

    btn.appendChild(spn)
    info.appendChild(btn)
    info
  }

  /**
    * Haupteinsprungpunkt für das Spiel.
    *
    * @param socketPath    Der relative Pfad zum Websocket.
    * @param canvasId      Die ID des Canvas-Elements für das eigene Spielfeld.
    * @param enemyCanvasId Die ID des Canvas-Elements für das gegnerische Spielfeld.
    * @param logId         Die ID einer Texteingabe, die als Logdatei genutzt wird.
    */
  @JSExport
  def runUnsafe(socketPath: String,
                canvasId: String,
                enemyCanvasId: String,
                logId: String): Unit = {
    val canvas      = dom.document.getElementById(canvasId).asInstanceOf[Canvas]
    val enemyCanvas = dom.document.getElementById(enemyCanvasId).asInstanceOf[Canvas]
    val fieldSize =
      Utils.calcFieldSize(myGameBoard.getOrElse(Board.createDefaultBoard))(canvas.parentElement)
    val messageLog          = dom.document.getElementById(logId).asInstanceOf[TextArea]
    val messageLogContainer = messageLog.parentElement.asInstanceOf[Div]
    val log: String => Unit = Utils.logModern(messageLog)

    if (enemyBoard.isEmpty)
      dom.window.alert("Enemy board not ready!")

    enemyBoard.foreach(_.paint(fieldSize)(enemyCanvas))
    myGameBoard.foreach(_.paint(fieldSize)(canvas))

    val gameId   = UUID.fromString(canvas.getAttribute("data-game"))
    val playerId = UUID.fromString(canvas.getAttribute("data-player"))

    val socketUrl = Utils.calcWSBaseUrl(dom.document) + socketPath
    val socket    = new dom.WebSocket(socketUrl)
    socket.onerror = (_: dom.Event) => {
      dom.console.error("Server communication error!")
    }

    val readyMsg: Message = Message.Ready(
      gameId = gameId,
      playerId = playerId
    )
    val pingServer = () => {
      socket.send(readyMsg.asJson.noSpaces)
    }

    socket.onmessage = (e: dom.MessageEvent) => {
      dom.console.log("Got message...")
      decode[Message](e.data.toString) match {
        case Left(error) => dom.console.error(error.toString)
        case Right(msg) =>
          msg match {
            case Message.GameError(s, d) =>
              val txt  = s"$s${d.fold("")(t => s", $t")}"
              val info = createAlertBox(AlertBoxType.Danger)(txt)
              messageLogContainer.appendChild(info)
              dom.console.error(txt)
            case Message.GameOver(`gameId`, b, eb, winner) =>
              pingServerJob.foreach(id => dom.window.clearInterval(id))
              myGameBoard = Option(b)
              myGameBoard.foreach(_.paint(fieldSize)(canvas))
              enemyBoard = Option(eb)
              enemyBoard.foreach(_.paint(fieldSize)(enemyCanvas))
              val won = winner.contains(playerId)
              if (won) {
                log("You won!")
              } else {
                log("You lost!")
              }
              val info = createAlertBox(AlertBoxType.Info)("Game Over!")
              messageLogContainer.appendChild(info)
            case Message.MoveResult(`gameId`, b, eb, continue, winner) =>
              myGameBoard = Option(b)
              myGameBoard.foreach(_.paint(fieldSize)(canvas))
              enemyBoard = Option(eb)
              enemyBoard.foreach(_.paint(fieldSize)(enemyCanvas))
              if (continue)
                firePos = None
              pingServerJob = Option(dom.window.setInterval(pingServer, pingServerJobInterval))
            case Message.MakeMove(`gameId`, b, eb) =>
              myGameBoard = Option(b)
              myGameBoard.foreach(_.paint(fieldSize)(canvas))
              enemyBoard = Option(eb)
              enemyBoard.foreach(_.paint(fieldSize)(enemyCanvas))
              firePos = None
              log("Make your move!")
              pingServerJob.foreach(id => dom.window.clearInterval(id))
            case Message.WaitForOtherPlayer(`gameId`) =>
              log("Wait for other player.")
            case _ => dom.console.error("Received unhandled message!")
          }
      }
    }

    val fire = (e: dom.MouseEvent) => {
      firePos match {
        case None =>
          val pos = Utils.calcClickPosition(enemyCanvas)(fieldSize)(e)
          firePos = Option(pos)
          val msg: Message = Message.Move(
            gameId = gameId,
            playerId = playerId,
            position = pos
          )
          log(s"Fire at position $pos")
          socket.send(msg.asJson.noSpaces)
        case Some(p) =>
          log(s"Already fired at $p")
      }
    }

    socket.onopen = (_: dom.Event) => {
      pingServer
      pingServerJob = Option(dom.window.setInterval(pingServer, pingServerJobInterval))
      enemyCanvas.addEventListener("click", fire)
    }
  }

}

sealed trait AlertBoxType extends Product with Serializable {

  /**
    * Gibt die CSS-Klasse des Typs zurück.
    *
    * @return Ein String, der die CSS-Klasse enthält.
    */
  def cssClass: String
}
object AlertBoxType {
  case object Danger extends AlertBoxType {
    override def cssClass: String = "alert alert-danger"
  }
  case object Info extends AlertBoxType {
    override def cssClass: String = "alert alert-info"
  }
  case object Success extends AlertBoxType {
    override def cssClass: String = "alert alert-success"
  }
  case object Warning extends AlertBoxType {
    override def cssClass: String = "alert alert-warning"
  }
}
