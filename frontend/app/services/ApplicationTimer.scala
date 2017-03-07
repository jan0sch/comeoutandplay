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

package services

import java.time.{ Clock, Instant }
import javax.inject._
import play.api.Logger
import play.api.inject.ApplicationLifecycle
import scala.concurrent.Future

/**
  * This class demonstrates how to run code when the
  * application starts and stops. It starts a timer when the
  * application starts. When the application stops it prints out how
  * long the application was running for.
  *
  * This class is registered for Guice dependency injection in the
  * [[Module]] class. We want the class to start when the application
  * starts, so it is registered as an "eager singleton". See the code
  * in the [[Module]] class to see how this happens.
  *
  * This class needs to run code when the server stops. It uses the
  * application's [[ApplicationLifecycle]] to register a stop hook.
  */
@Singleton
class ApplicationTimer @Inject()(clock: Clock, appLifecycle: ApplicationLifecycle) {

  // This code is called when the application starts.
  private val start: Instant = clock.instant
  Logger.info(s"ApplicationTimer demo: Starting application at $start.")

  // When the application starts, register a stop hook with the
  // ApplicationLifecycle object. The code inside the stop hook will
  // be run when the application stops.
  appLifecycle.addStopHook { () =>
    val stop: Instant     = clock.instant
    val runningTime: Long = stop.getEpochSecond - start.getEpochSecond
    Logger.info(
      s"ApplicationTimer demo: Stopping application at ${clock.instant} after ${runningTime}s."
    )
    Future.successful(())
  }
}
