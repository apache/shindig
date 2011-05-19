/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

$(function() {
	
	// Input field that contains gadget urls added by the user manually
	var newGadgetUrl= $( "#gadgetUrl" );
	
	//  Input fields for container event testing
	var newEventTopic = $( "#eventTopic" );
	var newEventPayload = $( "#eventPayload" );
	
	// Base html template that is used for the gadget wrapper and site
	var gadgetTemplate ='<div class="portlet">' +
				                '<div class="portlet-header">sample to replace</div>'+
				                '<div id="gadget-site" class="portlet-content"></div>' +
	                            '</div>';
	
	//variable to keep track of gadget current view for collapse and expand gadget actions.
	var currentView="default";
	
	// ID used to associate gadget site
	var curId = 0;
	
	//  Load the default collections stored and update the options with the collection name
	$.ajax({
			url: './gadgetCollections.json',
			dataType: 'json',
			success: function(data) {
			  $.each(data.collections, function(i,data){
				 var optionVal = [];
				 $.each(data.apps, function(i,data){
				   if (data.url.indexOf("http") < 0 && data.url.indexOf("/") == 0 ){
					 optionVal.push(urlBase+data.url);
				   }else{
					 optionVal.push(data.url);
				   }
				 });
			     $('#gadgetCollection').append('<option value="'+ optionVal.toString() + '">' + data.name +'</option>');
			   });
			}
	});	
	
	$.ajax({
		url: './viewsMenu.json',
		dataType: 'json',
		success: function(data) {
		  $.each(data.views, function(i,selection){
		     $('#viewOptions').append('<option value="'+ selection.value + '">' + selection.name +'</option>');
		  });
		}
	});
	
	//navigate to the new view and save it as current view
    navigateView=function(gadgetSite, gadgetURL, toView){
    	//save the current view for collapse, expand gadget
    	currentView = toView;
    	CommonContainer.navigateView(gadgetSite,gadgetURL,toView); 
    };
    
    //handle gadget collapse, expand, and remove gadget actions
    handleNavigateAction=function(portlet,gadgetSite,gadgetURL,actionId){
		 //remove button was click, remove the portlet/gadget
		 if(actionId==="remove"){
			if(confirm('This gadget will be removed, ok?')) { 
			  portlet.remove();
		    }						 
		 }else if (actionId==="expand" ){
			//navigate to currentView prior to colapse gadget
			CommonContainer.navigateView(gadgetSite,gadgetURL,currentView); 
	     }else if (actionId==="collapse"){
	    	CommonContainer.colapseGadget(gadgetSite);
		 }		
    };
    
    //create a gadget with navigation tool bar header enabling gadget collapse, expand, remove, navigate to view actions.
    buildGadget=function(result,gadgetURL){
      var gadgetSiteString="$(this).closest(\'.portlet\').find(\'.portlet-content\').data(\'gadgetSite\')";	
      var viewItems = "";	
      var gadgetViews = result[gadgetURL].views;
      for(var aView in gadgetViews){
	    viewItems = viewItems+'<li><a href="#" onclick="navigateView('+gadgetSiteString+','+'\''+gadgetURL+'\''+','+'\''+aView+'\''+'); return false;">'+aView+'</a></li>';
	  }
	  var newGadgetSite = gadgetTemplate;
	  newGadgetSite = newGadgetSite.replace(/(gadget-site)/g,'$1-'+ curId);
	  $(newGadgetSite).appendTo($( '#gadgetArea' )).addClass("ui-widget ui-widget-content ui-helper-clearfix ui-corner-all")
		.find(".portlet-header")
		.addClass("ui-widget-header ui-corner-all")
		.text(result[gadgetURL]['modulePrefs'].title)
		.append('<ul id="viewsDropdown">'+
			     '<li class="li-header">'+					
			       '<a href="#" class="hidden"><span id="dropdownIcon" class="ui-icon ui-icon-triangle-1-s"></span></a>'+				
				   '<ul>'+ 
				   	 viewItems +
				   '</ul>'+
			      '</li>'+
		         '</ul>')
		.append('<span id="remove" class="ui-icon ui-icon-closethick"></span>')         
		.append('<span id="expand" class="ui-icon ui-icon-plusthick"></span>')
		.append('<span id="collapse" class="ui-icon ui-icon-minusthick"></span>')
		.end()
	    .find(".portlet-content")
	    .data("gadgetSite", CommonContainer.renderGadget(gadgetURL, curId));
	 
		 //determine which button was click and handle the appropriate event.
		 $(".portlet-header .ui-icon").click(function(){						 
			 handleNavigateAction($(this).closest('.portlet'),$(this).closest('.portlet').find(".portlet-content").data("gadgetSite"),gadgetURL,this.id);
	     }); 
    };
		 
	//  Publish the container event
	$( "#pubEvent" ).click(function() {
		CommonContainer.inlineClient.publish(newEventTopic.val(), newEventPayload.val());
		
		//TODO:  Need to add in some additional logic in the Container to enable point to point for things like Embedded Experience...
		
		//var ppcont = CommonContainer.managedHub.getContainer("__gadget_1");
		//CommonContainer.managedHub.publishForClient(ppcont, newEventTopic.val(), newEventPayload.val());
		//ppcont.sendToClient('org.apache.shindig.random-number', '1111', ppcont.getClientID());
		//clear values
		newEventTopic.val( "" ); 
		newEventPayload.val( "" ); 
		    return true; 
	});
	
	//  Load single gadgets entered by user
	$( "#addGadget" ).click(function() {
		CommonContainer.preloadGadget(newGadgetUrl.val(), function(result){		
		  for (var gadgetURL in result) {
			 buildGadget(result,gadgetURL);
			 curId++;
		  }

	      //Clear Values
	      newGadgetUrl.val( "" ); 
		});
		
		return true; 
	});
	
	//  Load the select collection of gadgets and render them the gadget test area
	$( "#addGadgets" ).click(function() {
		
		//TODO:  This just provides and example to load configurations
		//var testGadgets=["http://localhost:8080/container/sample-pubsub-2-publisher.xml","http://localhost:8080/container/sample-pubsub-2-subscriber.xml"];
		var testGadgets = $('#gadgetCollection').val().split(",");
		CommonContainer.preloadGadgets(testGadgets, function(result){
		  for (var gadgetURL in result) {
			buildGadget(result,gadgetURL);
		    curId++;
		  }
		  
		});
		return true; 
		    
	});
	

});