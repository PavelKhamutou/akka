


/*
then do a simulator of load balancer
1 master
multiple slaves
master can start slave
and can give it a task
slave reports when its done
you have up to 5 slaves
and you ask user from console to schedule a task
task can take random (10;20) seconds to complete
when slave completes task it reports to master that it's ready
if all slaves are buzy and there are less than 5 upon new task master starts new slave
if all slaves are buzy and master has to schedule a task it waits until one of the slaves reports its done
then the first task goes to it
Павел Хомутов
30.09.2015 20:45
Павел Хомутов
yeap! looks awesome!
Piotr Kałamucki
30.09.2015 20:45
Piotr Kałamucki
start with exchanging messages between master and slave
then introduce multiple slaves
then add random time after they complete their task
i think this is a way to go
Павел Хомутов
30.09.2015 20:45
Павел Хомутов
and what could be a task?
or just sleep(5)
Piotr Kałamucki
30.09.2015 20:46
Piotr Kałamucki
no
you dont sleep on actors
it blocks the execution
task is logging to console "yo, i am slave #number i got a task!"
and it is completed after random amount of time
Павел Хомутов
30.09.2015 20:47
Павел Хомутов
aha
Piotr Kałamucki
30.09.2015 20:47
Piotr Kałamucki
check akka schedule message uxing context.scheduler.scheduleOnce
Павел Хомутов
30.09.2015 20:47
Павел Хомутов
ok
Piotr Kałamucki
30.09.2015 20:47
Piotr Kałamucki
this will invoke message after given time
and is not blocking
 */

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
