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

class InvalidateHandler extends DataRequestHandler {
  
  /**
   * @var InvalidateService
   */
  private $invalidateService;
  
  private static $INVALIDATE_PATH = "/invalidate";
  
  private static $KEYS_PARAM = "invalidationKeys";

  public function __construct() {
    $service = Config::get('invalidate_service');
    $cache = Cache::createCache(Config::get('data_cache'), 'RemoteContent');
    $this->invalidateService = new $service($cache);
  }

  public function handleDelete(RequestItem $request) {
    $this->handleGet($request);
  }

  public function handlePut(RequestItem $request) {
    $this->handleGet($request);
  }

  public function handlePost(RequestItem $request) {
    $this->handleGet($request);
  }

  public function handleGet(RequestItem $request) {
    if (!$request->getToken()->getAppId() && !$request->getToken()->getAppUrl()) {
      throw new SocialSpiException("Can't invalidate content without specifying application", ResponseError::$BAD_REQUEST);
    }
    
    $isBackendInvalidation = AuthenticationMode::$OAUTH_CONSUMER_REQUEST == $request->getToken()->getAuthenticationMode();
    $invalidationKeys = $request->getListParameter('invalidationKey');
    $resources = array();
    $userIds = array();
    if ($request->getToken()->getViewerId()) {
      $userIds[] = $request->getToken()->getViewerId();
    }
    foreach($invalidationKeys as $key) {
      if (strpos($key, 'http') !== false) {
        if (!$isBackendInvalidation) {
          throw new SocialSpiException('Cannot flush application resources from a gadget. Must use OAuth consumer request'); 
        }
        $resources[] = $key;
      } else {
        if ($key == '@viewer') {
          continue;
        }
        if (!$isBackendInvalidation) {
          throw new SocialSpiException('Cannot invalidate the content for a user other than the viewer from a gadget.');
        }
        $userIds[] = $key;
      }
    }
    $this->invalidateService->invalidateApplicationResources($resources, $request->getToken());
    $this->invalidateService->invalidateUserResources($userIds, $request->getToken());
  }
}
