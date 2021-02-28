import java.util
import java.util.Date
import akka.actor._

import scala.collection.mutable
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

abstract class Tick

case class CheckerTick() extends Tick

class CheckerActor(val id: Int, val terminaux: List[Terminal], electionActor: ActorRef) extends Actor {

  var time: Int = 1000
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
      //      if (this.leader == -1 || this.leader != 0) {
      if (this.leader == -1) {
        this.leader = nodeId
      }
      datesForChecking(nodeId) = new Date()
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
      this.father ! Message("---------------------------------------------")
      deadNodes.keys.foreach(dead => {
        if (dead == this.leader) {
          this.father ! Message(f"Leader ${dead} disconnected")
        } else {
          this.father ! Message(f"Node ${dead} disconnected")
        }
      })
      this.nodesAlive.foreach(x => {
        if (x == this.id) {
          if (x == this.leader) {
            this.father ! Message(f"Me,Leader ${x} is alive")
          } else {
            this.father ! Message(f"Me, ${x} is alive")
          }
        } else {
          if (x == this.leader) {
            this.father ! Message(f"Leader ${x} is alive")
          } else {
            this.father ! Message(f"${x} is alive")
          }
        }
      })

      // si le leader est mort => lancer l'election ou pas de leader
      //      if (!this.nodesAlive.contains(this.leader)) { => error
      if (deadNodes.contains(this.leader)) {
        this.leader = -1
        this.father ! Message("Since the Leader is dead, we start the election")
        electionActor ! StartWithNodeList(this.nodesAlive ::: List(this.id))
        //        electionActor ! StartWithNodeList(this.nodesAlive)
      }

      // re-programmer un autre CheckerTick après time (ms)
      context.system.scheduler.scheduleOnce(time milliseconds, self, CheckerTick)
    }

  }
}
