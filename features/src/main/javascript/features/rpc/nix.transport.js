/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

gadgets.rpctx = gadgets.rpctx || {};

/**
 * For Internet Explorer before version 8, the security model allows anyone
 * parent to set the value of the "opener" property on another window,
 * with only the receiving window able to read it.
 * This method is dubbed "Native IE XDC" (NIX).
 *
 * This method works by placing a handler object in the "opener" property
 * of a gadget when the container sets up the authentication information
 * for that gadget (by calling setAuthToken(...)). At that point, a NIX
 * wrapper is created and placed into the gadget by calling
 * theframe.contentWindow.opener = wrapper. Note that as a result, NIX can
 * only be used by a container to call a particular gadget *after* that
 * gadget has called the container at least once via NIX.
 *
 * The NIX wrappers in this RPC implementation are instances of a VBScript
 * class that is created when this implementation loads. The reason for
 * using a VBScript class stems from the fact that any object can be passed
 * into the opener property.
 * While this is a good thing, as it lets us pass functions and setup a true
 * bidirectional channel via callbacks, it opens a potential security hole
 * by which the other page can get ahold of the "window" or "document"
 * objects in the parent page and in turn wreak havok. This is due to the
 * fact that any JS object useful for establishing such a bidirectional
 * channel (such as a function) can be used to access a function
 * (eg. obj.toString, or a function itself) created in a specific context,
 * in particular the global context of the sender. Suppose container
 * domain C passes object obj to gadget on domain G. Then the gadget can
 * access C's global context using:
 * var parentWindow = (new obj.toString.constructor("return window;"))();
 * Nulling out all of obj's properties doesn't fix this, since IE helpfully
 * restores them to their original values if you do something like:
 * delete obj.toString; delete obj.toString;
 * Thus, we wrap the necessary functions and information inside a VBScript
 * object. VBScript objects in IE, like DOM objects, are in fact COM
 * wrappers when used in JavaScript, so we can safely pass them around
 * without worrying about a breach of context while at the same time
 * allowing them to act as a pass-through mechanism for information
 * and function calls. The implementation details of this VBScript wrapper
 * can be found in the setupChannel() method below.
 *
 *   nix: Internet Explorer-specific window.opener trick.
 *     - Internet Explorer 6
 *     - Internet Explorer 7
 */
