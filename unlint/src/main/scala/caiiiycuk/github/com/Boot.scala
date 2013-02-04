package caiiiycuk.github.com

import caiiiycuk.github.com.controller.Errors
import caiiiycuk.github.com.controller.UnlintSock
import xitrum.handler.Server
import xitrum.routing.Routes
import caiiiycuk.github.com.controller.Errors
import caiiiycuk.github.com.controller.UnlintSock

object Boot {
  def main(args: Array[String]) {
    Routes.error = classOf[Errors]
    Routes.sockJs(classOf[UnlintSock], "unlint")
    Server.start()
  }
}
