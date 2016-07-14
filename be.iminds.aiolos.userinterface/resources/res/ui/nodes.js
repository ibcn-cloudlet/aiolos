/*
 * Renders the nodes tab
 */

// ui elements
var nodesTable    = false;
var nodesBody     = false;
var nodesTemplate = false;
var nodeOpError   = false;
var drawDetails   = false;
var rows = 0;
var startCustomDialog 	= false;
var startComponentDialog = false;
var stopComponentDialog = false;
var allComponents = false;
var allBndruns = false;

function renderData( eventData )  {
	rows = 0;
	nodesData = eventData;

	var status = eventData.status;
    $('.statline').html(i18n.statline.msgFormat(status[0]));
	nodesBody.empty();
    for ( var idx in eventData.nodes ) {
        entry( eventData.nodes[idx] );
    }
    allComponents = eventData.components;
    allBndruns = eventData.bndruns;
    
    // enable start node buttons if bndruns is not undefined, null or false
    if (allBndruns) {
    	$('.startCustom').removeClass('ui-helper-hidden');
    }
    
    initStaticWidgets();

	var cv = $.cookies.get("aiolos-nodes");
	if (cv && ! nodesBody.is(":empty")) 
		nodesTable.trigger('sorton', [cv]);

	// show dialog on error
	if (eventData.error) nodeOpError.dialog('open').find('pre').text(eventData.error)
	
	stopLoadAnimation();
}

function entry( /* Object */ node ) {
    entryInternal( node ).appendTo(nodesBody);
}

function hasStop(node)  { return true; } // TODO when to enable stop button.
function hasStartComponent(node) { return true; } // TODO when to show start/stop component
function hasStopComponent(node) { return true; }

function entryInternal( /* Object */ node ) {
	var tr = nodesTemplate.clone();
    var id = node.id;
	tr.attr('id', 'entry' + id);
	tr.find('td:eq(0)').text(rows++);
	tr.find('.bIcon').attr('id', 'img' + id).click(function() {showDetails( node )});
	tr.find('.bName').html( id );
//	tr.find('td:eq(2)').text( node.hostname );
	tr.find('td:eq(3)').text( node.ip );
	if (node.console) {
		tr.find('.link').attr('href', node.console );
	} else {
		tr.find('td:eq(4)').empty().text("-");
	}
	entrySetupState( node, tr, id );
	return tr;
}

function entrySetupState( /* Object */ node, tr, id) {
	var stop = tr.find('td:eq(5) ul li:eq(0)').removeClass('ui-helper-hidden').unbind('click');
	stop = hasStop(node) ?
			stop.click(function() { return changeDataEntryState(id, 'stop'); }) :
			stop.addClass('ui-helper-hidden');
	var startComponent = tr.find('td:eq(5) ul li:eq(1)').removeClass('ui-helper-hidden').unbind('click');
	startComponent = hasStartComponent(node) ?
			startComponent.click(function() { return openStartComponentDialog(node)}) :
			startComponent.addClass('ui-helper-hidden');
	var stopComponent = tr.find('td:eq(5) ul li:eq(2)').removeClass('ui-helper-hidden').unbind('click');
	stopComponent = hasStopComponent(node) ?
			stopComponent.click(function() { return openStopComponentDialog( node ) }) :
			stopComponent.addClass('ui-helper-hidden');
}

function changeDataEntryState(/* long */ id, /* String */ action) {
	loadAnimation();
    $.post(pluginRoot + '/' + id, {'action':action, 'id':id}, function(b) {
			var _tr = nodesBody.find('#entry' + id);
			if (action == 'stop') {
				_tr.remove();
    		}
			stopLoadAnimation();
		}, 'json');
	return false;
}

function openScaleDialog(node) {
	scaleDialog.data('select_values', node.components);
	scaleDialog.dialog('open');
	return false;
}

function openStartCustomDialog() {
	startCustomDialog.data('select_values', allBndruns)
	startCustomDialog.dialog('open');
	return false;
}

function openStartComponentDialog(node) {
	startComponentDialog.data('select_values', allComponents);
	startComponentDialog.data('node_id', node.id);
	startComponentDialog.dialog('open');
	return false;
}

function openStopComponentDialog(node) {
	stopComponentDialog.data('select_values', node.components);
	stopComponentDialog.data('node_id', node.id);
	stopComponentDialog.dialog('open');
	return false;
}

