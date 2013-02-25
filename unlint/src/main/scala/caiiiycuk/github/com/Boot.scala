package caiiiycuk.github.com

import caiiiycuk.github.com.api.Pull
import caiiiycuk.github.com.api.Status
import caiiiycuk.github.com.api.Statuses
import caiiiycuk.github.com.controller.UnlintHook
import caiiiycuk.github.com.engine.AdviceChecks
import xitrum.routing.Routes
import xitrum.handler.Server
import caiiiycuk.github.com.controller.UnlintSock
import caiiiycuk.github.com.controller.Errors

object Boot {

  def main(args: Array[String]) {
    AdviceChecks.update()
    Routes.error = classOf[Errors]
    Routes.sockJs(classOf[UnlintSock], "unlint")
    Server.start()
  }
  
}
