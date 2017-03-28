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
import play.api.i18n.I18nSupport
import play.api.mvc._
import play.api.routing._
import utils.auth.DefaultEnv

import scala.concurrent.Future

/**
  * The basic application controller.
  *
  * @param silhouette The Silhouette stack.
  * @param socialProviderRegistry The social provider registry.
  */
class ApplicationController @Inject()(components: ControllerComponents,
                                      silhouette: Silhouette[DefaultEnv])(
    socialProviderRegistry: SocialProviderRegistry,
    implicit val webJarsUtil: org.webjars.play.WebJarsUtil,
    implicit val assets: AssetsFinder
) extends AbstractController(components)
    with I18nSupport {

  /**
    * Handles the index action.
    *
    * @return The result to display.
    */
  def index: Action[AnyContent] = silhouette.SecuredAction.async { implicit request =>
    Future.successful(Ok(views.html.home(request.identity)))
  }

  /**
    * Handles the Sign Out action.
    *
    * @return The result to display.
    */
  def signOut: Action[AnyContent] = silhouette.SecuredAction.async { implicit request =>
    val result = Redirect(routes.ApplicationController.index())
    silhouette.env.eventBus.publish(LogoutEvent(request.identity, request))
    silhouette.env.authenticatorService.discard(request.authenticator, result)
  }

  /**
    * Generiert Routingdaten für die Nutzung seitens Javascript.
    *
    * @return Ein Javascript-Routing Objekt.
    */
  def javascriptRoutes: Action[AnyContent] = silhouette.SecuredAction.async { implicit request =>
    val jsRoutes = JavaScriptReverseRouter("jsRoutes")(routes.javascript.SeabattleController.socket)
    Future.successful(Ok(jsRoutes).as("text/javascript"))
  }
}
