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

package models.services

import java.util.UUID
import javax.inject.Inject

import com.mohiva.play.silhouette.api.util.Clock
import models.AuthToken
import models.daos.AuthTokenDAO
import org.joda.time.DateTimeZone

import scala.concurrent.duration._
import scala.concurrent.{ ExecutionContext, Future }
import scala.language.postfixOps

/**
  * Handles actions to auth tokens.
  *
  * @param authTokenDAO The auth token DAO implementation.
  * @param clock The clock instance.
  */
class AuthTokenServiceImpl @Inject()(authTokenDAO: AuthTokenDAO,
                                     clock: Clock,
                                     implicit val ec: ExecutionContext)
    extends AuthTokenService {

  /**
    * Creates a new auth token and saves it in the backing store.
    *
    * @param userID The user ID for which the token should be created.
    * @param expiry The duration a token expires.
    * @return The saved auth token.
    */
  @SuppressWarnings(Array("org.wartremover.warts.DefaultArguments"))
  def create(userID: UUID, expiry: FiniteDuration = 5 minutes): Future[AuthToken] = {
    val token = AuthToken(UUID.randomUUID(),
                          userID,
                          clock.now.withZone(DateTimeZone.UTC).plusSeconds(expiry.toSeconds.toInt))
    authTokenDAO.save(token)
  }

  /**
    * Validates a token ID.
    *
    * @param id The token ID to validate.
    * @return The token if it's valid, None otherwise.
    */
  def validate(id: UUID): Future[Option[AuthToken]] = authTokenDAO.find(id)

  /**
    * Cleans expired tokens.
    *
    * @return The list of deleted tokens.
    */
  def clean: Future[Seq[AuthToken]] =
    authTokenDAO.findExpired(clock.now.withZone(DateTimeZone.UTC)).flatMap { tokens =>
      Future.sequence(tokens.map { token =>
        authTokenDAO.remove(token.id).map(_ => token)
      })
    }
}
