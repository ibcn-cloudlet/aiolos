
// width / height of center column
var width;
var height;

// the graph, layout and renderer
var graph;
var layout;
var renderer;

/**
 * Core initialization functions
 */

// Drag & Drop / Details Expanding
//

$(function() {
	$(".draggable").draggable({
	    cursor: 'move',
	    containment: 'document',
	    helper: 'clone',
	});
	$('.droppable').droppable( {
	    drop: handleDropEvent
	});
	//$('.expander').simpleexpand();
});

function handleDropEvent( event, ui ) {
	var draggable = ui.draggable;
	var offset = $('#graph').offset();
	var x = event.clientX - offset.left;
	var y = event.clientY - offset.top;
	var target = inFramework(x,y);
	if(target){
		startComponent(draggable.attr('id'), target);
	}
}

//
//  adjust style depending on size

function adjustStyle() {
	// adapt style (2vs3column) depending on total width/height ratio
    total_w = parseFloat($(window).width());
    total_h = parseFloat($(window).height());
    var ratio = total_w/total_h;
    
    if (ratio < 1.7) {
        $("#size-stylesheet").attr("href", "style/narrow.css");
    } else {
       $("#size-stylesheet").attr("href", "style/wide.css"); 
    }
   
    // update graph width and height
    width = $("#graph").width();
    height = $("#graph").height();
    
    node_w = Math.min(width/5, height/5);
    node_h = node_w*0.8;
    node_caption = node_h/5;
    
    component_w = Math.min(width/30, height/30);
    component_h = component_w*0.7;
    component_caption = component_h/2;
    
    icon_size = height/10;
    
    font_size = Math.ceil(width/70);
}



$(function() {
    adjustStyle();
    $(window).resize(function() {
        // trigger event if resize has ended
    	if(this.resizeTO) clearTimeout(this.resizeTO);
        this.resizeTO = setTimeout(function() {
            $(this).trigger('resizeEnd');
        }, 500);
    	
    	// update style
    	adjustStyle();
        // invalidate graph
        renderer.r.remove();
        graph = false;
        
        
    });
    
    $(window).bind('resizeEnd', function() {
        updateState();
    });
});


//
//  Init

$(window).load(function () {
	adjustStyle();

    $('#componentInfo').hide();
	$('#nodeInfo').hide();
    
    updateState();

    updateRepository();
    
    // keep updating in the background
    setInterval(updateState, 10000);
    
    $(document).tooltip();
});


/**
 * Functions for rendering all dynamic parts of the UI
 */

