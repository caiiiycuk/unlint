package caiiiycuk.github.com.controller

import java.util.concurrent.AbstractExecutorService
import java.util.concurrent.TimeUnit
import scala.concurrent.ExecutionContext
import scala.concurrent.future
import com.ning.http.client.AsyncHttpClient
import com.ning.http.client.AsyncHttpClientConfig
import com.ning.http.client.Realm.AuthScheme
import com.ning.http.client.Realm.RealmBuilder
import caiiiycuk.github.com.engine.AdviceEngine
import xitrum.Logger
import xitrum.Logger
import xitrum.SockJsHandler
import xitrum.util.Json
import caiiiycuk.github.com.engine.AdviceChecks
import com.sun.org.apache.xalan.internal.xsltc.compiler.Sort

case class UnlintRequest(uuid: Long, action: String, data: String)
case class DownloadRequest(url: String, username: String, password: String)
case class AnalyzeRequest(filename: String, source: String)

object AkkaExecutorService extends AbstractExecutorService {
  implicit val ec: ExecutionContext =
    xitrum.Config.actorSystem.dispatcher

  override def execute(r: Runnable) = ec.execute(r);

  override def isTerminated(): Boolean = false

  override def isShutdown(): Boolean = false

  override def shutdownNow(): java.util.List[Runnable] = throw new UnsupportedOperationException("Unable to shutdown AkkaExecutorService")

  override def shutdown(): Unit = throw new UnsupportedOperationException("Unable to shutdown AkkaExecutorService")

  override def awaitTermination(timeout: Long, unit: TimeUnit): Boolean = throw new UnsupportedOperationException()
}

object UnlintSock {
  val configBuilder = new AsyncHttpClientConfig.Builder()
  configBuilder.setAllowPoolingConnection(true)
  configBuilder.setMaximumConnectionsTotal(100)
  configBuilder.setConnectionTimeoutInMs(10000)
  configBuilder.setRequestTimeoutInMs(10000)
  configBuilder.setFollowRedirects(true)
  configBuilder.setExecutorService(AkkaExecutorService)

  val ws = new AsyncHttpClient(configBuilder.build())
}

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
    val get = UnlintSock.ws.prepareGet(request.url)

    request match {
      case DownloadRequest(_, "", "") =>
      case DownloadRequest(_, user, password) =>
        get.setRealm((new RealmBuilder()
          .setScheme(AuthScheme.BASIC)
          .setPrincipal(request.username)
          .setPassword(request.password)
          .setUsePreemptiveAuth(true).build()))
    }

    get
  }
}
