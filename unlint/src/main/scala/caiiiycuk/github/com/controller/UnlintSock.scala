package caiiiycuk.github.com.controller

import scala.concurrent.ExecutionContext
import scala.concurrent.future
import com.ning.http.client.AsyncHttpClient
import caiiiycuk.github.com.engine.AdviceChecks
import caiiiycuk.github.com.engine.AdviceEngine
import caiiiycuk.github.com.ws.WS
import xitrum.SockJsHandler
import xitrum.util.Json
import caiiiycuk.github.com.api.Pull
import org.slf4j.Logger
import caiiiycuk.github.com.engine.AdviceEngine
import caiiiycuk.github.com.api.Change
import caiiiycuk.github.com.engine.AnalyzeProgress
import caiiiycuk.github.com.engine.SourceWithAdvice
import xitrum.Cache
import caiiiycuk.github.com.secret.Secrets

class Report(val file: String, val raw: String, val advice: SourceWithAdvice)

class Error(val data: String, val action: String = "error")
class Changes(val data: List[Change], val action: String = "changes")
class FileStatus(val data: (String, Int), val action: String = "fileStatus")
class FileReport(val data: Report, val action: String = "fileReport")

class SecretError(message: String) extends Exception(message)

class UnlintSock extends SockJsHandler {
  implicit val ec: ExecutionContext =
    xitrum.Config.actorSystem.dispatcher

  implicit val l: Logger = logger

  var alive = false
  
  def onOpen(controller: xitrum.Controller) {
    alive = true
  }

  def onMessage(reportSecret: String) {
    future {
    	buildReport(reportSecret)
    }
  }

  def buildReport(reportSecret: String) {
    try {
      val pull =
        Secrets.pullOfSecret(reportSecret) match {
          case Some(pull) =>
            pull
          case None =>
            throw new SecretError(s"Report not found for $reportSecret, please rebuild it from unlint.github.com")
        }

      val engine = new AdviceEngine(pull)

      send(Json.generate(new Changes(engine.changes)))

      for (change <- engine.changes; if alive) {
        val file = change.file
        val raw = change.raw

        send(Json.generate(new FileStatus((file, 0))))
        val advice = engine.analyze(change, new AnalyzeProgress() {
          def notifyDownload(): Unit = {
            send(Json.generate(new FileStatus((file, 1))))
          }
          def notifyAnalyze(): Unit = {
            send(Json.generate(new FileStatus((file, 2))))
          }
        })
        send(Json.generate(new FileStatus((file, 3))))
        send(Json.generate(new FileReport(new Report(file, raw, advice))))
      }
    } catch {
      case e: SecretError =>
        send(Json.generate(new Error(e.getMessage)))
      case e: Throwable =>
        send(Json.generate(new Error(e.getMessage)))
        logger.error(e.getMessage, e)
    } finally {
      close()
    }
  }

  def onClose() {
    alive = false
  }

}
