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

package actors

import java.time.ZonedDateTime
import java.util.UUID

import actors.FriendsSearchWebsocketActor.Page._
import actors.FriendsSearchWebsocketActor._
import akka.actor.{ Actor, ActorLogging, ActorRef, Props }
import models._
import models.daos.FriendsDAO
import play.api.i18n.{ I18nSupport, Messages, MessagesApi }
import play.api.libs.json.{ JsValue, Json }

import scala.concurrent.{ ExecutionContext, Future }

/**
  * WebSocket that connects the client with the backend for requests regarding
  * the friends search.
  *
  * @param out        The ActorRef of the WebSocket.
  * @param user       The user that is signed into the system.
  * @param friendsDAO The friendsDAO for access to the database.
  */
class FriendsSearchWebsocketActor(val messagesApi: MessagesApi,
                                  out: ActorRef,
                                  user: User,
                                  friendsDAO: FriendsDAO)
    extends Actor
    with ActorLogging
    with I18nSupport
    with WebsocketHelper {

  import context.dispatcher

  /**
    * Receive method for incoming messages.
    *
    * @return Result
    */
  override def receive: Receive = {

    case json: JsValue =>
      log.info(s"Received JSValue from friends search WebSocket: $json")
      val typeOption = (json \ "type").toOption.getOrElse("").toString.replaceAll("\"", "").trim
      typeOption match {
        case ClientRequestUpdatePages =>
          for {
            result <- updatePages
          } yield out ! result
        case ClientRequestAcceptFriend =>
          for {
            result <- acceptFriend(json)
          } yield out ! result
        // Block a specific user
        case ClientRequestBlockUser =>
          for {
            result <- blockUser(json)
          } yield out ! result
        // Unblock a user
        case ClientRequestUnblockUser =>
          for {
            result <- unblockUser(json)
          } yield out ! result
        // Create a friend request
        case ClientRequestRequestFriendship =>
          for {
            result <- requestFriendship(json)
          } yield out ! result
        // Destroy a friendship connection
        case ClientRequestDestroyFriendship =>
          for {
            result <- destroyFriendship(json)
          } yield out ! result
        // Destroy a friends request connection
        case ClientRequestDestroyFriendshipRequest =>
          for {
            result <- destroyFriendshipRequest(json)
          } yield out ! result
        // Search for other users
        case ClientRequestStartSearch =>
          for {
            result <- startSearch(json)
          } yield out ! result
        // Show the list of blocked users
        case ClientRequestShowBlockedUsers =>
          for {
            result <- showBlockedUsers
          } yield out ! result
        // Show the list of own friends
        case ClientRequestShowFriends =>
          for {
            result <- showFriends
          } yield out ! result
        // Show the list of open friend requests
        case ClientRequestShowOpenRequests =>
          for {
            result <- showOpenRequests
          } yield out ! result
      }

  }

  /**
    * Get the list of own friends.
    *
    * @return Future of JsValue
    */
  def showFriends(implicit ec: ExecutionContext): Future[JsValue] =
    for {
      myOpenRequests <- friendsDAO.getMyOpenRequests(user)
      list           <- friendsDAO.getFriends(user)
    } yield createFriendsList(list, MyFriendsPage, myOpenRequests)

  /**
    * Get the list of open requests.
    *
    * @return Future of JsValue.
    */
  def showOpenRequests(implicit ec: ExecutionContext): Future[JsValue] =
    for {
      list <- friendsDAO.getOpenRequests(user)
    } yield createFriendsList(list, OpenRequestsPage)

  /**
    * Get the list of blocked users.
    *
    * @return Future of JsValue.
    */
  def showBlockedUsers(implicit ec: ExecutionContext): Future[JsValue] =
    for {
      list <- friendsDAO.getBlocked(user)
    } yield createFriendsList(list, BlockedPage)

  /**
    * Return the actual content for all pages.
    *
    * @return Future of JsValue with the content for all pages.
    */
  def updatePages(implicit ec: ExecutionContext): Future[JsValue] =
    for {
      myOpenRequests <- friendsDAO.getMyOpenRequests(user)
      friends        <- friendsDAO.getFriends(user)
      openRequests   <- friendsDAO.getOpenRequests(user)
      blocked        <- friendsDAO.getBlocked(user)
    } yield {
      Json.parse(s"""
       {
       "type": "updatePagesResponse",
       "friends": "${createFriendsListHtml(friends, MyFriendsPage, myOpenRequests)}",
       "openRequests": "${createFriendsListHtml(openRequests, OpenRequestsPage)}",
       "blocked": "${createFriendsListHtml(blocked, BlockedPage)}"
       }
      """)
    }

  /**
    * Accept the friendship request for an open request.
    *
    * @param json The request JSON.
    * @return Future of JsValue.
    */
  def acceptFriend(json: JsValue): Future[JsValue] = {
    val uuidJsonOption = (json \ "uuid").toOption
    val uuidOpt        = uuidJsonOption.fold(None: Option[UUID])(r => r.asOpt[UUID])

    uuidOpt.fold(
      Future.successful(
        createMessage("acceptFriendResponse", "danger", Messages("friends.my-requests.error"))
      )
    ) { uuid =>
      for {
        _ <- friendsDAO.destroyFriendRequest(user.userID, uuid)
        _ <- friendsDAO.destroyFriendRequest(uuid, user.userID)
        _ <- friendsDAO.createFriend(Friend(user.userID, uuid, ZonedDateTime.now()))
        _ <- friendsDAO.createFriend(Friend(uuid, user.userID, ZonedDateTime.now()))
      } yield {
        createMessage("acceptFriendResponse", "success", Messages("friends.my-requests.success"))
      }
    }
  }

  /**
    * Block a specific user.
    *
    * @param json The request JSON.
    * @return Future of JsValue.
    */
  def blockUser(json: JsValue): Future[JsValue] = {
    val uuidJsonOption = (json \ "uuid").toOption
    val uuidOpt        = uuidJsonOption.fold(None: Option[UUID])(r => r.asOpt[UUID])

    uuidOpt.fold(
      Future.successful(
        createMessage("blockUserResponse", "danger", Messages("friends.block.error"))
      )
    ) { uuid =>
      for {
        _ <- friendsDAO.blockUser(UserBlocked(user.userID, uuid, ZonedDateTime.now()))
        _ <- friendsDAO.destroyFriendRequest(user.userID, uuid)
        _ <- friendsDAO.destroyFriendRequest(uuid, user.userID)
        _ <- friendsDAO.destroyFriend(user.userID, uuid)
        _ <- friendsDAO.destroyFriend(uuid, user.userID)
      } yield {
        createMessage("blockUserResponse", "success", Messages("friends.block.success"))
      }
    }
  }

  /**
    * Delete the block of a specific user.
    *
    * @param json The request JSON.
    * @return Future of JsValue.
    */
  def unblockUser(json: JsValue): Future[JsValue] = {
    val uuidJsonOption = (json \ "uuid").toOption
    val uuidOpt        = uuidJsonOption.fold(None: Option[UUID])(r => r.asOpt[UUID])

    uuidOpt.fold(
      Future.successful(
        createMessage("unblockUserResponse", "danger", Messages("friends.unblock.error"))
      )
    ) { uuid =>
      for {
        _ <- friendsDAO.unblockUser(user, uuid)
      } yield {
        createMessage("unblockUserResponse", "success", Messages("friends.unblock.success"))
      }
    }
  }

  /**
    * Search for friends.
    *
    * @param json The request JSON.
    * @return Future of JsValue.
    */
  def startSearch(json: JsValue)(implicit ec: ExecutionContext): Future[JsValue] = {
    val queryOption = (json \ "q").toOption
    val query       = queryOption.fold("")(r => r.as[String])
    for {
      list <- friendsDAO.searchFriends(user, query) if query.nonEmpty
    } yield createFriendsList(list, SearchPage)
  }

  /**
    * Create a new friendship request.
    *
    * @param json The request JSON.
    * @return Future of JsValue.
    */
  def requestFriendship(json: JsValue): Future[JsValue] = {
    val uuidJsonOption = (json \ "uuid").toOption
    val uuidOpt        = uuidJsonOption.fold(None: Option[UUID])(r => r.asOpt[UUID])

    for {
      result <- uuidOpt.fold(
        Future.successful(
          createMessage("requestFriendshipResponse", "danger", Messages("friends.request.error"))
        )
      ) { uuid =>
        val fr = FriendRequest(user.userID, uuid, ZonedDateTime.now())
        friendsDAO
          .createFriendRequest(fr)
          .map(
            r =>
              createMessage("requestFriendshipResponse",
                            "success",
                            Messages("friends.request.success"))
          )
      }
    } yield result
  }

  /**
    * Destroy a friendship request.
    *
    * @param json The request JSON.
    * @return Future of JsValue.
    */
  def destroyFriendshipRequest(json: JsValue): Future[JsValue] = {
    val uuidJsonOption = (json \ "uuid").toOption
    val uuidOpt        = uuidJsonOption.fold(None: Option[UUID])(r => r.asOpt[UUID])

    uuidOpt.fold(
      Future.successful(
        createMessage("destroyFriendsRequestResponse",
                      "danger",
                      Messages("friends.request-destroy.error"))
      )
    ) { uuid =>
      for {
        _ <- friendsDAO.destroyFriendRequest(user.userID, uuid)
        _ <- friendsDAO.destroyFriendRequest(uuid, user.userID)
      } yield {
        createMessage("destroyFriendsRequestResponse",
                      "success",
                      Messages("friends.request-destroy.success"))
      }
    }
  }

  /**
    * Destroy a friendship.
    *
    * @param json The request JSON.
    * @return Future of JsValue.
    */
  def destroyFriendship(json: JsValue): Future[JsValue] = {
    val uuidJsonOption = (json \ "uuid").toOption
    val uuidOpt        = uuidJsonOption.fold(None: Option[UUID])(r => r.asOpt[UUID])

    uuidOpt.fold(
      Future.successful(
        createMessage("destroyFriendshipRequestResponse",
                      "danger",
                      Messages("friends.destroy.error"))
      )
    ) { uuid =>
      for {
        _ <- friendsDAO.destroyFriend(user.userID, uuid)
        _ <- friendsDAO.destroyFriend(uuid, user.userID)
      } yield {
        createMessage("destroyFriendshipRequestResponse",
                      "success",
                      Messages("friends.destroy.success"))
      }
    }
  }

  /**
    * Create an alert message.
    *
    * @param responseType The identifier for the client that describes the response.
    * @param msgType      The type of the message (`danger`, `success`, `warning`, `info`).
    * @param msg          The message that will be displayed.
    * @return JsValue
    */
  def createMessage(responseType: String, msgType: String, msg: String): JsValue =
    Json.parse(s"""
       {
       "type": "$responseType",
       "msgType": "$msgType",
       "msg": "$msg"
       }
      """)

  /**
    * Create the Html String that will be displayed to the client.
    *
    * @param list             The list of user entries.
    * @param page             The page of the user list.
    * @param myOpenRequests   The open requests for friendship, if exists.
    * @return String
    */
  def createFriendsListHtml(list: Seq[FriendWithInfo],
                            page: Page,
                            myOpenRequests: Seq[FriendWithInfo] = Seq.empty): String = {
    val emptyMessage =
      page match {
        case BlockedPage      => Messages("friends.blocked.empty")
        case OpenRequestsPage => Messages("friends.requests.empty")
        case MyFriendsPage    => Messages("friends.my.empty")
        case SearchPage       => Messages("friends.listing.empty")
        case _                => throw new RuntimeException(s"Page type unknown: $page")
      }

    val html =
      if (list.isEmpty)
        cleanHtmlTemplate(
          views.html.friends.snippets.listingEmpty(emptyMessage)
        )
      else
        cleanHtmlTemplate(views.html.friends.snippets.friendsListing(list))

    if (myOpenRequests.nonEmpty)
      cleanHtmlTemplate(views.html.friends.snippets.myOpenRequests(myOpenRequests)) + html
    else
      html
  }

  /**
    * Create the final list of users for the display.
    *
    * @param list The list of users.
    * @param page The page where the listing should be displayed.
    * @param myOpenRequests Friendship requests that are open for the current user.
    * @return JsValue
    */
  def createFriendsList(list: Seq[FriendWithInfo],
                        page: Page,
                        myOpenRequests: Seq[FriendWithInfo] = Seq.empty): JsValue = {
    val typeString =
      page match {
        case BlockedPage      => "showBlockedUserResponse"
        case OpenRequestsPage => "showOpenRequestsResponse"
        case MyFriendsPage    => "showFriendsResponse"
        case SearchPage       => "showSearchResponse"
        case _                => throw new RuntimeException(s"Page type unknown: $page")
      }

    val html = createFriendsListHtml(list, page, myOpenRequests)

    Json.parse(
      s"""
        {
        "type" : "$typeString",
        "userListing" : "$html"
        }
      """
    )
  }

}

