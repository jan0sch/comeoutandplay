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

import com.wegtam.books.comeoutandplay.games.seabattle.adt.EnemyBoardFieldState._
import com.wegtam.books.comeoutandplay.games.seabattle.adt._
import org.scalajs.dom.html.Canvas
import org.scalajs.dom.raw.CanvasRenderingContext2D

/**
  * Definition einer "type class", die Funktionen zum Zeichnen
  * bereitstellt.
  */
abstract class Painter[T] {

  /**
    * Zeichne auf das übergebene Canvas.
    *
    * '''Achtung!''' Diese Funktion verändert das übergebene Canvas!
    *
    * @param t         Das zu zeichnende Objekt.
    * @param fieldSize Die Feldgröße.
    * @param canvas    Ein HTML-5 Canvas Element.
    * @return
    */
  def paint(t: T)(fieldSize: Int)(canvas: Canvas): Unit

}

object Painter {
  final val ColourHit     = "#ec7820"
  final val ColourShip    = "#5f4d35"
  final val ColourSunk    = "#3a2e20"
  final val ColourUnknown = "#f8f8f8"
  final val ColourWater   = "#4796c1"

  implicit val EnemyBoardPainter: Painter[EnemyBoard] = new Painter[EnemyBoard] {

    override def paint(b: EnemyBoard)(fieldSize: Int)(canvas: Canvas): Unit = {
      val boardWidth  = b.rows.head.size * fieldSize
      val boardHeight = b.rows.size * fieldSize

      val renderer = canvas
        .getContext("2d")
        .asInstanceOf[CanvasRenderingContext2D]
      canvas.width = boardWidth
      canvas.height = boardHeight

      renderer.fillStyle = ColourUnknown
      renderer.fillRect(0, 0, canvas.width, canvas.height)

      renderer.fillStyle = "#000000"
      renderer.lineWidth = 1
      for {
        (r, ri) <- b.rows.zipWithIndex
        (c, ci) <- r.zipWithIndex
        x = ci * fieldSize
        y = ri * fieldSize
        _ = renderer.fillStyle = "#000000"
        _ = renderer.rect(x, y, fieldSize, fieldSize)
        colour = c match {
          case Hit     => ColourHit
          case Sunk    => ColourSunk
          case Unknown => ColourUnknown
          case Water   => ColourWater
        }
        _ = renderer.fillStyle = colour
        _ = renderer.fillRect(x, y, fieldSize, fieldSize)
      } yield ()
      if (b.isFinished) {
        renderer.fillStyle = "#ffffff"
        val fsize = boardWidth / 8
        renderer.font = s"${fsize}px sans-serif"
        renderer.textAlign = "center"
        renderer.textBaseline = "middle"
        renderer.fillText("Game finished!", canvas.width / 2, canvas.height / 2)
      }
      renderer.stroke()
    }
  }

  implicit val BoardPainer: Painter[Board] = new Painter[Board] {

    override def paint(b: Board)(fieldSize: Int)(canvas: Canvas): Unit = {
      val boardWidth  = (b.columns + 1) * fieldSize
      val boardHeight = (b.rows + 1) * fieldSize

      val renderer = canvas
        .getContext("2d")
        .asInstanceOf[CanvasRenderingContext2D]
      canvas.width = boardWidth
      canvas.height = boardHeight

      val hits = b.ships.flatMap(s => s.hits)

      renderer.fillStyle = ColourWater
      renderer.fillRect(0, 0, canvas.width, canvas.height)

      renderer.fillStyle = "#000000"
      renderer.lineWidth = 1
      for {
        row <- 0 to b.rows
        col <- 0 to b.columns
        x = col * fieldSize
        y = row * fieldSize
        p = Position(col, row)
        _ = if (hits.contains(p))
          renderer.fillStyle = ColourHit
        else
          renderer.fillStyle = ColourShip
        _ = if (Board.hit(b, Position(col, row)))
          renderer.fillRect(x, y, fieldSize, fieldSize)
        else
          renderer.rect(x, y, fieldSize, fieldSize)
      } yield ()
      renderer.stroke()
    }
  }

  object syntax {
    implicit final class WrapBoardOps(val b: Board) extends AnyVal {
      def paint(fieldSize: Int)(canvas: Canvas)(implicit ev: Painter[Board]): Unit =
        ev.paint(b)(fieldSize)(canvas)
    }

    implicit final class WrapEnemyBoardOps(val b: EnemyBoard) extends AnyVal {
      def paint(fieldSize: Int)(canvas: Canvas)(implicit ev: Painter[EnemyBoard]): Unit =
        ev.paint(b)(fieldSize)(canvas)
    }
  }
}
