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

package models

import java.time.ZonedDateTime
import java.util.UUID

import com.mohiva.play.silhouette.api.util.PasswordInfo
import com.mohiva.play.silhouette.api.{ Identity, LoginInfo }
import com.mohiva.play.silhouette.impl.providers.{ OAuth1Info, OAuth2Info }

/**
  * The user object.
  *
  * @param userID       The unique ID of the user.
  * @param loginInfo    Login information from silhouette to identify the user.
  * @param email        The E-Mail address of the user.
  * @param firstName    The first name of the user.
  * @param lastName     The last name of the user.
  * @param passwordInfo Password information of the user.
  * @param oauth1Info   OAuth1 information when the user was authenticated with this method.
  * @param oauth2Info   OAuth2 information when the user was authenticated with this method.
  * @param avatarUrl    The Url of the user avatar.
  * @param activated    Whether the account was activated.
  * @param active       Whether the account is active.
  * @param created      When the account was created.
  * @param updated      When the account was updated.
  */
case class User(userID: UUID,
                loginInfo: LoginInfo,
                email: Option[String],
                firstName: Option[String],
                lastName: Option[String],
                passwordInfo: PasswordInfo,
                oauth1Info: OAuth1Info,
                oauth2Info: OAuth2Info,
                avatarUrl: Option[String],
                activated: Boolean,
                active: Boolean,
                created: Option[ZonedDateTime],
                updated: Option[ZonedDateTime],
                admin: Boolean = false,
                moderator: Boolean = false)
    extends Identity {

  /**
    * Construct a full name of the user.
    *
    * @return The full name of the user.
    */
  def fullName(): Option[String] =
    firstName -> lastName match {
      case (Some(f), Some(l)) => Some(f + " " + l)
      case (Some(f), None)    => Some(f)
      case (None, Some(l))    => Some(l)
      case _                  => None
    }

}
