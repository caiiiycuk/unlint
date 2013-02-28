var sockUrl = 'http://unlint.org:8000/unlint';
//var sockUrl = 'http://127.0.0.1:8000/unlint';

var handlers = {
    error: onError,
    changes: onChanges,
    fileStatus: onFileStatus,
    fileReport: onFileReport
};

function unlint(report) {
    $('.error').hide();
    
    Templates.initialize(function() {
        renderReport(report);
    });
}

function renderReport(report) {
    var transport = new SockJS(sockUrl);
    transport.onopen = function() {
        transport.send(report);
    };

    transport.onmessage = function (message) {
        var json    = JSON.parse(message.data);
        var handler = handlers[json.action];
    
        if (!handler) {
            console.log("Handler not set", message);
            throw "Handler not set for " + json.action;
        }
    
        handler(json.data);
    };

    window.onbeforeunload = function() {
        transport.close();
    };

    window.unload = function () { 
        transport.close();
    };
}

function onChanges(data) {
    if (Object.prototype.toString.call( data ) !== '[object Array]') {
        onError("<b>Error:</b> " + JSON.stringify(data));
        return;
    }
	
	var files = [];
	var rawfiles = [];
	
	for (var index=0; index<data.length; ++index) {
        files.push(data[index].file);
		rawfiles.push(data[index].raw);
	}
	
    var content = Templates
        .template("/templates/changes.tmpl")({ files: files });

    $('.changes-container').html(content);
}

function onFileStatus(data) {
    var file = data._1;
    var status = data._2;

    if (status == 0) {
        $("a[href='#" + file + "']>div").html('<span style="color: blue;">(0/3) Ready for start, please wait...</span>');
    } else if (status == 1) {
        $("a[href='#" + file + "']>div").html('<span style="color: blue;">(1/3) Downloading, please wait...</span>');
    } else if (status == 2) {
        $("a[href='#" + file + "']>div").html('<span style="color: blue;">(2/3) Analyzing, please wait...</span>');
    } else if (status == 3) {
        $("a[href='#" + file + "']>div").html('<span style="color: blue;">(3/3) Analyzed...</span>');
    }
}

function onFileReport(data) {
    var file = data.file;
    var raw = data.raw;
    var source = data.advice.source;
    var advice = data.advice.advice;

    var xml = $($.parseXML(advice));
    var skips = xml.find("skip");
    
    if (skips.length > 0) {
        renderSkip(file, source, asObject(skips[0].attributes).message, raw);
    } else {
        renderAdvice(file, source, xml, raw);   
    }
}

function renderSkip(filename, source, fileAdvice, raw) {
    var content = Templates.template("/templates/advices.tmpl")({
        filename: filename,
        raw: raw,
        source: source.replace(/>/g, "&gt;").replace(/</g, "&lt;").split("\n"),
        fileAdvice: fileAdvice,
        errors: {}
    });

    $('.advice-container').append(content);
}

function renderAdvice(filename, source, xml, raw) {
    var nodes = xml.find("error");
    var errors = {};

    var error;
    for (var i = 0; i < nodes.length; ++i) {
        var error = nodes[i];
        var lineError = asObject(error.attributes);
        var lines = lineError['line'].split(',');
        
        for (var j = 0; j < lines.length; ++j) {
            var lineNumber = parseInt(lines[j]);
        
            if (!errors[lineNumber]) {
                errors[lineNumber] = [];
            }

            errors[lineNumber].push(lineError);
        }
    }

    var content = Templates.template("/templates/advices.tmpl")({
        filename: filename,
        raw: raw,
        fileAdvice: 'checked',
        source: source.replace(/>/g, "&gt;").replace(/</g, "&lt;").split("\n"),
        errors: errors
    });

    $('.advice-container').append(content);
}

function asObject(array) {
    var result = {};

    for (var i = 0; i < array.length; ++i) {
        var attribute = array[i];
        result[attribute.nodeName] = attribute.nodeValue;
    }

    return result;
}

function onError(message) {
    $('.error').html(message);
    $('.error').show();
}