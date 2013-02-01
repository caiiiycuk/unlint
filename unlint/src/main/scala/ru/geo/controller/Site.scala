package ru.geo.controller

import ru.geo.DefaultLayout

object Site extends Site

class Site extends DefaultLayout {
  def index = GET {
    respondView()
  }
}
