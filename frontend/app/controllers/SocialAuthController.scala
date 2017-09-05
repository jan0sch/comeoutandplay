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

import com.mohiva.play.silhouette.api._
import com.mohiva.play.silhouette.api.exceptions.ProviderException
import com.mohiva.play.silhouette.api.repositories.AuthInfoRepository
import com.mohiva.play.silhouette.impl.providers._
import models.services.UserService
import play.api.i18n.{ I18nSupport, Messages }
import play.api.mvc._
import utils.auth.DefaultEnv

import scala.concurrent.{ ExecutionContext, Future }

/**
  * The social auth controller.
  *
  * @param silhouette The Silhouette stack.
  * @param userService The user service implementation.
  * @param authInfoRepository The auth info service implementation.
  * @param socialProviderRegistry The social provider registry.
  */
class SocialAuthController @Inject()(components: ControllerComponents,
                                     silhouette: Silhouette[DefaultEnv],
                                     userService: UserService,
                                     authInfoRepository: AuthInfoRepository,
                                     socialProviderRegistry: SocialProviderRegistry,
                                     implicit val ec: ExecutionContext,
                                     implicit val webJarsUtil: org.webjars.play.WebJarsUtil,
                                     implicit val assets: AssetsFinder)
    extends AbstractController(components)
    with I18nSupport
    with Logger {

  /**
    * Authenticates a user against a social provider.
    *
    * @param provider The ID of the provider to authenticate against.
    * @return The result to display.
    */
  def authenticate(provider: String): Action[AnyContent] = Action.async { implicit request =>
    (socialProviderRegistry.get[SocialProvider](provider) match {
      case Some(p: SocialProvider with CommonSocialProfileBuilder) =>
        p.authenticate().flatMap {
          case Left(result) => Future.successful(result)
          case Right(authInfo) =>
            for {
              profile       <- p.retrieveProfile(authInfo)
              user          <- userService.save(profile)
              _             <- authInfoRepository.save(profile.loginInfo, authInfo)
              authenticator <- silhouette.env.authenticatorService.create(profile.loginInfo)
              value         <- silhouette.env.authenticatorService.init(authenticator)
              result <- silhouette.env.authenticatorService
                .embed(value, Redirect(routes.ApplicationController.index()))
            } yield {
              silhouette.env.eventBus.publish(LoginEvent(user, request))
              result
            }
        }
      case _ =>
        Future.failed(
          new ProviderException(s"Cannot authenticate with unexpected social provider $provider")
        )
    }).recover {
      case e: ProviderException =>
        logger.error("Unexpected provider error", e)
        Redirect(routes.SignInController.view())
          .flashing("error" -> Messages("could.not.authenticate"))
    }
  }
}
