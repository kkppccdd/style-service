package me.raycai.play.aws

import scala.collection.JavaConversions._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import scala.concurrent.duration._
import com.amazonaws.services.sqs.AmazonSQSClient
import com.amazonaws.services.sqs.model._
import com.amazonaws.auth.BasicAWSCredentials
import play.api.Logger
import akka.actor.Actor
import akka.actor.Props
import play.api.libs.concurrent.Akka
import play.modules.reactivemongo.ReactiveMongoApi

class SQSReceiveActor(sqsClient: AmazonSQSClient, queueUrl: String, f: (Message) => Unit) extends Actor {
  def receive = {
    case "poll" =>
      Logger.debug(s"Polling message from $queueUrl")
      sqsClient.receiveMessage(queueUrl).getMessages.foreach {
        message: Message =>
          {
            f(message)
            sqsClient.deleteMessage(new DeleteMessageRequest().withQueueUrl(queueUrl).withReceiptHandle(message.getReceiptHandle))
          }
      }

  }
}

class SQSReceiver(queueUrl: String, f: (Message) => Unit){
  implicit val app = play.api.Play.current
  private val sqsClient = {
    val awsAccessKey = app.configuration.getString("aws.access.key").get
    val awsAccessSecret = app.configuration.getString("aws.access.secret").get

    val credential = new BasicAWSCredentials(awsAccessKey, awsAccessSecret)

    new AmazonSQSClient(credential)
  }

  val receiveActor = {
    Akka.system.actorOf(Props(new SQSReceiveActor(sqsClient, queueUrl, f)), "sqs.queue.receiver")
  }

  Akka.system.scheduler.schedule(FiniteDuration(0, SECONDS), FiniteDuration(60, SECONDS), receiveActor, "poll")

}

class SQSSender(queueUrl: String) {
  implicit val app = play.api.Play.current
  private val sqsClient = {
    val awsAccessKey = app.configuration.getString("aws.access.key").get
    val awsAccessSecret = app.configuration.getString("aws.access.secret").get

    val credential = new BasicAWSCredentials(awsAccessKey, awsAccessSecret)

    new AmazonSQSClient(credential)
  }

  def sendMessage(messageBody: String): Option[String] = {
    try {
      val result = sqsClient.sendMessage(queueUrl, messageBody)
      return Some(result.getMessageId)
    } catch {
      case ex: Exception =>
        Logger.error(ex.getMessage, ex)
        None
    }
  }
}