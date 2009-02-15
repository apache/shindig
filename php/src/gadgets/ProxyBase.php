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

/**
 * This class contains the shared methods between the Proxy and makeRequest handlers
 */
class ProxyBase {
  public $context;
  protected $disallowedHeaders = array('Keep-Alive', 'Host', 'Accept-Encoding', 'Set-Cookie', 'Content-Length', 'Content-Encoding', 'ETag', 'Last-Modified', 'Accept-Ranges', 'Vary', 'Expires', 'Date', 'Pragma', 'Cache-Control', 'Transfer-Encoding');

  public function __construct($context) {
    $this->context = $context;
  }

  /**
   * Retrieves the actual content
   *
   * @param string $url the url to fetch
   * @return the filled in request (RemoteContentRequest)
   */
  protected function fetchContent($url, $method = 'GET') {
    // Check the protocol requested - curl doesn't really support file://
    // requests but the 'error' should be handled properly
    $protocolSplit = explode('://', $url, 2);
    if (count($protocolSplit) < 2) {
      throw new Exception("Invalid protocol specified");
    } else {
      $protocol = strtoupper($protocolSplit[0]);
      if ($protocol != "HTTP" && $protocol != "HTTPS") {
        throw new Exception("Invalid protocol specified in url: " . htmlentities($protocol));
      }
    }
    $headers = '';
    $requestHeaders = $this->request_headers();
    foreach ($requestHeaders as $key => $val) {
      $key = str_replace(' ', '-', ucwords(str_replace('-', ' ', $key))); // force the header name to have the proper Header-Name casing
      if (! in_array($key, $this->disallowedHeaders)) {
        // propper curl header format according to http://www.php.net/manual/en/function.curl-setopt.php#80099
        $headers .= "$key: $val\n";
      }
    }
    if ($method == 'POST') {
      $data = isset($_GET['postData']) ? $_GET['postData'] : false;
      if (! $data) {
        $data = isset($_POST['postData']) ? $_POST['postData'] : false;
      }
      $postData = '';
      if ($data) {
        $data = urldecode($data);
        $entries = explode('&', $data);
        foreach ($entries as $entry) {
          $parts = explode('=', $entry);
          // Process only if its a valid value=something pair
          if (count($parts) == 2) {
            $postData .= urlencode($parts[0]) . '=' . urlencode($parts[1]) . '&';
          }
        }
        // chop of the trailing &
        if (strlen($postData)) {
          $postData = substr($postData, 0, strlen($postData) - 1);
        }
      }
      // even if postData is an empty string, it will still post (since RemoteContentRquest checks if its false)
      // so the request to POST is still honored
      $request = new RemoteContentRequest($url, $headers, $postData);
      $request = $this->context->getHttpFetcher()->fetch($request, $this->context);
    } else {
      $request = new RemoteContentRequest($url, $headers);
      $request = $this->context->getHttpFetcher()->fetch($request, $this->context);
    }
    return $request;
  }

  /**
   * Sets the caching (Cache-Control & Expires) with a cache age of $lastModified
   * or if $lastModified === false, sets Pragma: no-cache & Cache-Control: no-cache
   */
  protected function setCachingHeaders($lastModified = false) {
    $maxAge = $this->context->getIgnoreCache() ? false : $this->context->getRefreshInterval();
    if ($maxAge) {
      if ($lastModified) {
        header("Last-Modified: $lastModified");
      }
      // time() is a kernel call, so lets avoid it and use the request time instead
      $time = $_SERVER['REQUEST_TIME'];
      $expires = $maxAge !== false ? $time + $maxAge : $time - 3000;
      $public = $maxAge ? 'public' : 'private';
      $maxAge = $maxAge === false ? '0' : $maxAge;
      header("Cache-Control: {$public}; max-age={$maxAge}", true);
      header("Expires: " . gmdate("D, d M Y H:i:s", $expires) . " GMT", true);
    } else {
      header("Cache-Control: no-cache", true);
      header("Pragma: no-cache", true);
    }
  }

  /**
   * Does a quick-and-dirty url validation
   *
   * @param string $url
   * @return string the 'validated' url
   */
  protected function validateUrl($url) {
    if (! @parse_url($url)) {
      throw new Exception("Invalid Url");
    } else {
      return $url;
    }
  }

  /**
   * Returns the request headers, using the apache_request_headers function if it's
   * available, and otherwise tries to guess them from the $_SERVER superglobal
   *
   * @return unknown
   */
  protected function request_headers() {
    // Try to use apache's request headers if available
    if (function_exists("apache_request_headers")) {
      if (($headers = apache_request_headers())) {
        return $headers;
      }
    }
    // if that failed, try to create them from the _SERVER superglobal
    $headers = array();
    foreach (array_keys($_SERVER) as $skey) {
      if (substr($skey, 0, 5) == "HTTP_") {
        $headername = str_replace(" ", "-", ucwords(strtolower(str_replace("_", " ", substr($skey, 0, 5)))));
        $headers[$headername] = $_SERVER[$skey];
      }
    }
    return $headers;
  }
}
