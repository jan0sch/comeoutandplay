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
import play.api.i18n.{ I18nSupport, Messages }
import play.api.mvc._
import utils.auth.DefaultEnv

import scala.concurrent.{ ExecutionContext, Future }

/**
  * Controller for the removal of an account from the system.
  *
  * @param silhouette The Silhouette stack.
  * @param socialProviderRegistry The social provider registry.
  * @param userDAOImpl Implementation of the user DAO functionalities.
  */
class DestroyAccountController @Inject()(components: ControllerComponents,
                                         silhouette: Silhouette[DefaultEnv],
                                         socialProviderRegistry: SocialProviderRegistry,
                                         userDAOImpl: UserDAOImpl,
                                         implicit val ec: ExecutionContext,
                                         implicit val webJarsUtil: org.webjars.play.WebJarsUtil,
                                         implicit val assets: AssetsFinder)
    extends AbstractController(components)
    with I18nSupport {

  /**
    * Intermediate page to destroy the account of the user.
    *
    * @return Action result
    */
  def destroyAccount: Action[AnyContent] = silhouette.SecuredAction.async { implicit request =>
    Future.successful(Ok(views.html.destroyAccount()))
  }

  /**
    * Handles the removal of an account action.
    *
    * @return The result to display.
    */
  def destroy: Action[AnyContent] = silhouette.SecuredAction.async { implicit request =>
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