function hideDetails( data ) {
    $("#img" + data.id).each(function() {
        $("#pluginInlineDetails" + data.id).remove();
        $(this).
            removeClass('ui-icon-triangle-1-w').//left
            removeClass('ui-icon-triangle-1-s').//down
            addClass('ui-icon-triangle-1-e').//right
            attr("title", "Details").
            unbind('click').click(function() {showDetails( data )});
    });
}

function showDetails( data ) {
    $("#entry" + data.id + " > td").eq(1).append("<div id='pluginInlineDetails"  + data.id + "'/>");
    $("#img" + data.id).each(function() {
            $(this).
                removeClass('ui-icon-triangle-1-w').//left
                removeClass('ui-icon-triangle-1-e').//right
                addClass('ui-icon-triangle-1-s').//down
                attr("title", "Hide Details").
                unbind('click').click(function() {hideDetails( data )});
    });
    $("#pluginInlineDetails" + data.id).append("<table border='0'><tbody></tbody></table>");
    var details = data.components;
    for (var idx in details) {
        var prop = details[idx];
        var txt = "<tr><td class='aligntop' style='border:0px none'>" + prop + "</td></tr>";
        $("#pluginInlineDetails" + data.id + " > table > tbody").append(txt);
    }
}

function refreshNodes(){
	loadAnimation();
	$.get(pluginRoot + "/.json", null, renderData, "json");
}

function interceptSubmit( object ) {
	object.find('form').submit();
	object.dialog("close");
}

$(document).ready(function() {
	$('.startCustom').click(openStartCustomDialog);
	$('.refresh').click(refreshNodes);

	nodeOpError = $('#nodeOpError').dialog({
		autoOpen: false,
		modal   : true,
		width   : '80%'
	});
	nodeOpError.parent().addClass('ui-state-error');
	
	// start custom dialog
	var startCustomDialogButtons = {};
	startCustomDialogButtons['start'] = function() {
		interceptSubmit($(this));
	}
	startCustomDialog = $('#startCustomDialog').dialog({
		autoOpen: false,
		modal   : true,
		width   : '400px',
		buttons : startCustomDialogButtons,
		open: function (event, ui) {
			var select = $(this).find("select").empty();
			var values = $(this).data('select_values');
			$.each(values, function(key, value) {
			    select.append($("<option>").html(value));
			});
		}
	});
	
	// start component dialog
	var startComponentDialogButtons = {};
	startComponentDialogButtons['start'] = function() {
		interceptSubmit($(this));
	}
	startComponentDialog = $('#startComponentDialog').dialog({
		autoOpen: false,
		modal   : true,
		width   : '400px',
		buttons : startComponentDialogButtons,
		open: function (event, ui) {
			var select = $(this).find("select").empty();
			var values = $(this).data('select_values');
			$.each(values, function(key, value) {
			    select.append($("<option>").html(value));
			});
			var nodeid = $(this).data('node_id');
			$(this).find('#node_id').val(nodeid);
		}
	});
	
	// stop component dialog
	var stopComponentDialogButtons = {};
	stopComponentDialogButtons['stop'] = function() {
		interceptSubmit($(this));
	}
	stopComponentDialog = $('#stopComponentDialog').dialog({
		autoOpen: false,
		modal   : true,
		width   : '400px',
		buttons : stopComponentDialogButtons,
		open: function (event, ui) {
			var select = $(this).find("select").empty();
			var values = $(this).data('select_values');
			$.each(values, function(key, value) {
			    select.append($("<option>").html(value));
			});
			var nodeid = $(this).data('node_id');
			$(this).find('#node_id ').val(nodeid);
		}
	});
	
	$('form').submit(function( event ) {
		event.preventDefault();
		loadAnimation();
		$.post( $(this).attr('action'), // the form's action
		        $(this).serialize(),   // the form data serialized
		        renderData,
		        "json");
		return false;
	});
	
	// check for cookie
	nodesTable = $("#plugin_table").tablesorter({
		headers: {
			0: { sorter:"digit" },
			5: { sorter: false }
		},
		textExtraction:mixedLinksExtraction
	}).bind("sortEnd", function() {
		var t = nodesTable.eq(0).attr("config");
		if (t.sortList) 
			$.cookies.set("aiolos-nodes", t.sortList);
	});
	nodesBody = nodesTable.find('tbody');
	nodesTemplate = nodesBody.find('tr').clone();
	renderData(nodesData);
});