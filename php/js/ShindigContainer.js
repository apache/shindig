/**
* @constructor
*/
opensocial.ShindigContainer = function() {
	opensocial.Container.call(this);
};
opensocial.ShindigContainer.inherits(opensocial.Container);

Array.prototype.each = function(fun) {
	for (var cnt=0; cnt<this.length; cnt++) {
		fun(this[cnt]);
	};
};
/**
* Initialize the container with the specified state.
*
* @param {Object} viewer Person object that corresponds to the viewer.
* @param {Object} opt_owner Person object that corresponds to the owner.
* @param {Map<String, String>} opt_friends A map of friends of the viewer,
* keyed by their Id.
* @param {Array<Object>} opt_activities An array of activity objects.
* @param  {Map<String, String>} opt_nonFriends A map of contacts who are
* not friends.
*/
opensocial.ShindigContainer.prototype.init = function(appName) {
	/*
	if (appName = '/46/o') {
	this.appName = hashAppName;
	}
	else {
	this.appName = appName;
	}
	*/
	if (this.containerInitialized) {
		return;
	}
	this.containerInitialized = true;



	this.appName = hashAppName;


	this.viewerFriends = {};
	this.viewerActivities = {};
	this.ownerActivities = {};
	this.ownerFriends ={};

	//the framework calls this function passing a URL from which to load the data?
	//looks like the   google.opensocial.Container.get().init("/46/o");    call ends up passing the /46/o as viewer...
	//interesting -- for us the person that embedded the object is hard to ascertain, while for social networks profile pages
	//are a first-class concept... it's going to be tricky to map this in a non-fragile way
	//each widget will have to track who inserted it
	this.owner = xnGetOwner();
	//TODO load the profile that owns the gadget I'm looking at...
	this.viewer = xnGetViewer();

	if (xnGetViewerFriends().contacts && xnGetViewerFriends().contacts.values) {
		var contactsArr = xnGetViewerFriends().contacts.values;
		var resultArr = new Array();
		for (var i = 0; i < contactsArr.length; i++) {
			fObj = contactsArr[i];
			//todo load the app data
			resultArr[resultArr.length] = new opensocial.ShindigPerson(fObj.id, fObj.name, '', fObj.photoUrl, '', new Array(),'');
		}
		this.viewerFriends = resultArr;
		//console.log('viewerFriends ' + resultArr.length);
		//console.log('viewerFriendsx ' + contactsArr);
	}
	if (xnGetOwnerFriends().contacts && xnGetOwnerFriends().contacts.values) {
		var contacts2Arr = xnGetOwnerFriends().contacts.values;
		var result2Arr = new Array();
		for (var i = 0; i < contacts2Arr.length; i++) {
			fObj = contacts2Arr[i];
			//todo load the app data
			result2Arr[result2Arr.length] = new opensocial.ShindigPerson(fObj.id, fObj.name, '', fObj.photoUrl, '', new Array(),'');
		}
		this.ownerFriends = result2Arr;
	}
	this.containerInitialized = true;
	this.globalAppData = {};
	this.instanceAppData = {};

	this.owner.appData = preloadedUserData;
	this.viewer.appData = preloadedViewerData;

	this.activities = new Array();


};
opensocial.ShindigContainer.prototype.getAppName = function() {
	return this.appName;
};

opensocial.ShindigContainer.prototype.isContainerInitialized = function() {
	return this.containerInitialized;
}
opensocial.ShindigContainer.prototype.internalLoadAppData = function(person, callback) {
	if (person) {
		onSuccess = function(evaldObj) {
			//this.appData = evaldObj;//console.log('success!');
			//console.log('obj=');
			//console.log(evaldObj);
			//console.log('nummsg=' + evaldObj["numMessages"]);
			person.appData = evaldObj;
			//console.log(person.getAppField("numMessages"));
			person.isAppDataLoaded = true;
			callback();
		};
		onError = function(context, msg) {
			console.log('error = ' + msg);
		};
		var handlers = {
			success: onSuccess,
			failure: onError
		};
		//var form = new Array();
		content = {'user':  person.id, 'op':'get-app-data', 'app': this.appName, 'xark': Shindig._.os.xark, 'origin': Shindig._.os.origin};
		Shindig.api.post("/gadgets/index/api", content, handlers);
	}
};

