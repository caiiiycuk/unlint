var socket = undefined;
var reChanges = new RegExp("https://github.com/(.*)/(.*)/pull/(.*)/*");

function unlint() {
    $('.advice').show();
    $('.changes-container').html("<img src='/images/ajax-loader.gif' alt='Loading, please wait...'/>");
    
	socket = new SockJS('http://192.168.0.47:8000/unlint');
	
	var options = {
		url: $('[name="url"]').val(),
		username: $('[name="username"]').val(),
		password:  $('[name="password"]').val(),
	}
	
	socket.onopen = function() {
		inspect(options);
	};
}

function inspect(options) {
     var match = options.url.match(reChanges);
    
    if (match) {
    	var changesUrl = makeChangesUrl(match);
    	download({
    		url: changesUrl,
    		username: options.username,
    		password: options.password,
    		data: "json",
    		callback: function(data) {
    			changes(data, options);
    		}
    	});
    } else {
    	alert("Please enter correct github url");
    }
}

function changes(data, options) {
	var files = [];
	var rawfiles = [];
	
	for (var index=0; index<data.length; ++index) {
		files.push(data[index].filename);
		rawfiles.push(data[index].raw_url);
	}
	
    Templates.get("/templates/changes.tmpl", function(template) {
        var content = template({
            files: files
        });

        $('.changes-container').html(content);
    });

    var sources = []
    
    for (var index = 0; index < files.length; ++index) {
        var filename = files[index];
        var raw = rawfiles[index];

        download({
    		url: raw,
    		username: options.username,
    		password: options.password,
    		callback: function (source) {
    			sources.push({
    				filename: filename,
    				source: source,
    				raw: raw
    			});
    			
    			if (sources.length == files.length) {
    				analyzeSources(sources);
    			}
    		}
    	});
    }
}

function analyzeSources(sources) {
	for (var index = 0; index < sources.length; ++index) {
		analyze(sources[index].filename,
				sources[index].source,
				sources[index].raw);
	}
}

function renderSimpleAdvice(filename, source, fileAdvice, raw) {
    Templates.get("/templates/advices.tmpl", function(template) {
        var content = template({
            filename: filename,
            raw: raw,
            source: source.replace(/>/g, "&gt;").replace(/</g, "&lt;").split("\n"),
            fileAdvice: fileAdvice,
            errors: {}
        });

        $('.advice-container').append(content);
    });
}

function renderAdvice(filename, source, xml, raw) {
    var nodes = xml.evaluate(
        "//error", 
        xml.activeElement, 
        null, 
        XPathResult.ANY_TYPE, 
        null);

    var errors = {};

    var error;
    while (error = nodes.iterateNext()) {
        var lineError = asObject(error.attributes);
        var lineNumber = parseInt(lineError['line']);

        if (!errors[lineNumber]) {
            errors[lineNumber] = [];
        }

        errors[lineNumber].push(lineError);
    }

    Templates.get("/templates/advices.tmpl", function(template) {
        var content = template({
            filename: filename,
            raw: raw,
            fileAdvice: 'checked',
            source: source.replace(/>/g, "&gt;").replace(/</g, "&lt;").split("\n"),
            errors: errors
        });

        $('.advice-container').append(content);
    });
}

function asObject(array) {
    var result = {};

    for (var i = 0; i < array.length; ++i) {
        var attribute = array[i];
        result[attribute.nodeName] = attribute.nodeValue;
    }

    return result;
}

function error(message) {
    alert('Error ' + message + ' see console');
    console.log(message);
}

function toggleAuthBlock() {
    $(".auth-block").toggle();
}

function makeChangesUrl(match) {
    return "https://api.github.com/repos/" + match[1] + "/" + match[2] + "/pulls/" + match[3] + "/files";
}

function download(options) {
	var request = JSON.stringify({
			action: 'proxy',
			data: JSON.stringify({
				url: options.url,
				username: options.username,
				password: options.password
			})
		});

	socket.onmessage = function (message) {
		var data = message.data;
		if (options.data === "json") {
			data = JSON.parse(message.data);
		}
		options.callback(data);
	};
	
	socket.send(request);
}

function analyze(filename, source, raw) {
	var request = JSON.stringify({
		action: 'analyze',
		data: JSON.stringify({
			filename: filename,
			source: source
		})
	});
	
	socket.onmessage = function (message) {
		console.log(message.data);
	};
	
	socket.send(request);
}