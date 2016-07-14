/*
 * Renders the components tab
 */

var rows = 0;
var scaleDialog = false;

function renderData( eventData )  {
	rows = 0;
	var status = eventData.status;
    $('.statline').html(i18n.statline.msgFormat(status[0]));
	tableBody.empty();
    for ( var idx in eventData.components ) {
        entry( eventData.components[idx] );
    }
    
    initStaticWidgets();

	var cv = $.cookies.get("aiolos-components");
	if (cv && ! tableBody.is(":empty")) 
		table.trigger('sorton', [cv]);

	// show dialog on error
	if (eventData.error) nodeOpError.dialog('open').find('pre').text(eventData.error)
	
	stopLoadAnimation();
}

function entry( /* Object */ component ) {
    entryInternal( component ).appendTo(tableBody);
}

function entryInternal( /* Object */ component ) {
	component.row = rows++;
	var tr = template.clone();
	tr.attr('id', 'entry' + component.row);
	tr.find('td:eq(0)').text(component.row);
	tr.find('.bIcon').attr('id', 'img' + component.row).click(function() {showDetails( component )});
	var name = component.id;
	name = name.concat(" (",component.version,")");
	tr.find('.bName').html( name );
	tr.find('td:eq(2)').text( component.nodes.length );
	entrySetupState(component, tr);
	return tr;
}

function hasScaleComponent(component)  { return true; } 

function entrySetupState( /* Object */ component, tr) {
	var scaleComponent = tr.find('td:eq(3) ul li:eq(0)').removeClass('ui-helper-hidden').unbind('click');
	scaleComponent = hasScaleComponent(component) ?
			scaleComponent.click(function() { return openScaleDialog(component)}) :
			scaleComponent.addClass('ui-helper-hidden');
}

function hideDetails( data ) {
    $("#img" + data.row).each(function() {
        $("#pluginInlineDetails" + data.row).remove();
        $(this).
            removeClass('ui-icon-triangle-1-w').//left
            removeClass('ui-icon-triangle-1-s').//down
            addClass('ui-icon-triangle-1-e').//right
            attr("title", "Details").
            unbind('click').click(function() {showDetails( data )});
    });
}

function showDetails( data ) {
	console.log("test");
    $("#entry" + data.row + " > td").eq(1).append("<div id='pluginInlineDetails"  + data.row + "'/>");
    $("#img" + data.row).each(function() {
            $(this).
                removeClass('ui-icon-triangle-1-w').//left
                removeClass('ui-icon-triangle-1-e').//right
                addClass('ui-icon-triangle-1-s').//down
                attr("title", "Hide Details").
                unbind('click').click(function() {hideDetails( data )});
    });
    $("#pluginInlineDetails" + data.row).append("<table border='0'><tbody></tbody></table>");
    var details = data.nodes;
    for (var idx in details) {
        var prop = details[idx];
        var txt = "<tr><td class='aligntop' style='border:0px none'>" + prop + "</td></tr>";
        $("#pluginInlineDetails" + data.row + " > table > tbody").append(txt);
    }
}

function refresh(){
	loadAnimation();
	$.get(pluginRoot + "/.json", null, renderData, "json");
}

function openScaleDialog(component) {
	scaleDialog.data('component', component.id);
	scaleDialog.data('version', component.version);
	scaleDialog.dialog('open');
	return false;
}

function interceptSubmit( object ) {
	object.find('form').submit();
	object.dialog("close");
}

$(document).ready(function() {
	$('.refresh').click(refresh);

	nodeOpError = $('#nodeOpError').dialog({
		autoOpen: false,
		modal   : true,
		width   : '80%'
	});
	nodeOpError.parent().addClass('ui-state-error');
	

	// scale dialog
	var scaleDialogButtons = {};
	scaleDialogButtons['scale'] = function() {
		interceptSubmit($(this));
	}
	scaleDialog = $('#scaleDialog').dialog({
		autoOpen: false,
		modal : true,
		width   : '400px',
		buttons : scaleDialogButtons,
		open: function (event, ui) {
			var component = $(this).data('component');
			$(this).find('#component').val(component);
			var version = $(this).data('version');
			$(this).find('#version').val(version);
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
	table = $("#plugin_table").tablesorter({
		headers: {
			0: { sorter:"digit" },
			3: { sorter: false }
		},
		textExtraction:mixedLinksExtraction
	}).bind("sortEnd", function() {
		var t = table.eq(0).attr("config");
		if (t.sortList) 
			$.cookies.set("aiolos-components", t.sortList);
	});
	tableBody = table.find('tbody');
	template = tableBody.find('tr').clone();
	renderData(jsonData);
});


