<?php
namespace apache\shindig\social\servlet;
use apache\shindig\social\service\RpcRequestItem;
use apache\shindig\social\service\ResponseItem;
use apache\shindig\social\spi\RestfulCollection;
use apache\shindig\social\spi\DataCollection;

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
 * JSON-RPC handler servlet.
 */
class JsonRpcServlet extends ApiServlet {

  protected $resultKey = 'result';

  /**
   * Single request through GET
   * http://api.example.org/rpc?method=people.get&id=myself&params.userId=@me&params.groupId=@self
   */
  public function doGet() {
    $token = $this->getSecurityToken();
    if ($token == null) {
      $this->sendSecurityError();
      return;
    }

    $request = $this->parseGetRequest($_SERVER['QUERY_STRING']);

    $this->dispatch($request, $token);
  }

  /**
   * parses all $_GET parameters according to rpc spec
   * @see http://opensocial-resources.googlecode.com/svn/spec/1.1/Core-API-Server.xml#urlAddressing
   *
   * @param string $parameters should be $_GET on production
   * @return array
   */
  public function parseGetRequest($parameterString)
  {
    // we have to parse the query parameters by hand because parse_str or the built in
    // $_GET replace '.' with '_' in parameter keys
    $parameters = array();
    $pairs = explode('&', $parameterString);
    foreach ($pairs as $pair) {
      if (strpos($pair, '=') !== false) {
        list($key, $value) = explode('=', $pair);
        $parameters[$key] = urldecode($value);
      }
    }
    $request = array();
    foreach($parameters as $key => $value) {
      // parse value lists like field=1,2,3,4,5
      if (strpos($value, ',') !== false) {
        $parsedValue = explode(',', $value);
      } else {
        $parsedValue = $value;
      }
      // handle multidimensional nested keys like field.nested=value
      if (strpos($key, '.') > 0) {
        $keyParts = explode('.', $key);
        $request = $this->getMultiDimensionalArray($request, $keyParts, $parsedValue);
      } else {
        $request = $this->getMultiDimensionalArray($request, array($key), $parsedValue);
      }
    }
    return $request;
  }

  /**
   * parses a multidimensional parameter
   * e.g. params.foo=bar to 'params' => array('foo' => 'bar')
   *
   * @param array $request
   * @param array $keyParts
   * @param mixed $value
   * @return array
   */
  private function getMultiDimensionalArray($request, $keyParts, $value)
  {
    if (! $keyParts) {
      return $value;
    }
    $key = array_shift($keyParts);

    $matches = array();

    // handle something like field(0).nested1=value1&field(1).nested2=value2
    if (preg_match('/^([a-zA-Z0-9]*)\(([0-9]*)\)$/', $key, $matches)) {
      $key = $matches[1];
      array_unshift($keyParts, $matches[2]);
    }

    if (! isset($request[$key])) {
      $request[$key] = array();
    }
    $value = $this->getMultiDimensionalArray($request[$key], $keyParts, $value);

    if (is_array($value)) {
      $request[$key] = $value + $request[$key];
    } else {
      $request[$key] = $value;
    }

    return $request;
  }

  /**
   * RPC Post request
   */
  public function doPost() {
    $token = $this->getSecurityToken();
    if ($token == null || $token == false) {
      $this->sendSecurityError();
      return;
    }
    if (isset($GLOBALS['HTTP_RAW_POST_DATA']) || isset($_POST['request'])) {
      $requestParam = isset($GLOBALS['HTTP_RAW_POST_DATA']) ? $GLOBALS['HTTP_RAW_POST_DATA'] : (get_magic_quotes_gpc() ? stripslashes($_POST['request']) : $_POST['request']);
      $request = json_decode($requestParam, true);
      if ($request == $requestParam) {
        throw new \InvalidArgumentException("Malformed json string");
      }
    } else {
      throw new \InvalidArgumentException("Missing POST data");
    }
    if ((strpos($requestParam, '[') !== false) && strpos($requestParam, '[') < strpos($requestParam, '{')) {
      // Is a batch
      $this->dispatchBatch($request, $token);
    } else {
      $this->dispatch($request, $token);
    }
  }

