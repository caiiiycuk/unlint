function onSave() {
    self.port.emit('token', document.getElementById("token").value);
    self.port.emit('hide');
}

function onClose() {
    self.port.emit('hide');
}

document.getElementById("save").onclick = onSave;
document.getElementById("cancel").onclick = onClose;