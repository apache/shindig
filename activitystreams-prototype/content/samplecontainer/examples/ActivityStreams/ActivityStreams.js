/**
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

var ActivityStreams = new function() {

	// =============================== PEOPLE ===============================
	// Loads viewer and friends
	this.loadPeople = function() {
		var req = opensocial.newDataRequest();
		req.add(req.newFetchPersonRequest(opensocial.IdSpec.PersonId.VIEWER), 'viewer');
		req.add(req.newFetchPersonRequest(opensocial.IdSpec.PersonId.OWNER), 'owner');
		var idSpec = opensocial.newIdSpec({'userId':'VIEWER', 'groupId':'FRIENDS'});
		req.add(req.newFetchPeopleRequest(idSpec), 'viewerFriends');
		req.send(this.onLoadPeople);	
	}

	// Callback for loading viewer and friends
	this.onLoadPeople = function(response) {
		viewer = response.get('viewer').getData();					// Load viewer (type: person)
		owner = response.get('owner').getData();					// Load owner (type: person)
		viewerFriends = response.get('viewerFriends').getData();	// Load viewer's friends (type: Collection<Person>)
		CommentStream.renderWelcome();
	}

	// Loads viewer and friends using OSAPI
	this.loadFriendsOsapi = function() {
		var batch = osapi.newBatch();
		batch.add('viewer', osapi.people.getViewer());
		batch.add('viewerFriends', osapi.people.getViewerFriends());
		batch.execute(this.onLoadFriendsOsapi);
	}

	// Callback for loading viewer and friends using OSAPI
	this.onLoadFriendsOsapi = function(response) {
		viewer = response.viewer;				// Type: JSON object
		viewerFriends = response.viewerFriends;	// Type: JSON object
		alert(JSON.stringify(response));		// Prints the raw JSON response :D
	}
	
	// ========================= ACTIVITY STREAMS =============================
	// Creates and sends an ActivityEntry
	this.postActivityEntry = function(comment) {
		var params = {
			userId: '@viewer',
			groupId: '@self',
			activityEntry: {
				id: 'myEntryId',
				permalink: 'http://www.myactivityentry1.com',
				postedTime: '2010-04-27T06:02:36+0000',
				title: 'My Comment',
				body: comment,
				actor: {
					id: 'john.doe',
					displayName: 'Eric Woods'
				},
				verb: ['play', 'post'],
				object: {
						id: 'activityObjectID',
						displayName: 'My Object',
						permalinkUrl: 'http://www.myobject.com',
						objectType: ['event', 'meetup']
				}
			}
		}
		
		// Send the request and register callback
		osapi.activitystreams.create(params).execute(this.onPostActivityEntry);
	}
	this.onPostActivityEntry = function(response) {
		//alert('onPostActivityEntry: ' + JSON.stringify(response));
	}
	
	// Deletes the ActivityEntry with the given id
	this.deleteActivityEntry = function(activityEntryId) {
		// Generate request
		var params = {
			userId: '@viewer',
			groupId: '@self',
			activityEntryId: activityEntryId
		}
		
		// Send request
		osapi.activitystreams.delete(params).execute(this.onDeleteActivityEntry);
	}
	this.onDeleteActivityEntry = function(response) {
		//alert('onDeleteActivityEntry: ' + JSON.stringify(response));
	}
	
	// Loads the ActivityEntries of the viewer
	this.loadActivityEntriesViewer = function() {
		var params = {userId: '@viewer', groupId: '@self'}
		osapi.activitystreams.get(params).execute(this.onLoadActivityEntriesViewer);
	}
	this.onLoadActivityEntriesViewer = function(response) {
		viewerActivityEntries = response;
		//alert('onLoadActivityEntriesViewer: ' + JSON.stringify(response));
	}
	
	// Loads the ActivityEntries of the viewer's friends
	this.loadActivityEntriesFriends = function() {
		var params = {userId: '@viewer', groupId: '@friends'}
		osapi.activitystreams.get(params).execute(this.onLoadActivityEntriesFriends);
	}
	this.onLoadActivityEntriesFriends = function(response) {
		//alert('onLoadActivityEntriesFriends: ' + JSON.stringify(response));
		friendActivityEntries = response;
	}
	
	// Loads the ActivityEntry with the given ID
	this.loadActivityEntryId = function(activityEntryId) {
		var params = {activityEntryIds: ['myEntryID', activityEntryId]};
		osapi.activitystreams.get(params).execute(this.onLoadActivityEntryId);
	}
	this.onLoadActivityEntryId = function(response) {
		alert('onLoadActivityEntryId: ' + JSON.stringify(response));
	}
	
	// ============================== ACTIVITIES ==============================
	// Gets the activities of the viewer
	this.loadActivitiesViewer = function() {
		var req = osapi.activities.get({userId: '@viewer', groupId: '@self'});
		req.execute(this.onLoadActivitiesViewer);
	}
	
	// Callback to get the activities of the viewer
	this.onLoadActivitiesViewer = function(response) {
		viewerActivities = response;
		//alert(JSON.stringify(response));		// Prints the raw JSON response :D
	}
	
	// Gets the activities of the viewer's friends
	this.loadActivitiesFriends = function() {
		var req = osapi.activities.get({userId: '@viewer', groupId: '@friends'});
		req.execute(this.onLoadActivitiesFriends);
	}
	
	// Callback to get the activities of the viewer's friends
	this.onLoadActivitiesFriends = function(response) {
		friendActivities = response;
		//alert(JSON.stringify(response));		// Prints the raw JSON response :D
	}
	
	// Creates and sends an activity.
	this.postActivity = function(title, body, photoURL, photoUploaded) {
		//alert('postActivity(' + title + ', ' + body + ',' + photoURL + ',' + photoUploaded + ')');
		
		if(false && photoURL != '') {
			alert('uploading image at URL: ' + photoURL);
			
			var params = {};
			params[opensocial.MediaItem.Field.MIME_TYPE] = 'image/jpeg';
			params[opensocial.MediaItem.Field.TYPE] = opensoical.MediaItem.Type.IMAGE;
			params[opensocial.MediaItem.Field.URL] = photoURL;
			var media = opensocial.newMediaItem('image/jpeg', photoURL, params);
			var req = opensocial.newDataRequest();
			var idSpec = opensocial.newIdSpec({'userId':'VIEWER', 'groupId':'FRIENDS'});
			req.add(req.newCreateMediaItemRequest(idSpec, '1', media), 'media');
			req.send(function(response) {
				alert('respone!');
				alert(JSON.stringify(response));
			});
		}
			
		var params = {
			auth: {"default" : null, "type" : "AuthToken"},
			userId: '@viewer',
			groupId: '@self',
			activity: {
				userId: viewer.getId(),
				title: title,
				body: body,
				updated: '2009-06-01T12:54:00Z'
			}
		};
		osapi.activities.create(params).execute(this.onPostActivity);
	}
	
	// Callback for posting activities
	this.onPostActivity = function(response) {
		alert(JSON.stringify(response));		// Prints the raw JSON response :D
	}
}