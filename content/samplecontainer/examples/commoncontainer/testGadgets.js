$(function() {

// Input field that contains gadget urls added by the user manually
var newGadgetUrl= $( "#gadgetUrl" );

// Base html template that is used for the gadget wrapper and site
var gadgetTemplate ='<div class="portlet">' +
			                '<div class="portlet-header">sample to replace</div>'+
			                '<div id="gadget-site" class="portlet-content"></div>' +
                            '</div>';
// ID used to associate gadget site
curId = 0;

//  Load the defaultl collections stored and update the options with the collection name
$.ajax({
		url: './gadgetCollections.json',
		dataType: 'json',
		success: function(data) {
		$.each(data.collections, function(i,data)
			{
			 var optionVal = [];
			 $.each(data.apps, function(i,data)
			 	 {
			      optionVal.push(data.url);
			 	 }
			 	);
			 $('#gadgetCollection').append('<option value="'+ optionVal.toString() + '">' + data.name +'</option>');
					}
					);
			  }
			});	
	 
//  Load single gadgets entered by user
$( "#addGadget" ).click(function() {
	
	CommonContainer.preloadGadget(newGadgetUrl.val(), function(result){
		
		  for (var id in result) {
			  var newGadgetSite = gadgetTemplate;
				 newGadgetSite = newGadgetSite.replace(/(gadget-site)/g,'$1-'+ curId);
				 $(newGadgetSite).appendTo($( '#gadgetArea' )).addClass("ui-widget ui-widget-content ui-helper-clearfix ui-corner-all")
					.find(".portlet-header")
					.addClass("ui-widget-header ui-corner-all")
					.prepend('<span class="ui-icon ui-icon-minusthick"></span>')
					.text(result[id]['modulePrefs'].title)
					.end()
				.find(".portlet-content");
		
				 
				 $(".portlet-header .ui-icon").click(function() {
						$(this).toggleClass("ui-icon-minusthick").toggleClass("ui-icon-plusthick");
						$(this).parents(".portlet:first").find(".portlet-content").toggle();
					});

				    //Call gadget render
				  
				   CommonContainer.renderGadget(id, curId);
				   curId++;
		  }

	    //Clear Values
	    newGadgetUrl.val( "" ); });
	    return true; 
});

//  Load the select collection of gadgets and render them the gadget test area
$( "#addGadgets" ).click(function() {
	
	//TODO:  This just provides and example to load configurations
	//var testGadgets=["http://localhost:8080/container/sample-pubsub-2-publisher.xml","http://localhost:8080/container/sample-pubsub-2-subscriber.xml"];
	var testGadgets = $('#gadgetCollection').val().split(",");
	CommonContainer.preloadGadgets(testGadgets, function(result){
		
		  for (var id in result) {
			  var newGadgetSite = gadgetTemplate;
				 newGadgetSite = newGadgetSite.replace(/(gadget-site)/g,'$1-'+ curId);
				 $(newGadgetSite).appendTo($( '#gadgetArea' )).addClass("ui-widget ui-widget-content ui-helper-clearfix ui-corner-all")
					.find(".portlet-header")
					.addClass("ui-widget-header ui-corner-all")
					.prepend('<span class="ui-icon ui-icon-minusthick"></span>')
					.text(result[id]['modulePrefs'].title)
					.end()
				.find(".portlet-content");
		
				 
				 $(".portlet-header .ui-icon").click(function() {
						$(this).toggleClass("ui-icon-minusthick").toggleClass("ui-icon-plusthick");
						$(this).parents(".portlet:first").find(".portlet-content").toggle();
					});

				    //Call gadget render
				  
				   CommonContainer.renderGadget(id, curId);
				   curId++;
		  }
	});
	    return true; 
	    
});
});