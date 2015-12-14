package me.raycai.style.service.model

case class Task(private val _id:String, style:String,contentImageUrl:String,outputImageUrl:Option[String]){
  def id = _id
}

case class TaskSpec(style:String)

object JsonFormats {
  import play.api.libs.json.Json
  implicit val taskFormat = Json.format[Task]
}