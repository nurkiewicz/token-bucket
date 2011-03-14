$(document).ready(function() {
	new JmxChart('memoryChart', 'java.lang:type=Memory', 'HeapMemoryUsage', 'used');
	new JmxChart('totalThreadsCountChart', 'java.lang:type=Threading', 'ThreadCount', '');
	new JmxChart('httpBusyChart', 'Catalina:name="http-bio-8080",type=ThreadPool', 'currentThreadsBusy', '');
	new JmxChart('httpQueueSize', 'Catalina:name=executor,type=Executor', 'queueSize', '');
});

function JmxChart(id, mbean, attribute, path) {
	var jmx = new Jolokia("/jolokia");

	new Highcharts.Chart({
		chart: {
			renderTo: id,
			animation: false,
			defaultSeriesType: 'spline',
			marginRight: 10,
			events: {
				load: function() {
					var series = this.series[0];
					setInterval(function() {
						series.addPoint(getAttribute(), true, series.data.length >= 30);
					}, 1000);
				}
			}
		},

		title: {
			text: mbean
		},
		xAxis: {
			type: 'datetime'
		},
		yAxis: {
			title: {
				text: attribute
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
				data: [getAttribute()],
				name: path || attribute
			}
		]
	});

	function getAttribute() {
		return {
			x: new Date().getTime(),
			y: parseInt(jmx.getAttribute(mbean, attribute, path))
		}
	}


}