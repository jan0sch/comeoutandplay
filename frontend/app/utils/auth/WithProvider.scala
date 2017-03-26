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

package utils.auth

import com.mohiva.play.silhouette.api.{ Authenticator, Authorization }
import models.User
import play.api.mvc.Request

import scala.concurrent.Future

/**
  * Grants only access if a user has authenticated with the given provider.
  *
  * @param provider The provider ID the user must authenticated with.
  * @tparam A The type of the authenticator.
  */
case class WithProvider[A <: Authenticator](provider: String) extends Authorization[User, A] {

  /**
    * Indicates if a user is authorized to access an action.
    *
    * @param user The usr object.
    * @param authenticator The authenticator instance.
    * @param request The current request.
    * @tparam B The type of the request body.
    * @return True if the user is authorized, false otherwise.
    */
  override def isAuthorized[B](user: User,
                               authenticator: A)(implicit request: Request[B]): Future[Boolean] =
    Future.successful(user.loginInfo.providerID == provider)
}
