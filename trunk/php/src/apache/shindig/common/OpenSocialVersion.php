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
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

/**
 * Convenience class for working with OpenSocial Specification and Feature versions.
 * Applies the rules specified in the OS specification
 * http://opensocial-resources.googlecode.com/svn/spec/1.0/Core-Gadget.xml#Versioning
 * 
 */
class OpenSocialVersion {
    
  public $major = null;
  public $minor = null;
  public $patch = null;
  
  /**
   * Create a new OpenSocialVersion based upon a versionString
   * @param string $versionString Version string
   */
  public function __construct($versionString = null){
    if (! $versionString) {
      return;
    }
    $versionParts = explode('.', $versionString);
    if (isset($versionParts[0]) && is_numeric($versionParts[0])) {
      $this->major = (int) $versionParts[0];
    }
    if (isset($versionParts[1]) && is_numeric($versionParts[1])) {
      $this->minor = (int) $versionParts[1];
    }
    if (isset($versionParts[2]) && is_numeric($versionParts[2])) {
      $this->patch = (int) $versionParts[2];
    }
  }

  /**
   * Tests if OpenSocialVersion is equivalent to the parameter version
   * @param OpenSocialVersion $version Compare with this version
   * @return boolean TRUE if is equivalent to version
   */
  public function isEquivalent(OpenSocialVersion $version){
    if ($this->major === null || $version->major === null) {
      return true;
    }
    $cmp = $version->major - $this->major;
    if($cmp == 0 && $version->minor && $this->minor){
      $cmp = $version->minor - $this->minor;
    }
    if($cmp == 0 && $version->patch && $this->patch){
      $cmp = $version->patch - $this->patch;
    }
    return $cmp == 0;
  }
  
  /**
   * Tests if OpenSocialVersion is equal to or greater than parameter version
   * @param OpenSocialVersion $version Compare with this version
   * @return boolean TRUE if is equal or greater than version
   */
  public function isEqualOrGreaterThan(OpenSocialVersion $version){
    if ($this->major === null || $version->major === null) {
      return true;
    }
    $cmp = $version->major - $this->major;
    if($cmp == 0){
      if($version->minor && $this->minor){
        $cmp = $version->minor - $this->minor;
      } else {
        $cmp = $version->minor;
      }
    }
    if($cmp == 0){
      if($version->patch && $this->patch){
        $cmp = $version->patch - $this->patch;
      } else {
        $cmp = $version->patch;
      }
    }
    return $cmp <= 0;
  }
  
  /**
   * @return string
   */
  public function __toString() {
    return $this->major . '.' . $this->minor . '.' . $this->patch;
  }
}

