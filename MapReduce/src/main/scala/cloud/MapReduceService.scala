package cloud

import akka.actor.Actor
import akka.actor.Props
import akka.routing.RoundRobinPool
import akka.cluster.routing.{ ClusterRouterPool, ClusterRouterPoolSettings }
import akka.routing.ConsistentHashingPool
import akka.routing.Broadcast
import akka.actor.PoisonPill
import com.typesafe.config.ConfigFactory
import org.slf4j.LoggerFactory;
import common._

class MapReduceService extends Actor {
  val reducers = ConfigFactory.load.getInt("number-reducers")
  val mappers = ConfigFactory.load.getInt("number-mappers")
  val logger = LoggerFactory.getLogger(classOf[MapReduceService])
  val reduceActors = context.actorOf(ClusterRouterPool(ConsistentHashingPool(0), ClusterRouterPoolSettings(
    totalInstances = 1000, maxInstancesPerNode = reducers, allowLocalRoutees = true)).props(Props(classOf[ReduceActor], self)),
  name = "reducers")

  val mapActors = context.actorOf(ClusterRouterPool(RoundRobinPool(0), ClusterRouterPoolSettings(
    totalInstances = 1000, maxInstancesPerNode = mappers, allowLocalRoutees = true)).props(Props(classOf[MapActor], reduceActors)),
  name = "mappers")

  var pending = 16;
  
  def receive = {
    case msg: Book =>
      logger.info("Received job, forwarding to map actors pool")
      mapActors ! BookJob(msg, sender)
    case Flush =>
      logger.info("Received flush, forwarding to map actors pool")
      mapActors ! Broadcast(Flush)
    case Done =>
      pending -= 1
      if (pending == 0) 
      {
        // mapActors ! PoisonPill
        // reduceActors ! PoisonPill
        // sender ! PoisonPill
        // context.system.terminate
        logger.info("All jobs complete.")
      }
  }
}

