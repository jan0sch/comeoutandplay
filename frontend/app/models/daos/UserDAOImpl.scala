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
import javax.inject.Inject

import com.mohiva.play.silhouette.api.LoginInfo
import com.mohiva.play.silhouette.api.util.PasswordInfo
import com.mohiva.play.silhouette.impl.providers.{ OAuth1Info, OAuth2Info }
import models.User
import play.api.Configuration
import play.api.db.slick.DatabaseConfigProvider

import scala.concurrent.{ ExecutionContext, Future }

/**
  * Give access to the user object.
  */
class UserDAOImpl @Inject()(override protected val configuration: Configuration,
                            override protected val dbConfigProvider: DatabaseConfigProvider)
    extends Tables(configuration, dbConfigProvider)
    with UserDAO {

  import driver.api._

  /**
    * Delete the account from the database.
    *
    * @param loginInfo LoginInfo of the user account.
    * @return Future of a number holding the affected rows.
    */
  def destroy(loginInfo: LoginInfo): Future[Int] =
    dbConfig.db.run(
      users
        .filter(
          u => u.provider_key === loginInfo.providerKey && u.provider_id === loginInfo.providerID
        )
        .delete
    )

  /**
    * Finds a user by its LoginInfo.
    *
    * @param loginInfo The login info of the user to find.
    * @return The found user or None if no user for the given login info could be found.
    */
  def find(loginInfo: LoginInfo): Future[Option[User]] =
    dbConfig.db.run(
      users
        .filter(
          u => u.provider_id === loginInfo.providerID && u.provider_key === loginInfo.providerKey
        )
        .result
        .headOption
    )

  /**
    * Finds a user by its user ID.
    *
    * @param userID The ID of the user to find.
    * @return The found user or None if no user for the given ID could be found.
    */
  def find(userID: UUID): Future[Option[User]] =
    dbConfig.db.run(users.filter(_.user_id === userID).result.headOption)

  /**
    * Return the users that contain the given query in their `username`.
    *
    * @param query The query string that should be part of the `username`.
    * @return Future of Seq of Users.
    */
  def findUsers(query: String): Future[Seq[User]] =
    dbConfig.db.run(users.filter(_.username.toLowerCase.like(s"%${query.toLowerCase}%")).result)

  /**
    * Return the OAuth1Info information for the user described for the
    * LoginInfo.
    *
    * @param loginInfo LoginInfo of the user account.
    * @return Optional OAuth1Info for the user account.
    */
  def getOAuth1Info(
      loginInfo: LoginInfo
  ): Future[Option[OAuth1Info]] =
    dbConfig.db.run(
      users
        .filter(
          u => u.provider_id === loginInfo.providerID && u.provider_key === loginInfo.providerKey
        )
        .map(_.oAuth1Info)
        .result
        .headOption
    )

  /**
    * Return the OAuth2Info information for the user described for the
    * LoginInfo.
    *
    * @param loginInfo  LoginInfo of the user account.
    * @return Optional OAuth2Info for the user account.
    */
  def getOAuth2Info(
      loginInfo: LoginInfo
  ): Future[Option[OAuth2Info]] =
    dbConfig.db.run(
      users
        .filter(
          u => u.provider_id === loginInfo.providerID && u.provider_key === loginInfo.providerKey
        )
        .map(_.oAuth2Info)
        .result
        .headOption
    )

  /**
    * Return the PasswordInfo for a user identified by the provided LoginInfo
    *
    * @param loginInfo  LoginInfo for the User account.
    * @return The associated PasswordInfo.
    */
  def getPasswordInfo(
      loginInfo: LoginInfo
  ): Future[Option[PasswordInfo]] =
    dbConfig.db.run(
      users
        .filter(
          u => u.provider_id === loginInfo.providerID && u.provider_key === loginInfo.providerKey
        )
        .map(_.passwordInfo)
        .result
        .headOption
    )

  /**
    * Saves a user.
    *
    * @param user The user to save.
    * @return The saved user.
    */
  def save(user: User): Future[User] =
    dbConfig.db.run(
      (users returning users.map(_.user_id) into ((user, id) => user.copy(userID = id))) += user
    )

  /**
    * Save an OAuth1Info for the user account described by the provided LoginInfo.
    *
    * @param loginInfo  The LoginInfo of the user account.
    * @param auth1Info  The new OAuth1Info.
    * @return Future of the stored OAuth1Info.
    */
  def saveOAuth1Info(loginInfo: LoginInfo,
                     auth1Info: OAuth1Info)(implicit ec: ExecutionContext): Future[OAuth1Info] =
    for {
      user <- find(loginInfo)
      result <- {
        user.fold(Future.successful(auth1Info))(
          u => update(u.copy(oauth1Info = auth1Info)).map(r => auth1Info)
        )
      }
    } yield {
      result
    }

  /**
    * Save an OAuth2Info for the user account described by the provided LoginInfo.
    *
    * @param loginInfo  The LoginInfo of the user account.
    * @param auth2Info  The new OAuth2Info.
    * @return Future of the stored OAuth2Info.
    */
  def saveOAuth2Info(loginInfo: LoginInfo,
                     auth2Info: OAuth2Info)(implicit ec: ExecutionContext): Future[OAuth2Info] =
    for {
      user <- find(loginInfo)
      result <- {
        user.fold(Future.successful(auth2Info))(
          u => update(u.copy(oauth2Info = auth2Info)).map(r => auth2Info)
        )
      }
    } yield {
      result
    }

  /**
    * Save a PasswordInfo for the user account described by the provided LoginInfo.
    *
    * @param loginInfo  The LoginInfo of the user account.
    * @param authInfo   The new PasswordInfo.
    * @return Future of the stored PasswordInfo.
    */
  def savePasswordInfo(loginInfo: LoginInfo, authInfo: PasswordInfo)(
      implicit ec: ExecutionContext
  ): Future[PasswordInfo] =
    for {
      user <- find(loginInfo)
      result <- {
        user.fold(Future.successful(authInfo))(
          u => update(u.copy(passwordInfo = authInfo)).map(r => authInfo)
        )
      }
    } yield {
      result
    }

  /**
    * Update the given user.
    *
    * @param user The user to update.
    * @return An option to the updated user.
    */
  def update(user: User): Future[Int] =
    dbConfig.db.run(users.filter(_.user_id === user.userID).update(user))
}
