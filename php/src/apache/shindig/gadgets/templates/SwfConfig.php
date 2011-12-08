<?php
namespace apache\shindig\gadgets\templates;

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

class SwfConfig {
  public static $FLASH_VER = '9.0.115';
  public static $PARAMS = array('loop', 'menu', 'quality', 'scale', 'salign', 'wmode', 'bgcolor',
      'swliveconnect', 'flashvars', 'devicefont', 'allowscriptaccess', 'seamlesstabbing',
      'allowfullscreen', 'allownetworking');
  public static $ATTRS = array('id', 'name', 'styleclass', 'align');

  /**
   *
   * @param array $swfConfig
   * @param string $altContentId
   * @param string $flashVars
   * @return string
   */
  public static function buildSwfObjectCall($swfConfig, $altContentId, $flashVars = 'null') {
    $params = SwfConfig::buildJsObj($swfConfig, SwfConfig::$PARAMS);
    $attrs = SwfConfig::buildJsObj($swfConfig, SwfConfig::$ATTRS);
    $flashVersion = SwfConfig::$FLASH_VER;
    $swfObject = "swfobject.embedSWF(\"{$swfConfig['swf']}\", \"{$altContentId}\", \"{$swfConfig['width']}\", \"{$swfConfig['height']}\", \"{$flashVersion}\", null, {$flashVars}, {$params}, {$attrs});";
    return $swfObject;
  }

  private static function buildJsObj($swfConfig, $keymap) {
    $arr = array();
    foreach ($swfConfig as $key => $value) {
      if (in_array($key, $keymap)) {
        $arr[] = "{$key}:\"{$value}\"";
      }
    }
    $output = implode(",", $arr);
    $output = '{' . $output . '}';
    return $output;
  }
}