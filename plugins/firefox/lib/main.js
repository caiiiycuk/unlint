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
    options.show();
});

var URL_REGEX = new RegExp("https://github.com/(.*)/(.*)/pull/([^/]*)/*");

function unlint(url) {
    var match = url.match(URL_REGEX);
    
    if (match) {
        var token  = ss.storage.token || "";
    	var link = "http://unlint.org/byurl?url="
    		+ url + "&token=" + token;

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

options.port.on("token", function(token) {
  ss.storage.token = token || "";
});