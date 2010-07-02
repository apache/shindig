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

function OpenSocialWrapper() {
	
	// =============================== PEOPLE ===============================
	
	/*
	 * Loads the owner, the viewer, the owner's friends, and the viewer's
	 * friends.  Response data is put into the variables owner, viewer,
	 * ownerFriends, and viewerFriends, respectively.
	 * 
	 * @param callback is the function to return the response to
	 */
	this.loadPeople = function(callback) {
		var batch = osapi.newBatch();
		batch.add('viewer', osapi.people.getViewer());
		batch.add('owner', osapi.people.getOwner());
		batch.add('viewerFriends', osapi.people.getViewerFriends());
		batch.add('ownerFriends', osapi.people.getOwnerFriends());
		batch.execute(callback);
	}
	
	this.loadViewerFriends = function(callback) {
		osapi.people.getViewerFriends().execute(callback);
	}
	
	this.loadOwnerFriends = function(callback) {
		osapi.people.getOwnerFriends().execute(callback);
	}
	
	// ========================= ACTIVITIES =============================
	this.loadActivities = function(callback) {
		var batch = osapi.newBatch();
		batch.add('viewerActivities', osapi.activities.get({userId: '@viewer', groupId: '@self'}));
		batch.add('ownerActivities', osapi.activities.get({userId: '@owner', groupId: '@self'}));
		batch.add('friendActivities', osapi.activities.get({userId: '@viewer', groupId: '@friend'}));
		batch.execute(callback);
	}
	
	this.loadViewerActivities = function(callback) {
		var req = osapi.activities.get({userId: '@viewer', groupId: '@self'});
		req.execute(callback);
	}
	
	this.loadViewerFriendsActivities = function(callback) {
		var req = osapi.activities.get({userId: '@viewer', groupId: '@friends'});
		req.execute(this.onLoadActivitiesFriends);
	}
	
	this.loadOwnerActivities = function(callback) {
		var req = osapi.activities.get({userId: '@owner', groupId: '@self'});
		req.execute(callback);
	}

	
	// ========================= ACTIVITY STREAMS =============================
	this.loadActivityEntries = function(callback) {
		var batch = osapi.newBatch();
		batch.add('viewerEntries', osapi.activitystreams.get({userId: '@viewer', groupId: '@self'}));
		batch.add('ownerEntries', osapi.activitystreams.get({userId: '@owner', groupId: '@self'}));
		batch.add('friendEntries', osapi.activitystreams.get({userId: '@viewer', groupId: '@friend'}));
		batch.execute(callback);
	}
	
	this.loadViewerActivityEntries = function(callback) {
		var params = {userId: '@viewer', groupId: '@self'}
		osapi.activitystreams.get(params).execute(callback);
	}
	
	this.loadOwnerActivityEntries = function(callback) {
		var params = {userId: '@owner', groupId: '@self'}
		osapi.activitystreams.get(params).execute(callback);
	}
	
	this.loadViewerFriendsActivityEntries = function(callback) {
		var params = {userId: '@viewer', groupId: '@friends'}
		osapi.activitystreams.get(params).execute(callback);
	}
	
	this.postActivityEntry = function(title, body, standardLink, verbs, actorId, actorName, objectName, objectSummary,
									  objectImage, objectPermalink, objectTypes, callback) { 
		var params = {
			userId: '@viewer',
			groupId: '@self',
			activityEntry: {
				standardLink: [standardLink],
				time: '2010-04-27T06:02:36+0000',
				title: title,
				body: body,
				actor: {
					id: actorId,
					name: actorName
				},
				verb: verbs,
				object: {
					id: 'entryId123',
					name: objectName,
					permalink: objectPermalink,
					type: objectTypes,
					media: {
						target: 'http://myvideos.com/raftingtrip/raftingvideo.avi',
						type: 'http://activitystrea.ms/schema/1.0/video',
						width: '400',
						height: '300',
						duration: '93'
					},
					action: {
						target: 'http://myvideos.com/raftingvideo',
						caption: 'Went white water rafting in the great lakes - ga hah!'
					}
				}
			}
		}
		osapi.activitystreams.create(params).execute(callback);
	}
	
	this.deleteActivityEntryById = function(activityEntryId, callback) {
		var params = {
			userId: '@viewer',
			groupId: '@self',
			activityEntryId: activityEntryId
		}
		osapi.activitystreams.delete(params).execute(callback);
	}
	
	this.getActivityEntryById = function(activityEntryId, callback) {
		var params = {activityEntryId: activityEntryId};
		osapi.activitystreams.get(params).execute(callback);
	}
}