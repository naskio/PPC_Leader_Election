import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

import akka.actor._

sealed trait BeatMessage

case class Beat(id: Int) extends BeatMessage

case class BeatLeader(id: Int) extends BeatMessage

case class BeatTick() extends Tick

case class LeaderChanged(nodeId: Int)

class BeatActor(val id: Int) extends Actor {

  val time: Int = 1000 // envoyer un Beat chaque time (ms)
  val father = context.parent
  var leader: Int = 0 // on estime que le premier Leader est 0

  def receive = {

    // Initialisation
    case Start => {
      self ! BeatTick
    }

    // Prevenir tous les autres nodes qu'on est en vie
    case BeatTick => {
      // envoyer un Beat ou BeatLeader selon le cas
      if (this.id == this.leader) {
        this.father ! BeatLeader(this.id)
      } else {
        this.father ! Beat(this.id)
      }
      // re-programmer un autre BeatTick après time (ms)
      context.system.scheduler.scheduleOnce(time milliseconds, self, BeatTick)
    }

    // mise à jour de leader
    case LeaderChanged(nodeId) => {
      this.leader = nodeId
      this.father ! Message(f"New leader ${nodeId}")
    }

  }
}
