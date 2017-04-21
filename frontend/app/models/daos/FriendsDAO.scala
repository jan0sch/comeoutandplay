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

import java.util.UUID
import javax.inject.Inject

import models._
import play.api.Configuration
import play.api.db.slick.DatabaseConfigProvider

import scala.concurrent.{ ExecutionContext, Future }

/**
  * Provides access to the friends objects of the database.
  */
class FriendsDAO @Inject()(override protected val configuration: Configuration,
                           override protected val dbConfigProvider: DatabaseConfigProvider)
    extends Tables(configuration, dbConfigProvider) {

  import driver.api._

  /**
    * Block a user.
    *
    * @param blocked  The user that should be blocked.
    * @return Future of blocked user.
    */
  def blockUser(
      blocked: UserBlocked
  ): Future[UserBlocked] =
    dbConfig.db.run((usersBlocked returning usersBlocked) += blocked)

  /**
    * Remove the block entry for a specific user from the database.
    *
    * @param user     The current user.
    * @param blocked  The user that should be unblocked.
    * @return Future of Int for affected rows.
    */
  def unblockUser(
      user: User,
      blocked: UUID
  ): Future[Int] =
    dbConfig.db.run(
      usersBlocked.filter(e => e.user === user.userID && e.blocked === blocked).delete
    )

  /**
    * Create a new friendship connection between two users.
    *
    * @param friend The new friendship connection.
    * @return Future of Friend
    */
  def createFriend(friend: Friend): Future[Friend] =
    dbConfig.db.run((friends returning friends) += friend)

  /**
    * Create a new friend request.
    *
    * @param friendRequest  A new friend request
    * @return Future of a FriendRequest
    */
  def createFriendRequest(
      friendRequest: FriendRequest
  ): Future[FriendRequest] =
    dbConfig.db.run((friendRequests returning friendRequests) += friendRequest)

  /**
    * Destroy a friendship connection between two users.
    *
    * @param userId User ID of the first user
    * @param friend The user ID of the friend.
    * @return Future of Int of deleted rows.
    */
  def destroyFriend(userId: UUID, friend: UUID): Future[Int] =
    dbConfig.db.run(
      friends
        .filter(e => e.user === userId && e.friend === friend)
        .delete
    )

  /**
    * Destroy a friendship request between two users.
    *
    * @param userId User ID of the first user
    * @param friend The user ID of the friend.
    * @return Future of Int of deleted rows.
    */
  def destroyFriendRequest(userId: UUID, friend: UUID): Future[Int] =
    dbConfig.db.run(
      friendRequests
        .filter(e => e.user === userId && e.friend === friend)
        .delete
    )

  /**
    * Get the blocked users for this user.
    *
    * @param user The current user.
    * @return Future of sequence of FriendWithInfo.
    */
  def getBlocked(user: User)(implicit ec: ExecutionContext): Future[Seq[FriendWithInfo]] = {
    val q =
    usersBlocked.filter(_.user === user.userID) joinLeft
    users on (_.blocked === _.user_id)
    dbConfig.db
      .run(q.result)
      .map(
        r =>
          r.flatMap(
            e =>
              e._2.fold(None: Option[FriendWithInfo])(
                u => Option(FriendWithInfo(u, None, None, Option(e._1), None))
            )
        )
      )
  }

  /**
    * Get the friends for the provided user.
    *
    * @param user The actual user.
    * @return Future of sequence of FriendWithInfo.
    */
  def getFriends(user: User)(implicit ec: ExecutionContext): Future[Seq[FriendWithInfo]] = {
    val q =
    friends.filter(_.user === user.userID) joinLeft
    users on (_.friend === _.user_id)
    dbConfig.db
      .run(q.result)
      .map(
        r =>
          r.flatMap(
            e =>
              e._2.fold(None: Option[FriendWithInfo])(
                u => Option(FriendWithInfo(u, Option(e._1), None, None, None))
            )
        )
      )
  }

  /**
    * Get the open requests that  were made from the provided user.
    *
    * @param user The actual user.
    * @return Future of sequence of FriendWithInfo.
    */
  def getOpenRequests(user: User)(implicit ec: ExecutionContext): Future[Seq[FriendWithInfo]] = {
    val q =
    friendRequests.filter(_.user === user.userID) joinLeft
    users on (_.friend === _.user_id)
    dbConfig.db
      .run(q.result)
      .map(
        r =>
          r.flatMap(
            e =>
              e._2.fold(None: Option[FriendWithInfo])(
                u => Option(FriendWithInfo(u, None, Option(e._1), None, None))
            )
        )
      )
  }

  /**
    * Get the open requests that are open for the provided user.
    *
    * @param user The actual user.
    * @return Future of sequence of FriendWithInfo.
    */
  def getMyOpenRequests(user: User)(implicit ec: ExecutionContext): Future[Seq[FriendWithInfo]] = {
    val q =
    friendRequests.filter(_.friend === user.userID) joinLeft
    users on (_.user === _.user_id)
    dbConfig.db
      .run(q.result)
      .map(
        r =>
          r.flatMap(
            e =>
              e._2.fold(None: Option[FriendWithInfo])(
                u => Option(FriendWithInfo(u, None, Option(e._1), None, None))
            )
        )
      )
  }

  /**
    * Search for new friends in the database.
    *
    * @param user   The current user model that started the search.
    * @param query  The query that is used to search in the database.
    * @return Future of Seq of possible new friends (User).
    */
  def searchFriends(
      user: User,
      query: String
  )(implicit ec: ExecutionContext): Future[Seq[FriendWithInfo]] = {
    val q = users
      .filterNot(_.user_id === user.userID)
      .filter(_.username.toLowerCase.like(s"%${query.toLowerCase}%")) joinLeft
    friends.filter(_.user === user.userID) on (_.user_id === _.friend) joinLeft
    friendRequests.filter(_.user === user.userID) on (_._1.user_id === _.friend) joinLeft
    usersBlocked.filter(e => e.user === user.userID || e.blocked === user.userID) on (_._1._1.user_id === _.blocked) joinLeft
    usersBlocked.filter(e => e.user === user.userID || e.blocked === user.userID) on (_._1._1._1.user_id === _.user)
    dbConfig.db
      .run(q.result)
      .map(
        r =>
          r.filterNot(_._1._2.isDefined) // this user is blocked by the signed in user
            .filterNot(_._2.isDefined) // this user blocked the signed in user
            .map(e => FriendWithInfo(e._1._1._1._1, e._1._1._1._2, e._1._1._2, e._1._2, e._2))
      )
  }

}
