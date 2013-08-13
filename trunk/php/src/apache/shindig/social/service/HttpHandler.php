<?php
namespace apache\shindig\social\service;
use apache\shindig\gadgets\MakeRequestOptions;
use apache\shindig\common\Config;


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
 * Handler for http.* requests.  This is required to support osapi.http, which
 * has an implicit RPC service dependency.  More information about what we're
 * supporting here is available at:
 * http://www.opensocial.org/Technical-Resources/opensocial-spec-v09/OpenSocial-Specification.html#osapi.http
 */
class HttpHandler extends DataRequestHandler {

  /**
   * Yet another do nothing constructor.
   */
  public function __construct() {
    // Nothing to see here.
  }

  /**
   * Processes an RPC request for http data.
   *
   * @param RequestItem $requestItem The request parameters.
   * @return array An array of content, status code, and headers from the
   *     response.  The expected structure is undocumented in the spec, sadly.
   *     TODO: Filter some/most headers from the response (waste of bandwidth).
   */
  public function handleItem(RequestItem $requestItem) {
    try {
      // We should only get RPC requests at this point.  There's a class cast
      // here from RequestItem->RpcRequestItem, but PHP doesn't seem to
      // complain.  
      $options = MakeRequestOptions::fromRpcRequestItem($requestItem);
      $makeRequestClass = Config::get('makerequest_class');
      $makeRequest = new $makeRequestClass();
      $contextClass = Config::get('gadget_context_class');
      $context = new $contextClass('GADGET');
      $response = $makeRequest->fetch($context, $options);

      // try to decode json object here since in order
      // to not break gadgets.io.makeRequest functionality
      // $response->getResponseContent() has to return a string
      $content = json_decode($response->getResponseContent(), true);

      $result = array(
        'content' => $content ? $content : $response->getResponseContent(),
        'status' => $response->getHttpCode(),
        'headers' => $response->getResponseHeaders()
      );
    } catch (SocialSpiException $e) {
      $result = new ResponseItem($e->getCode(), $e->getMessage());
    } catch (\Exception $e) {
      $result = new ResponseItem(ResponseError::$INTERNAL_ERROR, "Internal error: " . $e->getMessage());
    }
    return $result;
  }

  /**
   * Only RPC operations supported.
   * @param RequestItem $request The request item.
   */
  public function handleDelete(RequestItem $request) {
    throw new SocialSpiException("DELETE not allowed for http service", ResponseError::$BAD_REQUEST);
  }

  /**
   * Only RPC operations supported.
   * @param RequestItem $request The request item.
   */
  public function handlePut(RequestItem $request) {
    throw new SocialSpiException("PUT not allowed for http service", ResponseError::$BAD_REQUEST);
  }

  /**
   * Only RPC operations supported.
   * @param RequestItem $request The request item.
   */
  public function handlePost(RequestItem $request) {
    throw new SocialSpiException("POST not allowed for http service", ResponseError::$BAD_REQUEST);
  }

  /**
   * Only RPC operations supported.
   * @param RequestItem $request The request item.
   */
  public function handleGet(RequestItem $request) {
    throw new SocialSpiException("GET not allowed for http service", ResponseError::$BAD_REQUEST);
  }
}
