package controllers

import org.specs2.mutable._
import org.specs2.runner.JUnitRunner
import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.services.s3.AmazonS3Client
import com.amazonaws.services.s3.model.PutObjectRequest
import java.io.File
import org.junit.runner.RunWith

@RunWith(classOf[JUnitRunner])
class TasksTest extends Specification {
  private val accessKey = "AKIAJJSPPIELQLNXEXHQ"
  private val accessSecret = "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"
  private val bucketName="ideawork-service-dev"
  "Upload image to S3" should {
    "Sucessful" in {
      val credential = new BasicAWSCredentials(accessKey,accessSecret)
       val s3Client = new AmazonS3Client(credential);
      
      val putObjectRequest = new PutObjectRequest(bucketName,"unittest/temp-1.jpg",new File("test/controllers/tmp-1.jpg"))
      val result = s3Client.putObject(putObjectRequest)
      
      result.getContentMd5 must beNull
    }
  }
}