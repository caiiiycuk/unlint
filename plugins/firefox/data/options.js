function onSave() {
    self.port.emit('login', document.getElementById("login").value);
    self.port.emit('password', document.getElementById("password").value);
    self.port.emit('hide');
}

function onClose() {
    self.port.emit('hide');
}

document.getElementById("save").onclick = onSave;
document.getElementById("cancel").onclick = onClose;