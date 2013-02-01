package ru.geo.controller

import xitrum.ErrorController
import ru.geo.DefaultLayout

class Errors extends DefaultLayout with ErrorController {
  def error404 = errorAction {
    respondView()
  }

  def error500 = errorAction {
    respondView()
  }
}
