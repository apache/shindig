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
tamings___.push(function(imports) {
  // TODO(felix8a): tame these
  // ___.grantInnocentMethod(JsonPerson.prototype, 'getDisplayName');
  // ___.grantInnocentMethod(JsonPerson.prototype, 'getAppData');

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
    [opensocial, 'MessageCollection', Object],
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

  caja___.grantTameAsRead(opensocial, 'CreateActivityPriority');
  caja___.grantTameAsRead(opensocial, 'EscapeType');
  caja___.grantTameAsRead(opensocial.Activity, 'Field');
  caja___.grantTameAsRead(opensocial.Address, 'Field');
  caja___.grantTameAsRead(opensocial.Album, 'Field');
  caja___.grantTameAsRead(opensocial.BodyType, 'Field');
  caja___.grantTameAsRead(opensocial.DataRequest, 'ActivityRequestFields');
  caja___.grantTameAsRead(opensocial.DataRequest, 'DataRequestFields');
  caja___.grantTameAsRead(opensocial.DataRequest, 'FilterType');
  caja___.grantTameAsRead(opensocial.DataRequest, 'Group');
  caja___.grantTameAsRead(opensocial.DataRequest, 'PeopleRequestFields');
  caja___.grantTameAsRead(opensocial.DataRequest, 'SortOrder');
  caja___.grantTameAsRead(opensocial.Email, 'Field');
  caja___.grantTameAsRead(opensocial.Enum, 'Smoker');
  caja___.grantTameAsRead(opensocial.Enum, 'Drinker');
  caja___.grantTameAsRead(opensocial.Enum, 'Gender');
  caja___.grantTameAsRead(opensocial.Enum, 'LookingFor');
  caja___.grantTameAsRead(opensocial.Enum, 'Presence');
  caja___.grantTameAsRead(opensocial.Environment, 'ObjectType');
  caja___.grantTameAsRead(opensocial.IdSpec, 'Field');
  caja___.grantTameAsRead(opensocial.IdSpec, 'GroupId');
  caja___.grantTameAsRead(opensocial.IdSpec, 'PersonId');
  caja___.grantTameAsRead(opensocial.MediaItem, 'Field');
  caja___.grantTameAsRead(opensocial.MediaItem, 'Type');
  caja___.grantTameAsRead(opensocial.Message, 'Field');
  caja___.grantTameAsRead(opensocial.Message, 'Type');
  caja___.grantTameAsRead(opensocial.MessageCollection, 'Field');
  caja___.grantTameAsRead(opensocial.Name, 'Field');
  caja___.grantTameAsRead(opensocial.NavigationParameters, 'DestinationType');
  caja___.grantTameAsRead(opensocial.NavigationParameters, 'Field');
  caja___.grantTameAsRead(opensocial.Organization, 'Field');
  caja___.grantTameAsRead(opensocial.Person, 'Field');
  caja___.grantTameAsRead(opensocial.Phone, 'Field');
  caja___.grantTameAsRead(opensocial.ResponseItem, 'Error');
  caja___.grantTameAsRead(opensocial.Url, 'Field');

  // TODO(jasvir): The following object *is* exposed to gadget
  // code because its returned by opensocial.DataRequest.*
  // but isn't documented in gadget API.
  // TODO(felix8a): tame these
  //caja___.grantTameAsRead(JsonRpcRequestItem, 'rpc');
  //caja___.grantTameAsRead(JsonRpcRequestItem, 'processData');
  //caja___.grantTameAsRead(JsonRpcRequestItem, 'processResponse');
  //caja___.grantTameAsRead(JsonRpcRequestItem, 'errors');

});
