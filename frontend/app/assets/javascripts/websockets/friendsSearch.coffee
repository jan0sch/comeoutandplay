window.FriendsSearch or= {}
#
# Constants
#
# Client request methods
window.ClientRequestAcceptFriend = "acceptFriend"
window.ClientRequestBlockUser = "blockUser"
window.ClientRequestRequestFriendship = "requestFriendship"
window.ClientRequestDestroyFriendship = "destroyFriendship"
window.ClientRequestDestroyFriendshipRequest = "destroyFriendshipRequest"
window.ClientRequestStartSearch = "startSearch"
window.ClientRequestShowBlockedUsers = "showBlockedUsers"
window.ClientRequestShowFriends = "showFriends"
window.ClientRequestShowOpenRequests = "showOpenRequests"
window.ClientRequestUnblockUser = "unblockUser"
window.ClientRequestUpdatePages = "updatePages"

# Different pages
PAGE_TYPE_BLOCKED = "blocked"
PAGE_TYPE_MY_FRIENDS = "friends"
PAGE_TYPE_OPEN_REQUESTS = "open"
PAGE_TYPE_SEARCH = "search"

#
# Client methods
#

# Send a request to the backend with a provided user id
FriendsSearch.clientRequest = (requestType, userId) ->
  if (userId)
    req = {
      type: requestType,
      uuid: userId
    }
  else
    req = {
      type: requestType
    }
  FriendsSearch.sendData(JSON.stringify(req))

# Request a new search with the query from the searchbox
FriendsSearch.startSearch = ->
  query = document.getElementById("friends-query").value
  req = {
    type: ClientRequestStartSearch,
    q : query
  }
  FriendsSearch.sendData(JSON.stringify(req))

#
# Display content
#

# Display the result from the friend request and update the search listing for
# the current query
FriendsSearch.showRequestFriendshipResponse = (msgType, msg) ->
#  FriendsSearch.showAlert(msgType, msg)
  FriendsSearch.startSearch()

FriendsSearch.displayHtml = (element, html) ->
  if (element && html)
    $(element).html(html)

FriendsSearch.showAlertAndUpdatePages = (msgType, msg) ->
#  FriendsSearch.showAlert(msgType, msg)
  FriendsSearch.clientRequest(ClientRequestUpdatePages)
  FriendsSearch.startSearch()

# Display an alert to the user
FriendsSearch.showAlert = (msgType, msg) ->
  if(msgType && msg)
    $("#alertContainer").html("""<div class="alert alert-"""+msgType+""" alert-dismissible"><button type="button" class="close" data-dismiss="alert" aria-label="Close"><span aria-hidden="true">&times;</span></button>"""+msg+"""</div>""")
    $("#alertContainer").show()
    $("#alertContainer").delay(100000).fadeOut(2000)

# Update all pages with the appropriate content
FriendsSearch.showUpdatePagesResponse = (obj) ->
  if (obj.friends)
    FriendsSearch.displayHtml("#friendsListing", obj.friends)
  if (obj.openRequests)
    FriendsSearch.displayHtml("#openRequestsListing", obj.openRequests)
  if (obj.blocked)
    FriendsSearch.displayHtml("#blockedListing", obj.blocked)

#
# Process server answers
#
FriendsSearch.processMessage = (json) ->
  if (json.data)
    obj = JSON.parse(json.data)
    if (obj)
      switch
        when obj.type == "acceptFriendResponse" then FriendsSearch.showAlertAndUpdatePages(obj.msgType, obj.msg)
        when obj.type == "blockUserResponse" then FriendsSearch.showAlertAndUpdatePages(obj.msgType, obj.msg, obj.page)
        when obj.type == "destroyFriendsRequestResponse" then FriendsSearch.showAlertAndUpdatePages(obj.msgType, obj.msg)
        when obj.type == "destroyFriendshipRequestResponse" then FriendsSearch.showAlertAndUpdatePages(obj.msgType, obj.msg)
        when obj.type == "requestFriendshipResponse" then FriendsSearch.showRequestFriendshipResponse(obj.msgType, obj.msg)
        when obj.type == "showBlockedUserResponse" then FriendsSearch.displayHtml("#blockedListing", obj.userListing)
        when obj.type == "showFriendsResponse" then FriendsSearch.displayHtml("#friendsListing", obj.userListing)
        when obj.type == "showOpenRequestsResponse" then FriendsSearch.displayHtml("#openRequestsListing", obj.userListing)
        when obj.type == "showSearchResponse" then FriendsSearch.displayHtml("#searchListing", obj.userListing)
        when obj.type == "updatePagesResponse" then FriendsSearch.showUpdatePagesResponse(obj)
        when obj.type == "unblockUserResponse" then FriendsSearch.showAlertAndUpdatePages(obj.msgType, obj.msg)
        else null

#
# General
#
FriendsSearch.connect = ->
  baseUrl = window.location.hostname + ":" + window.location.port + "/friends/websocket/search"
  requestUrl = if (window.location.protocol != "http:") then "wss://" + baseUrl else "ws://" + baseUrl
  webSocket = new WebSocket(requestUrl)

  webSocket.onopen = (e) ->
    console.log("Friends Search Websocket Open")
    FriendsSearch.clientRequest(ClientRequestShowFriends)

  webSocket.onerror = (e) ->
    console.error("Friends Search Websocket error: " + e.data)

  webSocket.onmessage = (e) ->
    if (e && e.data)
      FriendsSearch.processMessage(e)

  FriendsSearch.disconnect = ->
    console.log("Friends Search Websocket disconnect")
    webSocket.disconnect()

  FriendsSearch.sendData = (json) ->
    webSocket.send(json)

