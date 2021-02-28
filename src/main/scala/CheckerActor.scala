import java.util
import java.util.Date
import akka.actor._

import scala.collection.mutable
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

abstract class Tick

case class CheckerTick() extends Tick

// TODO: fix when there is a Leader (!=0) and we start 0 (that leader will consider that 0 is the Leader !!!)
class CheckerActor(val id: Int, val terminaux: List[Terminal], electionActor: ActorRef) extends Actor {

  var time: Int = 1100
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
      //      this.father ! Message(f"${nodeId} is alive")
      datesForChecking(nodeId) = new Date()
    }

    case IsAliveLeader(nodeId) => {
      //      this.father ! Message(f"Leader ${nodeId} is alive")
      if (this.leader == -1) { // sinon ça ne marche pas si on relance le 0
        this.leader = nodeId
      }
      datesForChecking(nodeId) = new Date()
    }

    case LeaderChanged(nodeId) => {
      if (this.leader == -1) { // sinon ça ne marche pas si on relance le 0
        this.leader = nodeId
      }
      father ! Message("I am the new leader")
    }

    // A chaque fois qu'on recoit un CheckerTick : on verifie qui est mort ou pas
    // Objectif : lancer l'election si le leader est mort
    case CheckerTick => {
      this.lastDate = new Date()

      // la liste des morts
      val deadNodes = this.datesForChecking.filter(x =>
        Math.abs(this.lastDate.getTime - x._2.getTime) > this.time
      )

      // mise à jour de la liste des vivants
      this.nodesAlive = this.datesForChecking.keys.toList.filter(p => !deadNodes.contains(p))
      // supprimer les morts de la map
      this.datesForChecking --= deadNodes.keys

      // affichage
      //      if (deadNodes.size + this.nodesAlive.size > 0) {
      //        this.father ! Message(f"---------- Updating status (I am ${this.id}${if (this.id == this.leader) " [Leader] " else ""}) ----------")
      //        this.father ! Message(f"my leader: ${this.leader}")
      //      }
      //      deadNodes.keys.foreach(dead => {
      //        if (dead == this.leader) {
      //          this.father ! Message(f"Leader ${dead} disconnected")
      //        } else {
      //          this.father ! Message(f"Node ${dead} disconnected")
      //        }
      //      })
      //      this.nodesAlive.foreach(x => {
      //        if (x == this.id) {
      //          if (x == this.leader) {
      //            this.father ! Message(f"Me,Leader ${x} is alive")
      //          } else {
      //            this.father ! Message(f"Me, ${x} is alive")
      //          }
      //        } else {
      //          if (x == this.leader) {
      //            this.father ! Message(f"Leader ${x} is alive")
      //          } else {
      //            this.father ! Message(f"${x} is alive")
      //          }
      //        }
      //      })

      // affichage 2
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

      // si le leader est mort => lancer l'election ou pas de leader
      //      if (!this.nodesAlive.contains(this.leader)) { => error
      if (deadNodes.contains(this.leader)) {
        this.father ! Message(f"We start the election because the Leader ${this.leader} has disconnected")
        this.leader = -1
        electionActor ! StartWithNodeList(this.nodesAlive ::: List(this.id))
        //        electionActor ! StartWithNodeList(this.nodesAlive)
      }

      // re-programmer un autre CheckerTick après time (ms)
      context.system.scheduler.scheduleOnce(time milliseconds, self, CheckerTick)
    }

  }
}
