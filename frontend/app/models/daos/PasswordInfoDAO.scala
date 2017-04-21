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
import com.mohiva.play.silhouette.api.util.PasswordInfo
import com.mohiva.play.silhouette.persistence.daos.DelegableAuthInfoDAO

import scala.concurrent.Future

/**
  * Data Access Object for the access of the password information that is associated with
  * a registered user account.
  *
  * @param userDAOImpl Access to the user methods.
  */
class PasswordInfoDAO @Inject()(userDAOImpl: UserDAOImpl)
    extends DelegableAuthInfoDAO[PasswordInfo] {

  /**
    * Find a stored password information related to a given LoginInfo.
    *
    * @param loginInfo  Given LoginInfo.
    * @return Optional password info for the LoginInfo.
    */
  override def find(loginInfo: LoginInfo): Future[Option[PasswordInfo]] =
    userDAOImpl.getPasswordInfo(loginInfo)

  /**
    * Add a new password information to the related account described by
    * the provided LoginInfo.
    *
    * @param loginInfo  The LoginInfo of the associated account.
    * @param authInfo   The new password information for the associated account.
    * @return The updated PasswordInfo.
    */
  override def add(loginInfo: LoginInfo, authInfo: PasswordInfo): Future[PasswordInfo] =
    userDAOImpl.savePasswordInfo(loginInfo, authInfo)

  /**
    * Update new password information for the related account described by
    * the provided LoginInfo.
    *
    * @param loginInfo  The LoginInfo of the associated account.
    * @param authInfo   The new password information for the associated account.
    * @return The updated PasswordInfo.
    */
  override def update(loginInfo: LoginInfo, authInfo: PasswordInfo): Future[PasswordInfo] =
    userDAOImpl.savePasswordInfo(loginInfo, authInfo)

  /**
    * Save a password information to the related account described by
    * the provided LoginInfo.
    *
    * @param loginInfo  The LoginInfo of the associated account.
    * @param authInfo   The new password information for the associated account.
    * @return The updated PasswordInfo.
    */
  override def save(loginInfo: LoginInfo, authInfo: PasswordInfo): Future[PasswordInfo] =
    userDAOImpl.savePasswordInfo(loginInfo, authInfo)

  /**
    * Remove a password information of an account that is described by the
    * given LoginInfo.
    *
    * @param loginInfo  The LoginInfo of the account.
    * @return Unit
    */
  override def remove(loginInfo: LoginInfo): Future[Unit] = ???
}
