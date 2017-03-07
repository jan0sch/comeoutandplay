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

package filters

import akka.stream.Materializer
import javax.inject._
import play.api.mvc._
import scala.concurrent.{ ExecutionContext, Future }

/**
  * This is a simple filter that adds a header to all requests. It's
  * added to the application's list of filters by the
  * [[Filters]] class.
  *
  * @param mat This object is needed to handle streaming of requests
  * and responses.
  * @param exec This class is needed to execute code asynchronously.
  * It is used below by the `map` method.
  */
@Singleton
class ExampleFilter @Inject()(implicit override val mat: Materializer, exec: ExecutionContext)
    extends Filter {

  override def apply(
      nextFilter: RequestHeader => Future[Result]
  )(requestHeader: RequestHeader): Future[Result] =
    // Run the next filter in the chain. This will call other filters
    // and eventually call the action. Take the result and modify it
    // by adding a new header.
    nextFilter(requestHeader).map { result =>
      result.withHeaders("X-ExampleFilter" -> "foo")
    }

}
