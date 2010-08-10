<?php
/**
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
 * Media Service exposes an interface to access the media items.
 */
interface MediaItemService {
  
  /**
   * Returns mediaItems from an album
   *
   * @param userId The id of the person whose album to fetch
   * @param groupId The group Id
   * @param albumId The id of the album to fetch
   * @param mediaItemIds MediaItemIds to fetch. Fetch all mediaItems if this is empty
   * @param collectionOptions options for sorting, pagination etc
   * @param fields fields to fetch
   * @param token The gadget token
   * @return a list of media items
   */
  public function getMediaItems($userId, $groupId, $albumId, $mediaItemIds, $collectionOptions, $fields, $token);

  /**
   * Creates a media item in a specified album. The albumId is taken from the
   * mediaItem object. id of the media item object should not be set. A file may
   * be uploaded with the content type 'multipart/form-data', 'image/*', 'video/*'
   * or 'audio/*'. The uploaded file is moved to a temporary location. The file info
   * is stored in the 'file' param. After this method is invoked the file is deleted.
   *
   * @param userId id of the user for whom a media item is to be created
   * @param groupId group id
   * @param mediaItem specifies album-id and media item fields
   * @param An associative array that describes the uploaded file. The array is empty if
   *     there is no uploaded file. It has 'name', 'tmp_name', 'type' and 'size' fields.
   *     i.e. [tmp_name] => /tmp/upload//tmp/php/php1h4j1o, [type] => image/png,
   *     [size] => 123, [name] = user_file_name.png.  
   *     The file is a regular file and should not be moved by the move_uploaded_file method.
   * @param token security token to authorize this request
   * @return the created media item
   */
  public function createMediaItem($userId, $groupId, $mediaItem, $file, $token);

  /**
   * Updates a media item in an album. Album id and media item id is taken in
   * from albumMediaItem.
   *
   * @param userId id of user whose media item is to be updated
   * @param groupId group id
   * @param mediaItem specifies album id, media-item id, fields to update
   * @param token security token
   * @return updated album media item
   */
  public function updateMediaItem($userId, $groupId, $mediaItem, $token);

  /**
   * Deletes an album media item.
   *
   * @param id id of user whose media item is to be deleted
   * @param groupId group id
   * @param albumId id of album to update
   * @param mediaItemIds ids of media item to update
   * @param token security token to authorize this update request
   * @return void on successful completion
   */
  public function deleteMediaItems($userId, $groupId, $albumId, $mediaItemIds, $token);  
}
