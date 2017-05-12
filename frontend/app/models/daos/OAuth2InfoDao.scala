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

import javax.inject.Inject

import com.mohiva.play.silhouette.api.LoginInfo
import com.mohiva.play.silhouette.impl.providers.OAuth2Info
import com.mohiva.play.silhouette.persistence.daos.DelegableAuthInfoDAO

import scala.concurrent.Future

/**
  * Data access object for the access of the OAuth2Info that is
  * stored to a user account, if the account was registered
  * through a service that requested OAuth2.
  *
  * @param userDAOImpl Injected implementation of the UserDAO
  */
class OAuth2InfoDao @Inject()(userDAOImpl: UserDAOImpl) extends DelegableAuthInfoDAO[OAuth2Info] {

  /**
    * Find an OAuth2Info for the associated user account that is
    * identified by the provided LoginInfo.
    *
    * @param loginInfo  The LoginInfo of the user account.
    * @return Future of an Option of an OAuth2Info object.
    */
  def find(loginInfo: LoginInfo): Future[Option[OAuth2Info]] =
    userDAOImpl.getOAuth2Info(loginInfo)

  /**
    * Add a new OAuth2Info to the user account that is
    * identified by the provided LoginInfo.
    *
    * @param loginInfo  LoginInfo of the user account.
    * @param authInfo   The OAuth2Info that should be addded.
    * @return Future of the added OAuth2Info.
    */
  def add(loginInfo: LoginInfo, authInfo: OAuth2Info): Future[OAuth2Info] = {
    implicit val ec = play.api.libs.concurrent.Execution.Implicits.defaultContext
    userDAOImpl.saveOAuth2Info(loginInfo, authInfo)
  }

  /**
    * Update an OAuth2Info to the user account that is
    * identified by the provided LoginInfo.
    *
    * @param loginInfo  LoginInfo of the user account.
    * @param authInfo   The OAuth2Info that should be updated.
    * @return Future of the updated OAuth2Info.
    */
  def update(loginInfo: LoginInfo, authInfo: OAuth2Info): Future[OAuth2Info] = {
    implicit val ec = play.api.libs.concurrent.Execution.Implicits.defaultContext
    userDAOImpl.saveOAuth2Info(loginInfo, authInfo)
  }

  /**
    * Save an OAuth2Info to the user account that is
    * identified by the provided LoginInfo.
    *
    * @param loginInfo  LoginInfo of the user account.
    * @param authInfo   The OAuth2Info that should be saved.
    * @return Future of the saved OAuth2Info.
    */
  def save(loginInfo: LoginInfo, authInfo: OAuth2Info): Future[OAuth2Info] = {
    implicit val ec = play.api.libs.concurrent.Execution.Implicits.defaultContext
    userDAOImpl.saveOAuth2Info(loginInfo, authInfo)
  }

  def remove(loginInfo: LoginInfo): Future[Unit] = ???
}
