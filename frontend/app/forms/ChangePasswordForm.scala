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

package forms

import play.api.data.Forms._
import play.api.data._

/**
  * The `Change Password` form.
  */
object ChangePasswordForm {

  /**
    * A play framework form.
    */
  val form = Form(
    tuple(
      "current-password" -> nonEmptyText,
      "new-password"     -> nonEmptyText
    )
  )
}
