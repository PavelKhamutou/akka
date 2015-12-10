import akka.routing._
import akka.actor._


case class Message(delay: Int, amount: Int)
case object Start
case object Response
case object Shutdown

object HelloAkka extends App {
  println("Hello Akka")
  val system = ActorSystem("load-balancer")
  val master = system.actorOf(Props[Master], name = "master")
  while(true) {
    print(">>> ")
    val ln = scala.io.StdIn.readLine()
    ln match {
      case run if run.split(" ").size == 3 && run.startsWith("run") => {
        val srt = run.split(" ")
        master ! Message(delay = srt(1).toInt, amount = srt(2).toInt)
      }
      case "stop" =>
        master ! Shutdown
        System.exit(0)
      case "" => {}
      case _ =>
        println("Wrong command!")
    }
  }
}

class Master extends Actor with ActorLogging {
  val resizer = DefaultResizer(lowerBound = 1, upperBound = 5)
  val router = context.actorOf(RoundRobinPool(1, Some(resizer)).props(Props[Slave]), "router")
  def receive: Receive = {
    case Message(delay, amount) =>
      log.info(s"Master ${self.path.name} has run!")
      1 to amount foreach { i =>
        router ! Message(delay, amount)
      }

    case Response =>
      log.info(s"Slave ${sender().path.name} has finished his task!")

    case Shutdown =>
      router ! Broadcast(Kill)
  }
}

class Slave extends Actor with ActorLogging {
  import scala.concurrent.duration._
  implicit val dispatcher =  context.system.dispatcher
  override def receive: Actor.Receive = {
    case Message(delay, amount) =>
      log.info(s"Slave ${self.path.name} has started! Delay is $delay s.")
      context.system.scheduler.scheduleOnce(delay seconds, sender(), Response)
  }
}