/**
  * Object with the pops definition of the actor.
  */
object FriendsSearchWebsocketActor {

  final val ClientRequestAcceptFriend             = "acceptFriend"
  final val ClientRequestBlockUser                = "blockUser"
  final val ClientRequestRequestFriendship        = "requestFriendship"
  final val ClientRequestDestroyFriendship        = "destroyFriendship"
  final val ClientRequestDestroyFriendshipRequest = "destroyFriendshipRequest"
  final val ClientRequestStartSearch              = "startSearch"
  final val ClientRequestShowBlockedUsers         = "showBlockedUsers"
  final val ClientRequestShowFriends              = "showFriends"
  final val ClientRequestShowOpenRequests         = "showOpenRequests"
  final val ClientRequestUnblockUser              = "unblockUser"
  final val ClientRequestUpdatePages              = "updatePages"

  /**
    * A sealed trait for the possible page types.
    */
  sealed trait Page

  /**
    * A companion object for the sealed trait to keep the namespace clean.
    */
  object Page {
    // The page with the blocked users
    case object BlockedPage extends Page
    // The page with the own friends
    case object MyFriendsPage extends Page
    // The page with the open requests for friendship
    case object OpenRequestsPage extends Page
    // The page where the user can search for friends
    case object SearchPage extends Page
  }

  /**
    * A factory method to create the described actor.
    *
    * @param messagesApi  The Messages object.
    * @param out          The actor reference of the current actor.
    * @param user         The currently signed in user.
    * @param friendsDAO   The DAO to access the friends tables.
    * @return The properties needed to create the actor.
    */
  def props(messagesApi: MessagesApi, out: ActorRef, user: User, friendsDAO: FriendsDAO) =
    Props(new FriendsSearchWebsocketActor(messagesApi, out, user, friendsDAO))
}
