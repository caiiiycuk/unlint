function onSave() {
	localStorage['login'] = document.getElementById("login").value;
	localStorage['password'] = document.getElementById("password").value;
	window.close();
}

function onClose() {
	window.close();
}

document.getElementById("save").onclick = onSave;
document.getElementById("cancel").onclick = onClose;
document.getElementById("login").value = localStorage['login'] || "";
document.getElementById("password").value = localStorage['password'] || "";