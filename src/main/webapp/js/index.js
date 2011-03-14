$(document).ready(function() {
	new JmxChart('memoryChart', 'java.lang:type=Memory', 'HeapMemoryUsage', 'used');
});

function JmxChart(id, mbean, attribute, path) {
	var jmx = new Jolokia("/jolokia");

	var chart = new Highcharts.Chart({
		chart: {
			renderTo: id,
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
						var value = parseInt(jmx.getAttribute(mbean, attribute, path));
						series.addPoint({
							x: new Date().getTime(),
							y: value
						}, true, series.data.length >= 30);
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