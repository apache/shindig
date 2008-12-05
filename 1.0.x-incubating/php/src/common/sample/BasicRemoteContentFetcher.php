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
 * Basic remote content fetcher, uses curl_multi to fetch multiple resources at the same time
 */
class BasicRemoteContentFetcher extends RemoteContentFetcher {
  private $requests = array();
  const USER_AGENT = 'Shindig PHP';

  public function fetchRequest($request) {
    $request->handle = curl_init();
    curl_setopt($request->handle, CURLOPT_URL, $request->getUrl());
    curl_setopt($request->handle, CURLOPT_FOLLOWLOCATION, 1);
    curl_setopt($request->handle, CURLOPT_RETURNTRANSFER, 1);
    curl_setopt($request->handle, CURLOPT_AUTOREFERER, 1);
    curl_setopt($request->handle, CURLOPT_MAXREDIRS, 10);
    curl_setopt($request->handle, CURLOPT_CONNECTTIMEOUT, Config::get('curl_connection_timeout'));
    curl_setopt($request->handle, CURLOPT_TIMEOUT, 2);
    curl_setopt($request->handle, CURLOPT_HEADER, 1);
    curl_setopt($request->handle, CURLOPT_SSL_VERIFYPEER, 0);
    $proxy = Config::get('proxy');
    if (! empty($proxy)) {
      curl_setopt($request->handle, CURLOPT_PROXY, $proxy);
    }
    if ($request->hasHeaders()) {
      $headers = explode("\n", $request->getHeaders());
      $outHeaders = array();
      foreach ($headers as $header) {
        if (strpos($header, ':')) {
          $key = trim(substr($header, 0, strpos($header, ':')));
          $val = trim(substr($header, strpos($header, ':') + 1));
          if (strcmp($key, "User-Agent") != 0 && strcasecmp($key, "Transfer-Encoding") != 0 && strcasecmp($key, "Cache-Control") != 0 && strcasecmp($key, "Expires") != 0 && strcasecmp($key, "Content-Length") != 0) {
            $outHeaders[] = "$key: $val";
          }
        }
      }
      $outHeaders[] = "User-Agent: " . BasicRemoteContentFetcher::USER_AGENT;
      curl_setopt($request->handle, CURLOPT_HTTPHEADER, $outHeaders);
    }
    if ($request->isPost()) {
      curl_setopt($request->handle, CURLOPT_POST, 1);
      curl_setopt($request->handle, CURLOPT_POSTFIELDS, $request->getPostBody());
    }
    // Execute the request
    $content = @curl_exec($request->handle);
    $header = '';
    $body = '';
    // on redirects and such we get multiple headers back from curl it seems, we really only want the last one
    while (substr($content, 0, strlen('HTTP')) == 'HTTP' && strpos($content, "\r\n\r\n") !== false) {
      $header = substr($content, 0, strpos($content, "\r\n\r\n"));
      $content = $body = substr($content, strlen($header) + 4);
    }
    $httpCode = curl_getinfo($request->handle, CURLINFO_HTTP_CODE);
    $contentType = curl_getinfo($request->handle, CURLINFO_CONTENT_TYPE);
    if (! $httpCode) {
      $httpCode = '404';
    }
    $request->setHttpCode($httpCode);
    $request->setContentType($contentType);
    $request->setResponseHeaders($header);
    $request->setResponseContent($body);
    $request->setResponseSize(strlen($content));
    curl_close($request->handle);
    unset($request->handle);
    return $request;
  }

  public function multiFetchRequest(Array $requests) {
    $mh = curl_multi_init();
    foreach ($requests as $request) {
      $request->handle = curl_init();
      curl_setopt($request->handle, CURLOPT_URL, $request->getUrl());
      curl_setopt($request->handle, CURLOPT_FOLLOWLOCATION, 1);
      curl_setopt($request->handle, CURLOPT_RETURNTRANSFER, 1);
      curl_setopt($request->handle, CURLOPT_AUTOREFERER, 1);
      curl_setopt($request->handle, CURLOPT_MAXREDIRS, 10);
      curl_setopt($request->handle, CURLOPT_CONNECTTIMEOUT, Config::get('curl_connection_timeout'));
      curl_setopt($request->handle, CURLOPT_TIMEOUT, 2);
      curl_setopt($request->handle, CURLOPT_HEADER, 1);
      curl_setopt($request->handle, CURLOPT_SSL_VERIFYPEER, 0);
      // Set this so the multihandler will return data
      curl_setopt($request->handle, CURLOPT_RETURNTRANSFER, 1);
      
      $proxy = Config::get('proxy');
      if (! empty($proxy)) {
        curl_setopt($request->handle, CURLOPT_PROXY, $proxy);
      }
      if ($request->hasHeaders()) {
        $headers = explode("\n", $request->getHeaders());
        $outHeaders = array();
        foreach ($headers as $header) {
          if (strpos($header, ':')) {
            $key = trim(substr($header, 0, strpos($header, ':')));
            $val = trim(substr($header, strpos($header, ':') + 1));
            if (strcmp($key, "User-Agent") != 0 && strcasecmp($key, "Transfer-Encoding") != 0 && strcasecmp($key, "Cache-Control") != 0 && strcasecmp($key, "Expires") != 0 && strcasecmp($key, "Content-Length") != 0) {
              $outHeaders[] = "$key: $val";
            }
          }
        }
        $outHeaders[] = "User-Agent: " . BasicRemoteContentFetcher::USER_AGENT;
        curl_setopt($request->handle, CURLOPT_HTTPHEADER, $outHeaders);
      }
      if ($request->isPost()) {
        curl_setopt($request->handle, CURLOPT_POST, 1);
        curl_setopt($request->handle, CURLOPT_POSTFIELDS, $request->getPostBody());
      }
      curl_multi_add_handle($mh, $request->handle);
    }
    
    $running = null;
    //execute the handles
    do {
      curl_multi_exec($mh, $running);
    } while ($running > 0);
    
    //Ideally this should be 0 after curl_multi_info_read() is call, meaning all
    //calls have bee processed
    $msg_queue = null;
    $responses = curl_multi_info_read($mh, $msg_queue);
    
    foreach ($requests as $request) {
      // Execute the request
      $content = curl_multi_getcontent($request->handle);
      $header = '';
      $body = '';
      // on redirects and such we get multiple headers back from curl it seems, we really only want the last one
      while (substr($content, 0, strlen('HTTP')) == 'HTTP' && strpos($content, "\r\n\r\n") !== false) {
        $header = substr($content, 0, strpos($content, "\r\n\r\n"));
        $content = $body = substr($content, strlen($header) + 4);
      }
      $httpCode = curl_getinfo($request->handle, CURLINFO_HTTP_CODE);
      $contentType = curl_getinfo($request->handle, CURLINFO_CONTENT_TYPE);
      if (! $httpCode) {
        $httpCode = '404';
      }
      $request->setHttpCode($httpCode);
      $request->setContentType($contentType);
      $request->setResponseHeaders($header);
      $request->setResponseContent($body);
      $request->setResponseSize(strlen($content));
    }
    
    //	close the handles
    foreach ($requests as $request) {
      curl_close($request->handle);
      unset($request->handle);
    }
    curl_multi_close($mh);
    unset($mh);
    
    return $requests;
  }
}
