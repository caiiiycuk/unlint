package ru.geo

import xitrum.handler.Server
import xitrum.routing.Routes
import ru.geo.controller.Errors
import ru.geo.controller.Errors
import ru.geo.controller.UnlintSock

object Boot {
  def main(args: Array[String]) {
    Routes.error = classOf[Errors]
    Routes.sockJs(classOf[UnlintSock], "unlint")
    Server.start()
  }
}
