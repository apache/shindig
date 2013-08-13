README

This version of openajax.hub is slightly modified from original project. The original source can be found at:
http://sourceforge.net/projects/openajaxallianc/

The changes only appear in iframe.js. The following differences should be noted:

1) Added a handler for the iframe onload to fire gadget onload events. Modify the iframe.src to check for a hash in the url before appending the rpc token.


387	387	          var idText = "";
388	388	          if ( internalID !== clientID ) {
389	389	              idText = "&oahId=" + internalID.substring( internalID.lastIndexOf('_') + 1 );
390	390	          }
391		-         document.getElementById( internalID ).src = params.IframeContainer.uri +
392		-                 "#rpctoken=" + securityToken + tunnelText + idText;
391		+ 
392		+         var iframe = document.getElementById( internalID );
393		+         if(iframe.attachEvent) {
394		+           //Works for IE
395		+           iframe.attachEvent('onload', function(){
396		+             window[params.IframeContainer.onGadgetLoad]();
397		+           });
398		+         } else {
399		+           iframe.onload = function(){window[params.IframeContainer.onGadgetLoad]();};
400		+         }
401		+ 
402		+         var uri = params.IframeContainer.uri;
403		+         var hashIdx = uri.indexOf('#');
404		+         var joinToken = (hashIdx === -1)?'#':'&';
405		+ 
406		+         iframe.src = uri + joinToken + "rpctoken=" + securityToken + tunnelText + idText;
393	407	      }
394		-     
408		+ 
395	409	      // If the relay iframe used by RPC has not been loaded yet, then we won't have unload protection
396	410	      // at this point.  Since we can't detect when the relay iframe has loaded, we use a two stage



2) Slight style & efficiency changes. These do not appear to add an functionality.

522	536	      this._init = function() {
523		-         var urlParams = OpenAjax.gadgets.util.getUrlParameters();
537		+         var oaGadgets = OpenAjax.gadgets;
538		+         var urlParams = oaGadgets.util.getUrlParameters();
524	539	          if ( ! urlParams.parent ) {
525	540	              // The RMR transport does not require a valid relay file, but does need a URL
526	541	              // in the parent's domain. The URL does not need to point to valid file, so just
527	542	              // point to 'robots.txt' file. See RMR transport code for more info.
528		-             var parent = urlParams.oahParent + "/robots.txt";
529		-             OpenAjax.gadgets.rpc.setupReceiver( "..", parent );
543		+             var parent = urlParams['oahParent'] + "/robots.txt";
544		+             oaGadgets.rpc.setupReceiver( "..", parent );
530	545	          }
531	546	          
532	547	          if ( params.IframeHubClient && params.IframeHubClient.requireParentVerifiable &&
533		-              OpenAjax.gadgets.rpc.getReceiverOrigin( ".." ) === null ) {
548		+                 oaGadgets.rpc.getReceiverOrigin( ".." ) === null ) {
534	549	              // If user set 'requireParentVerifiable' to true but RPC transport does not
535	550	              // support this, throw error.
536		-             OpenAjax.gadgets.rpc.removeReceiver( ".." );
551		+             oaGadgets.rpc.removeReceiver( ".." );
537	552	              throw new Error( OpenAjax.hub.Error.IncompatBrowser );
538	553	          }
539	554	          
540	555	          OpenAjax.hub.IframeContainer._rpcRouter.add( "..", this );
541	556	  // XXX The RPC layer initializes immediately on load, in the child (IframeHubClient). So it is too
542	557	  //    late here to specify a security token for the RPC layer.  At the moment, only the NIX
543	558	  //    transport requires a child token (IFPC [aka FIM] is not supported).
544	559	  //        securityToken = generateSecurityToken( params, scope, log );
545	560	  
546	561	          clientID = OpenAjax.gadgets.rpc.RPC_ID;
547		-         if ( urlParams.oahId ) {
562		+         if ( urlParams['oahId'] ) {
548	563	              clientID = clientID.substring( 0, clientID.lastIndexOf('_') );
549	564	          }
550	565	      };