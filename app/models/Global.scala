import scala.io.Source

import play.api._
import play.api.Play.current
import play.api.libs.json._

// you need this import to have combinators
import play.api.libs.functional.syntax._
import play.api.libs.json.util._


import java.util.Date
import anorm.{Id, Pk, NotAssigned}

object Global extends GlobalSettings {

  implicit object DateRead extends Reads[Date] {
    val format = new java.text.SimpleDateFormat("yyyy-MM-dd")
    def reads(json:JsValue): JsResult[Date] = 
      JsSuccess(format.parse(json.as[String]))
  }

  implicit object PkRead extends Reads[Pk[Long]] {
    def reads(json: JsValue) = json match {
      case JsNumber(num) => JsSuccess(Id(num.longValue))
      case _ => JsSuccess(NotAssigned)
    }
  }

  implicit val computerReads = (
    (__ \ "id").read[Pk[Long]] and
    (__ \ "name").read[String] and
    (__ \ "introduced").readNullable[Date] and
    (__ \ "discontinued").readNullable[Date] and
    (__ \ "companyId").readNullable[Long] 
  )( (id, name, introduced, discontinued, companyId) => models.Computer(id, name, introduced, discontinued, companyId)) 


  override def onStart(app: Application) {
    val f = Play.getFile("/conf/datas/computers.json")    
    val src = Source.fromFile(f)
    val lines = src.getLines

    val json: JsValue = Json.parse(src.getLines.mkString)
    val computers = json.validate[List[models.Computer]].get

    computers.foreach( (c) => models.Computer.insert(c))

    Logger.info("Application has started with (%d) computers" format(models.Computer.allComputers.size))
  }  
     
  override def onStop(app: Application) {
    models.Computer.deleteAll
  }
}