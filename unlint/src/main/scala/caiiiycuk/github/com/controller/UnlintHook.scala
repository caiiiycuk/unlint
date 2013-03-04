package caiiiycuk.github.com.controller

import scala.concurrent.ExecutionContext
import scala.concurrent.future
import org.json4s.JInt
import org.json4s.JString
import org.json4s.jvalue2monadic
import org.json4s.native.JsonMethods.parse
import org.json4s.string2JsonInput
import org.slf4j.Logger
import caiiiycuk.github.com.api.Pull
import caiiiycuk.github.com.api.Status
import caiiiycuk.github.com.api.Statuses
import caiiiycuk.github.com.api.Tree
import caiiiycuk.github.com.engine.AdviceChecks
import caiiiycuk.github.com.engine.AdviceEngine
import caiiiycuk.github.com.ws.WS
import xitrum.Controller
import xitrum.SkipCSRFCheck
import caiiiycuk.github.com.engine.AdviceEngine
import caiiiycuk.github.com.secret.Secrets

class UnlintHook extends Controller with SkipCSRFCheck {

  implicit val ec: ExecutionContext =
    xitrum.Config.actorSystem.dispatcher

  implicit val _logger: Logger = logger

  private val MAX_SIZE = 1024 * 70 /* 70Kb */

  def hookWithoutToken = POST("github/hook") {
    respondText("Ok")
    hook(param("payload"))
  }

  def hookWithToken = POST("github/hook/:token") {
    respondText("Ok")
    hook(param("payload"), param("token"))
  }

  def hook(payload: String, token: String = "") {
    val json = parse(payload)

    val action = (json \ "action").toString()

    if (action != "JString(opened)" &&
      action != "JString(synchronize)" &&
      action != "JString(reopened)") {
      return
    }

    val pull = Pull(json, token)

    pull match {
      case Some(pull) =>
        future {
          val secret = Secrets.secretOfPull(pull)
          val host = request.getHeader("Host")
          val url = s"http://$host/report/$secret"
          
          try {
            check(pull, url)
          } catch {
            case e: Throwable =>
              logger.error(e.getMessage(), e)
              Statuses.create(pull,
                Status.failure(url, "Exception occured: " + e.getMessage()))
          }
        }
      case _ =>
      // nothing to do
    }
  }

  def check(pull: Pull, url: String) {
    Statuses.create(pull,
      Status.pending(url, "Checking pull request with unlint please wait..."))

    Thread.sleep(1000) // Wait while pending status applied

    val engine = new AdviceEngine(pull)

    var success = true
    var problemFile = ""

    for (change <- engine.changes; if success) {
      val file = change.file
      

      Statuses.create(pull,
        Status.pending(url, s"Checking file '$file'..."))
      Thread.sleep(1000) // Wait while pending status applied

      val xml = engine.analyze(change).advice

      success = !xml.contains("<error")
      if (!success) {
        problemFile = file
        logger.debug(s"Problem in '$file', problem: $xml")
      }
      
    }

    Thread.sleep(1000) // Wait while pending status applied

    if (success) {
      Statuses.create(pull,
        Status.success(url, "No problems found"))
    } else {
      Statuses.create(pull,
        Status.error(url, s"Problems in file '$problemFile'"))
    }
  }
}