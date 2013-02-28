var Templates = function () {
    
    var templates = {};
    var preloads = [
        "/templates/changes.tmpl",
        "/templates/advices.tmpl"
    ];
    
    return {
        
        initialize: function(callback) {
            if (preloads.length == 0) {
                callback();
                return;
            }

            var path = preloads.shift();

            $.get(path, function(template) {
                Templates.register(path, template);
                Templates.initialize(callback);
            }, 'text');
        },

        register: function(path, template) {
            var templateFunction = _.template(template);
            templates[path] = templateFunction;
        },

        template: function(path) {
            if (templates[path]) {
                return templates[path];
            }

            throw new Error("Undefiend template path " + path);
        }
        
    }
    
    
}();