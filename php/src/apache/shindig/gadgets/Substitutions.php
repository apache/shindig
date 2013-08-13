<?php
namespace apache\shindig\gadgets;

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

class Substitutions {
  /**
   * @var array
   */
  protected $types = array('MESSAGE' => 'MSG', 'BIDI' => 'BIDI', 'USER_PREF' => 'UP', 'MODULE' => 'MODULE');

  /**
   * @var array
   */
  protected $substitutions = array();

  public function __construct() {
    foreach ($this->types as $type) {
      $this->substitutions[$type] = array();
    }
  }

  /**
   *
   * @param string $type
   * @param string $key
   * @param string $value
   */
  public function addSubstitution($type, $key, $value) {
    $this->substitutions[$type]["__{$type}_{$key}__"] = $value;
  }

  /**
   *
   * @param string $type
   * @param array $array
   */
  public function addSubstitutions($type, $array) {
    foreach ($array as $key => $value) {
      $this->addSubstitution($type, $key, $value);
    }
  }

  /**
   *
   * @param string $input
   * @return string
   */
  public function substitute($input) {
    foreach ($this->types as $type) {
      $input = $this->substituteType($type, $input);
    }
    return $input;
  }

  /**
   *
   * @param string $type
   * @param string $input
   * @return string
   */
  public function substituteType($type, $input) {
    foreach ($this->substitutions[$type] as $key => $value) {
      if (! is_array($value)) {
        $input = str_replace($key, $value, $input);
      }
    }
    return $input;
  }

  /**
   * Substitutes a uri
   * @param string $type The type to substitute, or null for all types.
   * @param string $uri
   * @return string The substituted uri, or a dummy value if the result is invalid.
   */
  public function substituteUri($type, $uri) {
    if (empty($uri)) {
      return null;
    }
    try {
      if (! empty($type)) {
        return $this->substituteType($type, $uri);
      } else {
        return $this->substitute($uri);
      }
    } catch (\Exception $e) {
      return "";
    }
  }
}
