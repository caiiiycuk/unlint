package ru.geo.controller

import xitrum.Controller
import xitrum.SkipCSRFCheck

class Unlint extends Controller with SkipCSRFCheck {

  def changes = POST("/changes") {
    val url = param("url")
    val username = param("username")
    val password = param("password")
    
    respondText(url + username + password) 
  }

}