/**
* Deprecated.
*/
opensocial.ShindigContainer.prototype.requestInstallApp = function(appUrl,
opt_onSuccess, opt_onFailure) {
	opt_onSuccess();
};

/**
* Deprecated.
*/
opensocial.ShindigContainer.prototype.requestUninstallApp = function(appUrl,
opt_onSuccess, opt_onFailure) {
	opt_onSuccess();
};

/**
* Make the specified person as friend by moving them from the nonFriends
* list to the friends list.
*
* @param {String} id The id associated with the person to be made a friend.
* @param {Function} opt_onSuccess The callback method on successful completion.
* @param {Function} opt_onFailure The callback method on failure.
*/
opensocial.ShindigContainer.prototype.requestMakePersonAFriend = function(id,
onSuccess, onError) {
	if (!Shindig.CurrentProfile) {
		alert("signin", "You must sign in to make someone your friend.");
		return false;
	}
	if (Shindig.CurrentProfile.id.toLowerCase() == friendId.toLowerCase()) {
		alert("You can't be your own friend!");
		return false;
	}
	onSuccess = onSuccess || function() {
		alert("Friend request sent!");
	};
	onError = onError || function(msg) {
		if (msg == "Sorry, that person has blocked you") {
			// hide blocking from user
			onSuccess();
		}
		else {
			alert("make person friend error: " + msg);
		}
	};
	Shindig.social.sendFriendRequest(friendId, {
		success: onSuccess,
		error: onError
	});
	return false;
};

opensocial.ShindigContainer.prototype.requestCreateActivity = function(activity,
priority, opt_callback) {
	// TODO(doll): We need to include simulating opeShindig a user dialog if the
	// stream does not exist. The templates should also be translated at this
	// point.
	this.activities.push(activity);
	if (opt_callback) {
		opt_callback();
	}
};

/**
* Make the specified person as nonfriend by moving them from the friends
* list to the nonFriends list.
*
* @param {String} id The id associated with the person to be removed
* as a friend.
* @param {Function} opt_onSuccess The callback method on successful completion.
* @param {Function} opt_onFailure The callback method on failure.
*/
opensocial.ShindigContainer.prototype.requestMakePersonNotAFriend = function(id, opt_onSuccess, opt_onFailure) {

	var url =  dojo.string.substituteParams("/xn/rest/1.0/profile:%{id}/contact?xn_method=PUT", {
		id: Shindig.CurrentProfile.id
	});

	var handlers = {
		success: opt_onSuccess,
		failure: opt_onFailure
	};

	context = null;
	var post_data = {
		contact_relationship: "not-friend",
		contact_id: profileId
	};

	Shindig.api.post(url, post_data, handlers, context);

};

