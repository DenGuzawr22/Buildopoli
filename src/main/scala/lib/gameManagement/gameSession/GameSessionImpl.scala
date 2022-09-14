package lib.gameManagement.gameSession

import lib.gameManagement.diceGenerator.{Dice, SingleDice}
import lib.gameManagement.gameBank.{Bank, GameBankImpl}
import lib.gameManagement.gameOptions.GameOptions
import lib.gameManagement.gameStore.GameStore
import lib.gameManagement.gameTurn.GameTurn
import lib.lap.Lap
import lib.player.{Player, PlayerImpl}
import lib.terrain.{Buildable, GroupManager, Purchasable, PurchasableState, Terrain}
import org.slf4j.{Logger, LoggerFactory}

import scala.collection.mutable.ListBuffer
import scala.util.Random

case class GameSessionImpl(
    override val gameOptions: GameOptions,
    override val gameBank: Bank,
    override val gameTurn: GameTurn,
    override val gameStore: GameStore,
    override val gameLap: Lap
) extends GameSession:

  override val dice: Dice = SingleDice(gameOptions.diceFaces)
  override val logger: Logger = LoggerFactory.getLogger("GameSession")
  private var groupManager: GroupManager = _

  override def getGroupManager: GroupManager = this.groupManager

  override def startGame(): Unit =
    this.addManyPlayers(gameOptions.nUsers)
    initializePlayers()
    this.gameStore.startGame()
    this.groupManager = GroupManager(gameStore.terrainList.toList)

  private def addManyPlayers(n: Int): Unit =
    for _ <- 0 until n do this.addOnePlayer(Option.empty)

  private def addOnePlayer(playerId: Option[Int]): Unit =
    if playerId.isEmpty then this.gameStore.playerIdsCounter += 1
    this.gameStore.addPlayer(PlayerImpl(this.checkPlayerId(playerId)))

  private def initializePlayers(): Unit =
    this.gameStore.playersList.foreach(pl => pl.setPlayerMoney(gameOptions.playerInitialMoney))
    giveTerrains()

  private def giveTerrains(): Unit =
    if enoughPurchasableTerrains() then
      this.gameStore.playersList.foreach(pl =>
        for _ <- 0 until this.gameOptions.playerInitialCells do
          val purchasableTerrainList: Seq[Terrain] = this.gameStore.getTypeOfTerrains(tr =>
            tr.isInstanceOf[Purchasable] && tr.asInstanceOf[Purchasable].state == PurchasableState.IN_BANK
          )
          purchasableTerrainList(Random.nextInt(purchasableTerrainList.size))
            .asInstanceOf[Purchasable]
            .changeOwner(Option.apply(pl.playerId))
      )
    else throw new IllegalStateException("Not enough terrains !")

  def enoughPurchasableTerrains(): Boolean =
    this.gameStore.getNumberOfTerrains(tr =>
      tr.isInstanceOf[Purchasable]
    ) >= this.gameOptions.nUsers * this.gameOptions.playerInitialCells

  def checkPlayerId(playerId: Option[Int]): Int =
    playerId match
      case None =>
        while playerIdAlreadyExist(this.gameStore.playerIdsCounter) do this.gameStore.playerIdsCounter += 1
        this.gameStore.playerIdsCounter
      case Some(id) =>
        var player = id
        while playerIdAlreadyExist(player) do player += 1
        player

  def playerIdAlreadyExist(playerId: Int): Boolean =
    this.gameStore.playersList.exists(p => p.playerId.equals(playerId))

  override def setPlayerPosition(playerId: Int, nSteps: Int, isValidLap: Boolean): Unit =
    val player = gameStore.getPlayer(playerId)
    val result =
      gameLap.isNewLap(isValidLap, player.getPlayerPawnPosition, nSteps, gameStore.getNumberOfTerrains(_ => true))
    player.setPlayerPawnPosition(result._1)
    if result._2 then gameLap.giveReward(playerId)