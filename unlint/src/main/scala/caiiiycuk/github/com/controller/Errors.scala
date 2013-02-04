package caiiiycuk.github.com.controller

import xitrum.Controller
import xitrum.ErrorController

class Errors extends Controller with ErrorController {
  def error404 = errorAction {
    respondText("404")
  }

  def error500 = errorAction {
    respondText("500")
  }

}
