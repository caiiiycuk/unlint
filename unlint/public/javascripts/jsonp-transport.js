var JsonpTransport = function (sockUrl) {
    var self = this;

    this.sockTransport = new SockTransport(sockUrl);

    this.close = function() {
        this.sockTransport.close();
    };

    this.download = function (options) {
        var request = {
            url: options.url,
            dataType: "jsonp"
        }
 
        $.ajax(request)
            .success(function (message) {
                options.callback(message.data);
            })
            .error(self.error);
    };

    this.analyze = this.sockTransport.analyze;

    this.error = function (message) {
        alert('Error ' + message + ' see console');
        console.log(message);
    };
}