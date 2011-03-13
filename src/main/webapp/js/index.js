$(document).ready(function() {
	new Monitor().schedule();
});

function Monitor() {
	var values = [];
	var jmx = new Jolokia("/jolokia");

	var loadMemoryUsage = function() {
		var value = jmx.getAttribute("java.lang:type=Memory", "HeapMemoryUsage", "used");
		values.push(parseInt(value));
		if (values.length > 1) {
			$('#memoryChart')
					.jqplot([values], { })
					.replot();
		}
	};

	this.schedule = function () {
		setInterval(function() {
			loadMemoryUsage()
		}, 1000);
	};

}