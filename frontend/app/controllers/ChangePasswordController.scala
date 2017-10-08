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
import com.mohiva.play.silhouette.api.util.{ Credentials, PasswordHasherRegistry, PasswordInfo }
import com.mohiva.play.silhouette.impl.providers.CredentialsProvider
import forms.ChangePasswordForm
import models.services.UserService
import play.api.i18n.{ I18nSupport, Messages }
import play.api.mvc._
import utils.auth.{ DefaultEnv, WithProvider }

import scala.concurrent.{ ExecutionContext, Future }

/**
  * The `Change Password` controller.
  *
  * @param silhouette             The Silhouette stack.
  * @param userService            The user service implementation.
  * @param credentialsProvider    The credentials provider.
  * @param authInfoRepository     The auth info repository.
  * @param passwordHasherRegistry The password hasher registry.
  */
class ChangePasswordController @Inject()(components: ControllerComponents,
                                         silhouette: Silhouette[DefaultEnv],
                                         userService: UserService,
                                         credentialsProvider: CredentialsProvider,
                                         authInfoRepository: AuthInfoRepository,
                                         passwordHasherRegistry: PasswordHasherRegistry,
                                         implicit val ec: ExecutionContext,
                                         implicit val webJarsUtil: org.webjars.play.WebJarsUtil,
                                         implicit val assets: AssetsFinder)
    extends AbstractController(components)
    with I18nSupport {

  /**
    * Views the `Change Password` page.
    *
    * @return The result to display.
    */
  def view: Action[AnyContent] =
    silhouette.SecuredAction(WithProvider[DefaultEnv#A](CredentialsProvider.ID)) {
      implicit request =>
        Ok(views.html.changePassword(ChangePasswordForm.form, request.identity))
    }

  /**
    * Changes the password.
    *
    * @return The result to display.
    */
  def submit: Action[AnyContent] =
    silhouette.SecuredAction(WithProvider[DefaultEnv#A](CredentialsProvider.ID)).async {
      implicit request =>
        ChangePasswordForm.form.bindFromRequest.fold(
          form => Future.successful(BadRequest(views.html.changePassword(form, request.identity))),
          password => {
            val (currentPassword, newPassword) = password
            val credentials                    = Credentials(request.identity.email.getOrElse(""), currentPassword)
            credentialsProvider
              .authenticate(credentials)
              .flatMap { loginInfo =>
                val passwordInfo = passwordHasherRegistry.current.hash(newPassword)
                authInfoRepository.update[PasswordInfo](loginInfo, passwordInfo).map { _ =>
                  Redirect(routes.ChangePasswordController.view())
                    .flashing("success" -> Messages("password.changed"))
                }
              }
              .recover {
                case e: ProviderException =>
                  Redirect(routes.ChangePasswordController.view())
                    .flashing("error" -> Messages("current.password.invalid"))
              }
          }
        )
    }
}