function renderGraph(state){
	if(!graph){
		// init
		graph = new Graph();
		graph.edgeFactory.template.style.directed = true;
	    layout = new Graph.Layout.Circle(graph);
	    renderer = new Graph.Renderer.Raphael('graph', graph, width, height);
	    renderer.r.image("images/garbage.png", width - icon_size*1.2, height - icon_size*1.2, icon_size, icon_size);
	    
	    // override omouseup for dragging actions
	    var d = document.getElementById('graph');
	    var super_onmouseup = d.onmouseup;
	    d.onmouseup = function () {
	    	if(renderer.isDrag){
				var componentId = renderer.isDrag.set[0].node.id;
				var node = layout.graph.nodes[componentId];
				
	    		var offset = $('#graph').offset();
	    		var test = renderer.isDrag;
	    		var x = renderer.isDrag.dx - offset.left;
	    		var y = renderer.isDrag.dy;
	    		log("X "+x+" Y "+y+" W "+width+" H "+height);
	    		var target = inFramework(x,y);
	    		if(target){
	    			if(target!=node.framework){
	    				// migrate component
	    				migrateComponent(componentId, target);
	    			} else {
	    				// select component
	    				selectComponent(componentId);
	    			}
	    		} else if(x > width-icon_size && y > height-icon_size){ 
	    			// trash
	    			stopComponent(componentId);
	    		} else {
	    			// let component stay within framework boundaries
	    			selectComponent(componentId);
	    			renderer.draw();
	    		}
	    	} else if(renderer.isSelected){
	    		// select framework
	    		var framework = renderer.isSelected.set[0].node.id;
	    		selectFramework(framework);
	    	} 
	    	super_onmouseup();
	    };
	}
	// check whether changes occured
	var changed = false;
	
	// filter removed nodes/components
	var toRemove = [];
	for(var n in graph.nodes ){
		var node = graph.nodes[n];
		if(!inState(node, state)){
			toRemove.push(node);
		}
	}
	for(var n=0, len=toRemove.length; n < len; n++){
		var node = toRemove[n];
		graph.removeNode(node.id);
		changed = true;
	}

	for(var i in state.nodes){
		var node = state.nodes[i];
		var frameworkId = node.id;
		if(graph.nodes[frameworkId]==undefined)
			changed = true;
		graph.addNode(frameworkId, {label: node.name,
			render : drawNode,
			icon: getNodeIcon(node)});
	}
	for(var i in state.components){
		var component = state.components[i];
		var id = component.componentId+"-"+component.version+"@"+component.frameworkId;
		if(graph.nodes[id]==undefined)
			changed = true;
		graph.addNode(id, {framework : component.frameworkId, 
							label : component.name,
							render : drawComponent});
	}
	for(var i in state.links){
		var link = state.links[i];
		var id = link.from+"-"+link.to;
		if(graph.edges[id]==undefined){
			graph.addEdge(link.from, link.to);
			changed = true;
		}
	}

	if(changed){
		layout.layout();
		renderer.draw();
	}
}

// helper function to see whether node is still present in state
function inState(node, state){
	for(var i in state.nodes){
		var frameworkId = state.nodes[i];
		if(node.id == frameworkId)
			return true;
	}
	for(var i in state.components){
		var component = state.components[i];	
		var id = component.componentId+"-"+component.version+"@"+component.frameworkId;
		if(node.id == id)
			return true;
	}
	return false;
}

function renderRepository(components){
	var repository = $('#repository');
	for(var i in components){
		var componentItem = repository.find('li:first').clone();
		componentItem.attr("id",components[i].componentId+"-"+components[i].version);
		componentItem.attr("title",components[i].description);
		componentItem.find('span#name').text(components[i].name);
		componentItem.find('span#version').text("v"+components[i].version);
		componentItem.draggable({
		    cursor: 'move',
		    containment: 'document',
		    helper: 'clone'
		});
		componentItem.appendTo(repository);
	}
	// remove template
	repository.find('li:first').hide();
}

function renderNodeDetails(nodeInfo){
	var nodeDetails = $('#nodeInfo');
	
	nodeDetails.find('span#nodeId').text(nodeInfo.nodeId);
	nodeDetails.find('span#name').text(nodeInfo.name);
	nodeDetails.find('span#arch').text(nodeInfo.arch);
	nodeDetails.find('span#os').text(nodeInfo.os);
	nodeDetails.find('span#ip').text(nodeInfo.ip);
	nodeDetails.find('span#cores').text(nodeInfo.cores);
	nodeDetails.find('span#cpu').text(nodeInfo.cpu);

	$('#componentInfo').hide();
	nodeDetails.show();
	
}

