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


class RemoteContentRequest {
  // these are used for making the request
  private $uri = '';
  // to get real url after signed requests
  private $notSignedUri = '';
  private $method = '';
  private $headers = array();
  private $postBody = false;
  // these fields are filled in once the request has completed
  private $responseContent = false;
  private $responseSize = false;
  private $responseHeaders = false;
  private $httpCode = false;
  private $contentType = null;
  private $options;
  private $created;
  private static $SC_OK = 200; //Please, use only for testing!
  public $handle = false;
  public static $DEFAULT_CONTENT_TYPE = "application/x-www-form-urlencoded; charset=utf-8";

  public function __construct($uri, $headers = false, $postBody = false) {
    $this->uri = $uri;
    $this->notSignedUri = $uri;
    $this->headers = $headers;
    $this->postBody = $postBody;
    $this->created = time();
  }

  public function createRemoteContentRequest($method, $uri, $headers, $postBody, $options) {
    $this->method = $method;
    $this->uri = $uri;
    $this->options = $options;
    // Copy the headers
    if (! isset($headers)) {
      $this->headers = '';
    } else {
      $setPragmaHeader = false;
      $tmpHeaders = '';
      foreach ($headers as $key => $value) {
        // Proxies should be bypassed with the Pragma: no-cache check.
        if ($key == "Pragma" && $options->ignoreCache) {
          $value = "no-cache";
          $setPragmaHeader = true;
        }
        $tmpHeaders .= $key . ":" . $value . "\n";
      }
      // Bypass caching in proxies as well.
      if (! $setPragmaHeader && $options->ignoreCache) {
        $tmpHeaders .= "Pragma:no-cache\n";
      }
      $this->headers = $tmpHeaders;
    }
    if (! isset($postBody)) {
      $this->postBody = '';
    } else {
      $this->postBody = array_merge($postBody, $this->postBody);
    }
    $type = $this->getHeader("Content-Type");
    if (! isset($type)) {
      $this->contentType = RemoteContentRequest::$DEFAULT_CONTENT_TYPE;
    } else {
      $this->contentType = $type;
    }
  }

  /**
   * Creates a new request to a different URL using all request data from
   * an existing request.
   *
   * @param uri
   * @param base The base request to copy data from.
   */
  public static function createRemoteContentRequestWithUriBase($uri, $base) {
    $this->uri = $uri;
    $this->method = $base->method;
    $this->options = $base->options;
    $this->headers = $base->headers;
    $this->contentType = $base->contentType;
    $this->postBody = $base->postBody;
  }

  /**
   * Basic GET request.
   *
   * @param uri
   */
  public function createRemoteContentRequestWithUri($uri) {
    $this->createRemoteContentRequest("GET", $uri, null, null, RemoteContentRequest::getDefaultOptions());
  }

  /**
   * GET with options
   *
   * @param uri
   * @param options
   */
  public function createRemoteContentRequestWithUriOptions($uri, $options) {
    $this->createRemoteContentRequest("GET", $uri, null, null, $options);
  }

  /**
   * GET request with custom headers and default options
   * @param uri
   * @param headers
   */
  public function RemoteContentRequestWithUriHeaders($uri, $headers) {
    $this->createRemoteContentRequest("GET", $uri, $headers, null, RemoteContentRequest::getDefaultOptions());
  }

  /**
   * GET request with custom headers + options
   * @param uri
   * @param headers
   * @param options
   */
  public function createRemoteContentRequestWithUriHeadersOptions($uri, $headers, $options) {
    $this->createRemoteContentRequest("GET", $uri, $headers, null, $options);
  }

  /**
   * Basic POST request
   * @param uri
   * @param postBody
   */
  public function RemoteContentRequestWithUriPostBody($uri, $postBody) {
    $this->createRemoteContentRequest("POST", $uri, null, $postBody, RemoteContentRequest::getDefaultOptions());
  }

  /**
   * POST request with options
   * @param uri
   * @param postBody
   * @param options
   */
  public function createRemoteContentRequestWithUriPostBodyOptions($uri, $postBody, $options) {
    $this->createRemoteContentRequest("POST", $uri, null, $postBody, $options);
  }

