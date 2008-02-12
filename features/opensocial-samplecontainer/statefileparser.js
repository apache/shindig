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
 *
 * @fileoverview This file parses state files for the in memory container.
 */


/**
 * State file parsing class for the sample container
 *
 * @constructor
 */
var StateFileParser = function() {};


/**
 * Activity fields that make up the state file's stream object
 */
StateFileParser.STREAM_FIELDS = ['userId', 'streamTitle', 'streamUrl',
    'streamFaviconUrl', 'streamSourceUrl'];


/**
 * Make an ajax request to fetch the XML file.
 */
StateFileParser.refreshState = function(stateUrl, gadgetMessageDiv,
                                        container, callback) {
  // Make sure we have a valid state URL
  if (!stateUrl) {
    gadgetMessageDiv.innerHTML = 'Please enter a container state url. ';
    callback();
    return;
  }

  // Fetch the container state XML
  gadgetMessageDiv.innerHTML =
  'Loading container state from ' + stateUrl + '<br>' +
  'If the state does not load make sure your URLs are in the same ' +
  'domain as this page.';

  var me = this;


  $.ajax({type: "GET", url: stateUrl, dataType: "xml", timeout: 5000,
    error: function() {
      gadgetMessageDiv.innerHTML
          = 'Cannot fetch container state from ' + stateUrl;
      callback();
    },
    success: function(xmlState) {
      StateFileParser.onLoadState(xmlState, stateUrl, gadgetMessageDiv,
          container, callback);
    }
  }); // ajax request
};


/**
 * This function will get called after successful download of the
 * container state XML.
 * @private
 */
StateFileParser.onLoadState = function(xmlState, stateUrl, gadgetMessageDiv,
    container, callback) {
  // Get the high level container node
  var containerNode = $(xmlState).find('container')[0];
  if (!containerNode) {
    gadgetMessageDiv.innerHTML
        = 'Invalid container state XML at ' + stateUrl;
    callback();
    return;
  }

  // Get the viewer node
  var viewer;
  var viewerNode = $(containerNode).find('viewer')[0];
  if (viewerNode) {
    viewer = StateFileParser.loadPerson(container,
        $(viewerNode).find('person')[0], false, true);
  }

  // Get the owner node
  var owner;
  var ownerNode = $(containerNode).find('owner')[0];
  if (ownerNode) {
    owner = StateFileParser.loadPerson(container,
        $(ownerNode).find('person')[0], true);
  }

  // If the id of the owner is the same as the viewer, then set the viewer
  // as the primary source of truth
  if (!owner || (viewer && owner.getId() == viewer.getId())) {
    owner = viewer;
    owner.isViewer_ = true;
    owner.isOwner_ = true;
  }

  // Build the friends list
  var me = this;
  var viewerFriends = new Array();
  var friendsNode = $(containerNode).find('viewerFriends')[0];
  $(friendsNode).find('person').each(function() {
    viewerFriends.push(StateFileParser.loadPerson(container, $(this)));
  });
  var ownerFriends = new Array();
  friendsNode = $(containerNode).find('ownerFriends')[0];
  $(friendsNode).find('person').each(function() {
    ownerFriends.push(StateFileParser.loadPerson(container, $(this)));
  });

  // Build the App data
  var personAppData = {};
  var personDataNode = $(containerNode).find('personAppData')[0];
  if (personDataNode) {
    $(personDataNode).find('data').each(function() {
      if (personAppData[$(this).attr('person')] == null) {
        personAppData[$(this).attr('person')] = {};
      }
      personAppData[$(this).attr('person')][$(this).attr('field')]
          = $(this).text();
    });
  }


  // Build the activities list
  var appIdNode = $(containerNode).find('appId')[0];
  var appId = appIdNode ? $(appIdNode).text() : 'sampleContainerAppId';

  var activities = {};
  var activitiesNode = $(containerNode).find('activities')[0];
  $(activitiesNode).find('stream').each(function() {
    var userId = $(this).attr('userId');
    var streamTitle = $(this).attr('title');
    var streamUrl = $(this).attr('url');
    var streamSourceUrl = $(this).attr('sourceUrl');
    var streamFaviconUrl = $(this).attr('faviconUrl');

    activities[userId] = [];

    $(this).find('activity').each(function() {
      var mediaItems = [];
      $(this).find('mediaItem').each(function() {
        mediaItems.push(container.newActivityMediaItem(
            $(this).attr('mimeType'),
            $(this).attr('url'),
        {'type' : $(this).attr('type')}));
      });
      activities[userId].push(container.newActivity(
      {'title' : $(this).attr('title'),
        'id' : $(this).attr('id'),
        'externalId' : $(this).attr('externalId'),
        'body' : $(this).attr('body'),
        'appId' : appId,
        'userId' : userId,
        'streamTitle' : streamTitle,
        'streamUrl' : streamUrl,
        'streamSourceUrl' : streamSourceUrl,
        'streamFaviconUrl' : streamFaviconUrl,
        'url' : $(this).attr('url'),
        'postedTime' : $(this).attr('postedTime'),
        'mediaItems' : mediaItems}));
    });
  });

  // Initialize the sample container with the state that has been read
  container.resetData(viewer, owner,
      container.newCollection(viewerFriends),
      container.newCollection(ownerFriends),
      personAppData, activities, appId);
  callback();
};


