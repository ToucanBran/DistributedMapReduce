package common
import akka.actor.ActorRef
case class Book(title: String, url: String)
case class BookJob(book: Book, replyTo: ActorRef)
case class WordJob(word:String, title: String, replyTo: ActorRef)
case class Results(counts: String)
case object Flush
case object Done
