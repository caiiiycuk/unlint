var sockUrl = 'http://127.0.0.1:8000/unlint';
var reChanges = new RegExp("https://github.com/(.*)/(.*)/pull/(.*)/*");
var transport = undefined;

function unlint() {
    $('.advice').show();
    $('.changes-container').html("<img src='/images/ajax-loader.gif' alt='Loading, please wait...'/>");
    
    var options = {
        url: $('[name="url"]').val(),
        username: $('[name="username"]').val(),
        password:  $('[name="password"]').val(),
    }

    if (!transport) {
    // if (options.username.length > 0 && options.password > 0) {
        transport = new SockTransport(sockUrl, function() {
            inspect(options);
        });
    // } else {
        // transport = new JsonpTransport(sockUrl);
        // inspect(options);
    // }
    } else {
        inspect(options);
    }
}

function inspect(options) {
     var match = options.url.match(reChanges);
    
    if (match) {
    	var changesUrl = makeChangesUrl(match);
    	transport.download({
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
    if (typeof data  === 'string') {
	   data = JSON.parse(data);
    }
	
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

    for (var index = 0; index < files.length; ++index) {
    	var filename = files[index];
    	var raw = rawfiles[index];
    	
    	var callback = (function (filename, raw) {
			return function (source) {
    			transport.analyze(filename, source, raw, analyze);
    		}
		})(filename, raw);
    	
        transport.download({
    		url: raw,
    		username: options.username,
    		password: options.password,
    		callback: callback
    	});
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
    var nodes = xml.find("error");
    var errors = {};

    var error;
    for (var i = 0; i < nodes.length; ++i) {
        var error = nodes[i];
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

function toggleAuthBlock() {
    $(".auth-block").toggle();
}

function makeChangesUrl(match) {
    return "https://api.github.com/repos/" + match[1] + "/" + match[2] + "/pulls/" + match[3] + "/files";
}

function analyze(filename, source, raw, data) {
    if (data === 'not checked') {
        renderSimpleAdvice(filename, source, data, raw);
    } else {
        var xml = $.parseXML(data);
        renderAdvice(filename, source, $(xml), raw);
    }
}