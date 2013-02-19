function onSave() {
	localStorage['login'] = document.getElementById("login").value;
	localStorage['password'] = document.getElementById("password").value;
	localStorage['url'] = document.getElementById("url").value;
	window.close();
}

function onClose() {
	window.close();
}

document.getElementById("save").onclick = onSave;
document.getElementById("cancel").onclick = onClose;
document.getElementById("login").value = localStorage['login'] || "";
document.getElementById("password").value = localStorage['password'] || "";
document.getElementById("url").value = localStorage['url'] || "http://unlint.github.com";