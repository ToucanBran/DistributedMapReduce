package client
import common._
import akka.actor.Actor
import akka.actor.ActorSystem
import akka.actor.Address
import akka.actor.Props
import akka.actor.RelativeActorPath
import akka.actor.RootActorPath
import akka.cluster.Cluster
import akka.cluster.ClusterEvent._
import akka.cluster.MemberStatus
import scala.concurrent.duration._
import java.util.concurrent.ThreadLocalRandom
import org.slf4j.LoggerFactory;

class CloudClient(servicePath: String) extends Actor {
  val cluster = Cluster(context.system)
  val servicePathElements = servicePath match {
    case RelativeActorPath(elements) => elements
    case _ => throw new IllegalArgumentException(
      "servicePath [%s] is not a valid relative actor path" format servicePath)
  }
  import context.dispatcher
  val tickTask = context.system.scheduler.schedule(2.seconds, 2.seconds, self, "tick")
  val logger = LoggerFactory.getLogger(classOf[CloudClient])
  var nodes = Set.empty[Address]
  var resultsRecd = 0
  var jobsSent = false
  override def preStart(): Unit = {
    logger.info("Prestart!")
    cluster.subscribe(self, classOf[MemberEvent], classOf[ReachabilityEvent])
  }
  override def postStop(): Unit = {
    cluster.unsubscribe(self)
    tickTask.cancel()
  }

  val jobs = List (
     Book("A Tale of Two Cities", "http://reed.cs.depaul.edu/lperkovic/csc536/homeworks/gutenberg/pg98.txt"),
       Book("The Pickwick Papers", "http://reed.cs.depaul.edu/lperkovic/csc536/homeworks/gutenberg/pg580.txt"),
       Book("A Child's History of England", "http://reed.cs.depaul.edu/lperkovic/csc536/homeworks/gutenberg/pg699.txt"),
       Book("The Old Curiosity Shop", "http://reed.cs.depaul.edu/lperkovic/csc536/homeworks/gutenberg/pg700.txt"),
       Book("Oliver Twist", "http://reed.cs.depaul.edu/lperkovic/csc536/homeworks/gutenberg/pg730.txt"),
       Book("David Copperfield", "http://reed.cs.depaul.edu/lperkovic/csc536/homeworks/gutenberg/pg766.txt"),
       Book("Hunted Down", "http://reed.cs.depaul.edu/lperkovic/csc536/homeworks/gutenberg/pg807.txt"),
       Book("Dombey and Son", "http://reed.cs.depaul.edu/lperkovic/csc536/homeworks/gutenberg/pg821.txt"),
       Book("Our Mutual Friend", "http://reed.cs.depaul.edu/lperkovic/csc536/homeworks/gutenberg/pg883.txt"),
       Book("Little Dorrit", "http://reed.cs.depaul.edu/lperkovic/csc536/homeworks/gutenberg/pg963.txt"),
       Book("Life And Adventures Of Martin Chuzzlewit", "http://reed.cs.depaul.edu/lperkovic/csc536/homeworks/gutenberg/pg967.txt"),
       Book("The Life And Adventures Of Nicholas Nickleby", "http://reed.cs.depaul.edu/lperkovic/csc536/homeworks/gutenberg/pg968.txt"),
       Book("Bleak House", "http://reed.cs.depaul.edu/lperkovic/csc536/homeworks/gutenberg/pg1023.txt"),
       Book("Great Expectations", "http://reed.cs.depaul.edu/lperkovic/csc536/homeworks/gutenberg/pg1400.txt"),
       Book("A Christmas Carol", "http://reed.cs.depaul.edu/lperkovic/csc536/homeworks/gutenberg/pg19337.txt"),
       Book("The Cricket on the Hearth", "http://reed.cs.depaul.edu/lperkovic/csc536/homeworks/gutenberg/pg20795.txt"),
  )

  // Decided to put this in the receive so we only send when nodes are not empty. Also wanted
  // to make sure that jobs are only sent once so that's why the jobsSent val is there.
  def receive = {
    case "tick" if nodes.nonEmpty && !jobsSent =>
      jobsSent = true
      val address = nodes.toIndexedSeq(ThreadLocalRandom.current.nextInt(nodes.size))
      val service = context.actorSelection(RootActorPath(address) / servicePathElements)
      logger.info(s"Sending ${jobs.length} job(s)")
      println(s"TOTAL NODES: ${nodes.size}")
      jobs.foreach(service ! _)
      service ! Flush      
    case result: Results =>
      logger.info(s"Received Result: $result")
      resultsRecd += 1
      logger.info(s"Received ${resultsRecd} so far")
    case state: CurrentClusterState =>
      nodes = state.members.collect {
        case m if m.hasRole("compute") && m.status == MemberStatus.Up => m.address
      }
    case MemberUp(m) if m.hasRole("compute")        => nodes += m.address
    case other: MemberEvent                         => nodes -= other.member.address
    case UnreachableMember(m)                       => nodes -= m.address
    case ReachableMember(m) if m.hasRole("compute") => nodes += m.address
  }
}

object CloudClientOneMaster {
  def main(args: Array[String]): Unit = {
    // note that client is not a compute node, role not defined
    val system = ActorSystem("ClusterSystem")
    system.actorOf(Props(classOf[CloudClient], "/user/mapReduceServiceProxy"), "client")
  }
}
