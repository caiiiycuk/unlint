var URL_REGEX = new RegExp("https://github.com/(.*)/(.*)/pull/([^/]*)/*");

chrome.browserAction.onClicked.addListener(function(tab) {
 	var url = decodeURIComponent(tab.url);
    var match = url.match(URL_REGEX);
    
    if (match) {
    	var username  = localStorage['login'] || "";
    	var password  = localStorage['password'] || "";
        var unlintURL = localStorage['url'] || "http://unlint.github.com";
        
    	var link = unlintURL + "/advice.html?url="
    		+ url + "&username=" + username + "&password=" + password;

		chrome.tabs.create({'url': link});
    } else {
    	alert("Please enter correct github url like:\n'https://github.com/:owner/:repo/pull/:number'");
    }
}); 