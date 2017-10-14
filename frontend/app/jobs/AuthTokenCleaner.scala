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

package jobs

import javax.inject.Inject

import akka.actor._
import com.mohiva.play.silhouette.api.util.Clock
import jobs.AuthTokenCleaner.Clean
import models.services.AuthTokenService
import utils.Logger

import scala.concurrent.ExecutionContext.Implicits.global

/**
  * A job which cleanup invalid auth tokens.
  *
  * @param service The auth token service implementation.
  * @param clock The clock implementation.
  */
class AuthTokenCleaner @Inject()(service: AuthTokenService, clock: Clock)
    extends Actor
    with Logger {

  /**
    * Process the received messages.
    */
  @SuppressWarnings(Array("org.wartremover.warts.Any", "org.wartremover.warts.NonUnitStatements"))
  def receive: Receive = {
    case Clean =>
      val start = clock.now.getMillis
      val msg   = new StringBuffer("\n")
      msg
        .append("=================================\n")
        .append("Start to cleanup auth tokens\n")
        .append("=================================\n")
      val _ =
        service.clean
          .map { deleted =>
            val seconds = (clock.now.getMillis - start) / 1000
            msg
              .append(
                "Total of %s auth tokens(s) were deleted in %s seconds".format(deleted.length,
                                                                               seconds)
              )
              .append("\n")
              .append("=================================\n")
              .append("=================================\n")
            logger.info(msg.toString)
          }
          .recover {
            case e =>
              msg
                .append("Couldn't cleanup auth tokens because of unexpected error\n")
                .append("=================================\n")
              logger.error(msg.toString, e)
          }
  }
}

/**
  * The companion object.
  */
object AuthTokenCleaner {
  case object Clean
}