  /**
   *
   * @param array $batch
   * @param SecurityToken $token
   */
  public function dispatchBatch($batch, $token) {
    $responses = array();
    // Gather all Futures.  We do this up front so that
    // the first call to get() comes after all futures are created,
    // which allows for implementations that batch multiple Futures
    // into single requests.
    for ($i = 0; $i < count($batch); $i ++) {
      $batchObj = $batch[$i];
      $requestItem = new RpcRequestItem($batchObj, $token);
      $responses[$i] = $this->handleRequestItem($requestItem);
    }
    // Resolve each Future into a response.
    // TODO: should use shared deadline across each request
    $result = array();
    for ($i = 0; $i < count($batch); $i ++) {
      $batchObj = $batch[$i];
      $key = isset($batchObj["id"]) ? $batchObj["id"] : null;
      $responseItem = $this->getJSONResponse($key, $this->getResponseItem($responses[$i]));
      $result[] = $responseItem;
    }
    $this->encodeAndSendResponse($result);
  }

  /**
   *
   * @param array $request
   * @param SecurityToken $token
   */
  public function dispatch($request, $token) {
    $key = null;
    if (isset($request["id"])) {
      $key = $request["id"];
    }
    $requestItem = new RpcRequestItem($request, $token);
    // Resolve each Future into a response.
    // TODO: should use shared deadline across each request
    $a = $this->handleRequestItem($requestItem);
    $response = $this->getResponseItem($a);
    $result = $this->getJSONResponse($key, $response);
    $this->encodeAndSendResponse($result);
  }

  /**
   *
   * @param string $key
   * @param ResponseItem $responseItem
   * @return array
   */
  private function getJSONResponse($key, ResponseItem $responseItem) {
    $result = array();
    if ($key != null) {
      $result["id"] = $key;
    }
    if ($responseItem->getError() != null) {
      $result["error"] = $this->getErrorJson($responseItem);
    } else {
      $response = $responseItem->getResponse();
      $converted = $response;
      if ($response instanceof RestfulCollection) {
        // FIXME this is a little hacky because of the field names in the RestfulCollection
        $converted->list = $converted->entry;
        unset($converted->entry);
        $result[$this->resultKey] = $converted;
      } elseif ($response instanceof DataCollection) {
        $result[$this->resultKey] = $converted->getEntry();
      } else {
        $result[$this->resultKey] = $converted;
      }
    }
    return $result;
  }

  // TODO(doll): Refactor the responseItem so that the fields on it line up with this format.
  // Then we can use the general converter to output the response to the client and we won't
  // be harcoded to json.
  /**
   *
   * @param ResponseItem $responseItem
   * @return array
   */
  private function getErrorJson(ResponseItem $responseItem) {
    $error = array();
    $error["code"] = $responseItem->getError();
    $error["message"] = $responseItem->getErrorMessage();
    return $error;
  }

  /**
   * encodes data to json, adds jsonp callback if requested and sends response
   * to client
   *
   * @param array $data
   * @return string
   */
  private function encodeAndSendResponse($data) {
    // TODO: Refactor this class to use the OutputJsonConverter, so that we do not have to duplicate
    // encoding and JSONP handling here
    if (isset($_GET['callback']) && preg_match('/^[a-zA-Z0-9\_\.]*$/', $_GET['callback'])) {
        echo $_GET['callback'] . '(' . json_encode($data) . ')';
        return;
    }
    echo json_encode($data);
  }

  /**
   *
   * @param ResponseItem $responseItem
   */
  public function sendError(ResponseItem $responseItem) {
    $error = $this->getErrorJson($responseItem);
    $this->encodeAndSendResponse($error);
  }
}
