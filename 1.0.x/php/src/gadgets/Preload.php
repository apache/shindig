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

class Preload {
  public static $AUTHZ_ATTR = "authz";
  private $href;
  private $auth;
  private $signViewer;
  private $signOwner;
  private $views = array();

  /**
   * Creates a new Preload from an xml node.
   *
   * @param preload The Preload to create
   */
  public function __construct(SimpleXMLElement $preload) {
    $attributes = $preload->attributes();
    $this->signOwner = isset($attributes['sign_owner']) ? trim($attributes['sign_owner']) : true;
    $this->signViewer = isset($attributes['sign_viewer']) ? trim($attributes['sign_viewer']) : true;
    $this->href = isset($attributes['href']) ? trim($attributes['href']) : '';
    if (empty($this->href)) {
      throw new SpecParserException("Preload/@href is missing or invalid.");
    }
    // Record all the associated views
    $viewNames = isset($attributes['views']) ? trim($attributes['views']) : '';
    $views = array();
    $arrViewNames = explode(",", $viewNames);
    foreach ($arrViewNames as $view) {
      $view = trim($view);
      if (strlen($view) > 0) {
        $views[] = $view;
      }
    }
    $this->views = $views;
    $this->auth = Auth::parse($attributes[Preload::$AUTHZ_ATTR]);
  }

  public function getHref() {
    return $this->href;
  }

  public function getAuth() {
    return $this->auth;
  }

  public function isSignViewer() {
    return $this->signViewer;
  }

  public function isSignOwner() {
    return $this->signOwner;
  }

  public function getViews() {
    return $this->views;
  }

  public function substitute($substituter) {
    return $this->fillPreload($this, $substituter);
  }

  private function fillPreload(Preload $preload, $substituter) {
    $this->signOwner = $preload->signOwner;
    $this->signViewer = $preload->signViewer;
    $this->views = $preload->views;
    $this->auth = $preload->auth;
    $this->href = $substituter->substituteUri(null, $preload->href);
    return $this;
  }
}
