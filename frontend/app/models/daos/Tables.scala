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

import java.time.ZonedDateTime
import java.util.UUID
import javax.inject.Inject

import com.mohiva.play.silhouette.api.LoginInfo
import com.mohiva.play.silhouette.api.util.PasswordInfo
import com.mohiva.play.silhouette.impl.providers.{ OAuth1Info, OAuth2Info }
import models.User
import play.api.Configuration
import play.api.db.slick.{ DatabaseConfigProvider, HasDatabaseConfigProvider }
import slick.lifted.ProvenShape

/**
  * Table definitions for the available models.
  */
class Tables @Inject()(protected val configuration: Configuration,
                       protected val dbConfigProvider: DatabaseConfigProvider)
    extends HasDatabaseConfigProvider[CustomPostgresDriver] {

  import driver.api._

  /**
    * Helper function that transforms the database rows of an OAuth2Info
    * into an OAuth2Info object.
    *
    * @param row  The rows from the database for OAuth2Info
    * @return A new OAuth2Info object
    */
  def fromDatabaseOAuth2(row: (String, String, Int, String, Map[String, String])): OAuth2Info =
    OAuth2Info(row._1, Option(row._2), Option(row._3), Option(row._4), Option(row._5))

  /**
    * Helper function that transforms an OAuth2Info object into the corresponding
    * database rows.
    *
    * @param o  An OAuth2Info object
    * @return The database rows as tuple
    */
  def toDatabaseOAuth2(o: OAuth2Info): (String, String, Int, String, Map[String, String]) =
    (o.accessToken,
     o.tokenType.getOrElse(""),
     o.expiresIn.getOrElse(0),
     o.refreshToken.getOrElse(""),
     o.params.getOrElse(Map.empty))

  /**
    * Mapping helper that converts implicitly a Map[String,String] into
    * a Json-String and vice versa.
    */
  implicit val MapToJson = MappedColumnType.base[Map[String, String], String](
    m => play.api.libs.json.Json.toJson(m).toString(),
    s =>
      play.api.libs.json.Json
        .fromJson[Map[String, String]](play.api.libs.json.Json.toJson(s))
        .getOrElse(Map.empty)
  )

  /**
    * Slick table definition for the stores table.
    *
    * @param tag A Tag marks a specific row represented by an AbstractTable instance.
    */
  class Users(tag: Tag) extends Table[User](tag, "users") {
    def user_id              = column[UUID]("user_id", O.PrimaryKey)
    def provider_id          = column[String]("provider_id")
    def provider_key         = column[String]("provider_key")
    def email                = column[String]("email")
    def firstname            = column[Option[String]]("firstname")
    def lastname             = column[Option[String]]("lastname")
    def hasher               = column[String]("hasher")
    def password             = column[String]("password")
    def salt                 = column[String]("salt")
    def oauth1_token         = column[String]("oauth1_token")
    def oauth1_secret        = column[String]("oauth1_secret")
    def oauth2_access_token  = column[String]("oauth2_access_token")
    def oauth2_token_type    = column[String]("oauth2_token_type")
    def oauth2_expires       = column[Int]("oauth2_expires")
    def oauth2_refresh_token = column[String]("oauth2_refresh_token")
    def oauth2_params        = column[Map[String, String]]("oauth2_params")
    def avatar_url           = column[Option[String]]("avatar_url")
    def activated            = column[Boolean]("activated")
    def active               = column[Boolean]("active")
    def created_at           = column[Option[ZonedDateTime]]("created_at")
    def updated_at           = column[Option[ZonedDateTime]]("updated_at")
    def admin                = column[Boolean]("admin")
    def moderator            = column[Boolean]("moderator")

    def loginInfo = (provider_id, provider_key) <> (LoginInfo.tupled, LoginInfo.unapply)

    def passwordInfo = (hasher, password, salt.?) <> (PasswordInfo.tupled, PasswordInfo.unapply)

    def oAuth1Info = (oauth1_token, oauth1_secret) <> (OAuth1Info.tupled, OAuth1Info.unapply)

    def oAuth2Info =
      (oauth2_access_token, oauth2_token_type, oauth2_expires, oauth2_refresh_token, oauth2_params).shaped <> (fromDatabaseOAuth2, (o: OAuth2Info) =>
        Option(toDatabaseOAuth2(o)))

    def userType =
      (user_id,
       loginInfo,
       email.?,
       firstname,
       lastname,
       passwordInfo,
       oAuth1Info,
       oAuth2Info,
       avatar_url,
       activated,
       active,
       created_at,
       updated_at,
       admin,
       moderator)

    def unique_key = index("users_unique", email, unique = true)

    override def * : ProvenShape[User] = userType.shaped <> (User.tupled, User.unapply)
  }

  // The actual table query on which operations can be performed.
  val users = TableQuery[Users]

}