function renderComponentDetails(componentInfo){
	var componentDetails = $('#componentInfo');
	
	componentDetails.find('span#componentId').text(componentInfo.componentId);
	componentDetails.find('span#name').text(componentInfo.name);
	componentDetails.find('span#version').text(componentInfo.version);
	componentDetails.find('span#nodeId').text(componentInfo.nodeId);
	
	if(componentInfo.services){
		// fill in service infos
		var serviceDetails = componentDetails.find('div#services');
		
		// first remove all but template
		$(".serviceInfoInstance").each( function( index, element ){
			console.log("remove service "+index);
			$(this).remove();
		});
		
		for(var i in componentInfo.services){
			var serviceInfo = componentInfo.services[i];
			
			var serviceItem = serviceDetails.find('#serviceInfo').clone();
			serviceItem.attr('id', 'serviceInfoInstance');
			serviceItem.attr('class', 'serviceInfoInstance');
			serviceItem.find('span#serviceId').text(serviceInfo.serviceId);
			
			// fill in method infos
			var methodDetails = serviceItem.find('#methods');
			if(serviceInfo.methods){
				for(var j in serviceInfo.methods){
					var methodInfo = serviceInfo.methods[j];
					
					var methodItem = methodDetails.find("li:first").clone();
					
					methodItem.find('span#methodName').text(methodInfo.methodName);
					methodItem.find('span#time').text(methodInfo.time);
					methodItem.find('span#arg').text(methodInfo.arg);
					methodItem.find('span#ret').text(methodInfo.ret);
					
					methodItem.find('.expander').simpleexpand();
					methodItem.appendTo(methodDetails);
				}
				
				methodDetails.find("li:first").hide();
				methodDetails.show();
			} else {
				methodDetails.hide();
			}
			serviceItem.show();
			
			serviceItem.appendTo(serviceDetails);
			
		}
		
		// hide template
		serviceDetails.find('div#serviceInfo').hide();
		
		serviceDetails.show();
	} else {
		componentDetails.find('div#services').hide();
	}
	
	$('#nodeInfo').hide();
	componentDetails.show();
}

/**
 * Functions translating actions to calls to the server
 */

function updateState(){
	$.get("../aiolos",{'action':'status'},renderGraph,'json');
}

function updateRepository(){
	$.get("../aiolos",{'action':'repository'},renderRepository,'json');
}

function selectFramework(frameworkId){
	log("SELECT "+frameworkId);
	$.get("../aiolos",{'action':'details','node':frameworkId},renderNodeDetails,'json');
}

function selectComponent(componentId){
	log("SELECT "+componentId);
	$.get("../aiolos",{'action':'details','component':componentId},renderComponentDetails,'json');
}

function migrateComponent(componentId, target){
	log("MIGRATE "+componentId+" TO TARGET "+target);
	$.get("../aiolos",{'action':'migrate','component':componentId,'target':target},updateState,'json');
}

function startComponent(componentId, target){
	log("START "+componentId+" ON TARGET "+target);
	$.get("../aiolos",{'action':'start','component':componentId,'target':target},updateState,'json');
}

function stopComponent(componentId){
	log("STOP "+componentId);
	$.get("../aiolos",{'action':'stop','component':componentId},updateState,'json');
}


/**
 * Functions related to graph drawing and layouting
 */
var component_w = 30;
var component_h = 20;

var node_w = 180;
var node_h = 150;


function drawComponent(r, node){
    var color = Raphael.getColor();
    var ellipse = r.ellipse(0, 0, component_w, component_h).attr({fill: color, stroke: color, "stroke-width": 2});
    ellipse.node.id = node.id; // DOM id
    shape = r.set().
        push(ellipse).
        push(r.text(0, component_h*1.5, node.label || node.id).attr("font-size",font_size).attr("font-family","'pt_sansregular', verdana, arial, sans-serif"));
    node.ellipse = ellipse; // used for better edge render
    return shape;
}

function drawNode(r, node) {
    var color = "#cccccc";
    var ellipse = r.ellipse(0, 0, node_w, node_h).attr({fill: color, stroke: color, "stroke-width": 2});
    var icon = r.image(node.icon, node_w/1.5, node_h/1.5, icon_size, icon_size);
    ellipse.node.id = node.id; // DOM id
    ellipse.fixed = true;
    shape = r.set().
        push(ellipse).
        push(icon).
        push(r.text(0, node_h*1.2, node.label || node.id).attr("font-size",font_size+1).attr("font-family","'pt_sansregular', verdana, arial, sans-serif"));
    ellipse.toBack();
    node.ellipse = ellipse; // used for better edge render
    return shape;
}

