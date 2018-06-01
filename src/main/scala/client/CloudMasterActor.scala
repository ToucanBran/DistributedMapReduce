package client

import com.typesafe.config.ConfigFactory
import akka.actor.ActorSystem
import akka.actor.PoisonPill
import akka.actor.Props
import akka.cluster.singleton.ClusterSingletonManager
import akka.cluster.singleton.ClusterSingletonManagerSettings
import akka.cluster.singleton.ClusterSingletonProxy
import akka.cluster.singleton.ClusterSingletonProxySettings
import common._

object CloudMasterActor {
  def main(args: Array[String]): Unit = {
    if (args.isEmpty) {
      startup(Seq("0"))
      //CloudClientOneMaster.main(Array.empty)
    } else {
      startup(args)
    }
  }

  def startup(ports: Seq[String]): Unit = {
    println(ports)
    ports foreach { port =>
    println(port)
      // Override the configuration of the port when specified as program argument
      val config =
        ConfigFactory.parseString(s"""
          akka.remote.netty.tcp.port=$port
          akka.remote.artery.canonical.port=$port
          """).withFallback(
          ConfigFactory.parseString("akka.cluster.roles = [compute]")).
          withFallback(ConfigFactory.load("application"))

      val system = ActorSystem("ClusterSystem", config)

      system.actorOf(ClusterSingletonManager.props(
        singletonProps = Props[MapReduceService],
        terminationMessage = PoisonPill,
        settings = ClusterSingletonManagerSettings(system).withRole("compute")),
        name = "mapReduceService")

      system.actorOf(ClusterSingletonProxy.props(singletonManagerPath = "/user/mapReduceService",
        settings = ClusterSingletonProxySettings(system).withRole("compute")),
        name = "mapReduceServiceProxy")
    }
  }
}
