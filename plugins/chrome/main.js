var URL_REGEX = new RegExp("https://github.com/(.*)/(.*)/pull/([^/]*)/*");

chrome.browserAction.onClicked.addListener(function(tab) {
 	var url = decodeURIComponent(tab.url);
    var match = url.match(URL_REGEX);
    
    if (match) {
    	var token  = localStorage['token'] || "";
        
    	var link = "http://unlint.org/byurl?url="
    		+ url + "&token=" + token;

		chrome.tabs.create({'url': link});
    } else {
    	alert("Please enter correct github url like:\n'https://github.com/:owner/:repo/pull/:number'");
    }
}); 