/**
* This method returns the data requested about the viewer and his/her friends.
* Since this is an in memory container, it is merely returShindig the member
* variables. In a real world container, this would involve making an ajax
* request to fetch the values from the server.
*
* @param {Object} dataRequest The object that specifies the data requested.
* @param {Function} callback The callback method on completion.
*/
opensocial.ShindigContainer.prototype.requestData = function(dataRequest, callback) {

	var myself = this;
	/*
	if (!this.owner.isDataLoaded()) {
	console.log('calling load data for owner');
	this.internalLoadAppData(this.owner, function() {myself.requestData(dataRequest, callback);} );
	return;
	}
	if (!this.viewer.isDataLoaded()) {
	console.log('calling load data for current profile');
	this.internalLoadAppData(this.viewer, function() {myself.requestData(dataRequest, callback);} );
	return;
	}
	*/

	//TODO the requests right now include "BogusAppname" clearly a placeholder... wouldn't it be better to specify
	//the appID/name in the init() like I'm doing?

	var requestObjects = dataRequest.getRequestObjects();
	//var appName = dataRequest['AppName'];
	var dataResponseValues = {};
	//TODO for lists of friends requests we handle everything in-memory and return that data in this loop...
	//the expected behavior for multiple requests simultaneously is a bit unclear.
	for (var i = 0; i < requestObjects.length; i++) {
		var request = requestObjects[i].request;
		var responseName = requestObjects[i].key;
		var requestedValue;
		console.log('request type: ' + request.type);
		console.log('request response name: ' + responseName);

		switch(request.type) {
			case opensocial.DataRequest.RequestType.FETCH_GLOBAL_DATA :
			console.log('FETCH_GLOBAL_DATA');
			// TODO(doll): Filter by key
			requestedValue = this.globalAppData;
			break;

			case opensocial.DataRequest.RequestType.FETCH_INSTANCE_DATA :
			console.log('FETCH_INSTANCE_DATA');
			// TODO(doll): Filter by key
			requestedValue = this.instanceAppData;
			break;

			case opensocial.DataRequest.RequestType.UPDATE_INSTANCE_DATA :
			console.log('UPDATE_INSTANCE_DATA');
			this.instanceAppData[request.parameters.key] = request.parameters.value;
			break;

			case opensocial.DataRequest.RequestType.FETCH_PEOPLE :
			console.log('FETCH_PEOPLE');
			console.log(request);
			var groupId = request.parameters.idSpec;
			if (groupId == opensocial.DataRequest.Group.VIEWER_FRIENDS && this.viewer.getId() != 'xn_anonymous') {
				console.log('FETCH_PEOPLE: viewerFriends');
				console.log(this.viewerFriends);
				//requestedValue = new s2.data.ArrayWrapper(this.viewerFriends);
				requestedValue = this.viewerFriends;
			}
			else if (groupId == opensocial.DataRequest.Group.OWNER_FRIENDS) {
				console.log('FETCH_PEOPLE: ownerFriends');
				//requestedValue = new s2.data.ArrayWrapper(this.ownerFriends);
				requestedValue = this.ownerFriends;
			}

			break;

			case opensocial.DataRequest.RequestType.FETCH_PERSON :

			//opensocial.Container.PersonId.VIEWER returns person: viewer -- not clear in the docs
			var personId = request.parameters.id;
			if (personId == opensocial.DataRequest.PersonId.VIEWER) {
				console.log('FETCH_PERSON: viewer');
				console.log(this.viewer);
				requestedValue  = this.viewer;
			}
			else if (personId == opensocial.DataRequest.PersonId.OWNER) {
				console.log('FETCH_PERSON: owner');
				requestedValue  = this.owner;
			} else {
				console.log('FETCH_PERSON: specific friend');
				requestedValue = this.viewerFriends.get(personId)
				|| this.ownerFriends.get(personId);
			}


			//todo -- add waiting in case they request data from another profile
			break;

			case opensocial.DataRequest.RequestType.FETCH_ACTIVITIES :
			console.log('FETCH_ACTIVITIES');
			//http://gadgets.Shindig.com/activity/log/list?fmt=json&screenName=diego

			onSuccess = function(evaldObj) {
				friendCount = evaldObj.total;
				//        	console.log('count='+friendCount);
				//        	console.log('intct='+evaldObj.contacts.values.length);
				var activitiesArr = evaldObj.values;
				var resultArr = {};
				if (responseName == "viewer" || request.responseName == "activities") {
					this.viewerActivities = new Array();
					resultArr = this.viewerActivities;
				}
				if (responseName == "owner") {
					this.ownerActivities = new Array();
					resultArr = this.ownerActivities;
				}

/*
    for(var i = 0;i < activityResponse.length;i++) {
      var activity = activityResponse[i];
      activities.push(new opensocial.Person.Activity(activity.ApplicationId, activity.Body, activity.Title, activity.Url, activity.Summary, activity.FolderId))
    }
    opensocial.DataRequest.ActivityRequestFields = {APP_ID:"appId", FOLDER_ID:"folderId"};
    opensocial.DataRequest.prototype.newFetchActivitiesRequest = function(idSpec, opt_params) {
  opt_params = opt_params || {};
  var fields = opensocial.DataRequest.ActivityRequestFields, requestParams = {idSpec:idSpec, appId:opt_params[fields.APP_ID], folderId:opt_params[fields.FOLDER_ID]};
  return new opensocial.DataRequest.BaseDataRequest(opensocial.DataRequest.RequestType.FETCH_ACTIVITIES, requestParams)
  opensocial.Activity = function(stream, title, opt_params) {
};
opensocial.Activity.Field = {ID:"id", EXTERNAL_ID:"externalId", STREAM:"stream", TITLE:"title", SUMMARY:"summary", BODY:"body", URL:"url", MEDIA_ITEMS:"mediaItems", POSTED_TIME:"postedTime", CUSTOM_VALUES:"customValues"};

*/
				for (var i = 0; i < activitiesArr.length; i++) {
					fObj = activitiesArr[i];
					resultArr[resultArr.length] = new opensocial.Person.Activity(fObj.id, fObj.description, fObj.title, fObj.link);
				}

				dataResponseValues[responseName] = new opensocial.ResponseItem(request, resultArr);
				callback(new opensocial.DataResponse(dataRequest, dataResponseValues));
			};
			onError = function(context, msg) {

			};
			var handlers = {
				success: onSuccess,
				failure: onError
			};
			if (this.viewer.getName() == 'xn_anonymous') {
				Shindig.api.get("/activity/log/list?fmt=json&screenName=" + this.viewer.getId(), handlers);
			}
			else {
				console.log('not logged in');
			}
			return;

			case opensocial.DataRequest.RequestType.FETCH_PERSON_DATA:
			// TODO(doll): Filter by person and key
			console.log('FETCH_PERSON_DATA: ' + request.parameters.idSpec);
			var personId = request.parameters.idSpec || request.parameters.id;//request.parameters.id;
			if (personId == opensocial.DataRequest.PersonId.VIEWER) {
				requestedValue = this.viewer.getAppData();
			}
			else if (personId == opensocial.DataRequest.PersonId.OWNER) {
				requestedValue = this.owner.getAppData();
			}
			break;

			case opensocial.DataRequest.RequestType.UPDATE_PERSON_DATA :
			console.log('FETCH_PERSON_DATA: ' + request.parameters.person.id);
			var key = request.parameters.field;
			if (request.parameters.person.appData[key] != request.parameters.value) {
				request.parameters.person.appData[key] = request.parameters.value;
				if (this.viewer) {
					onSuccess = function(evaldObj) {
						console.log('success!');
						dataResponseValues[responseName] = new opensocial.ResponseItem(request, requestedValue);
						callback(new opensocial.DataResponse(dataRequest, dataResponseValues));
					};
					onError = function(context, msg) {
						console.log('error = ' + msg);
					};
					var handlers = {
						success: onSuccess,
						failure: onError
					};
					var userid = request.parameters.person.id;
					var value = request.parameters.value;
					//var form = new Array();
					content = {'user':  userid, 'op':'update-app-data', 'app': this.appName, 'key': key, 'value': value, 'xark': Shindig._.os.xark, 'origin': Shindig._.os.origin};
					Shindig.api.post("/gadgets/index/api", content, handlers);
				}
				else {
					console.log('not logged in');
				}
			}
			return;

			case opensocial.DataRequest.RequestType.POST_ACTIVITY :
			console.log('POST_ACTIVITY');
			console.log(request);
			console.log('end POST_ACTIVITY');
			
				if (this.viewer) {
					onSuccess = function(evaldObj) {
						console.log('success!');
						dataResponseValues[responseName] = new opensocial.ResponseItem(request, requestedValue);
						callback(new opensocial.DataResponse(dataRequest, dataResponseValues));
					};
					onError = function(context, msg) {
						console.log('error = ' + msg);
					};
					var handlers = {
						success: onSuccess,
						failure: onError
					};
					var userid = request.parameters.person.id;
					var value = request.parameters.value;
					//var form = new Array();
					content = {'user':  userid, 'op':'insert-activity', 'app': this.appName, 'description': request.parameters.body, 'title': request.parameters.title, 'link': request.parameters.url, 'xark': Shindig._.os.xark, 'origin': Shindig._.os.origin};
					Shindig.api.post("/gadgets/index/api", content, handlers);
				}
				else {
					console.log('not logged in');
				}
			
			/*
			this.activities.push(new opensocial.Person.Activity(
			request.parameters.appId, request.parameters.body,
			request.parameters.title, request.parameters.url));
			*/
			return;
		}
		console.log('setting response to '+responseName);
		dataResponseValues[responseName] = new opensocial.ResponseItem(request, requestedValue);
	}

		console.log('viewer  ==========')
	//console.log(dataResponseValues.get('viewer'))
		console.log('end viewer ========== ')
	
	callback(new opensocial.DataResponse(dataResponseValues));
};



