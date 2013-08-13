<?php
namespace apache\shindig\social\converters;
use apache\shindig\common\Config;
use apache\shindig\social\service\ResponseItem;
use apache\shindig\social\service\RestRequestItem;
use apache\shindig\common\SecurityToken;
use apache\shindig\social\spi\RestfulCollection;

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
 * Format = json output converter
 *
 */
class OutputJsonConverter extends OutputConverter {

  /**
   *
   * @param ResponseItem $responseItem
   * @param RestRequestItem $requestItem
   */
  function outputResponse(ResponseItem $responseItem, RestRequestItem $requestItem) {
    $response = $responseItem->getResponse();
    if ($response instanceof RestfulCollection) {
      $itemsPerPage = $requestItem->getCount();
      if ($itemsPerPage > 0) $response->itemsPerPage = $itemsPerPage;
    }
    // several service calls return a null value
    if (! is_null($response)) {
        $this->encodeAndSendResponse($response);
    }
  }

  /**
   *
   * @param array $responses
   * @param SecurityToken $token
   */
  function outputBatch(Array $responses, SecurityToken $token) {
    $this->boundryHeaders();
    foreach ($responses as $response) {
      $request = $response['request'];
      $response = $response['response'];
      $part = json_encode($response);
      $this->outputPart($part, $response->getError());
    }
  }

  /**
   *
   * @param array $responses
   * @param SecurityToken $token
   */
  function outputJsonBatch(Array $responses, SecurityToken $token) {
    $this->encodeAndSendResponse(array("responses" => $responses, "error" => false));
  }

  /**
   * encodes data to json, adds jsonp callback if requested and sends response
   * to client
   *
   * @param array $data
   */
  private function encodeAndSendResponse($data) {
    if (isset($_GET['callback']) && preg_match('/^[a-zA-Z0-9\_\.]*$/', $_GET['callback'])) {
        echo $_GET['callback'] . '(' . json_encode($data) . ')';
        return;
    }
    if (Config::get('debug')) {
        echo self::json_format(json_encode($data)); // TODO: add a query option to pretty-print json output
    } else {
        echo json_encode($data);
    }
  }

  /**
   * Generate a pretty-printed representation of a JSON object.
   * 
   * Taken from php comments for json_encode.
   *
   * @param string $json  JSON string
   * @return string|false The pretty version, false if JSON was invalid
   */
  static function json_format($json) {
    $tab = "  ";
    $new_json = "";
    $indent_level = 0;
    $in_string = false;
    $json_obj = json_decode($json);
    if (! $json_obj) {
      return false;
    }
    $json = json_encode($json_obj);
    $len = strlen($json);
    for ($c = 0; $c < $len; $c ++) {
      $char = $json[$c];
      switch ($char) {
        case '{':
        case '[':
          if (! $in_string) {
            $new_json .= $char . "\n" . str_repeat($tab, $indent_level + 1);
            $indent_level ++;
          } else {
            $new_json .= $char;
          }
          break;
        
        case '}':
        case ']':
          if (! $in_string) {
            $indent_level --;
            $new_json .= "\n" . str_repeat($tab, $indent_level) . $char;
          } else {
            $new_json .= $char;
          }
          break;
        
        case ',':
          if (! $in_string) {
            $new_json .= ",\n" . str_repeat($tab, $indent_level);
          } else {
            $new_json .= $char;
          }
          break;
        
        case ':':
          if (! $in_string) {
            $new_json .= ":";
          } else {
            $new_json .= $char;
          }
          break;
        
        case '"':
          $in_string = ! $in_string;
        
        default:
          $new_json .= $char;
          break;
      }
    }
    return $new_json;
  }
}
