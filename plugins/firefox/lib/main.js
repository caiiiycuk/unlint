var Widget = require("widget").Widget;
var tabs = require('tabs');
var data = require("sdk/self").data;
var panels = require('sdk/panel');
var notifications = require("sdk/notifications");
var ss = require("sdk/simple-storage");

var widget = new Widget({
    id: "unlint.github.com",

    label: "unlint.github.com",
    
    contentURL: data.url("icon.png"),
    contentScriptFile: data.url("click-listener.js")
});

widget.port.on("left-click", function(){
    unlint(tabs.activeTab.url);
});
 
widget.port.on("right-click", function(){
    notifications.notify({
      title: "unlint.github.com",
      text: "Authentication options",
      iconURL: data.url("icon128.png"),
      onClick: function() {
          options.show();
      }
    });
});

var URL_REGEX = new RegExp("https://github.com/(.*)/(.*)/pull/([^/]*)/*");

function unlint(url) {
    var match = url.match(URL_REGEX);
    
    if (match) {
        var username = ss.storage.login || "";
        var password = ss.storage.password || "";
    	var link = "http://unlint.github.com/advice.html?url="
    		+ url + "&username=" + username + "&password=" + password;

		tabs.open(link);
    } else {
        notifications.notify({
          title: "unlint.github.com",
          text: "Url should match https://github.com/:owner/:repo/pull/:number",
          iconURL: data.url("icon128.png")
        });
    }
}; 

var options = panels.Panel({
  contentURL: data.url('options.html'),
  contentScriptFile: data.url('options.js'),
  onShow: function() {
    this.postMessage('focus');
  }
});


options.port.on("hide", function() {
  options.hide();
});

options.port.on("login", function(login) {
  ss.storage.login = login;
});

options.port.on("password", function(password) {
  ss.storage.password = password;
});