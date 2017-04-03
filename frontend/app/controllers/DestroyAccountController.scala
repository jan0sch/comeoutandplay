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

import com.mohiva.play.silhouette.api.{ LogoutEvent, Silhouette }
import com.mohiva.play.silhouette.impl.providers.SocialProviderRegistry
import models.daos.UserDAOImpl
import play.api.i18n.{ I18nSupport, Messages, MessagesApi }
import play.api.mvc.Controller
import utils.auth.DefaultEnv
import play.api.libs.concurrent.Execution.Implicits._

import scala.concurrent.Future

/**
  * Controller for the removal of an account from the system.
  *
  * @param messagesApi The Play messages API.
  * @param silhouette The Silhouette stack.
  * @param socialProviderRegistry The social provider registry.
  * @param userDAOImpl Implementation of the user DAO functionalities.
  * @param webJarAssets The webjar assets implementation.
  */
class DestroyAccountController @Inject()(val messagesApi: MessagesApi,
                                         silhouette: Silhouette[DefaultEnv],
                                         socialProviderRegistry: SocialProviderRegistry,
                                         userDAOImpl: UserDAOImpl,
                                         implicit val webJarAssets: WebJarAssets)
    extends Controller
    with I18nSupport {

  /**
    * Intermediate page to destroy the account of the user.
    *
    * @return Action result
    */
  def destroyAccount = silhouette.SecuredAction.async { implicit request =>
    Future.successful(Ok(views.html.destroyAccount()))
  }

  /**
    * Handles the removal of an account action.
    *
    * @return The result to display.
    */
  def destroy = silhouette.SecuredAction.async { implicit request =>
    userDAOImpl.destroy(request.identity.loginInfo).flatMap { result =>
      if (result < 1) {
        Future.successful(
          Redirect(routes.ApplicationController.index())
            .flashing("error" -> Messages("destroy.account.error"))
        )
      } else {
        val r = Redirect(routes.ApplicationController.index())
          .flashing("success" -> Messages("destroy.account.success"))
        silhouette.env.eventBus.publish(LogoutEvent(request.identity, request))
        silhouette.env.authenticatorService.discard(request.authenticator, r)
      }
    }

  }

}
