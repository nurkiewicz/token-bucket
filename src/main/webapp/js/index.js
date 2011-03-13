$(document).ready(function() {
	new Monitor().schedule();
});

function Monitor() {
	var jmx = new Jolokia("/jolokia");

	var loadMemoryUsage = function() {
		var value = jmx.getAttribute("java.lang:type=Memory", "HeapMemoryUsage", "used");

	};

	this.schedule = function () {
		setInterval(function() {
			loadMemoryUsage()
		}, 1000);
	};

}