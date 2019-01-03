package com.wegtam.books.comeoutandplay.games.seabattle.adt

/**
  * Stati für ein Spielbrett.
  */
sealed trait BoardState extends Product with Serializable

object BoardState {

  /**
    * Das Spielbrett ist leer.
    */
  case object Empty extends BoardState

  /**
    * Das Spielbrett enthält nicht genug Schiffe.
    */
  case object NotReady extends BoardState

  /**
    * Das Spielbrett enthält alle nötigen Schiffe.
    */
  case object Ready extends BoardState

  /**
    * Alle Schiffe auf dem Brett wurden versenkt.
    */
  case object Finished extends BoardState

}
