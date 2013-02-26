package caiiiycuk.github.com.controller

import scala.concurrent.ExecutionContext
import scala.concurrent.future
import scala.reflect.runtime.universe

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

class UnlintHook extends Controller with SkipCSRFCheck {

  implicit val ec: ExecutionContext =
    xitrum.Config.actorSystem.dispatcher

  implicit val _logger: Logger = logger

  private val MAX_SIZE = 1024 * 70 /* 70Kb */

  def hookWithoutToken = POST("github/hook") {
    hook(param("payload"))
  }

  def hookWithToken = POST("github/hook/:token") {
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

    val pull = (json \ "number")
    val owner = (json \ "repository" \ "owner" \ "login")
    val repo = (json \ "repository" \ "name")
    val sha = (json \ "pull_request" \ "head" \ "sha")

    (owner, repo, pull, sha) match {
      case (JString(owner), JString(repo), JInt(num), JString(sha)) =>
        val pull = new Pull(owner, repo, num.toInt, sha, token)
        future {
          try {
            check(pull)
          } catch {
            case e: Throwable =>
              logger.error(e.getMessage(), e)
              Statuses.create(pull,
                Status.failure(pull.advice, "Exception occured: " + e.getMessage()))
          }
        }
      case _ =>
      // nothing to do
    }
  }

  def check(pull: Pull) {
    Statuses.create(pull,
      Status.pending(pull.advice, "Checking pull request with unlint please wait..."))

    Thread.sleep(2000) // Wait while pending status applied

    val changed = WS.downloadChanges(pull)

    val tree = new Tree(pull)

    var success = true

    for (change <- changed) {
      val file = change("file")
      val status = change("status")

      val advices =
        if (status == "removed") {
          List(<skip line="1" severity="info" message={ s"Removed" }></skip>)
        } else if (AdviceChecks.checksFor(file).isEmpty) {
          List(<skip line="1" severity="info" message={ s"Skipped (NO CHECKS)" }></skip>)
        } else {
          tree.blob(file) match {
            case Some(blob) =>
              val size = blob.size.getOrElse(0l)
              val sha = blob.sha

              if (size > MAX_SIZE) {
                List(<skip line="1" severity="info" message={ s"File too big" }></skip>)
              } else {
                WS.downloadBlob(pull, sha) match {
                  case Some(source) =>
                    AdviceEngine.analyze(file, source)
                  case None =>
                    List(<error line="1" severity="critical" message={ s"File not found, sha '$sha'" }></error>)
                }
              }
            case None =>
              List(<error line="1" severity="critical" message={ s"File not found" }></error>)
          }
        }

      val xml = <advice>{ advices.map(a => a) }</advice>.toString
      success = success && !xml.contains("<error")
    }

    if (success) {
      Statuses.create(pull,
        Status.success(pull.advice, "No problems found"))
    } else {
      Statuses.create(pull,
        Status.error(pull.advice, "Have some problems"))
    }
  }
}