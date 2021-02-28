import java.util
import java.util.Date
import akka.actor._

import scala.collection.mutable
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

abstract class Tick

case class CheckerTick() extends Tick

class CheckerActor(val id: Int, val terminaux: List[Terminal], electionActor: ActorRef) extends Actor {

  var time: Int = 2500
  val father = context.parent

  var nodesAlive: List[Int] = List()
  var datesForChecking: mutable.Map[Int, Date] = mutable.Map[Int, Date]()
  var lastDate: Date = null

  var leader: Int = -1

  def receive = {

    // Initialisation
    case Start => {
      self ! CheckerTick
    }

    // A chaque fois qu'on recoit un Beat : on met a jour la liste des nodes
    case IsAlive(nodeId) => {
      datesForChecking(nodeId) = new Date()
    }

    case IsAliveLeader(nodeId) => {
      //      if (this.leader != -1 && this.leader != nodeId) {
      //        // TODO: test this
      //        this.father ! Message(f"(at IsAliveLeader) but ${this.leader} != ${nodeId}")
      //      }
      //      if (this.leader == -1) {
      //        this.leader = nodeId
      //      }
      //      this.leader = nodeId
      //      datesForChecking(nodeId) = new Date()

      if (this.leader != -1 && this.leader != nodeId) {
        this.father ! Message(f"(DIFF) ${this.leader} != ${nodeId}")
      }
      if (this.leader == -1) {
        this.father ! Message("No leader yet!")
      }
      this.leader = nodeId
      //      datesForChecking(nodeId) = new Date()
      self ! IsAlive(nodeId)
    }

    // A chaque fois qu'on recoit un CheckerTick : on verifie qui est mort ou pas
    // Objectif : lancer l'election si le leader est mort
    case CheckerTick => {
      this.lastDate = new Date()

      // la liste des morts
      val deadNodes = datesForChecking.filter({
        case (_, d) => Math.abs(this.lastDate.getTime - d.getTime) >= this.time
      })

      // mise à jour de la liste des vivants
      this.nodesAlive = this.nodesAlive.filter(p => !deadNodes.contains(p))

      // supprimer les morts de la map
      this.datesForChecking --= deadNodes.keys

      // si le leader est mort => lancer l'election
      if (deadNodes.contains(this.leader)) {
        this.leader = -1
        this.father ! Message("Leader is dead, starting the election")
        electionActor ! StartWithNodeList(this.nodesAlive)
      }

      // re-programmer un autre CheckerTick après time (ms)
      context.system.scheduler.scheduleOnce(time milliseconds, self, CheckerTick)
    }

  }
}
