/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

/**
 * @class
 * Tame and expose opensocial.* API to cajoled gadgets
 */
var tamings___ = tamings___ || [];
tamings___.push(function(imports) {
  ___.grantRead(opensocial, 'CreateActivityPriority');
  ___.grantRead(opensocial, 'EscapeType');
  ___.grantRead(opensocial.Activity, 'Field');
  ___.grantRead(opensocial.Address, 'Field');
  ___.grantRead(opensocial.Album, 'Field');
  ___.grantRead(opensocial.BodyType, 'Field');
  ___.grantRead(opensocial.DataRequest, 'ActivityRequestFields');
  ___.grantRead(opensocial.DataRequest, 'DataRequestFields');
  ___.grantRead(opensocial.DataRequest, 'FilterType');
  ___.grantRead(opensocial.DataRequest, 'Group');
  ___.grantRead(opensocial.DataRequest, 'PeopleRequestFields');
  ___.grantRead(opensocial.DataRequest, 'SortOrder');
  ___.grantRead(opensocial.Email, 'Field');
  ___.grantRead(opensocial.Enum, 'Smoker');
  ___.grantRead(opensocial.Enum, 'Drinker');
  ___.grantRead(opensocial.Enum, 'Gender');
  ___.grantRead(opensocial.Enum, 'LookingFor');
  ___.grantRead(opensocial.Enum, 'Presence');
  ___.grantRead(opensocial.IdSpec, 'Field');
  ___.grantRead(opensocial.IdSpec, 'GroupId');
  ___.grantRead(opensocial.IdSpec, 'PersonId');
  ___.grantRead(opensocial.MediaItem, 'Field');
  ___.grantRead(opensocial.Message, 'Field');
  ___.grantRead(opensocial.MessageCollection, 'Field');
  ___.grantRead(opensocial.Name, 'Field');
  ___.grantRead(opensocial.NavigationParameters, 'Field');
  ___.grantRead(opensocial.Organization, 'Field');
  ___.grantRead(opensocial.Person, 'Field');
  ___.grantRead(opensocial.Phone, 'Field');
  ___.grantRead(opensocial.ResponseItem, 'Error');
  ___.grantRead(opensocial.Url, 'Field');

  // TODO(jasvir): The following object *is* exposed to gadget
  // code because its returned by opensocial.DataRequest.*
  // but isn't documented in gadget API.
  ___.grantRead(JsonRpcRequestItem, 'rpc');
  ___.grantRead(JsonRpcRequestItem, 'processData');
  ___.grantRead(JsonRpcRequestItem, 'processResponse');
  ___.grantRead(JsonRpcRequestItem, 'errors');

  ___.grantInnocentMethod(JsonPerson.prototype, 'getDisplayName');
  ___.grantInnocentMethod(JsonPerson.prototype, 'getAppData');

  caja___.whitelistCtors([
    [window, 'JsonRpcRequestItem', Object],
    [opensocial, 'Activity', Object],
    [opensocial, 'Address', Object],
    [opensocial, 'Album', Object],
    [opensocial, 'BodyType', Object],
    [opensocial, 'Container', Object],
    [opensocial, 'Collection', Object],
    [opensocial, 'DataRequest', Object],
    [opensocial, 'DataResponse', Object],
    [opensocial, 'Email', Object],
    [opensocial, 'Enum', Object],
    [opensocial, 'Environment', Object],
    [opensocial, 'IdSpec', Object],
    [opensocial, 'MediaItem', Object],
    [opensocial, 'Message', Object],
    [opensocial, 'Name', Object],
    [opensocial, 'NavigationParameters', Object],
    [opensocial, 'Organization', Object],
    [opensocial, 'Person', Object],
    [opensocial, 'Phone', Object],
    [opensocial, 'ResponseItem', Object],
    [opensocial, 'Url', Object]
  ]);
  caja___.whitelistMeths([
    [opensocial.Activity, 'getField'],
    [opensocial.Activity, 'getId'],
    [opensocial.Activity, 'setField'],
    [opensocial.Address, 'getField'],
    [opensocial.Album, 'getField'],
    [opensocial.Album, 'setField'],
    [opensocial.BodyType, 'getField'],
    [opensocial.Container, 'getEnvironment'],
    [opensocial.Container, 'requestSendMessage'],
    [opensocial.Container, 'requestShareApp'],
    [opensocial.Container, 'requestCreateActivity'],
    [opensocial.Container, 'hasPermission'],
    [opensocial.Container, 'requestPermission'],
    [opensocial.Container, 'requestData'],
    [opensocial.Container, 'newCreateAlbumRequest'],
    [opensocial.Container, 'newCreateMediaItemRequest'],
    [opensocial.Container, 'newDeleteAlbumRequest'],
    [opensocial.Container, 'newFetchPersonRequest'],
    [opensocial.Container, 'newFetchPeopleRequest'],
    [opensocial.Container, 'newFetchPersonAppDataRequest'],
    [opensocial.Container, 'newUpdatePersonAppDataRequest'],
    [opensocial.Container, 'newRemovePersonAppDataRequest'],
    [opensocial.Container, 'newUpdateAlbumRequest'],
    [opensocial.Container, 'newUpdateMediaItemRequest'],
    [opensocial.Container, 'newFetchActivitiesRequest'],
    [opensocial.Container, 'newFetchAlbumsRequest'],
    [opensocial.Container, 'newFetchMediaItemsRequest'],
    [opensocial.Container, 'newFetchMessageCollectionsRequest'],
    [opensocial.Container, 'newFetchMessagesRequest'],
    [opensocial.Container, 'newCollection'],
    [opensocial.Container, 'newPerson'],
    [opensocial.Container, 'newActivity'],
    [opensocial.Container, 'newAlbum'],
    [opensocial.Container, 'newMediaItem'],
    [opensocial.Container, 'newMessage'],
    [opensocial.Container, 'newIdSpec'],
    [opensocial.Container, 'newNavigationParameters'],
    [opensocial.Container, 'newResponseItem'],
    [opensocial.Container, 'newDataResponse'],
    [opensocial.Container, 'newDataRequest'],
    [opensocial.Container, 'newEnvironment'],
    [opensocial.Container, 'invalidateCache'],
    [opensocial.Collection, 'asArray'],
    [opensocial.Collection, 'each'],
    [opensocial.Collection, 'getById'],
    [opensocial.Collection, 'getOffset'],
    [opensocial.Collection, 'getTotalSize'],
    [opensocial.Collection, 'size'],
    [opensocial.DataRequest, 'add'],
    [opensocial.DataRequest, 'newCreateAlbumRequest'],
    [opensocial.DataRequest, 'newCreateMediaItemRequest'],
    [opensocial.DataRequest, 'newDeleteAlbumRequest'],
    [opensocial.DataRequest, 'newFetchActivitiesRequest'],
    [opensocial.DataRequest, 'newFetchAlbumsRequest'],
    [opensocial.DataRequest, 'newFetchMediaItemsRequest'],
    [opensocial.DataRequest, 'newFetchPeopleRequest'],
    [opensocial.DataRequest, 'newFetchPersonAppDataRequest'],
    [opensocial.DataRequest, 'newUpdateAlbumRequest'],
    [opensocial.DataRequest, 'newUpdateMediaItemRequest'],
    [opensocial.DataRequest, 'newFetchPersonRequest'],
    [opensocial.DataRequest, 'newRemovePersonAppDataRequest'],
    [opensocial.DataRequest, 'newUpdatePersonAppDataRequest'],
    [opensocial.DataRequest, 'send'],
    [opensocial.DataResponse, 'get'],
    [opensocial.DataResponse, 'getErrorMessage'],
    [opensocial.DataResponse, 'hadError'],
    [opensocial.Email, 'getField'],
    [opensocial.Enum, 'getDisplayValue'],
    [opensocial.Enum, 'getKey'],
    [opensocial.Environment, 'getDomain'],
    [opensocial.Environment, 'supportsField'],
    [opensocial.IdSpec, 'getField'],
    [opensocial.IdSpec, 'setField'],
    [opensocial.MediaItem, 'getField'],
    [opensocial.MediaItem, 'setField'],
    [opensocial.Message, 'getField'],
    [opensocial.Message, 'setField'],
    [opensocial.Name, 'getField'],
    [opensocial.NavigationParameters, 'getField'],
    [opensocial.NavigationParameters, 'setField'],
    [opensocial.Organization, 'getField'],
    [opensocial.Person, 'getDisplayName'],
    [opensocial.Person, 'getField'],
    [opensocial.Person, 'getId'],
    [opensocial.Person, 'isOwner'],
    [opensocial.Person, 'isViewer'],
    [opensocial.Phone, 'getField'],
    [opensocial.ResponseItem, 'getData'],
    [opensocial.ResponseItem, 'getErrorCode'],
    [opensocial.ResponseItem, 'getErrorMessage'],
    [opensocial.ResponseItem, 'getOriginalDataRequest'],
    [opensocial.ResponseItem, 'hadError'],
    [opensocial.Url, 'getField']
  ]);
  caja___.whitelistFuncs([
    [opensocial.Container, 'setContainer'],
    [opensocial.Container, 'get'],
    [opensocial.Container, 'getField'],
    [opensocial, 'getEnvironment'],
    [opensocial, 'hasPermission'],
    [opensocial, 'newActivity'],
    [opensocial, 'newAlbum'],
    [opensocial, 'newDataRequest'],
    [opensocial, 'newIdSpec'],
    [opensocial, 'newMediaItem'],
    [opensocial, 'newMessage'],
    [opensocial, 'newNavigationParameters'],
    [opensocial, 'requestCreateActivity'],
    [opensocial, 'requestPermission'],
    [opensocial, 'requestSendMessage'],
    [opensocial, 'requestShareApp']
  ]);
});
