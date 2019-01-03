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
import akka.actor.ActorSystem
import akka.stream.Materializer
import com.mohiva.play.silhouette.api.Silhouette
import javax.inject.Inject
import models.daos.SeabattleDAO
import play.api.i18n.I18nSupport
import play.api.mvc.{ AbstractController, Action, AnyContent, ControllerComponents }
import utils.auth.DefaultEnv

import scala.concurrent.Future

class GamesController @Inject()(components: ControllerComponents)(
    implicit system: ActorSystem,
    silhouette: Silhouette[DefaultEnv],
    materializer: Materializer,
    implicit val webJarsUtil: org.webjars.play.WebJarsUtil,
    implicit val assets: AssetsFinder
) extends AbstractController(components)
    with I18nSupport {

  /**
    * Startseite für die Spielseite.
    *
    * @return Eine HTML-Seite mit diversen Links.
    */
  def index: Action[AnyContent] = silhouette.SecuredAction.async { implicit request =>
    val user = request.identity
    Future.successful(Ok(views.html.games.index(user)))
  }

}
