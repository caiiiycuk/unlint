package controllers

import play.api._
import play.api.mvc._

import advice.engine.AdviceEngine
import advice.engine.AdviceRequests

import scala.concurrent.Future
import play.api.libs.ws._
import play.api.libs.concurrent._
import play.api.libs.concurrent.Execution.Implicits._
import com.ning.http.client.Realm._

import play.api.libs.json._

object Application extends Controller {
  val extensionPattern = """^.+\.([^.]*)$""".r
  
  def index = Action {
    Ok(views.html.index())
  }

  def changes = Action(parse.json) { request =>
    try {
      val url = (request.body \ "url").as[String]
      val username = (request.body \ "username").as[String]
      val password = (request.body \ "password").as[String]

      val changesRequest = AdviceRequests.changes(url)
      changesRequest.withAuth(username, password)

      val promise = changesRequest.get()

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
  }

  def raw = Action(parse.json) { request =>
    try {
      val url = (request.body \ "url").as[String]
      val username = (request.body \ "username").as[String]
      val password = (request.body \ "password").as[String]

      val rawRequest = AdviceRequests.raw(url)
      rawRequest.withAuth(username, password)

      val promise = rawRequest.get()

      Async {
        promise.map { response =>
          Ok(response.body)
        }
      }
    } catch {
      case e: Throwable =>      
        BadRequest(e.getMessage)
    }
  }

  def analyze = Action(parse.json) { request =>
    try {
      val filename = (request.body \ "filename").as[String]
      val source = (request.body \ "source").as[String]

      filename match {
        case extensionPattern(extension) => 
          val advices = AdviceEngine.analyze(filename, extension, source)
          Ok(<advice>{ advices.map(a => a) }</advice>)
        case _ => 
          Ok(<advice></advice>)
      }
    } catch {
      case e: Throwable =>      
        BadRequest(e.getMessage)
    }
  }

}