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

class MediaItemHandler extends DataRequestHandler {
  private static $MEDIA_ITEM_PATH = "/mediaitems/{userId}/{groupId}/{albumId}/{mediaItemId}";

  public function __construct() {
    parent::__construct('media_item_service');
  }

  /**
   * Deletes the media items. The URI structure: /{userId}/{groupId}/{albumId}/{mediaItemId}+
   */
  public function handleDelete(RequestItem $requestItem) {
    $this->checkService();
    $requestItem->applyUrlTemplate(self::$MEDIA_ITEM_PATH);

    $userIds = $requestItem->getUsers();
    $groupId = $requestItem->getGroup();
    $albumIds = $requestItem->getListParameter('albumId');
    $mediaItemIds = $requestItem->getListParameter('mediaItemId');

    HandlerPreconditions::requireSingular($userIds, "userId must be singular value.");
    HandlerPreconditions::requireNotEmpty($groupId, "groupId must be specified.");
    HandlerPreconditions::requireSingular($albumIds, "albumId must be singular value.");

    $this->service->deleteMediaItems($userIds[0], $groupId, $albumIds[0], $mediaItemIds, $requestItem->getToken());
  }

  /**
   * Gets the media items. The URI structure: /{userId}/{groupId}/{albumId}/{mediaItemId}+
   */
  public function handleGet(RequestItem $requestItem) {
    $this->checkService();
    $requestItem->applyUrlTemplate(self::$MEDIA_ITEM_PATH);

    $userIds = $requestItem->getUsers();
    $groupId = $requestItem->getGroup();
    $albumIds = $requestItem->getListParameter("albumId");
    $mediaItemIds = $requestItem->getListParameter("mediaItemId");

    HandlerPreconditions::requireSingular($userIds, "userId must be singular value.");
    HandlerPreconditions::requireNotEmpty($groupId, "groupId must be specified.");
    HandlerPreconditions::requireSingular($albumIds, "albumId must be singular value.");

    $options = new CollectionOptions($requestItem);
    $fields = $requestItem->getFields();

    return $this->service->getMediaItems($userIds[0], $groupId, $albumIds[0], $mediaItemIds, $options, $fields, $requestItem->getToken());
  }

  /**
   * Creates the media item. The URI structure: /{userId}/{groupId}/{albumId}.
   */
  public function handlePost(RequestItem $requestItem) {
    $this->checkService();
    $requestItem->applyUrlTemplate(self::$MEDIA_ITEM_PATH);

    $userIds = $requestItem->getUsers();
    $groupId = $requestItem->getGroup();
    $albumIds = $requestItem->getListParameter('albumId');
    $mediaItem = $requestItem->getParameter('mediaItem');
    if (! isset($mediaItem)) {
      // For the content upload REST api. The param is mediaType in the spec now. As there is no mediaType
      // field in MediaItem. It should be 'type'.
      $type = $requestItem->getParameter('mediaType');
      if (! isset($type)) {
        $type = $requestItem->getParameter('type');
      }
      if (in_array($type, MediaItem::$TYPES)) {
        $mediaItem = array('type' => $type);
        // Only support title and description for now.
        $mediaItem['title'] = $requestItem->getParameter('title');
        $mediaItem['description'] = $requestItem->getParameter('description');
      }
    }

    HandlerPreconditions::requireSingular($userIds, "userId must be of singular value");
    HandlerPreconditions::requireNotEmpty($groupId, "groupId must be specified.");
    HandlerPreconditions::requireSingular($albumIds, "albumId must be sigular value.");
    HandlerPreconditions::requireNotEmpty($mediaItem, "mediaItem must be specified.");
    $mediaItem['albumId'] = $albumIds[0];
    $file = array();
    if (isset($mediaItem['url']) && substr($mediaItem['url'], 0, strlen('@field:')) != '@field:') {
      $file = $this->processRemoteContent($mediaItem['url']);
    } else {
      $file = $this->processUploadedContent();
    }

    $ret = $this->service->createMediaItem($userIds[0], $groupId, $mediaItem, $file, $requestItem->getToken());
    if (isset($file['tmp_name']) && file_exists($file['tmp_name'])) {
      @unlink($file['tmp_name']);
    }
    return $ret;
  }

