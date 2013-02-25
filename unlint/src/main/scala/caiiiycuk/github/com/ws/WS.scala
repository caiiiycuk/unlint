package caiiiycuk.github.com.ws

import java.util.concurrent.AbstractExecutorService
import java.util.concurrent.TimeUnit

import scala.collection.mutable.ArrayBuffer
import scala.concurrent.ExecutionContext
import scala.math.BigInt.int2bigInt

import org.json4s.JInt
import org.json4s.JsonAST.JArray
import org.json4s.JsonAST.JString
import org.json4s.jvalue2monadic
import org.json4s.native.JsonMethods.parse
import org.json4s.string2JsonInput
import org.slf4j.Logger

import com.ning.http.client.AsyncHttpClient
import com.ning.http.client.AsyncHttpClientConfig
import com.ning.http.client.Realm.AuthScheme
import com.ning.http.client.Realm.RealmBuilder

import caiiiycuk.github.com.api.Pull
import xitrum.util.Base64

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
  private val MAX_SIZE = 1024 * 70 /* 70Kb */

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

  def downloadChanges(pull: Pull)(implicit logger: Logger) = {
    val json = WS.prepareGet(pull.changes()).execute().get().getResponseBody()
    val changes = parse(json)

    val files = (changes \ "filename")
    val rawFiles = (changes \ "raw_url")
    val statuses = (changes \ "status")

    val changed =
      (files, rawFiles, statuses) match {
        case (JString(file), JString(raw), JString(status)) =>
          List(Map("file" -> file, "raw" -> raw, "status" -> status))

        case (JArray(files), JArray(rawFiles), JArray(statuses)) =>
          val buffer = new ArrayBuffer[Map[String, String]]
          for (
            ((JString(file), JString(raw)), JString(status)) <- files.zip(rawFiles).zip(statuses)
          ) {
            buffer += Map("file" -> file, "raw" -> raw, "status" -> status)
          }
          buffer.toList
        case _ =>
          logger.error(s"Unable to parse changes json '$json'")
          List()
      }

    changed
  }

  def downloadBlob(pull: Pull, sha: String): Option[String] = {
    val data = WS.prepareGet(pull.blob(sha))
      .execute()
      .get().getResponseBody()

    val json = parse(data)
    val size = (json \ "size")
    val content = (json \ "content")
    
    size match {
      case JInt(size) =>
        if (size > MAX_SIZE) {
          None
        } else {
          content match {
            case JString(content) =>
              val decoded = Base64.decode(content)
              Some(new String(decoded.get))
            case _ =>
              None
          }
        }
      case _ =>
        None
    }
  }

}