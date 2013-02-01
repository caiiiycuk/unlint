package ru.geo.controller

import scala.concurrent.ExecutionContext
import scala.concurrent.future
import com.ning.http.client.AsyncHttpClient
import com.ning.http.client.Realm.AuthScheme
import com.ning.http.client.Realm.RealmBuilder
import xitrum.SockJsHandler
import xitrum.util.Json
import com.ning.http.client.AsyncHttpClientConfig
import scala.util.parsing.json.JSON

case class UnlintRequest(action: String, data: String)
case class ProxyRequest(url: String, username: String, password: String)
case class AnalyzeRequest(filename: String, source: String)

object UnlintSock {
  val configBuilder = new AsyncHttpClientConfig.Builder()
  configBuilder.setAllowPoolingConnection(true)
  configBuilder.setMaximumConnectionsTotal(100)
  configBuilder.setConnectionTimeoutInMs(10000)
  configBuilder.setRequestTimeoutInMs(10000)
  configBuilder.setFollowRedirects(true)
  
  val ws = new AsyncHttpClient(configBuilder.build())
}

class UnlintSock extends SockJsHandler {
  implicit val ec: ExecutionContext =
    xitrum.Config.actorSystem.dispatcher

  def onOpen(session: Map[String, Any]) {
  }

  def onMessage(message: String) {
    val request = Json.parse[UnlintRequest](message)
    
    request match {
      case UnlintRequest("proxy", data) =>
        proxyRequest(Json.parse[ProxyRequest](data))
      case UnlintRequest("analyze", data) =>
        analyzeRequest(Json.parse[AnalyzeRequest](data))
      case _ =>
        close()
    }
  }

  def proxyRequest(request: ProxyRequest) {
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

    future {
      send(get.execute().get().getResponseBody())
    }
  }
  
  def analyzeRequest(request: AnalyzeRequest) {
    println("!!!!AS>" + request.filename)
    println("!!!!BS>" + request.source)
    send("ok")
  }

  def onClose() {
  }
}
