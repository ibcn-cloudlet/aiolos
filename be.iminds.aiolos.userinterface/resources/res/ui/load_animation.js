var refresh_count = 0;
var progress_bar = false;

function loadAnimation() {
	refresh_count++;
	progress_bar.show("blind");
}

function stopLoadAnimation() {
	if (--refresh_count <= 0) {
		refresh_count = 0;
		progress_bar.hide("blind");
	}
}

$(document).ready(function() {
	progress_bar = $( "#progressbar" );
	progress_bar.hide();
	progress_bar.progressbar({
		value: false
	});
	progress_bar.removeClass("ui-corner-all");
	$( ".ui-progressbar-value").removeClass("ui-corner-left");
});