package common

import scala.concurrent.duration._
import akka.actor.Actor
import akka.actor.ActorRef
import akka.actor.Props
import akka.actor.ReceiveTimeout
import akka.routing.ConsistentHashingRouter.ConsistentHashableEnvelope
import akka.routing.FromConfig
import akka.routing.RoundRobinPool
import akka.cluster.routing.{ ClusterRouterPool, ClusterRouterPoolSettings }
import akka.routing.ConsistentHashingPool
import akka.routing.Broadcast
import com.typesafe.config.ConfigFactory

//#service
class MapReduceService extends Actor {
  val reducers = ConfigFactory.load.getInt("number-reducers")
  val mappers = ConfigFactory.load.getInt("number-mappers")
  val reduceActors = context.actorOf(ClusterRouterPool(ConsistentHashingPool(0), ClusterRouterPoolSettings(
    totalInstances = 1000, maxInstancesPerNode = reducers, allowLocalRoutees = true)).props(Props(classOf[ReduceActor], self)),
  name = "reducers")

  val mapActors = context.actorOf(ClusterRouterPool(RoundRobinPool(0), ClusterRouterPoolSettings(
    totalInstances = 1000, maxInstancesPerNode = mappers, allowLocalRoutees = true)).props(Props(classOf[MapActor], reduceActors)),
  name = "mappers")

  var pending = 16;
  
  def receive = {
    case msg: Book =>
      println("received job")
      mapActors ! BookJob(msg, sender)
    case Flush =>
      mapActors ! Broadcast(Flush)
    case Done =>
      pending -= 1
      if (pending == 0)
       context.system.terminate
  }
}

//#service
