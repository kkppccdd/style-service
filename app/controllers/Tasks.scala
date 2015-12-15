package controllers

import javax.inject.Inject
import scala.util.{ Failure, Success }
import scala.concurrent.Future
import play.api.Logger
import play.api.mvc._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.functional.syntax._
import play.api.libs.json._
import reactivemongo.bson.BSONDocument
import play.modules.reactivemongo.{ // ReactiveMongo Play2 plugin
  MongoController,
  ReactiveMongoApi,
  ReactiveMongoComponents
}
import play.modules.reactivemongo.json._
import play.modules.reactivemongo.json.collection._
import reactivemongo.bson.BSONObjectID
import me.raycai.style.service.model.Task
import me.raycai.style.service.model.JsonFormats._
import com.google.common.io.Files
import org.apache.commons.lang3.exception.ExceptionUtils
import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.services.s3.AmazonS3Client
import com.amazonaws.services.s3.model.PutObjectRequest
import com.amazonaws.AmazonClientException
import java.text.SimpleDateFormat
import java.util.Date
import com.amazonaws.AmazonServiceException
import me.raycai.style.service.model.Task
import com.amazonaws.services.s3.model.ObjectMetadata
import com.amazonaws.services.sqs.model._
import me.raycai.play.aws.SQSSender
import me.raycai.play.aws.SQSSender
import me.raycai.play.aws.SQSSender
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import me.raycai.play.aws.SQSReceiver

class Tasks @Inject() (val reactiveMongoApi: ReactiveMongoApi)
    extends Controller with MongoController with ReactiveMongoComponents {
  implicit val app = play.api.Play.current

  private val s3BucketName = app.configuration.getString("s3.bucket.name").get
  private val s3Client = {
    val awsAccessKey = app.configuration.getString("aws.access.key").get
    val awsAccessSecret = app.configuration.getString("aws.access.secret").get

    val credential = new BasicAWSCredentials(awsAccessKey, awsAccessSecret)

    new AmazonS3Client(credential);
  }

  private val sqsSender = {
    val sqsQueneUrl = app.configuration.getString("sqs.queue.request.url").get

    new SQSSender(sqsQueneUrl)
  }

  private val sqsReceiver = {
    val sqsQueneUrl = app.configuration.getString("sqs.queue.completion.url").get

    new SQSReceiver(sqsQueneUrl, { message: Message =>
      {
        Logger.debug(message.getBody)
        val task = objectMapper.readValue(message.getBody, classOf[Task])

        val selector = BSONDocument("_id" -> task.id)
        val modifier = BSONDocument(
          "$set" -> BSONDocument(
            "outputImageUrl" -> task.outputImageUrl.get))
        collection.update(selector, modifier) onComplete {
          case Failure(e) =>
            Logger.error(e.getMessage, e)
          case Success(lastError) => {
            Logger.info(s"Updated task#$task.id successfully.")
          }
        }
      }
    })
  }

  private val objectMapper = {
    val mapper = new ObjectMapper()
    mapper.registerModule(DefaultScalaModule)

    mapper
  }

  private val dateFormator = new SimpleDateFormat("yyyyMMdd")

  def collection: JSONCollection = db.collection[JSONCollection]("tasks")

  def createTask = Action.async(parse.multipartFormData) {
    request =>
      request.body.file("content-image").map { contentImage =>
        import java.io.File
        val filename = contentImage.filename
        val contentType = contentImage.contentType.get
        val style = request.body.asFormUrlEncoded.get("style").map(_.head).get

        try {
          val taskId = BSONObjectID.generate.stringify
          // upload image to s3
          val key = dateFormator.format(new Date()) + s"/$taskId/content-$filename"
          val metadata = new ObjectMetadata()
          metadata.setContentType(contentType)
          val putObjectRequest = new PutObjectRequest(s3BucketName, key, contentImage.ref.file)
          putObjectRequest.setMetadata(metadata)

          val result = s3Client.putObject(putObjectRequest)

          // create task record
          val task = new Task(taskId, style, s"http://$s3BucketName.s3.amazonaws.com/$key", None)

          collection.insert(task) map {
            lastError =>
              Logger.debug(s"Successfully inserted with LastError: $lastError")

              // send event
              val messageBody = objectMapper.writeValueAsString(task)
              sqsSender.sendMessage(messageBody).map { messageId =>
                Ok(Json.toJson(task))
              }.getOrElse {
                InternalServerError("Send event to sqs queue failed")
              }

          }

        } catch {
          case ex: AmazonClientException =>
            Logger.error(ex.getMessage, ex)
            Future.successful(BadRequest(s"File ($filename) uploaded failed"))
          case ex: AmazonServiceException =>
            Logger.error(ex.getMessage, ex)
            Future.successful(BadRequest(s"File ($filename) uploaded failed"))
        }

      }.getOrElse {
        Future.successful(BadRequest("Content image missed"))
      }
  }

  def getTask(id: String) = Action.async(parse.anyContent) {
    request =>
      val selector = BSONDocument("_id" -> id)
      val future = collection.find(selector).one[Task]
      for {
        maybeTask <- future
        result <- maybeTask.map { task => Future(Ok(Json.toJson(task))) }.getOrElse(Future(NotFound))
      } yield result

  }
}

