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

import com.wegtam.books.comeoutandplay.games.seabattle.Painter.syntax._
import com.wegtam.books.comeoutandplay.games.seabattle.adt.EnemyBoardFieldState._
import com.wegtam.books.comeoutandplay.games.seabattle.adt._
import org.scalajs.dom
import org.scalajs.dom.html.{ Button, Canvas, Select, TextArea }
import org.scalajs.dom.raw.HTMLOptionElement

import scala.scalajs.js.annotation._

@JSExportTopLevel("TestApp")
object TestApp {

  private var myGameBoard    = Board.createDefaultBoard
  private var enemyGameBoard = EnemyBoard.initialiseFromBoard(Board.createDefaultBoard)

  def clearGameBoard(fieldSize: Int): Unit = {
    myGameBoard = Board.createDefaultBoard
    val canvas =
      dom.document
        .getElementById("game-board-canvas")
        .asInstanceOf[Canvas]
    myGameBoard.paint(fieldSize)(canvas)
  }

  @JSExport
  def runUnsafe(): Unit = {
    dom.document.getElementById("scalaJsTest").textContent = "Hello World!"

    val messageLog = dom.document.getElementById("messages").asInstanceOf[TextArea]

    val clearBtn = dom.document.getElementById("clear-game-board").asInstanceOf[Button]

    val classes =
      List(ShipClass.Battleship, ShipClass.Cruiser, ShipClass.Destroyer, ShipClass.Submarine)
    val ships = dom.document.getElementById("shipyard-ships").asInstanceOf[Select]
    val shipList: List[(ShipClass, Orientation)] = classes.flatMap { c =>
      List((c, Orientation.Horizontal), (c, Orientation.Vertical))
    }
    var currentShipClass: ShipClass     = shipList.head._1
    var currentOrientation: Orientation = shipList.head._2

    shipList.foreach { sle =>
      val (c, o) = sle
      val elem   = dom.document.createElement("option").asInstanceOf[HTMLOptionElement]
      elem.setAttribute("value", s"$c|$o")
      elem.setAttribute("id", s"$c-$o")
      elem.appendChild(dom.document.createTextNode(s"$c (max. ${c.maxShips}) - $o"))
      ships.appendChild(elem)
    }

    ships.onchange = (_: dom.Event) => {
      val s = shipList(ships.selectedIndex)
      currentShipClass = s._1
      currentOrientation = s._2
    }

    val defaultBoard = myGameBoard
    // TODO Calculate game board size from client width and height.
    val cw = 400
    val ch = 400
    val size =
      if (cw < ch)
        cw / defaultBoard.columns
      else
        ch / defaultBoard.rows

    var b = defaultBoard
    val canvas =
      dom.document
        .getElementById("game-board-canvas")
        .asInstanceOf[Canvas]
    b.paint(size)(canvas)
    canvas.onmousedown = (e: dom.MouseEvent) => {
      val pos = getClickPosition(canvas)(size)(e)
      dom.console.log(s"Placing $currentShipClass in $currentOrientation orientation at $pos")
      Board.placeShip(b, currentShipClass, currentOrientation, pos) match {
        case Left(f) =>
          messageLog.textContent += s"Could not place ship: $f\n"
          dom.console.log(s"Could not place ship: $f")
        case Right(n) =>
          n.paint(size)(canvas)
          b = n
      }
    }
    val eCanvas =
      dom.document
        .getElementById("enemy-game-board-canvas")
        .asInstanceOf[Canvas]
    var enemy = EnemyBoard
      .initialiseFromBoard(b)
    enemy.paint(size)(eCanvas)
    eCanvas.onmousedown = (e: dom.MouseEvent) => {
      val pos = getClickPosition(eCanvas)(size)(e)
      if (enemy.isFinished) {
        messageLog.textContent += "Board already finished!\n"
        dom.console.log("Board already finished!")
      } else if (enemy.rows(pos.row)(pos.column) != EnemyBoardFieldState.Unknown) {
        messageLog.textContent += "Field already checked!\n"
        dom.console.log("Field already checked!")
      } else {
        val status = getEnemyPositionStatus(pos)
        messageLog.textContent += s"Updating $pos with $status.\n"
        dom.console.log(s"Updating $pos with $status.")
        enemy = enemy.update(pos)(status)
        enemy.paint(size)(eCanvas)
      }
    }
  }

  def getClickPosition(c: Canvas)(fieldSize: Int)(e: dom.MouseEvent): Position = {
    val r   = c.getBoundingClientRect()
    val col = ((e.clientX - r.left) / fieldSize).toInt
    val row = ((e.clientY - r.top) / fieldSize).toInt
    Position(col, row)
  }

  def getEnemyPositionStatus(p: Position): EnemyBoardFieldState = {
    val states = List(Hit, Sunk, Water)
    states(scala.util.Random.nextInt(states.size)) // DEBUG
  }
}
