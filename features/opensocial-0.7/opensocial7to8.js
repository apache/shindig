// stub javascript
opensocial.Activity.MediaItem = opensocial.MediaItem
opensocial.newActivityMediaItem = opensocial.newMediaItem;

opensocial.DataRequest.PersonId = opensocial.IdSpec.PersonId;

opensocial.DataRequest.Group = {
  OWNER_FRIENDS : 'OWNER_FRIENDS',
  VIEWER_FRIENDS : 'VIEWER_FRIENDS'
};

opensocial.DataRequest.prototype.newFetchPeopleRequestOld
    = opensocial.DataRequest.prototype.newFetchPeopleRequest;
opensocial.DataRequest.prototype.newFetchPeopleRequest = function(idSpec,
    opt_params) {
  return this.newFetchPeopleRequestOld(translateIdSpec(idSpec), opt_params);
}

opensocial.DataRequest.prototype.newFetchPersonAppDataRequestOld
    = opensocial.DataRequest.prototype.newFetchPersonAppDataRequest;
opensocial.DataRequest.prototype.newFetchPersonAppDataRequest = function(idSpec,
    keys, opt_params) {
  return this.newFetchPersonAppDataRequestOld(translateIdSpec(idSpec), keys,
      opt_params);
}

opensocial.DataRequest.prototype.newFetchActivitiesRequestOld
    = opensocial.DataRequest.prototype.newFetchActivitiesRequest;
opensocial.DataRequest.prototype.newFetchActivitiesRequest = function(idSpec,
    opt_params) {
  var request
      = this.newFetchActivitiesRequestOld(translateIdSpec(idSpec), opt_params);
  request.isActivityRequest = true;
  return request;
}

// TODO: handle making the last param valid json from any given string
// (is it already valid??)
// opensocial.DataRequest.prototype.newUpdatePersonAppDataRequest

opensocial.ResponseItem.prototype.getDataOld
    = opensocial.ResponseItem.prototype.getData;
opensocial.ResponseItem.prototype.getData = function() {
  var oldData = this.getDataOld();
  if (this.getOriginalDataRequest().isActivityRequest) {
    // The fetch activities request used to have an extra pointer to
    // the activities
    return {'activities' : oldData};
  }

  return oldData;
};

opensocial.Environment.ObjectType.ACTIVITY_MEDIA_ITEM
    = opensocial.Environment.ObjectType.MEDIA_ITEM;


opensocial.Person.prototype.getFieldOld = opensocial.Person.prototype.getField;
opensocial.Person.prototype.getField = function(key, opt_params) {
  var value =  this.getFieldOld(key, opt_params);
  if (key == 'lookingFor' && value) {
    // The lookingFor field used to return a string instead of an enum
    return value.getDisplayValue();
  } else {
    return value;
  }
};


function translateIdSpec(oldIdSpec) {
  if (oldIdSpec == 'OWNER_FRIENDS') {
    return new opensocial.IdSpec({userId : 'OWNER', groupId : 'FRIENDS'});
  } else if (oldIdSpec == 'VIEWER_FRIENDS') {
    return new opensocial.IdSpec({userId : 'VIEWER', groupId : 'FRIENDS'});
  } else {
    return new opensocial.IdSpec({userId : oldIdSpec});
  }
};