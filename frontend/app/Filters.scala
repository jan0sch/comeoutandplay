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

import javax.inject._
import play.api._
import play.api.http.HttpFilters
import play.api.mvc._

import filters.ExampleFilter

/**
  * This class configures filters that run on every request. This
  * class is queried by Play to get a list of filters.
  *
  * Play will automatically use filters from any class called
  * `Filters` that is placed the root package. You can load filters
  * from a different class by adding a `play.http.filters` setting to
  * the `application.conf` configuration file.
  *
  * @param env Basic environment settings for the current application.
  * @param exampleFilter A demonstration filter that adds a header to
  * each response.
  */
@Singleton
class Filters @Inject()(env: Environment, exampleFilter: ExampleFilter) extends HttpFilters {

  override val filters = {
    // Use the example filter if we're running development mode. If
    // we're running in production or test mode then don't use any
    // filters at all.
    if (env.mode == Mode.Dev) Seq(exampleFilter) else Seq.empty
  }

}
