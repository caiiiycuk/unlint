var Templates = function () {
    
    var templates = {};
    
    return {
        
        get: function(path, callback) {
            var template = templates[path];
            
            if (template) {
                callback(template);
            } else {
                $.get(path, function(template) {
                    var templateFunction = _.template(template);
                    templates[path] = templateFunction;
                    callback(templateFunction);
                }.bind(this), 'text');
            }
          }
        
    }
    
    
}();