package behaviour

import org.scalatest.BeforeAndAfterEach
import org.scalatest.funsuite.AnyFunSuite
import event.EventModule
import event.EventModule.*
import event.EventModule.Event.*
import org.scalatest.BeforeAndAfterEach
import org.scalatest.funsuite.AnyFunSuite
import util.mock.BankHelper.BankMock
import util.mock.BankHelper.BankAccount.*
import behaviour.BehaviourModule.*
import behaviour.BehaviourModule.Behaviour.{chooseEvent, getStories, printStories}

class CasinoTest extends AnyFunSuite with BeforeAndAfterEach:
  private val PLAYER_1: Int = 1

  var bank: BankMock = BankMock()

  override def beforeEach(): Unit =
    bank = BankMock()

  import EventFactory.*
  import EventOperation.*

  private val story = EventStory("You are in casino", Seq("play"))
  private val infoEvent = InfoEvent(story, _ => bank.money > 100)

  private val doubleGameStrategy: EventStrategy = id =>
    val opAmount = bank.getPaymentRequestAmount(Player(id), Bank)
    if opAmount.isEmpty then throw new IllegalStateException("Payment to casino not found")
    else if opAmount.get < 100 then throw new IllegalStateException("Payment too low")
    else
      bank.acceptPayment(Player(id), Bank)
      // assume we always lose

  private val storyGenerator: () => EventStory = () =>
    val desc = "base event description"
    var seq = Seq[String]()
    if bank.money <= 100 then EventStory("Not enough money", Seq())
    else
      for i <- 100 until bank.money by ((bank.money.toDouble / 500).ceil * 100).toInt do seq = seq :+ i.toString
      EventStory(desc, seq)

  private val doubleGameEvent = Event(Scenario(doubleGameStrategy, storyGenerator), WITHOUT_PRECONDITION)

  private val casinoBehaviour = Behaviour(Seq(EventGroup(infoEvent ++ doubleGameEvent)))

  test("Check casino behaviour configuration") {
    var events = casinoBehaviour.getInitialEvents(PLAYER_1)
    println(printStories(getStories(events)))
    assert(events.length == 1)
    assert(events.head.length == 1)
    assert(events.head.head.eventStory.actions.length == 1)
    events = chooseEvent(events)(PLAYER_1, (0, 0))
    assert(events.length == 1)
    assert(events.head.length == 1)
    assert(events.head.head.eventStory.actions.length == 5)
    println(printStories(getStories(events)))
    println(printStories(events))
  }

//  test("")