/**
* Create a person.
*
* @param {String} id The id of the person
* @param {String} name The name of the person
* @param {String} email The email of the person
* @param {String} pictureUrl The url of the person's photo
* @param {Array<String>} installedApps An array of strings representing which
*     apps are installed
* @param {Map<String, String>} appData A map from app fields to app values
* @param {Boolean} isViewerObject If true, this person is the currently logged
*   in user.
* @constructor
*/
opensocial.ShindigPerson = function(id, opt_name, opt_email, opt_pictureUrl, opt_installedApps, opt_appData, opt_isViewerObject) {
	this.id = id;
	this.name = opt_name;
	this.email = opt_email;

	this.pictureUrl = opt_pictureUrl;
	if (this.pictureUrl.indexOf('?') == -1) {
		this.pictureUrl += '?';
	}
	else {
		this.pictureUrl += '&';
	}
	// this.pictureUrl += 'width=32&height=32'; // Ning specific
	this.installedApps = opt_installedApps;
	this.appData = opt_appData;
	this.isViewerObject = opt_isViewerObject;
	this.isAppDataLoaded = false;
	this.isAppDataLoading = false;
};


opensocial.ShindigPerson.prototype.getField = function(key) {
	if (key == opensocial.Person.Field.ID) {
		return this.id;
	}
	if (key == opensocial.Person.Field.NAME) {
		return this.name;
	}
	if (key == opensocial.Person.Field.EMAIL) {
		return this.email;
	}
	return null;
	//return this.fields[key];
};