  /**
   * Fetches the remote media content and saves it as a temporary file. Returns the meta data of the file.
   */
  private function processRemoteContent($uri) {
    $request = new RemoteContentRequest($uri);
    $request->createRemoteContentRequestWithUri($uri);
    $brc = new BasicRemoteContent();
    $response = $brc->fetch($request);
    if ($response->getHttpCode() != 200) {
      throw new SocialSpiException("Failed to fetch the content from $uri code: " . $response->getHttpCode(), ResponseError::$BAD_REQUEST);
    }
    if (!$this->isValidContentType($response->getContentType())) {
      throw new SocialSpiException("The content type " . $response->getContentType() .
        " fetched from $uri is not valid.", ResponseError::$BAD_REQUEST);
    }
    return $this->writeBinaryContent($response->getResponseContent(), $response->getContentType());
  }

  /**
   * Checks the $_FILES and HTTP_RAW_POST_DATA variables to write the user uploaded content as a temporary file.
   * Returns the meta data of the file. 
   */
  private function processUploadedContent() {
    $file = array();
    if (! empty($_FILES)) {
      // The RPC api supports to post the file using the content type 'multipart/form-data'.
      $uploadedFile = current($_FILES);
      if ($uploadedFile['error'] != UPLOAD_ERR_OK) {
        if ($uploadedFile['error'] == UPLOAD_ERR_INI_SIZE || $uploadedFile == UPLOAD_ERR_FORM_SIZE) {
          throw new SocialSpiException("The uploaded file is too large.", ResponseError::$REQUEST_TOO_LARGE);
        } else {
          throw new SocialSpiException("Failed to upload the file.", ResponseError::$BAD_REQUEST);
        }
      }
      if (!$this->isValidContentType($uploadedFile['type'])) {
        throw new SocialSpiException("The content type of the uploaded file " . $uploadedFile['type'] . " is not valid.", ResponseError::$BAD_REQUEST); 
      }
      $tmpName = tempnam('', 'shindig');
      if (!move_uploaded_file($uploadedFile['tmp_name'], $tmpName)) {
        throw new SocialSpiException("Failed to move the uploaded file.", ResponseError::$INTERNAL_ERROR);
      }
      $file['tmp_name'] = $tmpName;
      $file['size'] = $uploadedFile['size'];
      $file['type'] = $uploadedFile['type'];
      $file['name'] = $uploadedFile['name'];
    } else if (isset($GLOBALS['HTTP_RAW_POST_DATA'])) {
      // The REST api supports to post the file using the content type 'image/*', 'video/*' or 'audio/*'.
      if ($this->isValidContentType($_SERVER['CONTENT_TYPE'])) {
        $file = $this->writeBinaryContent($GLOBALS['HTTP_RAW_POST_DATA'], $_SERVER['CONTENT_TYPE']);
      }
    }
    return $file;
  }
  
  /**
   * Writes the binary content to a temporary file and returns the meta data of the file.
   */
  private function writeBinaryContent(&$rawData, $contentType) {
    $tmpName = tempnam('', 'shindig');
    $fp = fopen($tmpName, 'w');
    if (!fwrite($fp, $rawData)) {
      throw new SocialSpiException("Failed to write the uploaded file.", ResponseError::$INTERNAL_ERROR);
    }
    fclose($fp);
    return array('tmp_name' => $tmpName, 'size' => filesize($tmpName), 'name' => basename($tmpName), 'type' => $contentType);
  }
  
  /**
   * Returns true if the given content type is valid.
   */
  private function isValidContentType($contentType) {
    $acceptedMediaPrefixes = array('image', 'video', 'audio');
    $prefix = substr($contentType, 0, strpos($contentType, '/'));
    return in_array($prefix, $acceptedMediaPrefixes);
  }

  /**
   * Updates the mediaItem. The URI structure: /{userId}/{groupId}/{albumId}/{mediaItemId}
   */
  public function handlePut(RequestItem $requestItem) {
    $this->checkService();
    $requestItem->applyUrlTemplate(self::$MEDIA_ITEM_PATH);

    $userIds = $requestItem->getUsers();
    $groupId = $requestItem->getGroup();
    $albumIds = $requestItem->getListParameter('albumId');
    $mediaItemIds = $requestItem->getListParameter('mediaItemId');
    $mediaItem = $requestItem->getParameter('mediaItem');

    HandlerPreconditions::requireSingular($userIds, "userId must be singular value.");
    HandlerPreconditions::requireNotEmpty($groupId, "groupId must be specified.");
    HandlerPreconditions::requireSingular($albumIds, "albumId must be sigular value.");
    HandlerPreconditions::requireSingular($mediaItemIds, "mediaItemId must be sigular value.");
    HandlerPreconditions::requireNotEmpty($mediaItem, "mediaItem must be specified.");

    $mediaItem['id'] = $mediaItemIds[0];
    $mediaItem['albumId'] = $albumIds[0];
    return $this->service->updateMediaItem($userIds[0], $groupId, $mediaItem, $requestItem->getToken());
  }
}
