package common

import scala.collection.mutable.HashMap

import akka.actor.{Actor, ActorRef}

class ReduceActor(service: ActorRef) extends Actor {
  var remainingMappers = 4
  var rMap = HashMap[String, List[String]]()
  // Just in case we have multiple clients asking for different books,
  // we should make sure to send back the words from books each one wanted
  var sendMap = HashMap[ActorRef, HashMap[String, List[String]]]()
  def receive = {
    case WordJob(word, title, replyTo) =>
      if(!sendMap.contains(replyTo))
        sendMap += (replyTo -> HashMap(word -> List(title)))
      else
      {
        var reduceMap = sendMap(replyTo)
        if (reduceMap.contains(word)) 
        {
          if (!reduceMap(word).contains(title))
            reduceMap += (word -> (title :: reduceMap(word)))
        }
        else
          reduceMap += (word -> List(title))
      }
    case Flush =>
      
      remainingMappers -= 1
      //println(s"Hi, reducer here who just received a flush ${self.path}. Remaining mappers: $remainingMappers")
      if (remainingMappers == 0) {
        for ((replyTo, reduceMap) <- sendMap) {
          replyTo ! Results(s"${self.path.toStringWithoutAddress}: $reduceMap")
        }
        service ! Done
      }
  }
}
