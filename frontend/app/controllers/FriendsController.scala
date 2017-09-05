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

package controllers

import javax.inject.Inject

import com.mohiva.play.silhouette.api.Silhouette
import models.services.UserService
import play.api.i18n.I18nSupport
import play.api.mvc._
import utils.auth.DefaultEnv

/**
  * Administer the friends
  *
  * @param silhouette             The Silhouette stack.
  * @param userService            The user service implementation.
  */
class FriendsController @Inject()(components: ControllerComponents,
                                  silhouette: Silhouette[DefaultEnv],
                                  userService: UserService,
                                  implicit val webJarsUtil: org.webjars.play.WebJarsUtil,
                                  implicit val assets: AssetsFinder)
    extends AbstractController(components)
    with I18nSupport {

  /**
    * Show the initial page for the friends administration.
    * @return Result
    */
  def index: Action[AnyContent] = silhouette.SecuredAction.apply { implicit request =>
    Ok(views.html.friends.index(request.identity))
  }

}
