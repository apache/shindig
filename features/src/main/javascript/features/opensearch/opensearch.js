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

/**
 * @fileoverview This library adds open search support to the container.
 */

(function() {
  // array of opensearch descriptors.
  var descriptions = {};
  var ids = {};
  // open search callbacks.
  var callbacks = new Array();

  /**
   * Converts an XML string into a DOM object
   *
   * @param {string} xmlString representation of a valid XML object.
   * @return DOM Object
   */
  function createDom(xmlString) {
    var xmlDoc;
    if (window.DOMParser) {
      var parser = new DOMParser();
      xmlDoc = parser.parseFromString(xmlString, 'text/xml');
    } else {
      xmlDoc = new ActiveXObject('Microsoft.XMLDOM');
      xmlDoc.async = 'false';
      xmlDoc.loadXML(xmlString);
    }
    return xmlDoc;
  }

  /**
   * Extracts OpenSearch descriptions from XML string containing the
   * description and stores them in the internal map If valid
   * descriptions are found, notifies all callbacks about changes.
   *
   * @param {string} domDesc
   *            stringified XML OpenSearch description.
   * @param {string} title
   *            the title of the gadget that the description belongs to.
   */
  function extractDescriptions(domDesc, url) {
    var jsonDesc = gadgets.json.xml.convertXmlToJson(domDesc);
    if (jsonDesc != null) {
      if (jsonDesc.OpenSearchDescription.Url != null) {
        if (descriptions[url] == null) {
          descriptions[url] = jsonDesc;
          applyCallbacks(true, jsonDesc);
        }
      }
    }
  }

  /**
   * Notifies the registered callbacks of changes in the OpenSearch registry.
   *
   * @param {boolean} added
   *            true if added, false if removed.
   * @param {string} description opensearch description.
   */
  function applyCallbacks(added, description) {
    for (var i in callbacks) {
      callbacks[i].apply(this, [description, added]);
    }
  }

  /**
   * Removes an opensearch description from the registry after the containing
   * gadget is unloaded or closed.
   *
   * @param {string} url Url of the gadget to be removed.
   */
  function removeDescription(url) {
    if (descriptions[url] != null) {
      applyCallbacks(false, descriptions[url]);
      delete descriptions[url];
    }
  }

  /**
   * Processes a new gadget definition and checks if it has an opensearch
   * feature
   *
   * @param {Array} response
   *            Metadata response containing the json representation of the
   *            gadget module.
   */
  function preloaded(response) {
    for (var item in response) {
      if (!response[item].error) {
        // check for os feature
        var feature = response[item].modulePrefs.features['opensearch'];
        var title = response[item].modulePrefs.title;
        if (feature != null) {
          var params = feature.params;
          if (params != null) {
            // retrieve the description
            var desc = params['opensearch-description'];
            // The full description is in the gadget
            if (desc != null) {
              // convert string to dom object
              var domDesc = createDom(desc);
              // convert dom object to json
              extractDescriptions(domDesc, response[item].url);
              // only the url to the full description is provided.
            } else {
              var openSearchUrl = params['opensearch-url'];
              if (openSearchUrl != null) {
                function urlCallback(response) {
                  if (response.errors.length == 0) {
                    var domData = response.data;
                    if (domData != null) {
                      extractDescriptions(domData, response[item].url);
                    }
                  }
                }
                var params = {};
                params[gadgets.io.RequestParameters.CONTENT_TYPE] =
                    gadgets.io.ContentType.DOM;
                gadgets.io.makeRequest(openSearchUrl, urlCallback, params);
              }
            }
          }
        }
      }
    }
  }

  /**
   * When a gadget is closed, checks if it has a corresponding OpenSearch
   * definition that needs to be removed, and notifies the appropriate
   * callbacks.
   *
   * @param {gadgetSite}
   *            site of the gadget being closed.
   */
  function closed(gadgetSite) {
    if (gadgetSite != null) {
      url = ids[gadgetSite.getId()];
      removeDescription(url);
    }
  }

  /**
   * called when a gadget is navigated to.
   *
   * @param {gadgetInfo}
   *            json object representing the gadget module.
   */
  function navigated(gadgetSite) {
    if (gadgetSite != null) {
      if (gadgetSite.getActiveSiteHolder() != null) {
        url = gadgetSite.getActiveSiteHolder().getUrl();
        if (descriptions[url] == null) {
          preloaded([gadgetSite.getActiveSiteHolder().getGadgetInfo()]);
        }
        ids[gadgetSite.getId()] = url;
      }
    }
  }

  /**
   * called when a gadget is unloaded
   *
   * @param {gadgetURL}
   *            the gadget url of the unloaded gadget.
   */
  function unloaded(gadgetURL) {
    // do nothing--this doesn't guarantee the gadget is actually removed.
  }

  /**
   * finds opensearch descriptions/urls based on the mimetype of the search
   * results
   *
   * @param {type}
   *            mimeType of the search results.
   * @param {isUrl}
   *            true if looking for template urls, false if looking for full
   *            OpenSearch descriptions.
   */
  var findByType = function(type, isUrl) {
    var typedDescriptions = [];
    for (url in descriptions) {
      var searchUrls = [];
      if (!(descriptions[url].OpenSearchDescription.Url instanceof Array)) {
        searchUrls.push(descriptions[url].OpenSearchDescription.Url);
      } else {
        searchUrls = descriptions[url].OpenSearchDescription.Url;
      }
      var found = false;
      // go through all the urls in a description.
      // if a description contains a template of the type, the
      // entire description is returned. For URLs, only the matching
      // template url is returned.
      for (var i in searchUrls) {
        var template = searchUrls[i]['@template'];
        if (template != null) {
          var descType = searchUrls[i]['@type'];
          if (descType == type || type == null) {
            if (isUrl) {
              typedDescriptions.push(template);
            } else {
              typedDescriptions.push(descriptions[url]);
              break;
            }
          }
        }
      }
    }
    return typedDescriptions;
  }

  var containerCallback = new Object();
  containerCallback[osapi.container.CallbackType.ON_PRELOADED] =
      preloaded;
  containerCallback[osapi.container.CallbackType.ON_BEFORE_CLOSE] =
      closed;
  containerCallback[osapi.container.CallbackType.ON_NAVIGATED] =
      navigated;

  osapi.container.Container.addMixin('opensearch', function(context) {
    context.addGadgetLifecycleCallback('opensearch', containerCallback);

    return /** @scope container.opensearch */ {
    /**
     * @param {type} type type name, eg search-xml, search-atom, search-hmtl.
     * @return opensearch template URLs of a given type, or all URLs if type
     *         was null.
     */
    getOpenSearchURLs: function(type) {
      return findByType(type, true);
    },

    /**
     * @param {type}
     *            type type name, eg search-xml, search-atom, search-html.
     * @return Returns OpenSearch descriptions of a given type, or all
     *         descriptions if type was null.
     */
    getOpenSearchDescriptions: function(type) {
      return findByType(type, false);
    },

    /**
     * Allows other functions to subscribe to changes in the OpenSearch
     * registry.
     *
     * @param {callback}
     *           function(description, boolean added), where description is the
     *           opensearch description being updated and where added is true
     *           if the gadget is new, and false if it has been
     *           closed/unloaded.
     *
     */
    addOpenSearchCallback: function(callback) {
      callbacks.push(callback);
    },

    /**
     * Removes a previously registered callback
     *
     * @param {callback}
     *            previously registered callback function.
     * @return {boolean} true if function was present, false otherwise.
     *
     */
    removeOpenSearchCallback: function(callback) {
      for (index in callbacks) {
        if (callbacks[index] == callback) {
          callbacks.splice(index, 1);
          return true;
        }
      }
      return false;
    }

    /*
     * Convenience method for unit testing, allowing the test script to set
     * internal descriptions. Uncomment for unit testing.
     *
     */
    //setDescriptions_: function(testDescriptions) {
    // descriptions=testDescriptions;
    //}
    };
  });
})();

