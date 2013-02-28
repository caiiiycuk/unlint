package caiiiycuk.github.com.controller

import caiiiycuk.github.com.api.Pull
import xitrum.Controller
import xitrum.SkipCSRFCheck
import caiiiycuk.github.com.secret.Secrets

class UnlintReport extends Controller with SkipCSRFCheck {

  def index = GET("report/:secret") {
    at("secret") = param("secret")
    respondView()
  }

  def push = POST("report/byurl") {
    try {
      val url = param("url")
      val token = param("token")

      val pull = Pull(url, token)

      pull match {
        case Some(pull) =>
          val secret = Secrets.secretOfPull(pull)
          redirectTo(s"/report/$secret")
        case _ =>
          respondText(s"Unable to find pull request url='$url', token='$token'")
      }
    } catch {
      case e: Throwable =>
        logger.error(e.getMessage, e)
        respondText("Exception on server " + e.getMessage)
    }
  }

}