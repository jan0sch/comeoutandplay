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

import java.net.URLDecoder
import java.util.UUID
import javax.inject.Inject

import cats.instances.string._
import cats.syntax.eq._
import com.mohiva.play.silhouette.api._
import com.mohiva.play.silhouette.impl.providers.CredentialsProvider
import models.services.{ AuthTokenService, UserService }
import play.api.i18n.{ I18nSupport, Messages }
import play.api.libs.mailer.{ Email, MailerClient }
import play.api.mvc._
import utils.auth.DefaultEnv

import scala.concurrent.{ ExecutionContext, Future }

/**
  * The `Activate Account` controller.
  *
  * @param silhouette       The Silhouette stack.
  * @param userService      The user service implementation.
  * @param authTokenService The auth token service implementation.
  * @param mailerClient     The mailer client.
  */
class ActivateAccountController @Inject()(components: ControllerComponents,
                                          silhouette: Silhouette[DefaultEnv],
                                          userService: UserService,
                                          authTokenService: AuthTokenService,
                                          mailerClient: MailerClient,
                                          implicit val ec: ExecutionContext,
                                          implicit val webJarsUtil: org.webjars.play.WebJarsUtil,
                                          implicit val assets: AssetsFinder)
    extends AbstractController(components)
    with I18nSupport {

  /**
    * Sends an account activation email to the user with the given email.
    *
    * @param email The email address of the user to send the activation mail to.
    * @return The result to display.
    */
  def send(email: String): Action[AnyContent] = silhouette.UnsecuredAction.async {
    implicit request =>
      val decodedEmail = URLDecoder.decode(email, "UTF-8")
      val loginInfo    = LoginInfo(CredentialsProvider.ID, decodedEmail)
      val result = Redirect(routes.SignInController.view())
        .flashing("info" -> Messages("activation.email.sent", decodedEmail))

      userService.retrieve(loginInfo).flatMap {
        case Some(user) if !user.activated =>
          authTokenService.create(user.userID).map { authToken =>
            val url = routes.ActivateAccountController.activate(authToken.id).absoluteURL()

            val _ = mailerClient.send(
              Email(
                subject = Messages("email.activate.account.subject"),
                from = Messages("email.from"),
                to = Seq(decodedEmail),
                bodyText = Some(views.txt.emails.activateAccount(user, url).body),
                bodyHtml = Some(views.html.emails.activateAccount(user, url).body)
              )
            )
            result
          }
        case None => Future.successful(result)
      }
  }

  /**
    * Activates an account.
    *
    * @param token The token to identify a user.
    * @return The result to display.
    */
  def activate(token: UUID): Action[AnyContent] = silhouette.UnsecuredAction.async {
    implicit request =>
      authTokenService.validate(token).flatMap {
        case Some(authToken) =>
          userService.retrieve(authToken.userID).flatMap {
            case Some(user) if user.loginInfo.providerID === CredentialsProvider.ID =>
              userService.update(user.copy(activated = true)).map { _ =>
                Redirect(routes.SignInController.view())
                  .flashing("success" -> Messages("account.activated"))
              }
            case _ =>
              Future.successful(
                Redirect(routes.SignInController.view())
                  .flashing("error" -> Messages("invalid.activation.link"))
              )
          }
        case None =>
          Future.successful(
            Redirect(routes.SignInController.view())
              .flashing("error" -> Messages("invalid.activation.link"))
          )
      }
  }
}
