$(document).ready(function() {
	var factory = new JmxChartsFactory();
	factory.create('memoryChart', {
		name:     'java.lang:type=Memory',
		attribute: 'HeapMemoryUsage',
		path:      'used'
	});
	factory.create('totalThreadsCountChart', {
		name:     'java.lang:type=Threading',
		attribute: 'ThreadCount'
	});
	factory.create('httpBusyChart', {
		name: 'Catalina:name="http-bio-8080",type=ThreadPool',
		attribute: 'currentThreadsBusy'
	});
	factory.create('httpQueueSize', {
		name: 'Catalina:name=executor,type=Executor',
		attribute: 'queueSize'
	});
	factory.pollAndUpdateCharts();
});

function JmxChartsFactory() {
	var jolokia = new Jolokia("/jolokia");
	var charts = [];
	var that = this;

	setInterval(function() {
		that.pollAndUpdateCharts();
	}, 1000);

	this.create = function(id, mbean) {
		charts.push({
			series: createChart(id, mbean).series[0],
			mbean: mbean
		});
	};

	this.pollAndUpdateCharts = function() {
		var requests = prepareBatchRequest();
		var responses = jolokia.request(requests);
		updateCharts(responses);
	};

	function prepareBatchRequest() {
		return $.map(charts, function(chart) {
			var mbean = chart.mbean;
			return {
				type: "read",
				mbean: mbean.name,
				attribute: mbean.attribute,
				path: mbean.path
			};
		});
	}

	function updateCharts(responses) {
		var curChart = 0;
		$.each(responses, function() {
			var series = charts[curChart++].series;
			var point = {
				x: this.timestamp,
				y: parseInt(this.value)
			};
			series.addPoint(point, true, series.data.length >= 60);
		});
	}

	function createChart(id, mbean) {
		return new Highcharts.Chart({
			chart: {
				renderTo: id,
				animation: false,
				defaultSeriesType: 'spline'
			},
			title: { text: mbean.name },
			xAxis: { type: 'datetime' },
			yAxis: {
				title: { text: mbean.attribute }
			},
			legend: { enabled: false },
			exporting: { enabled: false },
			plotOptions: {
				spline: {
					lineWidth: 1,
					marker: { enabled: false }
				}
			},
			series: [
				{
					type: 'spline',
					data: [],
					name: mbean.path || mbean.attribute
				}
			]
		})
	}
}