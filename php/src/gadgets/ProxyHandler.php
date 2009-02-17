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

// according to features/core/io.js, this is high on the list of things to scrap
define('UNPARSEABLE_CRUFT', "throw 1; < don't be evil' >");

/**
 * The ProxyHandler class does the actual proxy'ing work. it deals both with
 * GET and POST based input, and peforms a request based on the input, headers and
 * httpmethod params.
 *
 */
class ProxyHandler extends ProxyBase {

  /**
   * Fetches the content and returns it as-is using the headers as returned
   * by the remote host.
   *
   * @param string $url the url to retrieve
   */
  public function fetch($url) {
    $url = $this->validateUrl($url);
    $result = $this->fetchContent($url, 'GET');
    $httpCode = (int)$result->getHttpCode();
    $isShockwaveFlash = false;
    foreach ($result->getResponseHeaders() as $key => $val) {
      if (! in_array($key, $this->disallowedHeaders)) {
        header("$key: $val", true);
      }
      if ($key == 'Content-Type' && strtolower($val) == 'application/x-shockwave-flash') {
        // We're skipping the content disposition header for flash due to an issue with Flash player 10
        // This does make some sites a higher value phishing target, but this can be mitigated by
        // additional referer checks.
        $isShockwaveFlash = true;
      }
    }
    if (! $isShockwaveFlash) {
      header('Content-Disposition: attachment;filename=p.txt');
    }
    $lastModified = $result->getResponseHeader('Last-Modified') != null ? $result->getResponseHeader('Last-Modified') : gmdate('D, d M Y H:i:s', $result->getCreated()) . ' GMT';
    $notModified = false;
    if (isset($_SERVER['HTTP_IF_MODIFIED_SINCE']) && $lastModified && ! isset($_SERVER['HTTP_IF_NONE_MATCH'])) {
      $if_modified_since = strtotime($_SERVER['HTTP_IF_MODIFIED_SINCE']);
      // Use the request's Last-Modified, otherwise fall back on our internal time keeping (the time the request was created)
      $lastModified = strtotime($lastModified);
      if ($lastModified <= $if_modified_since) {
        $notModified = true;
      }
    }
    if ($httpCode == 200) {
      // only set caching headers if the result was 'OK'
      $this->setCachingHeaders($lastModified);
    }
    // If the cached file time is within the refreshInterval params value, return not-modified
    if ($notModified) {
      header('HTTP/1.0 304 Not Modified', true);
      header('Content-Length: 0', true);
    } else {
      // then echo the content
      echo $result->getResponseContent();
    }
  }
}
