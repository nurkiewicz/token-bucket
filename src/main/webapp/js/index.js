$(document).ready(function() {
	var factory = new JmxChartsFactory();
	factory.create('usedMemoryChart', [
		{
			name: 'java.lang:type=Memory',
			attribute: 'HeapMemoryUsage',
			path: 'committed'
		},
		{
			name: 'java.lang:type=Memory',
			attribute: 'HeapMemoryUsage',
			path: 'used'
		}
	]);
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

function JmxChartsFactory(keepHistorySec, pollInterval) {
	var jolokia = new Jolokia("/jolokia");
	var series = [];
	var monMbeans = [];
	var that = this;

	pollInterval = pollInterval || 1000;
	var keepPoints = (keepHistorySec || 600) / (pollInterval / 1000);

	setInterval(function() {
		that.pollAndUpdateCharts();
	}, pollInterval);

	this.create = function(id, mbeans) {
		mbeans = $.makeArray(mbeans);
		series = series.concat(createChart(id, mbeans).series);
		monMbeans = monMbeans.concat(mbeans);
	};

	this.pollAndUpdateCharts = function() {
		var requests = prepareBatchRequest();
		var responses = jolokia.request(requests);
		updateCharts(responses);
	};

	function prepareBatchRequest() {
		return $.map(monMbeans, function(mbean) {
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
			var point = {
				x: this.timestamp * 1000,
				y: parseInt(this.value)
			};
			var curSeries = series[curChart++];
			curSeries.addPoint(point, true, curSeries.data.length >= keepPoints);
		});
	}

	function createChart(id, mbeans) {
		return new Highcharts.Chart({
			chart: {
				renderTo: id,
				animation: false,
				defaultSeriesType: 'spline'
			},
			title: { text: mbeans[0].name },
			xAxis: { type: 'datetime' },
			yAxis: {
				title: { text: mbeans[0].attribute }
			},
			legend: {
				enabled: true,
				borderWidth: 0
			},
			exporting: { enabled: false },
			plotOptions: {
				spline: {
					lineWidth: 1,
					marker: { enabled: false }
				}
			},
			series: $.map(mbeans, function(mbean) {
				return {
					type: 'spline',
					data: [],
					name: mbean.path || mbean.attribute
				}
			})
		})
	}
}