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

import com.mohiva.play.silhouette.api.Authenticator.Implicits._
import com.mohiva.play.silhouette.api._
import com.mohiva.play.silhouette.api.exceptions.ProviderException
import com.mohiva.play.silhouette.api.repositories.AuthInfoRepository
import com.mohiva.play.silhouette.api.util.{ Clock, Credentials }
import com.mohiva.play.silhouette.impl.exceptions.IdentityNotFoundException
import com.mohiva.play.silhouette.impl.providers._
import forms.SignInForm
import models.services.UserService
import net.ceedubs.ficus.Ficus._
import play.api.Configuration
import play.api.i18n.{ I18nSupport, Messages }
import play.api.mvc._
import utils.auth.DefaultEnv

import scala.concurrent.duration._
import scala.concurrent.{ ExecutionContext, Future }

/**
  * The `Sign In` controller.
  *
  * @param silhouette The Silhouette stack.
  * @param userService The user service implementation.
  * @param authInfoRepository The auth info repository implementation.
  * @param credentialsProvider The credentials provider.
  * @param socialProviderRegistry The social provider registry.
  * @param configuration The Play configuration.
  * @param clock The clock instance.
  */
class SignInController @Inject()(components: ControllerComponents,
                                 silhouette: Silhouette[DefaultEnv],
                                 userService: UserService,
                                 authInfoRepository: AuthInfoRepository,
                                 credentialsProvider: CredentialsProvider,
                                 socialProviderRegistry: SocialProviderRegistry,
                                 configuration: Configuration,
                                 clock: Clock,
                                 implicit val ec: ExecutionContext,
                                 implicit val webJarsUtil: org.webjars.play.WebJarsUtil,
                                 implicit val assets: AssetsFinder)
    extends AbstractController(components)
    with I18nSupport {

  /**
    * Views the `Sign In` page.
    *
    * @return The result to display.
    */
  def view: Action[AnyContent] = silhouette.UnsecuredAction.async { implicit request =>
    Future.successful(Ok(views.html.signIn(SignInForm.form, socialProviderRegistry)))
  }

  /**
    * Handles the submitted form.
    *
    * @return The result to display.
    */
  def submit: Action[AnyContent] = silhouette.UnsecuredAction.async { implicit request =>
    SignInForm.form.bindFromRequest.fold(
      form => Future.successful(BadRequest(views.html.signIn(form, socialProviderRegistry))),
      data => {
        val credentials = Credentials(data.email, data.password)
        credentialsProvider
          .authenticate(credentials)
          .flatMap {
            loginInfo =>
              val result = Redirect(routes.ApplicationController.index())
              userService.retrieve(loginInfo).flatMap {
                case Some(user) if !user.activated =>
                  Future.successful(Ok(views.html.activateAccount(data.email)))
                case Some(user) =>
                  val c = configuration.underlying
                  silhouette.env.authenticatorService
                    .create(loginInfo)
                    .map {
                      case authenticator if data.rememberMe =>
                        authenticator.copy(
                          expirationDateTime = clock.now + c.as[FiniteDuration](
                            "silhouette.authenticator.rememberMe.authenticatorExpiry"
                          ),
                          idleTimeout = c.getAs[FiniteDuration](
                            "silhouette.authenticator.rememberMe.authenticatorIdleTimeout"
                          ),
                          cookieMaxAge = c.getAs[FiniteDuration](
                            "silhouette.authenticator.rememberMe.cookieMaxAge"
                          )
                        )
                      case authenticator => authenticator
                    }
                    .flatMap { authenticator =>
                      silhouette.env.eventBus.publish(LoginEvent(user, request))
                      silhouette.env.authenticatorService.init(authenticator).flatMap { v =>
                        silhouette.env.authenticatorService.embed(v, result)
                      }
                    }
                case None => Future.failed(new IdentityNotFoundException("Couldn't find user"))
              }
          }
          .recover {
            case e: ProviderException =>
              Redirect(routes.SignInController.view())
                .flashing("error" -> Messages("invalid.credentials"))
          }
      }
    )
  }
}