opensocial.ShindigPerson.prototype.isDataLoaded = function() {
	return this.isAppDataLoaded;
};


opensocial.ShindigPerson.prototype.isViewer = function() {
	return this.isViewerObject;
};


opensocial.ShindigPerson.prototype.getId = function() {
	return this.id;
};


opensocial.ShindigPerson.prototype.getName = function() {
	return this.name;
};


opensocial.ShindigPerson.prototype.getEmail = function() {
	return this.email;
};

/**
* If the display name field is set, then return it. If not try to do
* something meaShindigful using the email address.
*/
opensocial.ShindigPerson.prototype.getDisplayName = function() {
	if (this.getName()) {
		return this.getName();
	} else  {
		var email = this.getEmail();
		if (email && email.indexOf('@') > 0) {
			return email.substring(0, email.indexOf('@'));
		}
	}
	return '';
};


opensocial.ShindigPerson.prototype.getPicture = function() {
	return this.pictureUrl;
};


opensocial.ShindigPerson.prototype.hasApp = function(appName) {
	for (var i = 0; i < this.installedApps.length; i++) {
		if (this.installedApps[i] == appName) {
			return true;
		}
	}

	return false;
};



opensocial.ShindigPerson.prototype.getAppData = function(field) {
	return this.appData;
}
opensocial.ShindigPerson.prototype.getAppField = function(field) {
	if (typeof(this.appData[field]) == "undefined") {
		return '';
	}
	return this.appData[field];
};

if(window.magic_orkut) {
	opensocial.Container.setContainer(new opensocial.OrkutContainer)
}
else if(window.magic_shindig) {
	opensocial.Container.setContainer(new opensocial.ShindigContainer)
}
else {
	opensocial.Container.setContainer(new opensocial.IGoogleContainer)
};


