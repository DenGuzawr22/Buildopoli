package events

object EventModule:
  trait EventStory:
    def description: String
    def actions: Seq[String]
    def isSingleAction: Boolean = actions.length == 1

  trait Event:
    def run(playerId: Int): Option[Event]
    def eventStory: EventStory

//  enum EventResult:
//    case OKwolf
//    case INTERRUPT(msg: String)

//  type NextEvent = Option[Event]
  type EventStrategy = Int => Unit

  trait Scenario:
    def eventStrategy: EventStrategy
    def nextEvent: Option[Event]
    def eventStory: EventStory

  object EventStory:
    val MAIN_ACTION = 0
    def apply(desc: String, actions: Seq[String]): EventStory = EventStoryImpl(desc, actions)

    case class EventStoryImpl(
        override val description: String,
        override val actions: Seq[String]
    ) extends EventStory:
      override def toString: String =
        s"$description. Available actions: \n\t" + actions.mkString("\n\t")

  object Scenario:
    import EventStory.*
    val tempStory: EventStory = EventStory("My temp description", List("OK"))

    def apply(eventStrategy: EventStrategy, nextEvent: Option[Event]): Scenario =
      ScenarioImpl(eventStrategy, nextEvent, tempStory)

    def apply(nextEvent: Option[Event]): Scenario = ScenarioImpl(nextEvent = nextEvent, tempStory)

    def apply(eventStrategy: EventStrategy, nextEvent: Option[Event], story: EventStory): Scenario =
      ScenarioImpl(eventStrategy, nextEvent, story)

    case class ScenarioImpl(
        override val eventStrategy: EventStrategy = _ => (),
        override val nextEvent: Option[Event],
        override val eventStory: EventStory
    ) extends Scenario

  object Event:
    def apply(scenario: Scenario): Event = EventImpl(scenario)

    class EventImpl(scenario: Scenario) extends Event:
      override def run(playerId: Int): Option[Event] =
//        if actionIndex < 0 then throw new IllegalArgumentException("The action index can not be a negative number")
        scenario.eventStrategy(playerId)
        scenario.nextEvent

      override def eventStory: EventStory = scenario.eventStory
