package common

import scala.collection.mutable.HashSet
import akka.routing.ConsistentHashingRouter.ConsistentHashableEnvelope
import scala.io.Source
import akka.actor.{Actor, ActorRef}
import akka.routing.Broadcast
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class MapActor(reduceActors: ActorRef) extends Actor {
  val logger = LoggerFactory.getLogger(classOf[MapActor])
  val STOP_WORDS_LIST = List("a", "am", "an", "and", "are", "as", "at", "be",
    "do", "go", "if", "in", "is", "it", "of", "on", "the", "to")
  def receive = {
    case BookJob(book, replyTo) =>
      logger.info(s"${self.path.name}: Processing ${book.title}")
      process(book, replyTo)
    case Flush => 
      reduceActors ! Broadcast(Flush)
  }

  // Process book
  def process(book: Book, replyTo: ActorRef) = {
    val title = book.title
    val url = book.url
    val content = getContent(url)
    var namesFound = HashSet[String]()
    for (word <- content.split("[\\p{Punct}\\s]+")) {
      if ((!STOP_WORDS_LIST.contains(word)) && word(0).isUpper && !namesFound.contains(word)) {
	        reduceActors ! ConsistentHashableEnvelope(message = WordJob(word, title, replyTo), hashKey = word)
        namesFound += word
      }
    }
  }

  // Get the content at the given URL and return it as a string
  def getContent( url: String ) = {
    try {
      Source.fromURL(url).mkString
    } catch {     // If failure, just return an empty string
      case e: Exception => 
        logger.error("Couldn't get content from $url")
        ""
    }
  }
}
