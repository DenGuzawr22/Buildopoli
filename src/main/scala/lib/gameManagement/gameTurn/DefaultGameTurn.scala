package lib.gameManagement.gameTurn

import lib.gameManagement.gameOptions.GameOptions
import lib.gameManagement.gameStore.GameStore
import lib.player.Player

import scala.collection.mutable.ListBuffer

case class DefaultGameTurn(gameOptions: GameOptions, gameStore: GameStore) extends GameTurn with GameJail:
  override def selectNextPlayer(): Int =
    if !isNextTurnOpen then throw new RuntimeException("Previous player input values not emptied")
    val selection: Int = gameOptions.playerTurnSelector.apply(gameStore.playersList, playerWithTurn)
    playerWithTurn = playerWithTurn :+ selection
    this.everyoneHasDoneOneTurn()
    selection

  override protected def isNextTurnOpen: Boolean = gameStore.userInputs.isListEmpty

  private def everyoneHasDoneOneTurn(): Unit =
    if playerWithTurn.size == (gameStore.playersList.size - blockingList.size) then
      playerWithTurn = List()
      doTurn()

  override def lockPlayer(playerId: Int, blockingTime: Int): Unit =
    blockingList = blockingList + (playerId -> blockingTime)

  override def liberatePlayer(playerId: Int): Unit =
    blockingList = blockingList - playerId

  override def doTurn(): Unit =
    if blockingList.nonEmpty then
      blockingList.toList.foreach(_ match
        case (key: Int, value: Int) if value >= 0 => blockingList = blockingList + (key -> (value - 1))
      )
      blockingList = blockingList.filter(_._2 > 0)

  override def getRemainingBlockedMovements(playerId: Int): Option[Int] =
    blockingList.get(playerId)

  override def isPlayerBlocked(playerId: Int): Boolean = this.getRemainingBlockedMovements(playerId).isEmpty
