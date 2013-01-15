package controllers

import play.api._
import play.api.mvc._

import advice.engine.AdviceEngine
import advice.engine.AdviceRequest

import scala.concurrent.Future
import play.api.libs.ws._
import play.api.libs.concurrent._
import play.api.libs.concurrent.Execution.Implicits._

import play.api.libs.json._

object Application extends Controller {
  
  def index = Action {
    // AdviceEngine.advice("https://github.com/4geo/web-dev/pull/150")

    Ok(views.html.index())
  }

  def advice = Action { request =>
    Ok("ok")
    // request.body.asFormUrlEncoded.map { params =>
    //     try {
    //         val url = params.getOrElse("url", throw new IllegalArgumentException("url"))
    //         val username = params.getOrElse("username", throw new IllegalArgumentException("username"))
    //         val password = params.getOrElse("password", throw new IllegalArgumentException("password"))

    //         Ok("0")   
    //     } catch {
    //         case e =>
    //             BadRequest("ParameterNotSet: " + e.getMessage)
    //     }
    // } .getOrElse {
    //     BadRequest("Parameters not found")
    // }
  }

  def changes = Action(parse.json) { request =>
    // Logger.debug( request.body.asJson.mkString )
    try {
      val url = (request.body \ "url").as[String]
      val username = (request.body \ "username").as[String]
      val password = (request.body \ "password").as[String]

      val adviceRequest = AdviceRequest.fromUrl(url)
      adviceRequest.withAuth(username, password)

      val promise = adviceRequest.getChanges()

      Async {
        promise.map { response =>
          val filenames = (response.json \\ "filename")
          val rawfiles = (response.json \\ "raw_url")
          Ok(Json.toJson(
            Map(
              "filename" -> Json.toJson(filenames),
              "rawfiles" -> Json.toJson(rawfiles)
            )
          ))
        }
      }
    } catch {
      case e: Throwable =>      
        BadRequest(e.getMessage)
    }

    
    // Ok("1")

    // request.body.asFormUrlEncoded.map { params =>
    //   Logger.debug(params.toString())  

    //   try {
    //     val url = params.getOrElse("url", throw new IllegalArgumentException("url"))
    //     val username = params.getOrElse("username", throw new IllegalArgumentException("username"))
    //     val password = params.getOrElse("password", throw new IllegalArgumentException("password"))

    //     Logger.debug(params.toString)

    //     // val request = AdviceRequest.fromUrl(url.toString)
    //     Ok("1")
    //   } catch {
    //     case e =>
    //       BadRequest(e.getMessage)
    //   }
    // } .getOrElse {
    //   BadRequest("Parameters not found")
    // }
  }

}