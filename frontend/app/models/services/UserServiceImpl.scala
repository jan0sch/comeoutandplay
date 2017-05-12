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

import java.time.ZonedDateTime
import java.util.UUID
import javax.inject.Inject

import com.mohiva.play.silhouette.api.LoginInfo
import com.mohiva.play.silhouette.api.util.PasswordInfo
import com.mohiva.play.silhouette.impl.providers.{ CommonSocialProfile, OAuth1Info, OAuth2Info }
import models.User
import models.daos.UserDAO
import play.api.libs.concurrent.Execution.Implicits._

import scala.concurrent.Future

/**
  * Handles actions to users.
  *
  * @param userDAO The user DAO implementation.
  */
class UserServiceImpl @Inject()(userDAO: UserDAO) extends UserService {

  /**
    * Retrieves a user that matches the specified ID.
    *
    * @param id The ID to retrieve a user.
    * @return The retrieved user or None if no user could be retrieved for the given ID.
    */
  def retrieve(id: UUID): Future[Option[User]] = userDAO.find(id)

  /**
    * Retrieves a user that matches the specified login info.
    *
    * @param loginInfo The login info to retrieve a user.
    * @return The retrieved user or None if no user could be retrieved for the given login info.
    */
  def retrieve(loginInfo: LoginInfo): Future[Option[User]] = userDAO.find(loginInfo)

  /**
    * Saves a user.
    *
    * @param user The user to save.
    * @return The saved user.
    */
  def save(user: User): Future[User] = userDAO.save(user)

  /**
    * Saves the social profile for a user.
    *
    * If a user exists for this profile then update the user, otherwise create a new user with the given profile.
    *
    * @param profile The social profile to save.
    * @return The user for whom the profile was saved.
    */
  def save(profile: CommonSocialProfile): Future[User] =
    userDAO.find(profile.loginInfo).flatMap {
      case Some(user) => // Update user with profile
        userDAO
          .update(
            user.copy(
              username = UserServiceImpl.getSocialUsername(profile),
              firstName = profile.firstName,
              lastName = profile.lastName,
              email = profile.email,
              avatarUrl = profile.avatarURL
            )
          )
          .map { result =>
            user
          }
      case None => // Insert a new user
        userDAO.save(
          User(
            userID = UUID.randomUUID(),
            loginInfo = profile.loginInfo,
            email = profile.email,
            username = UserServiceImpl.getSocialUsername(profile),
            firstName = profile.firstName,
            lastName = profile.lastName,
            passwordInfo = PasswordInfo("", "", None),
            oauth1Info = OAuth1Info("", ""),
            oauth2Info = OAuth2Info("", Option(""), Option(0), Option(""), None),
            avatarUrl = profile.avatarURL,
            activated = true,
            active = true,
            created = Some(ZonedDateTime.now),
            updated = Some(ZonedDateTime.now)
          )
        )
    }

  /**
    * Update the given user.
    *
    * @param user The user to update.
    * @return If <0, the update was not successful.
    */
  def update(user: User): Future[Int] = userDAO.update(user)
}

object UserServiceImpl {

  /**
    * Determine a username for the user from the social profile.
    * The username is created depending on the fields that have be submitted
    * from the social provider.
    *
    * @param profile The social account profile.
    * @return String of the username.
    */
  def getSocialUsername(profile: CommonSocialProfile): String =
    profile.fullName.fold {
      profile.email.fold {
        "" // FIXME: Logic for automatic creation of unique username
      } { email =>
        email
      }
    } { fullname =>
      fullname
    }

}
