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

package models.daos

import com.github.tminglei.slickpg.{ ExPostgresDriver, PgDate2Support }
import slick.driver.JdbcProfile
import slick.profile.Capability

/**
  * To be able to use the postgresql extension for slick we have to
  * provide our own postgres driver.
  *
  * @see https://github.com/tminglei/slick-pg
  */
trait CustomPostgresDriver extends ExPostgresDriver with PgDate2Support {

  // Add back `capabilities.insertOrUpdate` to enable native `upsert` support; for postgres 9.5+
  protected override def computeCapabilities: Set[Capability] =
    super.computeCapabilities + JdbcProfile.capabilities.insertOrUpdate

  // Customise the api. Be sure to not add a type annotation to the overridden method.
  override val api = new API with DateTimeImplicits {}

}

/**
  * The object that will be used to provide postgresql functionalities for slick.
  */
object CustomPostgresDriver extends CustomPostgresDriver