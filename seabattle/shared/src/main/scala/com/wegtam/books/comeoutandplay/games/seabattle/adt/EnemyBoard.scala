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

package com.wegtam.books.comeoutandplay.games.seabattle.adt

import cats.instances.int._
import cats.syntax.eq._
import io.circe._
import io.circe.syntax._

import scala.collection.immutable.Seq

sealed trait EnemyBoardFieldState extends Product with Serializable

object EnemyBoardFieldState {

  implicit val decode: Decoder[EnemyBoardFieldState] = (c: HCursor) =>
    c.as[String].map {
      case "H" => Hit
      case "S" => Sunk
      case "U" => Unknown
      case "W" => Water
  }

  implicit val encode: Encoder[EnemyBoardFieldState] = {
    case Hit     => "H".asJson
    case Sunk    => "S".asJson
    case Unknown => "U".asJson
    case Water   => "W".asJson
  }

  /**
    * The field marks a hit on a ship which is not yet sunk.
    */
  case object Hit extends EnemyBoardFieldState

  /**
    * The field marks a part of a sunken ship.
    */
  case object Sunk extends EnemyBoardFieldState

  /**
    * The state of the field is unknown meaning that it has not been hit.
    */
  case object Unknown extends EnemyBoardFieldState

  /**
    * The field is water.
    */
  case object Water extends EnemyBoardFieldState

}

final case class EnemyBoard(rows: Seq[Seq[EnemyBoardFieldState]]) {
  def isFinished: Boolean                                    = EnemyBoard.isFinished(this)
  val update: Position => EnemyBoardFieldState => EnemyBoard = EnemyBoard.update(this)
  val updateFromState: Board => Position => EnemyBoard       = EnemyBoard.updateFromState(this)
}

object EnemyBoard {

  implicit val decode: Decoder[EnemyBoard] = Decoder.forProduct1("rows")(EnemyBoard.apply)

  implicit val encode: Encoder[EnemyBoard] = Encoder.forProduct1("rows")(_.rows)

  /**
    * Initialise a fresh enemy board from a given board.
    *
    * @param b A board which defines how many rows and columns the enemy board will have.
    * @return An enemy board with all fields set to `Unknown`.
    */
  def initialiseFromBoard(b: Board): EnemyBoard = {
    val rows = Seq.fill(b.rows + 1)(Seq.fill(b.columns + 1)(EnemyBoardFieldState.Unknown))
    EnemyBoard(rows)
  }

  /**
    * Return `true` if the given enemy board is finished e.g. no field has the `Unknown` state.
    *
    * @param eb The enemy board.
    * @return Returns `true` if no field is in the `Unknown` state.
    */
  def isFinished(eb: EnemyBoard): Boolean =
    eb.rows.forall(cs => !cs.contains(EnemyBoardFieldState.Unknown))

  /**
    * Update the given enemy board at the provided position.
    *
    * @param eb The enemy board.
    * @param p  A position on the board.
    * @param s  The new state for the position.
    * @return The updated enemy board.
    */
  def update(eb: EnemyBoard)(p: Position)(s: EnemyBoardFieldState): EnemyBoard = {
    val rows = eb.rows.zipWithIndex.map { a =>
      val (row, ridx) = a
      row.zipWithIndex.map { b =>
        val (col, cidx) = b
        if (p.row === ridx && p.column === cidx)
          s
        else
          col
      }
    }
    eb.copy(rows = rows)
  }

  /**
    * Aktualisiert die Spielbrettdarstellung für den Gegner anhand der
    * übergebenen Daten.
    *
    * @param eb Die aktuelle Darstellung.
    * @param b  Das Spielbrett.
    * @param p  Die Position des letzten Zuges.
    * @return Eine aktualisierte Spielbrettdarstellung.
    */
  def updateFromState(eb: EnemyBoard)(b: Board)(p: Position): EnemyBoard = {
    val s =
      if (Board.hit(b, p))
        EnemyBoardFieldState.Hit
      else
        EnemyBoardFieldState.Water
    update(eb)(p)(s)
  }

}
