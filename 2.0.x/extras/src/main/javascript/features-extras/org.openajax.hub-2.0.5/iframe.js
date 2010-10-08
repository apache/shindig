/*

        Copyright 2006-2009 OpenAjax Alliance

        Licensed under the Apache License, Version 2.0 (the "License"); 
        you may not use this file except in compliance with the License. 
        You may obtain a copy of the License at
        
                http://www.apache.org/licenses/LICENSE-2.0

        Unless required by applicable law or agreed to in writing, software 
        distributed under the License is distributed on an "AS IS" BASIS, 
        WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
        See the License for the specific language governing permissions and 
        limitations under the License.
*/

var OpenAjax = OpenAjax || {};
OpenAjax.hub = OpenAjax.hub || {};
OpenAjax.gadgets = typeof OpenAjax.gadgets === 'object' ? OpenAjax.gadgets :
                   typeof gadgets === 'object' ? gadgets :
                   {};
OpenAjax.gadgets.rpctx = OpenAjax.gadgets.rpctx || {};

(function() {
    // For now, we only use "oaaConfig" for the global "gadgets" object.  If the "gadgets" global
    // already exists, then there is no reason to check for "oaaConfig".  In the future, if we use
    // "oaaConfig" for other purposes, we'll need to remove the check for "!window.gadgets".
    if (typeof gadgets === 'undefined') {
        // "oaaConfig" can be specified as a global object.  If not found, then look for it as an
        // attribute on the script line for the OpenAjax Hub JS file.
        if (typeof oaaConfig === 'undefined') {
            var scripts = document.getElementsByTagName("script");
            // match "OpenAjax-mashup.js", "OpenAjaxManagedHub-all*.js", "OpenAjaxManagedHub-core*.js"
            var reHub = /openajax(?:managedhub-(?:all|core).*|-mashup)\.js$/i;
            for ( var i = scripts.length - 1; i >= 0; i-- ) {
                var src = scripts[i].getAttribute( "src" );
                if ( !src ) {
                    continue;
                }
                
                var m = src.match( reHub );
                if ( m ) {
                    var config = scripts[i].getAttribute( "oaaConfig" );
                    if ( config ) {
                        try {
                            oaaConfig = eval( "({ " + config + " })" );
                        } catch (e) {}
                    }
                    break;
                }
            }
        }
        
        if (typeof oaaConfig !== 'undefined' && oaaConfig.gadgetsGlobal) {
            gadgets = OpenAjax.gadgets;
        }
    }
})();