if (!gadgets.rpctx.nix) {  // make lib resilient to double-inclusion

  gadgets.rpctx.nix = function() {
    // Consts for NIX. VBScript doesn't
    // allow items to start with _ for some reason,
    // so we need to make these names quite unique, as
    // they will go into the global namespace.
    var NIX_WRAPPER = 'GRPC____NIXVBS_wrapper';
    var NIX_GET_INITIALIZER = 'GRPC____NIXVBS_get_init';
    var NIX_HANDLE_MESSAGE = 'GRPC____NIXVBS_handle_message';
    var NIX_CREATE_CHANNEL = 'GRPC____NIXVBS_create_channel';
    var NIX_CHALLENGE_RESPOND = 'GRPC____NIXVBS_challenge_resp';
    var NIX_GET_CHALLENGER = 'GRPC____NIXVBS_challenger';
    var MAX_NIX_SEARCHES = 10;
    var NIX_SEARCH_PERIOD = 500;

    // JavaScript reference to the NIX VBScript wrappers.
    // Gadgets will have but a single channel under
    // nix_channels['..'] while containers will have a channel
    // per gadget stored under the gadget's ID.
    var nix_channels = {};
    var isForceSecure = {};

    // Store the ready signal method for use on handshake complete.
    var ready;
    var numHandlerSearches = 0;

    // Search for NIX handler to parent. Tries MAX_NIX_SEARCHES times every
    // NIX_SEARCH_PERIOD milliseconds.
    function conductHandlerSearch() {
      // Call from gadget to the container.
      var handler = nix_channels['..'];
      if (handler) {
        return;
      }

      if (++numHandlerSearches > MAX_NIX_SEARCHES) {
        // Handshake failed. Will fall back.
        gadgets.warn('Nix transport setup failed, falling back...');
        ready('..', false);
        return;
      }

      // If the gadget has yet to retrieve a reference to
      // the NIX handler, start the challenge process. We don't do a
      // typeof(window.opener.GetAuthToken) check here
      // because it means accessing that field on the COM object, which,
      // being an internal function reference, is not allowed.
      // "in" works because it merely checks for the prescence of
      // the key, rather than actually accessing the object's property.
      // This is just a sanity check, not a validity check.
      if (!handler && window.opener && 'Initialize' in window.opener) {
        handler = window.opener;
        window.opener = null;

        // Send a challenge to the parent to re-ask for the auth token,
        // to ensure the parent is not being wrapped by an attacker.
        // A proper parent will respond to the challenge by passing
        // the token along with a wrapper object used to communicate.
        handler.Initialize(window[NIX_GET_CHALLENGER]());
      }

      // Try again.
      window.setTimeout(function() { conductHandlerSearch(); },
          NIX_SEARCH_PERIOD);
    }

    // Returns current window location, without hash values
    function getLocationNoHash() {
      var loc = window.location.href;
      var idx = loc.indexOf('#');
      if (idx == -1) {
        return loc;
      }
      return loc.substring(0, idx);
    }

    // When "forcesecure" is set to true, use the relay file and a simple variant of IFPC to first
    // authenticate the container and gadget with each other.  Once that is done, then initialize
    // the NIX protocol.
    function setupSecureRelayToParent(rpctoken) {
      // To the parent, transmit the child's URL, the passed in auth
      // token, and another token generated by the child.
      var childToken = (0x7FFFFFFF * Math.random()) | 0;    // TODO expose way to have child set this value
      var data = [
        getLocationNoHash(),
        childToken
      ];
      gadgets.rpc._createRelayIframe(rpctoken, data);

      // listen for response from parent
      var hash = window.location.href.split('#')[1] || '';

      function relayTimer() {
        var newHash = window.location.href.split('#')[1] || '';
        if (newHash !== hash) {
          clearInterval(relayTimerId);
          var params = gadgets.util.getUrlParameters(window.location.href);
          if (params.childtoken == childToken) {
            // parent has been authenticated; now init NIX
            conductHandlerSearch();
            return;
          }
          // security error -- token didn't match
          ready('..', false);
        }
      }
      var relayTimerId = setInterval(relayTimer, 100);
    }

    return {
      getCode: function() {
        return 'nix';
      },

      isParentVerifiable: function(opt_receiverId) {
        // NIX is only parent verifiable if a receiver was setup with "forcesecure" set to TRUE.
        if (opt_receiverId) {
          return isForceSecure[opt_receiverId];
        }
        return false;
      },

      init: function(processFn, readyFn) {
        ready = readyFn;

        // Ensure VBScript wrapper code is in the page and that the
        // global Javascript handlers have been set.
        // VBScript methods return a type of 'unknown' when
        // checked via the typeof operator in IE. Fortunately
        // for us, this only applies to COM objects, so we
        // won't see this for a real Javascript object.
        if (typeof window[NIX_GET_INITIALIZER] !== 'unknown') {
          window[NIX_HANDLE_MESSAGE] = function(data) {
            window.setTimeout(
                function() { processFn(gadgets.json.parse(data)); }, 0);
          };

          window[NIX_CREATE_CHANNEL] = function(name, channel, token) {
            if (gadgets.rpc.getAuthToken(name) == token) {
              // Re-verify the token for the child claiming to be the gadget,
              // to ensure no navigation attack has occurred.
              nix_channels[name] = channel;
              ready(name, true);
            }
          };
          
          window[NIX_CHALLENGE_RESPOND] = function(auth, wrapper) {
            var token = gadgets.rpc.getAuthToken('..');
            if (auth == token) {
              // Auth match - pass it back along with our wrapper to finish.
              // own wrapper and our authentication token for co-verification.
              wrapper.CreateChannel(window[NIX_GET_INITIALIZER]('..', token));
              
              // Set channel handler
              nix_channels['..'] = wrapper;

              // Signal success and readiness to send to parent.
              // Container-to-gadget bit flipped in CreateChannel.
              ready('..', true);
            }
          };

          // Inject the VBScript code needed.
          var vbscript =
              // We create a class to act as a wrapper for
              // a Javascript call, to prevent a break in of
              // the context.
              'Class ' + NIX_WRAPPER + '\n '

              // An internal member for keeping track of the
              // name of the document (container or gadget)
              // for which this wrapper is intended. For
              // those wrappers created by gadgets, this is not
              // used (although it is set to "..")
          + 'Private m_Intended\n'

              // Method for internally setting the value
              // of the m_Intended property.
          + 'Public Sub SetIntendedName(name)\n '
          + 'If isEmpty(m_Intended) Then\n'
          + 'm_Intended = name\n'
          + 'End If\n'
          + 'End Sub\n'
          
              // Store auth token in the wrapper as well, but do NOT
              // expose it in a Get method, which could be used by
              // an attacker wrapping a parent context to steal context.
              // This is used by the parent to validate that the child
              // calling this method is the child it created, and wasn't
              // navigated away in the meantime.
          + 'Private m_Auth\n'
          + 'Public Sub SetAuth(auth)\n'
          + 'If (isEmpty(m_Auth)) Then\n'
          + 'm_Auth = auth\n'
          + 'End If\n'
          + 'End Sub'
          
              // A wrapper method which actually causes a
              // message to be sent to the other context.
          + 'Public Sub SendMessage(data)\n '
          + NIX_HANDLE_MESSAGE + '(data)\n'
          + 'End Sub\n'

              // Method for setting up the container->gadget
              // channel. Not strictly needed in the gadget's
              // wrapper, but no reason to get rid of it. Note here
              // that we pass the intended name to the NIX_CREATE_CHANNEL
              // method so that it can save the channel in the proper place
              // *and* verify the channel via the authentication token passed
              // here.
          + 'Public Sub CreateChannel(channel)\n '
          + 'Call ' + NIX_CREATE_CHANNEL + '(m_Intended, channel, m_Auth)\n'
          + 'End Sub\n'
          + 'End Class\n'
          
              // To start up a NIX connection, parent first sets Initializer
              // on window.opener, to be read by the child. The child
              // cannot completely trust it, however, until the parent
              // proves that it knows the rpctoken it set when creating
              // the child. The window.opener wrapper can't simply have
              // a GetAuthToken() method on it, since this in turn could
              // be intercepted by a "super-parent" ie. parent.parent, which
              // can wrap the parent and replace the opener method with its
              // own. Thus the Initializer is called by the child, passing
              // its own Challenger object (below).
            'Class NixInitializer\n'
            
              // Stores the actual wrapper object passed to the child along
              // with the auth token as answer to the child's challenge.
              // Each setter is single-use only, so cannot be co-opted by
              // a wrapping context.
          + 'Private m_Wrapper\n'
          + 'Public Sub SetWrapper(wrapper)\n '
          + 'If isEmpty(m_Wrapper) Then\n'
          + 'm_Wrapper = wrapper\n'
          + 'End If'
          + 'End Sub'
          
              // Auth setter.
          + 'Private m_Auth\n'          
          + 'Public Sub SetAuth(auth)\n '
          + 'If isEmpty(m_Auth) Then\n'
          + 'm_Auth = auth\n'
          + 'End If\n'
          + 'End Sub\n'
          
              // Initialization method to which the Challenger is passed
          + 'Public Sub Initialize(challenger)\n'
          + 'challenger.Respond m_Auth, m_Wrapper\n'
          + 'End Sub'
          + 'End Class'
          
              // The child's Challenger object simply passes through to
              // JS that ensures that the parent really is the parent, by
              // checking that the passed-in Auth token is correct. If so,
              // communication channel creation occurs through CreateChannel,
              // on the passed NIX wrapper object.
          + 'Public Class Challenger\n'
          + 'Public Sub Respond(auth, wrapper)\n '
          + 'Call ' + NIX_CHALLENGE_RESPOND + '(auth, wrapper)\n'
          + 'End Sub'
          + 'End Class'  

              // Function to get a reference to the initializer, by the parent.
          + 'Function ' + NIX_GET_INITIALIZER + '(name, auth)\n'
          + 'Dim wrap\n'
          + 'Set wrap = New ' + NIX_WRAPPER + '\n'
          + 'wrap.SetIntendedName name\n'
          + 'wrap.SetAuth auth\n'
          + 'Dim init\n'
          + 'Set init = New NixInitializer\n'
          + 'init.SetAuth auth\n'
          + 'init.SetWrapper wrapper\n'
          + 'Set ' + NIX_GET_INITIALIZER + ' = init\n'
          + 'End Function'
          
              // Create challenger wrapper object to prevent context leak.
          + 'Function ' + NIX_GET_CHALLENGER + '()\n'
          + 'Set ' + NIX_GET_CHALLENGER + ' = new Challenger\n'
          + 'End Function';

          try {
            window.execScript(vbscript, 'vbscript');
          } catch (e) {
            return false;
          }
        }
        return true;
      },

      setup: function(receiverId, token, forcesecure) {
        isForceSecure[receiverId] = !!forcesecure;
        if (receiverId === '..') {
          if (forcesecure) {
            setupSecureRelayToParent(token);
          } else {
            conductHandlerSearch();
          }
          return true;
        }
        try {
          var frame = document.getElementById(receiverId);
          var initializer = window[NIX_GET_INITIALIZER](receiverId, token);
          frame.contentWindow.opener = initializer;
        } catch (e) {
          return false;
        }
        return true;
      },

      call: function(targetId, from, rpc) {
        try {
          // If we have a handler, call it.
          if (nix_channels[targetId]) {
            nix_channels[targetId].SendMessage(gadgets.json.stringify(rpc));
          }
        } catch (e) {
          return false;
        }
        return true;
      },

      // data = [child URL, child auth token]
      relayOnload: function(receiverId, data) {
        // transmit childtoken back to child to complete authentication
        var src = data[0] + '#childtoken=' + data[1];
        var childIframe = document.getElementById(receiverId);
        childIframe.src = src;
      }
    };
  }();

} // !end of double-inclusion guard
