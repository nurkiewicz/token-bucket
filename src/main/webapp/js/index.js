$(document).ready(function() {
	new Monitor();
});

function Monitor() {
	var jmx = new Jolokia("/jolokia");


	var chart = new Highcharts.Chart({
		chart: {
			renderTo: 'memoryChart',
			animation: {
				duration: 900,
				easing: 'linear'
			},
			defaultSeriesType: 'spline',
			marginRight: 10,
			events: {
				load: function() {
					var series = this.series[0];
					setInterval(function() {
						var x = (new Date()).getTime();
						var memoryUsed = jmx.getAttribute("java.lang:type=Memory", "HeapMemoryUsage", "used");
						series.addPoint({
							x: new Date().getTime(),
							y: parseInt(memoryUsed)
						}, true, series.data.length >= 50);
					}, 1000);
				}
			}
		},

		title: {
			text: 'Live random data'
		},
		xAxis: {
			type: 'datetime'
		},
		yAxis: {
			title: {
				text: 'HeapMemoryUsage'
			}
		},
		legend: {
			enabled: false
		},
		exporting: {
			enabled: false
		},
		plotOptions: {
			spline: {
				lineWidth: 1,
				marker: {
					enabled: false
				}
			}
		},

		series: [

			{
				type: 'spline',
				data: [],
				name: 'Rainfall'
			}
		]
	});


}