if (!OpenAjax.hub.IframeContainer) {

(function(){

/**
 * Create a new Iframe Container.
 * @constructor
 * @extends OpenAjax.hub.Container
 * 
 * IframeContainer implements the Container interface to provide a container
 * that isolates client components into secure sandboxes by leveraging the
 * isolation features provided by browser iframes.
 * 
 * SECURITY
 * 
 * In order for the connection between the IframeContainer and IframeHubClient
 * to be fully secure, you must specify a valid 'tunnelURI'. Note that if you
 * do specify a 'tunnelURI', then only the WPM and NIX transports are used,
 * covering the following browsers:
 *   IE 6+, Firefox 3+, Safari 4+, Chrome 2+, Opera 9+.
 * 
 * If no 'tunnelURI' is specified, then some security features are disabled:
 * the IframeContainer will not report FramePhish errors, and on some browsers
 * IframeContainer and IframeHubClient will not be able to validate the
 * identity of their partner (i.e. getPartnerOrigin() will return 'null').
 * However, not providing 'tunnelURI' allows the additional use of the RMR
 * and FE transports -- in addition to the above browsers, the Hub code will
 * also work on:
 *   Firefox 1 & 2, Safari 2 & 3, Chrome 1.
 * 
 * @param {OpenAjax.hub.ManagedHub} hub
 *    Managed Hub instance to which this Container belongs
 * @param {String} clientID
 *    A string ID that identifies a particular client of a Managed Hub. Unique
 *    within the context of the ManagedHub.
 * @param {Object} params  
 *    Parameters used to instantiate the IframeContainer.
 *    Once the constructor is called, the params object belongs exclusively to
 *    the IframeContainer. The caller MUST not modify it.
 *    The following are the pre-defined properties on params:
 * @param {Function} params.Container.onSecurityAlert
 *    Called when an attempted security breach is thwarted.  Function is defined
 *    as follows:  function(container, securityAlert)
 * @param {Function} [params.Container.onConnect]
 *    Called when the client connects to the Managed Hub.  Function is defined
 *    as follows:  function(container)
 * @param {Function} [params.Container.onDisconnect]
 *    Called when the client disconnects from the Managed Hub.  Function is
 *    defined as follows:  function(container)
 * @param {Object} [params.Container.scope]
 *    Whenever one of the Container's callback functions is called, references
 *    to "this" in the callback will refer to the scope object. If no scope is
 *    provided, default is window.
 * @param {Function} [params.Container.log]
 *    Optional logger function. Would be used to log to console.log or
 *    equivalent. 
 * @param {Object} params.IframeContainer.parent
 *    DOM element that is to be parent of iframe
 * @param {String} params.IframeContainer.uri
 *    Initial Iframe URI (Container will add parameters to this URI)
 * @param {String} [params.IframeContainer.tunnelURI]
 *    URI of the tunnel iframe. Must be from the same origin as the page which
 *    instantiates the IframeContainer. If not specified, connection will not
 *    be fully secure (see SECURITY section).
 * @param {Object} [params.IframeContainer.iframeAttrs]
 *    Attributes to add to IFRAME DOM entity.  For example:
 *              { style: { width: "100%",
 *                         height: "100%" },
 *                className: "some_class" }
 * @param {Number} [params.IframeContainer.timeout]
 *    Load timeout in milliseconds.  If not specified, defaults to 15000.  If
 *    the client at params.IframeContainer.uri does not establish a connection
 *    with this container in the given time, the onSecurityAlert callback is
 *    called with a LoadTimeout error code.
 * @param {Function} [params.IframeContainer.seed]
 *    A function that returns a string that will be used to seed the
 *    pseudo-random number generator, which is used to create the security
 *    tokens.  An implementation of IframeContainer may choose to ignore this
 *    value.
 * @param {Number} [params.IframeContainer.tokenLength]
 *    Length of the security tokens used when transmitting messages.  If not
 *    specified, defaults to 6.  An implementation of IframeContainer may choose
 *    to ignore this value.
 *
 * @throws {OpenAjax.hub.Error.BadParameters}   if required params are not
 *          present or null
 * @throws {OpenAjax.hub.Error.Duplicate}   if a Container with this clientID
 *          already exists in the given Managed Hub
 * @throws {OpenAjax.hub.Error.Disconnected}   if hub is not connected
 */
OpenAjax.hub.IframeContainer = function( hub, clientID, params )
{
    assertValidParams( arguments );
    
    var container = this;
    var scope = params.Container.scope || window;
    var connected = false;
    var subs = {};
    var securityToken;
    var internalID;
    var timeout = params.IframeContainer.timeout || 15000;
    var loadTimer;

    if ( params.Container.log ) {
        var log = function( msg ) {
            try {
                params.Container.log.call( scope, "IframeContainer::" + clientID + ": " + msg );
            } catch( e ) {
                OpenAjax.hub._debugger();
            }
        };
    } else {
        log = function() {};
    }
    
    
    this._init = function() {
        // add to ManagedHub first, to see if clientID is a duplicate
        hub.addContainer( this );
        
        // Create an "internal" ID, which is guaranteed to be unique within the
        // window, not just within the hub.
        internalID = OpenAjax.hub.IframeContainer._rpcRouter.add( clientID, this );
        securityToken = generateSecurityToken( params, scope, log );
        
        var relay = null;
        var transportName = OpenAjax.gadgets.rpc.getRelayChannel();
        if ( params.IframeContainer.tunnelURI ) {
            if ( transportName !== "wpm" && transportName !== "nix" ) {
                throw new Error( OpenAjax.hub.Error.IncompatBrowser );
            }
        } else {
            log( "WARNING: Parameter 'IframeContaienr.tunnelURI' not specified. Connection will not be fully secure." );
            if ( transportName === "rmr" ) {
                relay = OpenAjax.gadgets.rpc.getOrigin( params.IframeContainer.uri ) + "/robots.txt"; 
            }
        }
        
        // Create IFRAME to hold the client
        createIframe();
        
        OpenAjax.gadgets.rpc.setupReceiver( internalID, relay );
        
        startLoadTimer();
    };

        
  /*** OpenAjax.hub.Container interface ***/
   
    this.sendToClient = function( topic, data, subscriptionID ) {
        OpenAjax.gadgets.rpc.call( internalID, "openajax.pubsub", null, "pub", topic, data,
                                   subscriptionID );
    };

    this.remove = function() {
        finishDisconnect();
        clearTimeout( loadTimer );
        OpenAjax.gadgets.rpc.removeReceiver( internalID );
        var iframe = document.getElementById( internalID );
        iframe.parentNode.removeChild( iframe );
        OpenAjax.hub.IframeContainer._rpcRouter.remove( internalID );
    };

    this.isConnected = function() {
        return connected;
    };
    
    this.getClientID = function() {
        return clientID;
    };

    this.getPartnerOrigin = function() {
        if ( connected ) {
            var origin = OpenAjax.gadgets.rpc.getReceiverOrigin( internalID );
            if ( origin ) {
                // remove port if present
                return ( /^([a-zA-Z]+:\/\/[^:]+).*/.exec( origin )[1] );
            }
        }
        return null;
    };
    
    this.getParameters = function() {
        return params;
    };
    
    this.getHub = function() {
        return hub;
    };
    
    
  /*** OpenAjax.hub.IframeContainer interface ***/
    
    /**
     * Get the iframe associated with this iframe container
     * 
     * This function returns the iframe associated with an IframeContainer,
     * allowing the Manager Application to change its size, styles, scrollbars, etc.
     * 
     * CAUTION: The iframe is owned exclusively by the IframeContainer. The Manager
     * Application MUST NOT destroy the iframe directly. Also, if the iframe is
     * hidden and disconnected, the Manager Application SHOULD NOT attempt to make
     * it visible. The Container SHOULD automatically hide the iframe when it is
     * disconnected; to make it visible would introduce security risks. 
     * 
     * @returns iframeElement
     * @type {Object}
     */
    this.getIframe = function() {
        return document.getElementById( internalID );
    };
    
    
  /*** private functions ***/

    function assertValidParams( args ) {
        var hub = args[0],
            clientID = args[1],
            params = args[2];
        if ( ! hub || ! clientID || ! params || ! params.Container ||
             ! params.Container.onSecurityAlert || ! params.IframeContainer ||
             ! params.IframeContainer.parent || ! params.IframeContainer.uri ) {
            throw new Error( OpenAjax.hub.Error.BadParameters );
        }
    }
    
    this._handleIncomingRPC = function( command, topic, data ) {
        switch ( command ) {
            // publish
            // 'data' is topic message
            case "pub":
                hub.publishForClient( container, topic, data );
                break;
            
            // subscribe
            // 'data' is subscription ID
            case "sub":
                var errCode = "";  // empty string is success
                try {
                    subs[ data ] = hub.subscribeForClient( container, topic, data );
                } catch( e ) {
                    errCode = e.message;
                }
                return errCode;
            
            // unsubscribe
            // 'data' is subscription ID
            case "uns":
                var handle = subs[ data ];
                hub.unsubscribeForClient( container, handle );
                delete subs[ data ];
                return data;
            
            // connect
            case "con":
                finishConnect();
                return true;
            
            // disconnect
            case "dis":
                startLoadTimer();
                finishDisconnect();
                if ( params.Container.onDisconnect ) {
                    try {
                        params.Container.onDisconnect.call( scope, container );
                    } catch( e ) {
                        OpenAjax.hub._debugger();
                        log( "caught error from onDisconnect callback to constructor: " + e.message );
                    }
                }
                return true;
        }
    };
    
    this._onSecurityAlert = function( error ) {
        invokeSecurityAlert( rpcErrorsToOAA[ error ] );
    };
    
    // The RPC code requires that the 'name' attribute be properly set on the
    // iframe.  However, setting the 'name' property on the iframe object
    // returned from 'createElement("iframe")' doesn't work on IE --
    // 'window.name' returns null for the code within the iframe.  The
    // workaround is to set the 'innerHTML' of a span to the iframe's HTML code,
    // with 'name' and other attributes properly set.
    function createIframe() {
        var span = document.createElement( "span" );
        params.IframeContainer.parent.appendChild( span );
        
        var iframeText = '<iframe id="' + internalID + '" name="' + internalID +
                '" src="javascript:\'<html></html>\'"';
        
        // Add iframe attributes
        var styleText = '';
        var attrs = params.IframeContainer.iframeAttrs;
        if ( attrs ) {
            for ( var attr in attrs ) {
                switch ( attr ) {
                    case "style":
                        for ( var style in attrs.style ) {
                            styleText += style + ':' + attrs.style[ style ] + ';';
                        }
                        break;
                    case "className":
                        iframeText += ' class="' + attrs[ attr ] + '"';
                        break;
                    default:
                        iframeText += ' ' + attr + '="' + attrs[ attr ] + '"';
                }
            }
        }
        
        // initially hide IFRAME content, in order to lessen frame phishing impact
        styleText += 'visibility:hidden;';
        iframeText += ' style="' + styleText + '"></iframe>';
        
        span.innerHTML = iframeText;
        
        var tunnelText;
        if ( params.IframeContainer.tunnelURI ) {
            tunnelText = "&parent=" + encodeURIComponent( params.IframeContainer.tunnelURI ) +
                         "&forcesecure=true";
        } else {
            tunnelText = "&oahParent=" +
                         encodeURIComponent( OpenAjax.gadgets.rpc.getOrigin( window.location.href ));
        }
        var idText = "";
        if ( internalID !== clientID ) {
            idText = "&oahId=" + internalID.substring( internalID.lastIndexOf('_') + 1 );
        }
        document.getElementById( internalID ).src = params.IframeContainer.uri +
                "#rpctoken=" + securityToken + tunnelText + idText;
    }
    
    // If the relay iframe used by RPC has not been loaded yet, then we won't have unload protection
    // at this point.  Since we can't detect when the relay iframe has loaded, we use a two stage
    // connection process.  First, the child sends a connection msg and the container sends an ack.
    // Then the container sends a connection msg and the child replies with an ack.  Since the
    // container can only send a message if the relay iframe has loaded, then we know if we get an
    // ack here that the relay iframe is ready.  And we are fully connected.
    function finishConnect() {
        // connect acknowledgement
        function callback( result ) {
            if ( result ) {
                connected = true;
                clearTimeout( loadTimer );
                document.getElementById( internalID ).style.visibility = "visible";
                if ( params.Container.onConnect ) {
                    try {
                        params.Container.onConnect.call( scope, container );
                    } catch( e ) {
                        OpenAjax.hub._debugger();
                        log( "caught error from onConnect callback to constructor: " + e.message );
                    }
                }
            }
        }
        OpenAjax.gadgets.rpc.call( internalID, "openajax.pubsub", callback, "cmd", "con" );
    }
    
    function finishDisconnect() {
        if ( connected ) {
            connected = false;
            document.getElementById( internalID ).style.visibility = "hidden";
        
            // unsubscribe from all subs
            for ( var s in subs ) {
                hub.unsubscribeForClient( container, subs[s] );
            }
            subs = {};
        }
    }
    
    function invokeSecurityAlert( errorMsg ) {
        try {
            params.Container.onSecurityAlert.call( scope, container, errorMsg );
        } catch( e ) {
            OpenAjax.hub._debugger();
            log( "caught error from onSecurityAlert callback to constructor: " + e.message );
        }
    }
    
    function startLoadTimer() {
        loadTimer = setTimeout(
            function() {
                // alert the security alert callback
                invokeSecurityAlert( OpenAjax.hub.SecurityAlert.LoadTimeout );
                // don't receive any more messages from HubClient
                container._handleIncomingRPC = function() {};
            },
            timeout
        );
    }
    
    
    this._init();
};

////////////////////////////////////////////////////////////////////////////////

/**
 * Create a new IframeHubClient.
 * @constructor
 * @extends OpenAjax.hub.HubClient
 * 
 * @param {Object} params
 *    Once the constructor is called, the params object belongs to the
 *    HubClient. The caller MUST not modify it.
 *    The following are the pre-defined properties on params:
 * @param {Function} params.HubClient.onSecurityAlert
 *     Called when an attempted security breach is thwarted
 * @param {Object} [params.HubClient.scope]
 *     Whenever one of the HubClient's callback functions is called,
 *     references to "this" in the callback will refer to the scope object.
 *     If not provided, the default is window.
 * @param {Function} [params.HubClient.log]
 *     Optional logger function. Would be used to log to console.log or
 *     equivalent. 
 * @param {Boolean} [params.IframeHubClient.requireParentVerifiable]
 *     Set to true in order to require that this IframeHubClient use a
 *     transport that can verify the parent Container's identity.
 * @param {Function} [params.IframeHubClient.seed]
 *     A function that returns a string that will be used to seed the
 *     pseudo-random number generator, which is used to create the security
 *     tokens.  An implementation of IframeHubClient may choose to ignore
 *     this value.
 * @param {Number} [params.IframeHubClient.tokenLength]
 *     Length of the security tokens used when transmitting messages.  If
 *     not specified, defaults to 6.  An implementation of IframeHubClient
 *     may choose to ignore this value.
 *     
 * @throws {OpenAjax.hub.Error.BadParameters} if any of the required
 *          parameters is missing, or if a parameter value is invalid in 
 *          some way.
 */
OpenAjax.hub.IframeHubClient = function( params )
{
    if ( ! params || ! params.HubClient || ! params.HubClient.onSecurityAlert ) {
        throw new Error( OpenAjax.hub.Error.BadParameters );
    }
    
    var client = this;
    var scope = params.HubClient.scope || window;
    var connected = false;
    var subs = {};
    var subIndex = 0;
    var clientID;
//    var securityToken;    // XXX still need "securityToken"?
    
    if ( params.HubClient.log ) {
        var log = function( msg ) {
            try {
                params.HubClient.log.call( scope, "IframeHubClient::" + clientID + ": " + msg );
            } catch( e ) {
                OpenAjax.hub._debugger();
            }
        };
    } else {
        log = function() {};
    }
    
    this._init = function() {
        var urlParams = OpenAjax.gadgets.util.getUrlParameters();
        if ( ! urlParams.parent ) {
            // The RMR transport does not require a valid relay file, but does need a URL
            // in the parent's domain. The URL does not need to point to valid file, so just
            // point to 'robots.txt' file. See RMR transport code for more info.
            var parent = urlParams.oahParent + "/robots.txt";
            OpenAjax.gadgets.rpc.setupReceiver( "..", parent );
        }
        
        if ( params.IframeHubClient && params.IframeHubClient.requireParentVerifiable &&
             OpenAjax.gadgets.rpc.getReceiverOrigin( ".." ) === null ) {
            // If user set 'requireParentVerifiable' to true but RPC transport does not
            // support this, throw error.
            OpenAjax.gadgets.rpc.removeReceiver( ".." );
            throw new Error( OpenAjax.hub.Error.IncompatBrowser );
        }
        
        OpenAjax.hub.IframeContainer._rpcRouter.add( "..", this );
// XXX The RPC layer initializes immediately on load, in the child (IframeHubClient). So it is too
//    late here to specify a security token for the RPC layer.  At the moment, only the NIX
//    transport requires a child token (IFPC [aka FIM] is not supported).
//        securityToken = generateSecurityToken( params, scope, log );

        clientID = OpenAjax.gadgets.rpc.RPC_ID;
        if ( urlParams.oahId ) {
            clientID = clientID.substring( 0, clientID.lastIndexOf('_') );
        }
    };
    
  /*** HubClient interface ***/

    this.connect = function( onComplete, scope ) {
        if ( connected ) {
            throw new Error( OpenAjax.hub.Error.Duplicate );
        }
        
        // connect acknowledgement
        function callback( result ) {
            if ( result ) {
                connected = true;
                if ( onComplete ) {
                    try {
                        onComplete.call( scope || window, client, true );
                    } catch( e ) {
                        OpenAjax.hub._debugger();
                        log( "caught error from onComplete callback to connect(): " + e.message );
                    }
                }
            }
        }
        OpenAjax.gadgets.rpc.call( "..", "openajax.pubsub", callback, "con" );
    };
    
    this.disconnect = function( onComplete, scope ) {
        if ( !connected ) {
            throw new Error( OpenAjax.hub.Error.Disconnected );
        }
        
        connected = false;
        
        // disconnect acknowledgement
        var callback = null;
        if ( onComplete ) {
            callback = function( result ) {
                try {
                    onComplete.call( scope || window, client, true );
                } catch( e ) {
                    OpenAjax.hub._debugger();
                    log( "caught error from onComplete callback to disconnect(): " + e.message );
                }
            };
        }
        OpenAjax.gadgets.rpc.call( "..", "openajax.pubsub", callback, "dis" );
    };
    
    this.getPartnerOrigin = function() {
        if ( connected ) {
            var origin = OpenAjax.gadgets.rpc.getReceiverOrigin( ".." );
            if ( origin ) {
                // remove port if present
                return ( /^([a-zA-Z]+:\/\/[^:]+).*/.exec( origin )[1] );
            }
        }
        return null;
    };
    
    this.getClientID = function() {
        return clientID;
    };
    
  /*** Hub interface ***/
    
    this.subscribe = function( topic, onData, scope, onComplete, subscriberData ) {
        assertConn();
        assertSubTopic( topic );
        if ( ! onData ) {
            throw new Error( OpenAjax.hub.Error.BadParameters );
        }
    
        scope = scope || window;
        var subID = "" + subIndex++;
        subs[ subID ] = { cb: onData, sc: scope, d: subscriberData };
        
        // subscribe acknowledgement
        function callback( result ) {
            if ( result !== '' ) {    // error
                delete subs[ subID ];
            }
            if ( onComplete ) {
                try {
                    onComplete.call( scope, subID, result === "", result );
                } catch( e ) {
                    OpenAjax.hub._debugger();
                    log( "caught error from onComplete callback to subscribe(): " + e.message );
                }
            }
        }
        OpenAjax.gadgets.rpc.call( "..", "openajax.pubsub", callback, "sub", topic, subID );
        
        return subID;
    };
    
    this.publish = function( topic, data ) {
        assertConn();
        assertPubTopic( topic );
        OpenAjax.gadgets.rpc.call( "..", "openajax.pubsub", null, "pub", topic, data );
    };
    
    this.unsubscribe = function( subscriptionID, onComplete, scope ) {
        assertConn();
        if ( ! subscriptionID ) {
            throw new Error( OpenAjax.hub.Error.BadParameters );
        }
        
        // if no such subscriptionID, or in process of unsubscribing given ID, throw error
        if ( ! subs[ subscriptionID ] || subs[ subscriptionID ].uns ) {
            throw new Error( OpenAjax.hub.Error.NoSubscription );
        }
        
        // unsubscribe in progress
        subs[ subscriptionID ].uns = true;
        
        // unsubscribe acknowledgement
        function callback( result ) {
            delete subs[ subscriptionID ];
            if ( onComplete ) {
                try {
                    onComplete.call( scope || window, subscriptionID, true );
                } catch( e ) {
                    OpenAjax.hub._debugger();
                    log( "caught error from onComplete callback to unsubscribe(): " + e.message );
                }
            }
        }
        OpenAjax.gadgets.rpc.call( "..", "openajax.pubsub", callback, "uns", null, subscriptionID );
    };
    
    this.isConnected = function() {
        return connected;
    };
    
    this.getScope = function() {
        return scope;
    };
    
    this.getSubscriberData = function( subscriptionID ) {
        assertConn();
        if ( subs[ subscriptionID ] ) {
            return subs[ subscriptionID ].d;
        }
        throw new Error( OpenAjax.hub.Error.NoSubscription );
    };
    
    this.getSubscriberScope = function( subscriptionID ) {
        assertConn();
        if ( subs[ subscriptionID ] ) {
            return subs[ subscriptionID ].sc;
        }
        throw new Error( OpenAjax.hub.Error.NoSubscription );
    };
    
    this.getParameters = function() {
        return params;
    };
    
  /*** private functions ***/
    
    this._handleIncomingRPC = function( command, topic, data, subscriptionID ) {
        if ( command === "pub" ) {
            // if subscription exists and we are not in process of unsubscribing...
            if ( subs[ subscriptionID ] && ! subs[ subscriptionID ].uns ) {
                try {
                    subs[ subscriptionID ].cb.call( subs[ subscriptionID ].sc, topic,
                            data, subs[ subscriptionID ].d );
                } catch( e ) {
                    OpenAjax.hub._debugger();
                    log( "caught error from onData callback to subscribe(): " + e.message );
                }
            }
        }
        // else if command === "cmd"...
        
        // First time this function is called, topic should be "con".  This is the 2nd stage of the
        // connection process.  Simply need to return "true" in order to send an acknowledgement
        // back to container.  See finishConnect() in the container object.
        if ( topic === "con" ) {
          return true;
        }
        return false;
    };
    
    function assertConn() {
        if ( ! connected ) {
            throw new Error( OpenAjax.hub.Error.Disconnected );
        }
    }
    
    function assertSubTopic( topic )
    {
        if ( ! topic ) {
            throw new Error( OpenAjax.hub.Error.BadParameters );
        }
        var path = topic.split(".");
        var len = path.length;
        for (var i = 0; i < len; i++) {
            var p = path[i];
            if ((p === "") ||
               ((p.indexOf("*") != -1) && (p != "*") && (p != "**"))) {
                throw new Error( OpenAjax.hub.Error.BadParameters );
            }
            if ((p == "**") && (i < len - 1)) {
                throw new Error( OpenAjax.hub.Error.BadParameters );
            }
        }
    }
    
    function assertPubTopic( topic ) {
        if ( !topic || topic === "" || (topic.indexOf("*") != -1) ||
            (topic.indexOf("..") != -1) ||  (topic.charAt(0) == ".") ||
            (topic.charAt(topic.length-1) == "."))
        {
            throw new Error( OpenAjax.hub.Error.BadParameters );
        }
    }
    
//    function invokeSecurityAlert( errorMsg ) {
//        try {
//            params.HubClient.onSecurityAlert.call( scope, client, errorMsg );
//        } catch( e ) {
//            OpenAjax.hub._debugger();
//            log( "caught error from onSecurityAlert callback to constructor: " + e.message );
//        }
//    }

    
    this._init();
};

////////////////////////////////////////////////////////////////////////////////

    // RPC object contents:
    //   s: Service Name
    //   f: From
    //   c: The callback ID or 0 if none.
    //   a: The arguments for this RPC call.
    //   t: The authentication token.
OpenAjax.hub.IframeContainer._rpcRouter = function() {
    var receivers = {};
    
    function router() {
        var r = receivers[ this.f ];
        if ( r ) {
            return r._handleIncomingRPC.apply( r, arguments );
        }
    }
    
    function onSecurityAlert( receiverId, error ) {
        var r = receivers[ receiverId ];
        if ( r ) {
          r._onSecurityAlert.call( r, error );
        }
    }
    
    return {
        add: function( id, receiver ) {
            function _add( id, receiver ) {
                if ( id === ".." ) {
                    if ( ! receivers[ ".." ] ) {
                        receivers[ ".." ] = receiver;
                    }
                    return;
                }
                
                var newId = id;
                while ( document.getElementById(newId) ) {
                    // a client with the specified ID already exists on this page;
                    // create a unique ID
                    newId = id + '_' + ((0x7fff * Math.random()) | 0).toString(16);
                };
                receivers[ newId ] = receiver;
                return newId;
            }
            
            // when this function is first called, register the RPC service
            OpenAjax.gadgets.rpc.register( "openajax.pubsub", router );
            OpenAjax.gadgets.rpc.config({
                securityCallback: onSecurityAlert
            });

            rpcErrorsToOAA[ OpenAjax.gadgets.rpc.SEC_ERROR_LOAD_TIMEOUT ] = OpenAjax.hub.SecurityAlert.LoadTimeout;
            rpcErrorsToOAA[ OpenAjax.gadgets.rpc.SEC_ERROR_FRAME_PHISH ] = OpenAjax.hub.SecurityAlert.FramePhish;
            rpcErrorsToOAA[ OpenAjax.gadgets.rpc.SEC_ERROR_FORGED_MSG ] = OpenAjax.hub.SecurityAlert.ForgedMsg;
            
            this.add = _add;
            return _add( id, receiver );
        },
        
        remove: function( id ) {
            delete receivers[ id ];
        }
    };
}();

var rpcErrorsToOAA = {};

////////////////////////////////////////////////////////////////////////////////

function generateSecurityToken( params, scope, log ) {
    if ( ! OpenAjax.hub.IframeContainer._prng ) {
        // create pseudo-random number generator with a default seed
        var seed = new Date().getTime() + Math.random() + document.cookie;
        OpenAjax.hub.IframeContainer._prng = OpenAjax._smash.crypto.newPRNG( seed );
    }
    
    var p = params.IframeContainer || params.IframeHubClient;
    if ( p && p.seed ) {
        try {
            var extraSeed = p.seed.call( scope );
            OpenAjax.hub.IframeContainer._prng.addSeed( extraSeed );
        } catch( e ) {
            OpenAjax.hub._debugger();
            log( "caught error from 'seed' callback: " + e.message );
        }
    }
    
    var tokenLength = (p && p.tokenLength) || 6;
    return OpenAjax.hub.IframeContainer._prng.nextRandomB64Str( tokenLength );
}

})();
}
