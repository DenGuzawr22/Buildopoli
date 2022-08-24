package behaviour.event

import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.*
import behaviour.event.EventModule.*
import util.mock.BankHelper.*
import util.mock.BankHelper.BankMock.*
import EventStoryModule.*

class EventTest extends AnyFunSuite with BeforeAndAfterEach:
  val MOCK_ID: Int = 0
  var bank: BankMock = BankMock()
  override def beforeEach(): Unit =
    bank = BankMock()

  val eventStrategy: EventStrategy = _ => bank.decrement(TAX)
  test("Decrement bank money with event strategy") {
    assert(bank.money == BANK_MONEY)
    eventStrategy(MOCK_ID)
    assert(bank.money == BANK_MONEY - TAX)
    eventStrategy(MOCK_ID)
    assert(bank.money == BANK_MONEY - TAX * 2)
  }

  import behaviour.event.EventModule.*
  import EventStory.*

  val ev: Event = Event(Scenario(eventStrategy, Scenario.tempStory), Event.WITHOUT_PRECONDITION)
  test("An event that can change balance of a bank") {
    ev.run(MAIN_ACTION)
    assert(bank.money == BANK_MONEY - TAX)
    ev.run(MAIN_ACTION)
    assert(bank.money == BANK_MONEY - TAX * 2)
  }

  test("Two consecutive events in a chain") {
    assert(bank.money == BANK_MONEY)
    import EventOperation.*
    val ev2: Event
      = Event(Scenario(_ => bank.decrement(TAX * 2), Scenario.tempStory), Event.WITHOUT_PRECONDITION) ++ ev
    ev2.run(MAIN_ACTION)
    var nextEv = ev2.nextEvent

    while nextEv.nonEmpty do
      val ev = nextEv.get.head
      if ev.eventStory(MOCK_ID).isSingleAction then
        ev.run(MAIN_ACTION)
        nextEv = ev.nextEvent
      else fail("Multiple action case is not supported now")
    assert(bank.money == BANK_MONEY - TAX * 3)
  }

  import EventOperation.*
  test("Test ++ operator with Event object") {
    val myEvent: Event = Event(Scenario(Scenario.tempStory))
    val myEvent2: Event = Event(Scenario(Scenario.tempStory))
    val appended = myEvent ++ myEvent2
    assert(appended.isInstanceOf[Event])
    assert(appended.nextEvent.get.head.isInstanceOf[Event])
  }