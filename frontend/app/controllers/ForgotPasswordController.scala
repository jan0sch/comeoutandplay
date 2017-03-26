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
import com.mohiva.play.silhouette.impl.providers.CredentialsProvider
import forms.ForgotPasswordForm
import models.services.{ AuthTokenService, UserService }
import play.api.i18n.{ I18nSupport, Messages, MessagesApi }
import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.mailer.{ Email, MailerClient }
import play.api.mvc.Controller
import utils.auth.DefaultEnv

import scala.concurrent.Future

/**
  * The `Forgot Password` controller.
  *
  * @param messagesApi      The Play messages API.
  * @param silhouette       The Silhouette stack.
  * @param userService      The user service implementation.
  * @param authTokenService The auth token service implementation.
  * @param mailerClient     The mailer client.
  * @param webJarAssets     The WebJar assets locator.
  */
class ForgotPasswordController @Inject()(val messagesApi: MessagesApi,
                                         silhouette: Silhouette[DefaultEnv],
                                         userService: UserService,
                                         authTokenService: AuthTokenService,
                                         mailerClient: MailerClient,
                                         implicit val webJarAssets: WebJarAssets)
    extends Controller
    with I18nSupport {

  /**
    * Views the `Forgot Password` page.
    *
    * @return The result to display.
    */
  def view = silhouette.UnsecuredAction.async { implicit request =>
    Future.successful(Ok(views.html.forgotPassword(ForgotPasswordForm.form)))
  }

  /**
    * Sends an email with password reset instructions.
    *
    * It sends an email to the given address if it exists in the database. Otherwise we do not show the user
    * a notice for not existing email addresses to prevent the leak of existing email addresses.
    *
    * @return The result to display.
    */
  def submit = silhouette.UnsecuredAction.async { implicit request =>
    ForgotPasswordForm.form.bindFromRequest.fold(
      form => Future.successful(BadRequest(views.html.forgotPassword(form))),
      email => {
        val loginInfo = LoginInfo(CredentialsProvider.ID, email)
        val result =
          Redirect(routes.SignInController.view()).flashing("info" -> Messages("reset.email.sent"))
        userService.retrieve(loginInfo).flatMap {
          case Some(user) if user.email.isDefined =>
            authTokenService.create(user.userID).map {
              authToken =>
                val url = routes.ResetPasswordController.view(authToken.id).absoluteURL()

                mailerClient.send(
                  Email(
                    subject = Messages("email.reset.password.subject"),
                    from = Messages("email.from"),
                    to = Seq(email),
                    bodyText = Some(views.txt.emails.resetPassword(user, url).body),
                    bodyHtml = Some(views.html.emails.resetPassword(user, url).body)
                  )
                )
                result
            }
          case None => Future.successful(result)
        }
      }
    )
  }
}
