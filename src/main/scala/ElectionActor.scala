import akka.actor._

abstract class NodeStatus

case class Passive() extends NodeStatus

case class Candidate() extends NodeStatus

case class Dummy() extends NodeStatus

case class Waiting() extends NodeStatus

case class Leader() extends NodeStatus

abstract class LeaderAlgoMessage

case class Initiate() extends LeaderAlgoMessage

case class ALG(nodeId: Int) extends LeaderAlgoMessage

case class AVS(nodeId: Int) extends LeaderAlgoMessage

case class AVSRSP(nodeId: Int) extends LeaderAlgoMessage

case class StartWithNodeList(list: List[Int])

class ElectionActor(val id: Int, val terminaux: List[Terminal]) extends Actor {

  val father = context.parent
  var nodesAlive: List[Int] = List(id)

  var candSucc: Int = -1
  var candPred: Int = -1
  var status: NodeStatus = new Passive()

  def getRemoteById(nodeId: Int): ActorSelection = {
    this.terminaux.find(n => n.id == nodeId) match {
      case Some(n) => context.actorSelection("akka.tcp://LeaderSystem" + n.id + "@" + n.ip + ":" + n.port + "/user/Node")
      case None => null
    }
  }

  def getNeighbor(index: Int): Int = {
    var previousIndex = index
    var neighbor: Int = -1
    do {
      neighbor = (previousIndex + 1) % this.terminaux.length
      previousIndex = neighbor
    } while (!this.nodesAlive.contains(neighbor))
    neighbor
  }

  def receive = {

    // Initialisation
    case Start => {
      self ! Initiate
    }

    case StartWithNodeList(list) => {

      if (list.isEmpty) {
        this.nodesAlive = this.nodesAlive ::: List(id)
      }
      else {
        this.nodesAlive = list
      }

      self ! Initiate
    }

    case Initiate => {
      this.status = new Candidate()
      getRemoteById(this.getNeighbor(this.nodesAlive.indexOf(this.id))) ! ALG(id)
    }

    case ALG(init) => {
      if (this.status.equals(Passive())) {
        this.status = new Dummy()
        getRemoteById(this.getNeighbor(this.nodesAlive.indexOf(this.id))) ! ALG(id)
      }
      else if (this.status.equals(Candidate())) {

        this.candPred = init
        if (id > init) {
          if (this.candSucc == -1) {
            this.status = new Waiting()
            val nodeInit = getRemoteById(init)
            nodeInit ! AVS(id)
          }
          else {
            val nodeSucc = getRemoteById(this.candSucc)
            nodeSucc ! AVSRSP(this.candPred)
            this.status = new Dummy()
          }
        }

        if (init == id) {
          this.status = new Leader()
          father ! LeaderChanged(id)
        }
      }
    }

    case AVS(j) => {
      if (this.status.equals(Candidate())) {
        if (this.candPred == -1)
          this.candSucc = j
        else {
          val nodeJ = getRemoteById(j)
          nodeJ ! AVSRSP(this.candPred)
          this.status = new Dummy()
        }
      }
      else if (this.status.equals(Waiting()))
        this.candSucc = j
    }

    case AVSRSP(k) => {
      if (this.status.equals(Waiting())) {
        if (this.id == k)
          this.status = new Leader()
        else {
          this.candPred = k
          if (this.candSucc == -1) {
            if (k < this.id) {
              this.status = new Waiting()
              getRemoteById(k) ! AVS(this.id)
            }
          }
          else {
            this.status = new Dummy()
            getRemoteById(this.candSucc) ! AVSRSP(k)
          }
        }
      }
    }

  }
}