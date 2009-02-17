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

class BasicRemoteContent extends RemoteContent {

  public function fetch(RemoteContentRequest $request, $context) {
    $cache = Config::get('data_cache');
    $cache = new $cache();
    $remoteContentFetcher = new BasicRemoteContentFetcher();
    if (! $context->getIgnoreCache() && ! $request->isPost() && ($cachedRequest = $cache->get($request->toHash())) !== false) {
      $request = $cachedRequest;
    } else {
      $request = $remoteContentFetcher->fetchRequest($request);
      $this->setRequestCache($request, $cache, $context);
    }
    return $request;
  }

  public function multiFetch(Array $requests, Array $contexts) {
    $cache = Config::get('data_cache');
    $cache = new $cache();
    $remoteContentFetcher = new BasicRemoteContentFetcher();
    $rets = array();
    $requestsToProc = array();
    foreach ($requests as $request) {
      list(, $context) = each($contexts);
      if (! ($request instanceof RemoteContentRequest)) {
        throw new RemoteContentException("Invalid request type in remoteContent");
      }
      // determine which requests we can load from cache, and which we have to actually fetch
      if (! $context->getIgnoreCache() && ! $request->isPost() && ($cachedRequest = $cache->get($request->toHash())) !== false) {
        $rets[] = $cachedRequest;
      } else {
        $requestsToProc[] = $request;
      }
    }
    $newRets = $remoteContentFetcher->multiFetchRequest($requestsToProc);
    foreach ($newRets as $request) {
      $this->setRequestCache($request, $cache, $context);
      $rets[] = $request;
    }
    return $rets;
  }

  private function setRequestCache(RemoteContentRequest $request, Cache $cache, GadgetContext $context) {
    if (! $request->isPost() && ! $context->getIgnoreCache()) {
      $ttl = Config::get('cache_time');
      if ($request->getHttpCode() == '200') {
        // Got a 200 OK response, calculate the TTL to use for caching it
        if (($expires = $request->getResponseHeader('Expires')) != null) {
          // prefer to use the servers notion of the time since there could be a clock-skew, but otherwise use our own
          $date = $request->getResponseHeader('Date') != null ? $request->getResponseHeader('Date') : gmdate('D, d M Y H:i:s', $_SERVER['REQUEST_TIME']) . ' GMT';
          // convert both dates to unix epoch seconds, and calculate the TTL
          $date = strtotime($date);
          $expires = strtotime($expires);
          $ttl = $expires - $date;
          // Don't fall for the old expiration-date-in-the-past trick, we *really* want to cache stuff since a large SNS's traffic would devastate a gadget developer's server
          if ($expires - $date > 1) {
            $ttl = $expires - $date;
          }
        }
        // See http://www.w3.org/Protocols/rfc2616/rfc2616-sec14.html : The Cache-Control: max-age=<seconds> overrides the expires header, sp if both are present this one will overwrite the $ttl
        if (($cacheControl = $request->getResponseHeader('Cache-Control')) != null) {
          $bits = explode('=', $cacheControl);
          foreach ($bits as $key => $val) {
            if ($val == 'max-age' && isset($bits[$key + 1])) {
              $ttl = $bits[$key + 1];
              break;
            }
          }
        }
      } else {
        $ttl = 5 * 60; // cache errors for 5 minutes, takes the denial of service attack type behaviour out of having an error :)
      }
      $cache->set($request->toHash(), $request, $ttl);
    }
  }
}
