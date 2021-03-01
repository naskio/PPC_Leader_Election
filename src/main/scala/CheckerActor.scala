import java.util
import java.util.Date
import akka.actor._

import scala.collection.mutable
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

abstract class Tick

case class CheckerTick() extends Tick

class CheckerActor(val id: Int, val terminaux: List[Terminal], electionActor: ActorRef) extends Actor {

  var time: Int = 500
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
      if (this.leader == -1 || this.leader == 0) { // cas particulier lorsque on relance le 0, car au debut nous avons estimé que 0 est le leader, donc on remplace si l'estimation est fausse
        this.leader = nodeId
      }
      datesForChecking(nodeId) = new Date()
    }

    case LeaderChanged(nodeId) => {
      this.leader = nodeId
      if (this.id == nodeId) {
        father ! Message("I am the new leader")
      } else {
        father ! Message(f"${nodeId} is the new leader")
      }
    }

    // A chaque fois qu'on recoit un CheckerTick : on verifie qui est mort ou pas
    // Objectif : lancer l'election si le leader est mort
    case CheckerTick => {
      this.lastDate = new Date()

      // la liste des morts
      val deadNodes = this.datesForChecking.filter(x =>
        Math.abs(this.lastDate.getTime - x._2.getTime) > this.time * 2
      )

      // mise à jour de la liste des vivants
      this.nodesAlive = this.datesForChecking.keys.toList.filter(p => !deadNodes.contains(p))
      // supprimer les morts de la map
      this.datesForChecking --= deadNodes.keys

      // affichage
      if (deadNodes.size + this.nodesAlive.size > 0) {
        this.father ! Message(f"...")
      }
      deadNodes.keys.foreach(dead => {
        if (dead == this.leader) {
          this.father ! Message(f"Leader ${dead} has disconnected")
        } else {
          this.father ! Message(f"Node ${dead} has disconnected")
        }
      })
      this.nodesAlive.foreach(x => {
        if (x == this.leader) {
          this.father ! Message(f"Leader ${x} is alive")
        } else {
          this.father ! Message(f"${x} is alive")
        }
      })

      // si le leader est mort => lancer l'election
      if (deadNodes.contains(this.leader)) {
        this.father ! Message(f"We start the election because the previous leader ${this.leader} has disconnected")
        this.leader = -1
        electionActor ! StartWithNodeList(this.nodesAlive ::: List(this.id))
      }

      // re-programmer un autre CheckerTick après time (ms)
      context.system.scheduler.scheduleOnce(time milliseconds, self, CheckerTick)
    }

  }
}
