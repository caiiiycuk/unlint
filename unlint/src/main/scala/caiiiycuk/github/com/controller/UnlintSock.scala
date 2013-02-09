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
import xitrum.SockJsHandler
import xitrum.util.Json

case class UnlintRequest(uuid: Long, action: String, data: String)
case class ProxyRequest(url: String, username: String, password: String)
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

  val extensionPattern = """^.+\.([^.]*)$""".r

  def onOpen(session: Map[String, Any]) {
  }

  def onMessage(message: String) {
    try {
	    val request = Json.parse[UnlintRequest](message)
	
	    request match {
	      case UnlintRequest(uuid, "proxy", data) =>
	        future {
	        	proxyRequest(uuid, Json.parse[ProxyRequest](data))
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

  def proxyRequest(uuid: Long, request: ProxyRequest) {
    val get = UnlintSock.ws.prepareGet(request.url)

    request match {
      case ProxyRequest(_, "", "") =>
      case ProxyRequest(_, user, password) =>
    
   get.setRealm((new RealmBuilder()
      .setScheme(AuthScheme.BASIC)
          .setPrincipal(request.username)
          .setPassword(request.password)
      .setUsePreemptiveAuth(true).build()))
    }

    val data = get.execute().get().getResponseBody();
    send(Json.generate(Map("uuid" -> uuid, "data" -> data)))
  }

  def analyzeRequest(uuid: Long, request: AnalyzeRequest) {
    val filename = request.filename
    val source = request.source

    val data = filename match {
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
}
