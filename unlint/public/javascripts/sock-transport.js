var SockTransport = function (sockurl, whenReady) {
    var self = this;

    this.socket = new SockJS(sockurl);
    this.socketHandlers = {};
    this.uuid = 0;

    this.socket.onopen = function() {
        if (whenReady) {
          whenReady();  
        }
    };
    
    this.socket.onmessage = function (message) {
        var json = JSON.parse(message.data);
        var handler = self.socketHandlers[json.uuid];
        
        if (!handler) {
            console.log(message.data);
            throw "Handler not set for uuid " + json.uuid;
        }
        
        handler(json.data);
    };

    this.close = function() {
        this.socket.close();
    }

    this.download = function (options) {
        var uuid = this.nextUUID();
        var request = JSON.stringify({
            uuid: uuid,
            action: 'proxy',
            data: JSON.stringify({
                url: options.url,
                username: options.username,
                password: options.password
            })
        });
    
        this.socketHandlers[uuid] = options.callback;
        this.socket.send(request);
    };

    this.analyze = function (filename, source, raw, callback) {
        var uuid = this.nextUUID();
    
        var request = JSON.stringify({
            uuid: uuid,
            action: 'analyze',
            data: JSON.stringify({
                filename: filename,
                source: source
            })
        });
    
        this.socketHandlers[uuid] = function(data) {
            callback(filename, source, raw, data);
        };
    
        this.socket.send(request);
    };

    this.nextUUID = function() {
        this.uuid++; 
        return this.uuid; 
    };
}