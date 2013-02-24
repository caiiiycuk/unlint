package caiiiycuk.github.com.controller

import scala.concurrent.ExecutionContext
import scala.concurrent.future

import com.ning.http.client.AsyncHttpClient
import com.ning.http.client.Realm.AuthScheme
import com.ning.http.client.Realm.RealmBuilder

import caiiiycuk.github.com.engine.AdviceChecks
import caiiiycuk.github.com.engine.AdviceEngine
import caiiiycuk.github.com.ws.WS
import xitrum.SockJsHandler
import xitrum.util.Json

case class UnlintRequest(uuid: Long, action: String, data: String)
case class DownloadRequest(url: String, username: String, password: String)
case class AnalyzeRequest(filename: String, source: String)

class UnlintSock extends SockJsHandler {
  implicit val ec: ExecutionContext =
    xitrum.Config.actorSystem.dispatcher

  val SKIPPED = "/*SKIPPED*/"
  val BIG_FILE = "/*BIG FILE*/"
  val MAX_SIZE = 1024 * 70 /* 70Kb */

  val extensionPattern = """^.+\.([^.]*)$""".r

  def onOpen(session: Map[String, Any]) {
  }

  def onMessage(message: String) {
    try {
      val request = Json.parse[UnlintRequest](message)

      request match {
        case UnlintRequest(uuid, "changes", data) =>
          future {
            changesRequest(uuid, Json.parse[DownloadRequest](data))
          }
        case UnlintRequest(uuid, "raw", data) =>
          future {
            rawRequest(uuid, Json.parse[DownloadRequest](data))
          }
        case UnlintRequest(uuid, "analyze", data) =>
          future {
            analyzeRequest(uuid, Json.parse[AnalyzeRequest](data))
          }
        case _ =>
          close()
      }
    } catch {
      case e: Throwable =>
        logger.error(e.getMessage, e)
        close()
    }
  }

  def changesRequest(uuid: Long, request: DownloadRequest) {
    val get = prepare(request)
    val data = get.execute().get().getResponseBody();
    send(Json.generate(Map("uuid" -> uuid, "data" -> data)))
  }

  def rawRequest(uuid: Long, request: DownloadRequest) {
    val extension = request.url match {
      case url if url.endsWith(".min.js") || url.endsWith(".min.css") =>
        ""
      case extensionPattern(extension) =>
        extension
      case _ =>
        ""
    }

    val checks = AdviceChecks.checks.getOrElse(extension, List())

    if (checks.isEmpty) {
      send(Json.generate(Map("uuid" -> uuid, "data" -> SKIPPED)))
    } else {
      val get = prepare(request)
      val data = get.execute().get().getResponseBody();
      if (data.length() > MAX_SIZE) {
        send(Json.generate(Map("uuid" -> uuid, "data" -> BIG_FILE)))
      } else {
        send(Json.generate(Map("uuid" -> uuid, "data" -> data)))
      }
    }
  }

  def analyzeRequest(uuid: Long, request: AnalyzeRequest) {
    val filename = request.filename
    val source = request.source

    if (SKIPPED == source) {
      send(Json.generate(Map("uuid" -> uuid, "data" -> "not checked")))
      return ;
    }

    if (BIG_FILE == source) {
      send(Json.generate(Map("uuid" -> uuid, "data" -> "WARN: SKIPPED (BIG FILE)")))
      return ;
    }

    val data =
      filename match {
        case extensionPattern(extension) =>
          val advices = AdviceEngine.analyze(filename, extension, source)
          val xml = <advice>{ advices.map(a => a) }</advice>
          xml.toString
        case _ =>
          "not checked"
      }

    send(Json.generate(Map("uuid" -> uuid, "data" -> data)))
  }

  def onClose() {
  }

  def prepare(request: DownloadRequest): AsyncHttpClient#BoundRequestBuilder = {
    val get = WS.prepareGet(request.url)
    WS.basicAuth(get, request.username, request.password)
  }
}
