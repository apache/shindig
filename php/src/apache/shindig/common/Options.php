<?php
namespace apache\shindig\common;

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
 * Bag of options for making a request.
 *
 * This object is mutable to keep us sane. Don't mess with it once you've
 * sent it to RemoteContentRequest or bad things might happen.
 */
class Options {
  public $ignoreCache = false;
  public $ownerSigned = true;
  public $viewerSigned = true;

  public function __construct() {}

  /**
   * Copy constructor
   *
   * @param Options $copyFrom
   */
  public function copyOptions(Options $copyFrom) {
    $this->ignoreCache = $copyFrom->ignoreCache;
    $this->ownerSigned = $copyFrom->ownerSigned;
    $this->viewerSigned = $copyFrom->viewerSigned;
  }
}