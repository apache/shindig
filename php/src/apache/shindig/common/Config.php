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
 * Configuration class. It uses the keys/values from config/container.php
 * and (if the file exists) config/local.php.
 */
class Config {
  /**
   *
   * @var array
   */
  private static $config = false;

  /**
   *
   * @global array $shindigConfig
   * @param string $local
   */
  public static function loadConfig($localConfigPath = null) {
    if (! self::$config) {
      // load default configuration
      self::$config = require realpath(dirname(__FILE__) . "/../../../../config") . '/container.php';
      if ($localConfigPath) {
        if (file_exists($localConfigPath)) {
          // include local.php if it exists and merge the config arrays.
          // the second array values overwrites the first one's
          $localConfig = require $localConfigPath;
          self::$config = array_merge(self::$config, $localConfig);
        }
      }
    }
  }

  /**
   * Merges the given array with the config array. It uses the keys/values from config/container.php.
   *
   * @param array $tconfig
   */
  public static function setConfig($tconfig) {
    self::$config = array_merge(self::$config, $tconfig);
  }

  /**
   *
   * @param string $key
   * @return array
   * @throws ConfigException
   */
  public static function get($key) {
    if (isset(self::$config[$key])) {
      return self::$config[$key];
    } else {
      throw new ConfigException("Invalid Config Key " . $key);
    }
  }

  /**
   * Overrides a config value for as long as this object is loaded in memory.
   * This allows for overriding certain configuration values for the purposes
   * of unit tests.  Note that this does not commit a permanent change to the
   * configuration files.
   *
   * @param $key string Configuration key to set the value of.
   * @param $value string Value to assign to the specified key.
   */
  public static function set($key, $value) {
    self::$config[$key] = $value;
  }
}
