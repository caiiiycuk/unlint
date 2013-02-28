function onSave() {
	localStorage['token'] = document.getElementById("token").value;
	window.close();
}

function onClose() {
	window.close();
}

document.getElementById("save").onclick = onSave;
document.getElementById("cancel").onclick = onClose;
document.getElementById("token").value = localStorage['token'] || "";