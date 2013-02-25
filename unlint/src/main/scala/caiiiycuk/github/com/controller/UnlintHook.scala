package caiiiycuk.github.com.controller

import scala.concurrent.ExecutionContext
import scala.concurrent.future
import scala.reflect.runtime.universe
import org.json4s.JArray
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
import org.jboss.netty.util.CharsetUtil
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
      action != "JString(synchronize)") {
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
          check(pull)
        }
      case _ =>
      // nothing to do
    }
  }

  def check(pull: Pull) {
    Statuses.create(pull,
      Status.pending(pull.advice, "Checking pull request with unlint please wait..."))

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
          val sha = tree.blob(file)
          val contents: Option[String] = sha match {
            case Some(sha) =>
              WS.downloadBlob(pull, sha)
            case None =>
              None
          }

          contents match {
            case Some(source) =>
              if (source.length() > MAX_SIZE) {
                List(<skip line="1" severity="info" message={ s"File too big" }></skip>)
              } else {
                AdviceEngine.analyze(file, source)
              }
            case _ =>
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