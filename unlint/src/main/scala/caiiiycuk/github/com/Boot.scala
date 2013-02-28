package caiiiycuk.github.com

import caiiiycuk.github.com.api.Pull
import caiiiycuk.github.com.controller.Errors
import caiiiycuk.github.com.controller.UnlintHook
import caiiiycuk.github.com.controller.UnlintSock
import caiiiycuk.github.com.engine.AdviceChecks
import caiiiycuk.github.com.engine.AdviceEngine
import xitrum.routing.Routes
import xitrum.handler.Server

object Boot {

  def main(args: Array[String]) {
    AdviceChecks.update()
    Routes.error = classOf[Errors]
    Routes.sockJs(classOf[UnlintSock], "unlint")
    Server.start()
  }

}
