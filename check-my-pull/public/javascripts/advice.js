var url = undefined;
var username = undefined;
var password = undefined;

function advice() {
    Templates.get("/assets/templates/changes.tmpl", function(template) {
        var content = template({
            abc: "abcde"
        });

        alert(content);
    });

    return;

    url = $('[name="url"]').val();
    username = $('[name="username"]').val();
    password = $('[name="password"]').val();
    
    $.ajax({
        url: "/changes",
        type: "POST",
        contentType: "text/json",
        data: JSON.stringify({ 
            url: url,
            username: username,
            password: password
        })
    }).done(function(response) {
        renderChanges(response)
    }).fail(function(message) {
        alert("FAIL TO LOAD FILES: " + message);
    });
}

function renderChanges(json) {
    
}