  /**
   * POST request with headers
   * @param uri
   * @param headers
   * @param postBody
   */
  public function createRemoteContentRequestWithUriHeadersPostBody($uri, $headers, $postBody) {
    $this->createRemoteContentRequest("POST", $uri, $headers, $postBody, RemoteContentRequest::getDefaultOptions());
  }

  /**
   * POST request with options + headers
   * @param uri
   * @param headers
   * @param postBody
   * @param options
   */
  public function createRemoteContentRequestWithUriHeadersPostBodyOptions($uri, $headers, $postBody, $options) {
    $this->createRemoteContentRequest("POST", $uri, $headers, $postBody, $options);
  }

  /**
   * Creates a simple GET request
   *
   * @param uri
   * @param ignoreCache
   */
  public function getRequest($uri, $ignoreCache) {
    $options = new Options();
    $options->ignoreCache = $ignoreCache;
    return $this->createRemoteContentRequestWithUriOptions($uri, $options);
  }

  /**
   * Simple constructor for setting a basic response from a string. Mostly used
   * for testing.
   *
   * @param body
   */
  public function getHttpFalseResponseBody($body) {
    return $this->createFalseResponse(RemoteContentRequest::$SC_OK, $body, null);
  }

  private function createFalseResponse($httpCode, $body, $headers) {
    $this->httpCode = $httpCode;
    $this->responseContent = $body;
    $this->headers = $headers;
    return $this;
  }

  // returns a hash code which identifies this request, used for caching
  // takes url and postbody into account for constructing the sha1 checksum
  public function toHash() {
    return md5($this->uri . $this->postBody);
  }

  public static function getDefaultOptions() {
    return new Options();
  }

  public function getContentType() {
    return $this->contentType;
  }

  public function getHttpCode() {
    return $this->httpCode;
  }

  public function getResponseContent() {
    return $this->responseContent;
  }

  public function getResponseHeaders() {
    return $this->responseHeaders;
  }

  public function getResponseSize() {
    return $this->responseSize;
  }

  public function getHeaders() {
    return $this->headers;
  }

  public function isPost() {
    return ($this->postBody != false);
  }

  public function hasHeaders() {
    return ! empty($this->headers);
  }

  public function getPostBody() {
    return $this->postBody;
  }

  public function getUrl() {
    return $this->uri;
  }

  public function getNotSignedUrl() {
    return $this->notSignedUri;
  }

  public function getMethod() {
    return $this->method;
  }

  public function getOptions() {
    if (empty($this->options)) {
      return new Options();
    }
    return $this->options;
  }

  public function setContentType($type) {
    $this->contentType = $type;
  }

  public function setHttpCode($code) {
    $this->httpCode = intval($code);
  }

  public function setResponseContent($content) {
    $this->responseContent = $content;
  }

  public function setResponseHeaders($headers) {
    $this->responseHeaders = $headers;
  }

  public function setResponseSize($size) {
    $this->responseSize = intval($size);
  }

  public function setHeaders($headers) {
    $this->headers = $headers;
  }

  //FIXME: Find a better way to do this
  // The headers can be an array of elements.
  public function getHeader($headerName) {
    $headers = explode("\n", $this->headers);
    foreach ($headers as $header) {
      $key = explode(":", $header, 2);
      if ($key[0] == $headerName) return trim($key[1]);
    }
    return null;
  }

  //FIXME: Find a better way to do this
  // The headers can be an array of elements.
  public function getResponseHeader($headerName) {
    $headers = explode("\n", $this->responseHeaders);
    foreach ($headers as $header) {
      $key = explode(":", $header, 2);
      if ($key[0] == $headerName) {
        return trim($key[1]);
      }
    }
    return null;
  }

  public function getCreated() {
    return $this->created;
  }

  public function setPostBody($postBody) {
    $this->postBody = $postBody;
  }

  public function setUri($uri) {
    $this->uri = $uri;
  }

  public function setNotSignedUri($uri) {
    $this->notSignedUri = $uri;
  }
}

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
   */
  public function copyOptions(Options $copyFrom) {
    $this->ignoreCache = $copyFrom->ignoreCache;
    $this->ownerSigned = $copyFrom->ownerSigned;
    $this->viewerSigned = $copyFrom->viewerSigned;
  }

}
