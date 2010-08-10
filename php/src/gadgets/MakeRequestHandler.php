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

require_once 'src/gadgets/MakeRequest.php';

// according to features/core/io.js, this is high on the list of things to scrap
define('UNPARSEABLE_CRUFT', "throw 1; < don't be evil' >");

/**
 * Handles the gadget.io.makeRequest requests
 */
class MakeRequestHandler extends ProxyBase {

  /**
   * Constructor.
   *
   * @param GadgetContext $context Current rendering context
   */
  public function __construct(GadgetContext $context) {
    $this->context = $context;
    $this->makeRequest = new MakeRequest();
  }

  /**
   * Fetches content and echoes it in JSON format
   *
   * @param MakeRequestOptions $params  The request configuration.
   */
  public function fetchJson(MakeRequestOptions $params) {
    $result = $this->makeRequest->fetch($this->context, $params);
    $responseArray = array(
      'rc' => (int)$result->getHttpCode(),
      'body' => $result->getResponseContent(),
      'headers' => $this->makeRequest->cleanResponseHeaders($result->getResponseHeaders())
    );
    $responseArray = array_merge($responseArray, $result->getMetadatas());
    $json = array($params->getHref() => $responseArray);
    $json = json_encode($json);
    if (strpos($json, '\u')) {
      $json = $this->makeRequest->decodeUtf8($json);
    }
    $output = UNPARSEABLE_CRUFT . $json;
    if ($responseArray['rc'] == 200) {
      // only set caching headers if the result was 'OK'
      $this->setCachingHeaders();
    }
    if (!Config::get('debug')) {
      header('Content-Type: application/json; charset="UTF-8"');
      header('Content-Disposition: attachment;filename=p.txt');
    }
    echo $output;
  }
}
