var url = undefined;
var username = undefined;
var password = undefined;

function advice() {
    $('.advice').show();

    $('.changes-container').html("<img src='assets/images/ajax-loader.gif' alt='Loading, please wait...'/>");

    url = $('[name="url"]').val();
    username = $('[name="username"]').val();
    password = $('[name="password"]').val();
    
    var options = {
        url: "/changes",
        type: "POST",
        contentType: "text/json",
        data: JSON.stringify({ 
            url: url,
            username: username,
            password: password
        })
    };

    $.ajax(options)
        .done(changes)
        .fail(error);
}

function changes(json) {
    Templates.get("/assets/templates/changes.tmpl", function(template) {
        var content = template({
            files: json.filename
        });

        $('.changes-container').html(content);
    });

    for (var index = 0; index < json.filename.length; ++index) {
        var filename = json.filename[index];
        var raw = json.rawfiles[index];

        var options = {
            url: "/raw",
            type: "POST",
            contentType: "text/json",
            data: JSON.stringify({ 
                url: raw,
                username: username,
                password: password
            })
        };

        var analyzer = (function (filename) {
            return function (source) {
                analyze(filename, source);
            }
        })(filename);

        $.ajax(options)
            .done(analyzer)
            .fail(error);
    }
}

function analyze(filename, source) {
    var options = {
        url: "/analyze",
        type: "POST",
        contentType: "text/json",
        data: JSON.stringify({ 
            filename: filename,
            source: source
        })
    };

    $.ajax(options)
        .done(function(fileAdvice) {
            if (typeof fileAdvice == 'string') {
                renderSimpleAdvice(filename, source, fileAdvice);
            } else {
                renderAdvice(filename, source, fileAdvice);
            }
        })
        .fail(error);
}

function renderSimpleAdvice(filename, source, fileAdvice) {
    Templates.get("/assets/templates/advices.tmpl", function(template) {
        var content = template({
            filename: filename,
            source: source.replace(/>/g, "&gt;").replace(/</g, "&lt;").split("\n"),
            fileAdvice: fileAdvice,
            errors: {}
        });

        $('.advice-container').append(content);
    });
}

function renderAdvice(filename, source, xml) {
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

    Templates.get("/assets/templates/advices.tmpl", function(template) {
        var content = template({
            filename: filename,
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
    alert('error ' + message);
}

function toggleAuthBlock() {
    $(".auth-block").toggle();
}