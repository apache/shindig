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

/**
 * @fileoverview Caja is a whitelisting javascript sanitizing rewriter.
 * This file tames the APIs that are exposed to a gadget
 */

var caja___ = (function() {
    // URI policy: Rewrites all uris in a cajoled gadget
  var uriCallback = {
    rewrite: function rewrite(uri, mimeTypes) {
      uri = String(uri);
      // By default, only allow references to anchors.
      if (/^#/.test(uri)) {
        return '#' + encodeURIComponent(decodeURIComponent(uri.substring(1)));
        // and files on the same host
      } else if (/^\/(?:[^\/][^?#]*)?$/.test(uri)) {
        return encodeURI(decodeURI(uri));
      }
      // This callback can be replaced with one that passes the URL through
      // a proxy that checks the mimetype.
      return null;
    }
  };

  var tamingFunctions = [];
  // Registered a function to be called during taming
  var register = function(tamer) {
    tamingFunctions.push(tamer);
  }
  var fire = function(globalScope) {
    for (var tamer in tamingFunctions) {
      if (tamingFunctions.hasOwnProperty(tamer)) {
        // This is just tamingFunctions[tamer](globalScope)
        // but in a way that does not leak a potent "this"
        // to the taming functions
        (1, tamingFunctions[tamer])(globalScope);
      }
    }
  }
  function whitelist(schema, obj) {
      if (!obj) { return; }  // Occurs for optional features
      for (var k in schema) {
        if (schema.hasOwnProperty(k)) {
          var m = k.match(/^([mcsa])_(\w+)$/);
          var type = m[1], name = m[2];
          switch (type) {
            case 'c':
              ___.grantRead(obj, name);
              whitelist(schema[k], obj[name]);
              break;
              // grant access to a function that uses "this"
            case 'm':
              ___.grantGeneric(obj.prototype, name);
              break;
            case 'f':
              ___.grantRead(obj.prototype, name);
              break;
            case 'a': // attenuate function
              if ('function' === typeof obj[name] && schema[k]) {
                ___.handleGeneric(obj, name, schema[k](obj[name]));
              }
              break;
              // grant access to a variable or an instance
              // of a function that does not use "this"
            case 's':
              if ('function' === typeof obj[name]) {
                ___.grantFunc(obj, name);
              } else {
                ___.grantRead(obj, name);
              }
              break;
          }
        }
      }
    }

  function enable() {
    var imports = ___.copy(___.sharedImports);
    imports.outers = imports;
    
    var gadgetRoot = document.getElementById('cajoled-output');
    gadgetRoot.className = 'g___';
    document.body.appendChild(gadgetRoot);
    
    imports.htmlEmitter___ = new HtmlEmitter(gadgetRoot);
    attachDocumentStub('-g___', uriCallback, imports, gadgetRoot);
    
    imports.$v = valijaMaker.CALL___(imports.outers);
    
    ___.getNewModuleHandler().setImports(imports);
    
    fire(imports);
  }
  return {
    enable: enable,
    register: register,
    whitelist: whitelist
  };
})();

// Tame opensocial
// TODO(jasvir): Break this into smaller pieces and move to relavent
// features such the taming is only included if the feature is.
// TODO(jasvir): Express taming callbacks more succinctly
caja___.register(function(imports) {
    
  // Take a valija function and wrap it in a plain function so uncajoled
  // code can call it.
  function tameCallback($v, callback) {
    return callback && function tamedCallback() {
      return $v.cf(callback, Array.slice(arguments, 0));
    };
  };
  
  // Warning: multiple taming styles ahead...
  var taming = {
    flash: {
      embedFlash: function(orig) {
          var cleanse = (function () {
          // Gets a fresh Array and Object constructor that 
          // doesn't have the caja properties on it.  This is 
          // important for passing objects across the boundary 
          // to flash code.
          var ifr = document.createElement("iframe");
          ifr.width = 1; ifr.height = 1; ifr.border = 0;
          document.body.appendChild(ifr);
          var A = ifr.contentWindow.Array;
          var O = ifr.contentWindow.Object;
          document.body.removeChild(ifr);

          var c = function(obj) {
            var t = typeof obj, i;
            if (t === 'number' || t === 'boolean' || t === 'string') { 
                return obj; 
            }
            if (t === 'object') {
              var o;
              if (obj instanceof Array) { o = new A; }
              else if (obj instanceof Object) { o = new O; }
              for (i in obj) {
                if (/__$/.test(i)) { continue; }
                o[i] = c(obj[i]);
              }
              return o;
            }
            return (void 0);
          };

          return c;
        })();

        return ___.frozenFunc(function tamedEmbedFlash(
            swfUrl, 
            swfContainer,
            swfVersion, 
            opt_params) {
          // Check that swfContainer is a wrapped node
          if (typeof swfContainer === "string") {
            // This assumes that there's only one gadget in the frame.
            var $v = ___.getNewModuleHandler().getImports().$v;
            swfContainer = $v.cm(
                $v.ro("document"), 
                "getElementById", 
                [swfContainer]);
          } else if (typeof swfContainer !== "object" || !swfContainer.node___) {
            return false;
          }

          // Generate a random number for use as the channel name
          // for communication between the bridge and the contained
          // flash object.
          // TODO: Use true randomness.
          var channel = "_flash" + ("" + Math.random()).substring(2);

          // Strip out allowNetworking and allowScriptAccess, 
          //   as well as any caja-specific properties.
          var new_params = {};
          for (i in opt_params) {
            if (i.match(/___$/)) { continue; }
            var ilc = i.toLowerCase();
            if (ilc === "allownetworking" || ilc === "allowscriptaccess") {
              continue;
            }
            var topi = typeof opt_params[i];
            if (topi !== "string" && topi !== "number") { continue; }
            new_params[i] = opt_params[i];
          }
          new_params.allowNetworking = "never";
          new_params.allowScriptAccess = "none";
          if (!new_params.flashVars) { new_params.flashVars = ""; }
          new_params.flashVars += "&channel=" + channel;

          // Load the flash.
          orig(swfUrl, swfContainer.node___, 10, new_params);

          if (bridge___.channels) {
            // If the bridge hasn't loaded, queue up the channel names
            // for later registration
            bridge___.channels.push(channel);
          } else {
            // Otherwise, register the channel immediately.
            bridge___.registerChannel(channel);
          }

          // Return the ability to talk to the boxed swf.
          return ___.primFreeze({
            callSWF: (function (channel) { 
              return ___.func(function (methodName, argv) {
                  return bridge___.callSWF(
                      "" + channel, 
                      "" + methodName, 
                      cleanse(argv));
                });
            })(channel)
          });
        });
      }
    },

    MiniMessage: function($vs) {
      var untamedMiniMessage = gadgets.MiniMessage;
      var tamedMiniMessage = function(opt_moduleId, opt_container) {
        this.mm_ = new untamedMiniMessage(opt_moduleId, opt_container);
      };

      tamedMiniMessage.prototype.createDismissibleMessage = function(message,
                                                                     opt_callback) {
        message = html_sanitize(message);
        return this.mm_.createDismissibleMessage(message,
                                                 tameCallback($vs, opt_callback));
      };
      tamedMiniMessage.prototype.createStaticMessage = function(message,
                                                                opt_callback) {
        message = html_sanitize(message);
        return this.mm_.createStaticMessage(message,
                                            tameCallback($vs, opt_callback));
      };
      tamedMiniMessage.prototype.createTimerMessage = function(message, seconds,
                                                               opt_callback) {
        message = html_sanitize(message);
        return this.mm_.createTimerMessage(message, seconds,
                                           tameCallback($vs, opt_callback));
      };
      // FIXME: message should be a DOM element within our tree, other
      // than the root (dismissMessage deletes it).
      tamedMiniMessage.prototype.dismissMessage = function(message) {
        return this.mm_.dismissMessage(message);
      };
      return tamedMiniMessage;
    },
    
    newDataRequest: function($v, orig) {
      return function tamedNewDataRequest() {
        var dr = {
          super___: orig(),

          add: ___.frozenFunc(
              function(thing, str) {
                return this.super___.add(thing, str);
              }),
          newFetchPersonAppDataRequest: ___.frozenFunc(
              function(person, what) {
                return this.super___.newFetchPersonAppDataRequest(person, what);
              }),
          newFetchPersonRequest: ___.frozenFunc(
              function(person, opts) {
                return this.super___.newFetchPersonRequest(person, opts);
              }),
          newFetchPeopleRequest: ___.frozenFunc(
              function(person, opts) {
                return this.super___.newFetchPeopleRequest(person, opts);
              }),
          newUpdatePersonAppDataRequest: ___.frozenFunc(
              function(person, opts) {
                return this.super___.newUpdatePersonAppDataRequest(person, opts);
              }),
          send: ___.frozenFunc(
              function(callback) {
                return this.super___.send(tameCallback($v, callback));
              })
        };
        return dr;
      };
    },

    TabSet: function($v, orig) {
      var tamedTabSet = function(opt_moduleId, opt_defaultTab, opt_container) {
        this.ts_ = new orig(opt_moduleId, opt_defaultTab, opt_container);
      };
      
      tamedTabSet.prototype.addTab = function(tabName, opt_params) {
        // TODO(benl): tame the rest of opt_params
        if (opt_params) {
          opt_params.contentContainer = opt_params.contentContainer ?
          undefined : ___.guard(blah) && opt_params.contentContainer.node___;
        }
        this.ts___.addTab(html_sanitize(tabName), opt_params);
      };
      
      tamedTabSet.prototype.alignTabs = function(align, opt_offset) {
        this.ts___.alignTabs(String(align), Number(opt_offset));
      };
      
      tamedTabSet.prototype.displayTabs = function(display) {
        this.ts___.displayTabs(Boolean(display));
      };
      
      return tamedTabSet;
    },
    
    util: {
      registerOnLoadHandler: function($v, orig) {
        return function tamedRegisterOnLoadHandler(callback) {
          orig(tameCallback($v, callback));
        };
      }
    },
    
    views: {
      // note, we are going to monkey-patch just this function instead of wrapping the whole of views...
      getCurrentView: function(orig) {
        return function tamedGetCurrentView() {
          // Note, taming decision was s___, so maybe we don't need this?
          var view = orig.call(this);
          ___.grantGeneric(view, 'getName');
          ___.grantGeneric(view, 'isOnlyVisibleGadget');
          return view;
        };
      }
    }
  };
      
  // The below described the opensocial reference APIs.
  // A prefix of "c_" specifies a class, "m_" a method, "f_" a field,
  // and "s_" a static member.
  // Derived from http://code.google.com/apis/opensocial/docs/0.8/reference/ .
  var opensocialSchema = {
    c_gadgets: {
      c_MiniMessage: {
        m_createDismissibleMessage: 0,
        m_createStaticMessage: 0,
        m_createTimerMessage: 0,
        m_dismissMessage: 0
      },
      c_Prefs: {
        m_getArray: 0,
        m_getBool: 0,
        m_getCountry: 0,
        m_getFloat: 0,
        m_getInt: 0,
        m_getLang: 0,
        m_getMsg: 0,
        m_getString: 0,
        m_set: 0,
        m_setArray: 0
      },
      c_Tab: {
        m_getCallback: 0,
        m_getContentContainer: 0,
        m_getIndex: 0,
        m_getName: 0,
        m_getNameContainer: 0
      },
      c_TabSet: {
        m_addTab: 0
        //        m_alignTabs: 0,
        //        m_displayTabs: 0,
        //        m_getHeaderContainer: 0,
        //        m_getSelectedTab: 0,
        //        m_getTabs: 0,
        //        m_removeTab: 0,
        //        m_setSelectedTab: 0,
        //        m_swapTabs: 0
      },
      c_flash: {
        s_embedCachedFlash: 0,
        s_embedFlash: 0,
        s_getMajorVersion: 0
      },
      c_io: {
        c_AuthorizationType: {
          s_NONE: 0,
          s_OAUTH: 0,
          s_SIGNED: 0
        },
        c_ContentType: {
          s_DOM: 0,
          s_FEED: 0,
          s_JSON: 0,
          s_TEXT: 0
        },
        c_MethodType: {
          s_DELETE: 0,
          s_GET: 0,
          s_HEAD: 0,
          s_POST: 0,
          s_PUT: 0
        },
        c_ProxyUrlRequestParameters: {
          s_REFRESH_INTERVAL: 0
        },
        c_RequestParameters: {
          s_AUTHORIZATION: 0,
          s_CONTENT_TYPE: 0,
          s_GET_SUMMARIES: 0,
          s_HEADERS: 0,
          s_METHOD: 0,
          s_NUM_ENTRIES: 0,
          s_POST_DATA: 0
        },
        s_encodeValues: 0,
        s_getProxyUrl: 0,
        s_makeRequest: 0
      },
      c_json: {
        s_parse: 0,
        s_stringify: 0
      },
      c_pubsub: {
        s_publish: 0,
        s_subscribe: 0,
        s_unsubscribe: 0
      },
      c_rpc: {
        s_call: 0,
        s_register: 0,
        s_registerDefault: 0,
        s_unregister: 0,
        s_unregisterDefault: 0
      },
      c_skins: {
        c_Property: {
          s_ANCHOR_COLOR: 0,
          s_BG_COLOR: 0,
          s_BG_IMAGE: 0,
          s_FONT_COLOR: 0
        },
        s_getProperty: 0
      },
      c_util: {
        s_escapeString: 0,
        s_getFeatureParameters: 0,
        s_hasFeature: 0,
        s_registerOnLoadHandler: 0,
        s_unescapeString: 0
      },
      c_views: {
        c_View: {
          m_bind: 0,
          m_getUrlTemplate: 0,
          m_isOnlyVisibleGadget: 0
        },
        c_ViewType: {
          s_CANVAS: 0,
          s_HOME: 0,
          s_PREVIEW: 0,
          s_PROFILE: 0
        },
        s_bind: 0,
        // FIXME(benl): Why do we think getCurrentView does not use "this"?
        s_getCurrentView: 0,
        s_getParams: 0,
        s_requestNavigateTo: 0
      },
      c_window: {
        s_adjustHeight: 0,
        s_getViewportDimensions: 0,
        s_setTitle: 0
      }
    },
    c_opensocial: {
      c_Activity: {
        c_Field: {
          s_APP_ID: 0,
          s_BODY: 0,
          s_BODY_ID: 0,
          s_EXTERNAL_ID: 0,
          s_ID: 0,
          s_MEDIA_ITEMS: 0,
          s_POSTED_TIME: 0,
          s_PRIORITY: 0,
          s_STREAM_FAVICON_URL: 0,
          s_STREAM_SOURCE_URL: 0,
          s_STREAM_TITLE: 0,
          s_STREAM_URL: 0,
          s_TEMPLATE_PARAMS: 0,
          s_TITLE: 0,
          s_TITLE_ID: 0,
          s_URL: 0,
          s_USER_ID: 0
        },
        m_getField: 0,
        m_getId: 0,
        m_setField: 0
      },
      c_Address: {
        c_Field: {
          s_COUNTRY: 0,
          s_EXTENDED_ADDRESS: 0,
          s_LATITUDE: 0,
          s_LOCALITY: 0,
          s_LONGITUDE: 0,
          s_POSTAL_CODE: 0,
          s_PO_BOX: 0,
          s_REGION: 0,
          s_STREET_ADDRESS: 0,
          s_TYPE: 0,
          s_UNSTRUCTURED_ADDRESS: 0
        },
        m_getField: 0
      },
      c_BodyType: {
        c_Field: {
          s_BUILD: 0,
          s_EYE_COLOR: 0,
          s_HAIR_COLOR: 0,
          s_HEIGHT: 0,
          s_WEIGHT: 0
        },
        m_getField: 0
      },
      c_Collection: {
        m_asArray: 0,
        m_each: 0,
        m_getById: 0,
        m_getOffset: 0,
        m_getTotalSize: 0,
        m_size: 0
      },
      c_CreateActivityPriority: {
        s_HIGH: 0,
        s_LOW: 0
      },
      c_DataRequest: {
        c_DataRequestFields: {
          s_ESCAPE_TYPE: 0
        },
        c_FilterType: {
          s_ALL: 0,
          s_HAS_APP: 0,
          s_TOP_FRIENDS: 0
        },
        c_PeopleRequestFields: {
          s_FILTER: 0,
          s_FILTER_OPTIONS: 0,
          s_FIRST: 0,
          s_MAX: 0,
          s_PROFILE_DETAILS: 0,
          s_SORT_ORDER: 0
        },
        c_SortOrder: {
          s_NAME: 0,
          s_TOP_FRIENDS: 0
        },
        m_add: 0,
        m_newFetchActivitiesRequest: 0,
        m_newFetchPeopleRequest: 0,
        m_newFetchPersonAppDataRequest: 0,
        m_newFetchPersonRequest: 0,
        m_newRemovePersonAppDataRequest: 0,
        m_newUpdatePersonAppDataRequest: 0,
        m_send: 0
      },
      c_DataResponse: {
        m_get: 0,
        m_getErrorMessage: 0,
        m_hadError: 0
      },
      c_Email: {
        c_Field: {
          s_ADDRESS: 0,
          s_TYPE: 0
        },
        m_getField: 0
      },
      c_Enum: {
        c_Drinker: {
          s_HEAVILY: 0,
          s_NO: 0,
          s_OCCASIONALLY: 0,
          s_QUIT: 0,
          s_QUITTING: 0,
          s_REGULARLY: 0,
          s_SOCIALLY: 0,
          s_YES: 0
        },
        c_Gender: {
          s_FEMALE: 0,
          s_MALE: 0
        },
        c_LookingFor: {
          s_ACTIVITY_PARTNERS: 0,
          s_DATING: 0,
          s_FRIENDS: 0,
          s_NETWORKING: 0,
          s_RANDOM: 0,
          s_RELATIONSHIP: 0
        },
        c_Presence: {
          s_AWAY: 0,
          s_CHAT: 0,
          s_DND: 0,
          s_OFFLINE: 0,
          s_ONLINE: 0,
          s_XA: 0
        },
        c_Smoker: {
          s_HEAVILY: 0,
          s_NO: 0,
          s_OCCASIONALLY: 0,
          s_QUIT: 0,
          s_QUITTING: 0,
          s_REGULARLY: 0,
          s_SOCIALLY: 0,
          s_YES: 0
        },
        m_getDisplayValue: 0,
        m_getKey: 0
      },
      c_Environment: {
        c_ObjectType: {
          s_ACTIVITY: 0,
          s_ACTIVITY_MEDIA_ITEM: 0,
          s_ADDRESS: 0,
          s_BODY_TYPE: 0,
          s_EMAIL: 0,
          s_FILTER_TYPE: 0,
          s_MESSAGE: 0,
          s_MESSAGE_TYPE: 0,
          s_NAME: 0,
          s_ORGANIZATION: 0,
          s_PERSON: 0,
          s_PHONE: 0,
          s_SORT_ORDER: 0,
          s_URL: 0
        },
        m_getDomain: 0,
        m_supportsField: 0
      },
      c_EscapeType: {
        s_HTML_ESCAPE: 0,
        s_NONE: 0
      },
      c_IdSpec: {
        c_Field: {
          s_GROUP_ID: 0,
          s_NETWORK_DISTANCE: 0,
          s_USER_ID: 0
        },
        c_PersonId: {
          s_OWNER: 0,
          s_VIEWER: 0
        },
        m_getField: 0,
        m_setField: 0
      },
      c_MediaItem: {
        c_Field: {
          s_MIME_TYPE: 0,
          s_TYPE: 0,
          s_URL: 0
        },
        c_Type: {
          s_AUDIO: 0,
          s_IMAGE: 0,
          s_VIDEO: 0
        },
        m_getField: 0,
        m_setField: 0
      },
      c_Message: {
        c_Field: {
          s_BODY: 0,
          s_BODY_ID: 0,
          s_TITLE: 0,
          s_TITLE_ID: 0,
          s_TYPE: 0
        },
        c_Type: {
          s_EMAIL: 0,
          s_NOTIFICATION: 0,
          s_PRIVATE_MESSAGE: 0,
          s_PUBLIC_MESSAGE: 0
        },
        m_getField: 0,
        m_setField: 0
      },
      c_Name: {
        c_Field: {
          s_ADDITIONAL_NAME: 0,
          s_FAMILY_NAME: 0,
          s_GIVEN_NAME: 0,
          s_HONORIFIC_PREFIX: 0,
          s_HONORIFIC_SUFFIX: 0,
          s_UNSTRUCTURED: 0
        },
        m_getField: 0
      },
      c_NavigationParameters: {
        c_DestinationType: {
          s_RECIPIENT_DESTINATION: 0,
          s_VIEWER_DESTINATION: 0
        },
        c_Field: {
          s_OWNER: 0,
          s_PARAMETERS: 0,
          s_VIEW: 0
        },
        m_getField: 0,
        m_setField: 0
      },
      c_Organization: {
        c_Field: {
          s_ADDRESS: 0,
          s_DESCRIPTION: 0,
          s_END_DATE: 0,
          s_FIELD: 0,
          s_NAME: 0,
          s_SALARY: 0,
          s_START_DATE: 0,
          s_SUB_FIELD: 0,
          s_TITLE: 0,
          s_WEBPAGE: 0
        },
        m_getField: 0
      },
      c_Permission: {
        s_VIEWER: 0
      },
      c_Person: {
        c_Field: {
          s_ABOUT_ME: 0,
          s_ACTIVITIES: 0,
          s_ADDRESSES: 0,
          s_AGE: 0,
          s_BODY_TYPE: 0,
          s_BOOKS: 0,
          s_CARS: 0,
          s_CHILDREN: 0,
          s_CURRENT_LOCATION: 0,
          s_DATE_OF_BIRTH: 0,
          s_DRINKER: 0,
          s_EMAILS: 0,
          s_ETHNICITY: 0,
          s_FASHION: 0,
          s_FOOD: 0,
          s_GENDER: 0,
          s_HAPPIEST_WHEN: 0,
          s_HAS_APP: 0,
          s_HEROES: 0,
          s_HUMOR: 0,
          s_ID: 0,
          s_INTERESTS: 0,
          s_JOBS: 0,
          s_JOB_INTERESTS: 0,
          s_LANGUAGES_SPOKEN: 0,
          s_LIVING_ARRANGEMENT: 0,
          s_LOOKING_FOR: 0,
          s_MOVIES: 0,
          s_MUSIC: 0,
          s_NAME: 0,
          s_NETWORK_PRESENCE: 0,
          s_NICKNAME: 0,
          s_PETS: 0,
          s_PHONE_NUMBERS: 0,
          s_POLITICAL_VIEWS: 0,
          s_PROFILE_SONG: 0,
          s_PROFILE_URL: 0,
          s_PROFILE_VIDEO: 0,
          s_QUOTES: 0,
          s_RELATIONSHIP_STATUS: 0,
          s_RELIGION: 0,
          s_ROMANCE: 0,
          s_SCARED_OF: 0,
          s_SCHOOLS: 0,
          s_SEXUAL_ORIENTATION: 0,
          s_SMOKER: 0,
          s_SPORTS: 0,
          s_STATUS: 0,
          s_TAGS: 0,
          s_THUMBNAIL_URL: 0,
          s_TIME_ZONE: 0,
          s_TURN_OFFS: 0,
          s_TURN_ONS: 0,
          s_TV_SHOWS: 0,
          s_URLS: 0
        },
        m_getDisplayName: 0,
        m_getField: 0,
        m_getId: 0,
        m_isOwner: 0,
        m_isViewer: 0
      },
      c_Phone: {
        c_Field: {
          s_NUMBER: 0,
          s_TYPE: 0
        },
        m_getField: 0
      },
      c_ResponseItem: {
        c_Error: {
          s_BAD_REQUEST: 0,
          s_FORBIDDEN: 0,
          s_INTERNAL_ERROR: 0,
          s_LIMIT_EXCEEDED: 0,
          s_NOT_IMPLEMENTED: 0,
          s_UNAUTHORIZED: 0
        },
        m_getData: 0,
        m_getErrorCode: 0,
        m_getErrorMessage: 0,
        m_getOriginalDataRequest: 0,
        m_hadError: 0
      },
      c_Url: {
        c_Field: {
          s_ADDRESS: 0,
          s_LINK_TEXT: 0,
          s_TYPE: 0
        },
        m_getField: 0
      },
      s_getEnvironment: 0,
      s_hasPermission: 0,
      s_newActivity: 0,
      s_newDataRequest: 0,
      s_newIdSpec: 0,
      s_newMediaItem: 0,
      s_newMessage: 0,
      s_newNavigationParameters: 0,
      s_requestCreateActivity: 0,
      s_requestPermission: 0,
      s_requestSendMessage: 0,
      s_requestShareApp: 0
    }
  };
  
  // Taming
  if (gadgets.flash) {
    var d = document.createElement('div');
    d.appendChild(document.createTextNode("bridge"));
    document.body.appendChild(d);
    
    gadgets.flash.embedFlash(
        "/gadgets/files/container/Bridge.swf", 
        d,
        10,
        {
          allowNetworking: "always",
          allowScriptAccess: "all",
          width: 0,
          height: 0,
          flashvars: "logging=true"
        });
    bridge___ = d.childNodes[0];
    bridge___.channels = [];

    callJS = function (functionName, argv) {
      // This assumes that there's a single gadget in the frame.
      var $v = ___.getNewModuleHandler().getImports().$v;
      return $v.cf($v.ro(functionName), [argv]);
    };
    
    onFlashBridgeReady = function () {
      var len = bridge___.channels.length;
      for(var i = 0; i < len; ++i) {
        bridge___.registerChannel(bridge___.channels[i]);
      }
      delete bridge___.channels;
      var outers = ___.getNewModuleHandler().getImports().$v.getOuters();
      if (outers.onFlashBridgeReady) {
        callJS("onFlashBridgeReady");
      }
    };

    gadgets.flash.embedFlash =
        taming.flash.embedFlash(gadgets.flash.embedFlash);
  }
  
  if (window.gadgets) 
    imports.outers.gadgets = gadgets;
  if (window.opensocial)
    imports.outers.opensocial = opensocial; 

  // Temprorary taming for callbacks
  // TODO(jasvir): Replace with new automatic taming api once its complete
  if (window.gadgets) {
    gadgets.util.registerOnLoadHandler
      = taming.util.registerOnLoadHandler(imports.$v,
                                          gadgets.util.registerOnLoadHandler);
    if (gadgets.flash) {
      gadgets.flash.embedFlash
        = taming.flash.embedFlash(gadgets.flash.embedFlash);
    }
    if (gadgets.views) {
      gadgets.views.getCurrentView
        = taming.views.getCurrentView(gadgets.views.getCurrentView);
    }
    if (gadgets.MiniMessage) {
      gadgets.MiniMessage = taming.MiniMessage(imports.$v);
    }
    if (gadgets.TabSet) {
      gadgets.TabSet = taming.TabSet(imports.$v, gadgets.TabSet);
    }
  }
  if (window.opensocial) {
    opensocial.newDataRequest = taming.newDataRequest(imports.$v,
                                                      opensocial.newDataRequest);
  }
  
  caja___.whitelist(opensocialSchema, imports.outers);
  if (gadgets.MiniMessage)
    ___.ctor(gadgets.MiniMessage, Object, 'MiniMessage');
  if (gadgets.TabSet)
    ___.ctor(gadgets.TabSet, Object, 'TabSet');
});

// Expose alert and console.log to cajoled programs
caja___.register(function(imports) {
  imports.outers.alert = function(msg) { alert(msg); };
  ___.grantFunc(imports.outers, 'alert');

  if (console && console.log) {
    imports.outers.console = console;
    ___.grantRead(imports.outers, 'console');
    ___.grantFunc(imports.outers.console, 'log');
  }
});
