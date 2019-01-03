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

import java.io.{ PrintWriter, StringWriter }

import io.circe._

import scala.util.Try

/**
  * An error information class.
  *
  * @param summary A short summary or error message.
  * @param details A more detailed error message or stacktrace.
  */
final case class ErrorInfo(
    summary: String,
    details: String
) {
  override def toString: String = s"$summary: $details"
}

object ErrorInfo {

  /**
    * Create an error information with just a summary.
    *
    * @param summary A short summary or error message.
    * @return An error information.
    */
  def apply(summary: String): ErrorInfo = new ErrorInfo(summary = summary, details = "")

  /**
    * Create an error information with summary and details.
    *
    * @param summary A short summary or error message.
    * @param details A more detailed error message or stacktrace.
    * @return An error information.
    */
  def apply(summary: String, details: String): ErrorInfo =
    new ErrorInfo(summary = summary, details = details)

  /**
    * Create an error information from a `Throwable`.
    *
    * @param t A throwable (exception).
    * @return An error information.
    */
  def fromThrowable(t: Throwable): ErrorInfo = {
    val summary = Try(t.getMessage).toOption.getOrElse("")
    val details = Try {
      val sw = new StringWriter()
      val pw = new PrintWriter(sw)
      t.printStackTrace(pw)
      sw.toString
    }.toOption.getOrElse("")
    ErrorInfo(summary, details)
  }

  // Codec for decoding ErrorInfo from JSON.
  implicit val decodeErrorInfo: Decoder[ErrorInfo] =
    Decoder.forProduct2("summary", "details")(ErrorInfo.apply)

  // Codec for encoding ErrorInfo to JSON.
  implicit val encodeErrorInfo: Encoder[ErrorInfo] =
    Encoder.forProduct2("summary", "details")(e => (e.summary, e.details))

}
