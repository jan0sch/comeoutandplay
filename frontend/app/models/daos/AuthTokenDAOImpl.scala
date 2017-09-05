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

package models.daos

import java.util.UUID

import models.AuthToken
import models.daos.AuthTokenDAOImpl._
import org.joda.time.DateTime

import scala.collection.mutable
import scala.concurrent.Future

/**
  * Give access to the [[AuthToken]] object.
  */
class AuthTokenDAOImpl extends AuthTokenDAO {

  /**
    * Finds a token by its ID.
    *
    * @param id The unique token ID.
    * @return The found token or None if no token for the given ID could be found.
    */
  def find(id: UUID): Future[Option[AuthToken]] = Future.successful(tokens.get(id))

  /**
    * Finds expired tokens.
    *
    * @param dateTime The current date time.
    */
  def findExpired(dateTime: DateTime): Future[Seq[AuthToken]] = Future.successful {
    tokens
      .filter {
        case (_, token) =>
          token.expiry.isBefore(dateTime)
      }
      .values
      .toSeq
  }

  /**
    * Saves a token.
    *
    * @param token The token to save.
    * @return The saved token.
    */
  def save(token: AuthToken): Future[AuthToken] = {
    val _ = tokens += (token.id -> token)
    Future.successful(token)
  }

  /**
    * Removes the token for the given ID.
    *
    * @param id The ID for which the token should be removed.
    * @return A future to wait for the process to be completed.
    */
  def remove(id: UUID): Future[Unit] = {
    val _ = tokens -= id
    Future.successful(())
  }
}

/**
  * The companion object.
  */
object AuthTokenDAOImpl {

  /**
    * The list of tokens.
    */
  val tokens: mutable.HashMap[UUID, AuthToken] = mutable.HashMap()
}