// get an icon for the node
function getNodeIcon(node){
	if(node.name == "gladstone"){
		return "images/xps12.png";
	} else if(node.name == "Nexus 4"){
		return "images/nexus4.png";
	} else if(node.name == "MZ601"){
		return "images/xoom.png";
	} else if(node.name == "M100"){
		return "images/vuzix.png";
	} else if(node.name == "colombus"){
		return "images/raspberry.png";
	} else if(node.os == "Android"){
		return "images/android.png";
	} else if(node.os == "Linux"){
		return "images/linux.png";
	}
}

// return whether or not x,y coordinates lays in a framework node
function inFramework(x, y){
	for(i in layout.frameworks){
		var node = layout.frameworks[i];
		if(Math.abs(x - node.layoutPosX) < node_w 
				&& Math.abs(y - node.layoutPosY) < node_h){
			return i;
		}
	}
	return null;
}

Graph.Layout.Circle = function(graph) {
    this.graph = graph;
    this.frameworks = {};
    this.components = {};
    this.layout();
};
Graph.Layout.Circle.prototype = {	
    layout: function() {
        this.layoutPrepare();
        this.layoutFrameworks();
        this.layoutComponents();
        this.layoutCalcBounds();
    },
    
    layoutPrepare: function() {
    	delete this.frameworks;
    	this.frameworks = {};
    	delete this.components;
    	this.components = {};
        for (i in this.graph.nodes) {
            var node = this.graph.nodes[i];
            if(!node.framework){
            	this.frameworks[node.id] = node;
            } else {
            	if(!this.components[node.framework])
            		this.components[node.framework] = 0;
            	
            	this.components[node.framework] = this.components[node.framework]+1;
            }
        }  
    },
    
    layoutFrameworks: function(){
    	var center_x = width/2;
    	var center_y = height/2;

    	var noFrameworks = Object.keys(this.frameworks).length;
    	if(noFrameworks>1){
    		var counter = 0;
    		var offset = 0;
    		if(noFrameworks > 2)
    			offset = -Math.PI/noFrameworks;
	    	for(i in this.frameworks){
	    		var node = this.frameworks[i];
	    		node.layoutPosX = center_x + width/3 * Math.cos(2*Math.PI*counter/noFrameworks + offset);
	    		node.layoutPosY = center_y + height/3 * Math.sin(2*Math.PI*counter/noFrameworks + offset);
	    		counter++;
	    	}
    	} else {
    		for(i in this.frameworks){
	    		var node = this.frameworks[i];
	    		node.layoutPosX = center_x;
	    		node.layoutPosY = center_y;
	    	}
    	}
    },
    
    layoutComponents: function(){
    	var r_x = node_w*0.8; // TODO should be based on actual radius of framework node?
    	var r_y = node_h*0.8;
    	for(i in this.frameworks){
    		var center_x = this.frameworks[i].layoutPosX;
    		var center_y = this.frameworks[i].layoutPosY;
    		var noComponents = this.components[i];
    		var random = Math.random();
    		
    		var counter = 0;
    		for(j in this.graph.nodes){
    			var node = this.graph.nodes[j];
    			if(node.framework && node.framework==i){
    				node.layoutPosX = center_x + r_x * Math.cos(2*Math.PI*counter/noComponents + random);
    	    		node.layoutPosY = center_y + r_y * Math.sin(2*Math.PI*counter/noComponents + random);
    				counter++;
    			}
    		}
    	}
    },
   
    layoutCalcBounds: function() {
        this.graph.layoutMinX = 0;
        this.graph.layoutMaxX = width;

        this.graph.layoutMinY = 0;
        this.graph.layoutMaxY = height;
    }
};
