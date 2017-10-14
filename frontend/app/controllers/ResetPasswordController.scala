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

import java.util.UUID
import javax.inject.Inject

import com.mohiva.play.silhouette.api._
import com.mohiva.play.silhouette.api.repositories.AuthInfoRepository
import com.mohiva.play.silhouette.api.util.{ PasswordHasherRegistry, PasswordInfo }
import com.mohiva.play.silhouette.impl.providers.CredentialsProvider
import forms.ResetPasswordForm
import models.services.{ AuthTokenService, UserService }
import play.api.i18n.{ I18nSupport, Messages }
import play.api.mvc._
import utils.auth.DefaultEnv

import scala.concurrent.{ ExecutionContext, Future }

/**
  * The `Reset Password` controller.
  *
  * @param silhouette             The Silhouette stack.
  * @param userService            The user service implementation.
  * @param authInfoRepository     The auth info repository.
  * @param passwordHasherRegistry The password hasher registry.
  * @param authTokenService       The auth token service implementation.
  */
class ResetPasswordController @Inject()(components: ControllerComponents,
                                        silhouette: Silhouette[DefaultEnv],
                                        userService: UserService,
                                        authInfoRepository: AuthInfoRepository,
                                        passwordHasherRegistry: PasswordHasherRegistry,
                                        authTokenService: AuthTokenService,
                                        implicit val ec: ExecutionContext,
                                        implicit val webJarsUtil: org.webjars.play.WebJarsUtil,
                                        implicit val assets: AssetsFinder)
    extends AbstractController(components)
    with I18nSupport {

  /**
    * Views the `Reset Password` page.
    *
    * @param token The token to identify a user.
    * @return The result to display.
    */
  def view(token: UUID): Action[AnyContent] = silhouette.UnsecuredAction.async {
    implicit request =>
      authTokenService.validate(token).map {
        case Some(_) => Ok(views.html.resetPassword(ResetPasswordForm.form, token))
        case None =>
          Redirect(routes.SignInController.view())
            .flashing("error" -> Messages("invalid.reset.link"))
      }
  }

  /**
    * Resets the password.
    *
    * @param token The token to identify a user.
    * @return The result to display.
    */
  def submit(token: UUID): Action[AnyContent] = silhouette.UnsecuredAction.async {
    implicit request =>
      authTokenService.validate(token).flatMap {
        case Some(authToken) =>
          ResetPasswordForm.form.bindFromRequest.fold(
            form => Future.successful(BadRequest(views.html.resetPassword(form, token))),
            password =>
              userService.retrieve(authToken.userID).flatMap {
                case Some(user) if user.loginInfo.providerID == CredentialsProvider.ID =>
                  val passwordInfo = passwordHasherRegistry.current.hash(password)
                  authInfoRepository.update[PasswordInfo](user.loginInfo, passwordInfo).map { _ =>
                    Redirect(routes.SignInController.view())
                      .flashing("success" -> Messages("password.reset"))
                  }
                case _ =>
                  Future.successful(
                    Redirect(routes.SignInController.view())
                      .flashing("error" -> Messages("invalid.reset.link"))
                  )
            }
          )
        case None =>
          Future.successful(
            Redirect(routes.SignInController.view())
              .flashing("error" -> Messages("invalid.reset.link"))
          )
      }
  }
}
