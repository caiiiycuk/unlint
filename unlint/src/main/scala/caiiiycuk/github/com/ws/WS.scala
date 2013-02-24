package caiiiycuk.github.com.ws

import com.ning.http.client.AsyncHttpClientConfig
import com.ning.http.client.AsyncHttpClient
import scala.concurrent.ExecutionContext
import java.util.concurrent.TimeUnit
import java.util.concurrent.AbstractExecutorService
import com.ning.http.client.Realm.RealmBuilder
import com.ning.http.client.Realm.AuthScheme

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

object WS {
  private val configBuilder = new AsyncHttpClientConfig.Builder()

  configBuilder.setAllowPoolingConnection(true)
  configBuilder.setMaximumConnectionsTotal(100)
  configBuilder.setConnectionTimeoutInMs(10000)
  configBuilder.setRequestTimeoutInMs(10000)
  configBuilder.setFollowRedirects(true)
  configBuilder.setExecutorService(AkkaExecutorService)

  private val ws = new AsyncHttpClient(configBuilder.build())

  def prepareGet(url: String) = ws.prepareGet(url)

  def preparePost(url: String) = ws.preparePost(url)

  def basicAuth(builder: AsyncHttpClient#BoundRequestBuilder, username: String, password: String) = {
    (username, password) match {
      case ("", "") =>
      case (user, password) =>
        builder.setRealm((new RealmBuilder()
          .setScheme(AuthScheme.BASIC)
          .setPrincipal(username)
          .setPassword(password)
          .setUsePreemptiveAuth(true).build()))
    }
    
    builder
  }

}