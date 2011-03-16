$(document).ready(function() {
	var factory = new JmxChartsFactory();
	factory.create([
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
	factory.create({
		name:     'java.lang:type=Threading',
		attribute: 'ThreadCount'
	});
	factory.create({
		name: 'Catalina:name="http-bio-8080",type=ThreadPool',
		attribute: 'currentThreadsBusy'
	});
	factory.create({
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

	setupPortletsContainer();
	pollInterval = pollInterval || 1000;
	var keepPoints = (keepHistorySec || 600) / (pollInterval / 1000);

	setInterval(function() {
		that.pollAndUpdateCharts();
	}, pollInterval);

	this.create = function(mbeans) {
		mbeans = $.makeArray(mbeans);
		var id = createPortlet();
		series = series.concat(createChart(id, mbeans).series);
		monMbeans = monMbeans.concat(mbeans);
	};

	this.pollAndUpdateCharts = function() {
		var requests = prepareBatchRequest();
		var responses = jolokia.request(requests);
		updateCharts(responses);
	};

	function createPortlet() {
		var idx = series.length;
		var id = 'portlet-' + idx;
		$('#portlet-template')
				.clone(true)
				.find('.portlet-content').attr('id', id).end()
				.appendTo($('.column')[idx % 3])
				.removeAttr('id');
		return id;
	}

	function setupPortletsContainer() {
		$(".column").sortable({
			connectWith: ".column"
		});

		$(".portlet-header .ui-icon").click(function() {
			$(this).toggleClass("ui-icon-minusthick").toggleClass("ui-icon-plusthick");
			$(this).parents(".portlet:first").find(".portlet-content").toggle();
		});
		$(".column").disableSelection();
	}

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
			credits: {enabled: false},
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