/**
 * load a person related info from the XML node. Person could be
 * viewer, owner or friend. Return value is the person object.
 * @private
 */
StateFileParser.loadPerson = function(container, xmlNode, isOwner, isViewer) {
  var fields = {
    'id' : $(xmlNode).attr(opensocial.Person.Field.ID),
    'name' : new opensocial.Name(
        {'unstructured' : $(xmlNode).attr(opensocial.Person.Field.NAME)}),
    'thumbnailUrl' : $(xmlNode).attr(opensocial.Person.Field.THUMBNAIL_URL),
    'profileUrl' : $(xmlNode).attr(opensocial.Person.Field.PROFILE_URL)};
  return container.newPerson(fields, isOwner, isViewer);
};


/**
 * Dumps the current state of the container in XML.
 */
StateFileParser.dumpState = function(container, stateDiv) {
  var xmlText = '<container>\n';

  xmlText += '  <viewer>\n';
  xmlText += StateFileParser.dumpPerson(container.viewer);
  xmlText += '  </viewer>\n';

  xmlText += '  <owner>\n';
  xmlText += StateFileParser.dumpPerson(container.owner);
  xmlText += '  </owner>\n';

  xmlText += '  <viewerFriends>\n';
  container.viewerFriends.each(function(friend) {
    xmlText += StateFileParser.dumpPerson(friend);
  });
  xmlText += '  </viewerFriends>\n';

  xmlText += '  <ownerFriends>\n';
  container.ownerFriends.each(function(friend) {
    xmlText += StateFileParser.dumpPerson(friend);
  });
  xmlText += '  </ownerFriends>\n';

  // Dump App Data
  xmlText += '  <personAppData>\n';
  for (var person in container.personAppData) {
    if (___.canInnocentEnum(container.personAppData, person)) {
      for (var field in container.personAppData[person]) {
        if (___.canInnocentEnum(container.personAppData[person], field)) {
          xmlText += '    <data person="' + person + '" ';
          xmlText += 'field="' + field + '">';
          xmlText += container.personAppData[person][field];
          xmlText += '</data>\n';
        }
      }
    }
  }
  xmlText += '  </personAppData>\n';

  // Dump the activities. Since only 1 stream is supported, use the first one
  xmlText += '  <activities>\n';
  var streamWritten = false;
  for (var id in container.activities) {
    if (___.canInnocentEnum(container.activities, id)) {
      var activity = container.activities[id];
      if (!streamWritten) {
        var streamTitle = activity.getField('streamTitle');
        if (!streamTitle) {
          continue;
        }
        xmlText += '    <stream';
        for (var field in StateFileParser.STREAM_FIELDS) {
          if (___.canInnocentEnum(StateFileParser.STREAM_FIELDS, field)) {
            var value = activity.getField(field);
            if (value == null) {
              continue;
            }
            xmlText += ' ' + field + '="' + value + '"';
          }
        }
        xmlText += '>\n';
        streamWritten = true;
      }

      xmlText += '      <activity';
      for (var field in activity.fields_) {
        if (___.canInnocentEnum(activity.fields_, field)) {
          var value = activity.getField(field);
          if (value == null || field == 'mediaItems'
              || field in StateFileParser.STREAM_FIELDS) {
            continue;
          }
          xmlText += ' ' + field + '="' + value + '"';
        }
      }
      xmlText += '>';
      var mediaItems = activity.mediaItems;
      for (var i = 0; mediaItems && i < mediaItems.length; i++) {
        var mediaItem = mediaItem[i];
        xmlText += '        <mediaItem ';
        if (mediaItem.mimeType) {
          xmlText += ' mimeType="' + mediaItem.mimeType + '"';
        }
        if (mediaItem.url) {
          xmlText += ' url="' + mediaItem.url + '"';
        }
        if (mediaItem.opt_params && mediaItem.opt_params.type) {
          xmlText += ' type="' + mediaItem.opt_params.type + '"';
        }
        xmlText += '/>\n';
      }
      xmlText += '</activity>\n';
    }
  }
  if (streamWritten) {
    xmlText += '    </stream>\n';
  }
  xmlText += '  </activities>\n';

  xmlText += '</container>';
  stateDiv.value = xmlText;
};


/**
 * @private
 */
StateFileParser.dumpPersonField = function(personObj, name) {
  var field = personObj.getField(name);
  if (field) {
    return ' ' + name + '="' + field + '"';
  }
  return '';
};


/**
 * Dump the state of a person object.
 * @private
 */
StateFileParser.dumpPerson = function(personObj) {
  var xmlText = '    <person';
  xmlText += StateFileParser.dumpPersonField(personObj,
      opensocial.Person.Field.ID);
  xmlText += StateFileParser.dumpPersonField(personObj,
      opensocial.Person.Field.THUMBNAIL_URL);
  xmlText += StateFileParser.dumpPersonField(personObj,
      opensocial.Person.Field.PROFILE_URL);

  // TODO: Change the sample container to understand all of the name fields
  var name = personObj.getField(opensocial.Person.Field.NAME);
  if (name) {
    var unstructured = name.getField(opensocial.Name.Field.UNSTRUCTURED);
    if (unstructured) {
      xmlText += ' name="' + unstructured + '"';
    }
  }

  xmlText += '></person>\n';
  return xmlText;
};
