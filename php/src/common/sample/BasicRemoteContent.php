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
  /**
   * @var BesicRemoteContentFetcher
   */
  private $basicFetcher = null;

  /**
   * @var SigningFetcherFactory
   */
  private $signingFetcherFactory = null;

  /**
   * @var SecurityTokenDecoder
   */
  private $signer = null;

  /**
   * @var Cache
   */
  private $cache = null;

  /**
   * @var InvalidateService
   */
  private $invalidateService = null;

  /**
   * @param RemoteContentFetcher $basicFetcher
   * @param SigningFetcherFactory $signingFetcherFactory
   * @param SecurityTokenDecoder $signer
   */
  public function __construct(RemoteContentFetcher $basicFetcher = null, $signingFetcherFactory = null, $signer = null) {
    $this->basicFetcher = $basicFetcher ? $basicFetcher : new BasicRemoteContentFetcher();
    $this->signingFetcherFactory = $signingFetcherFactory;
    $this->signer = $signer;
    $this->cache = Cache::createCache(Config::get('data_cache'), 'RemoteContent');
    $this->invalidateService = new DefaultInvalidateService($this->cache);
  }

  public function setBasicFetcher(RemoteContentFetcher $basicFetcher) {
    $this->basicFetcher = $basicFetcher;
  }

  public function fetch(RemoteContentRequest $request, GadgetContext $context) {
    if (! $context->getIgnoreCache() && ! $request->isPost() && ($cachedRequest = $this->cache->get($request->toHash())) !== false && $this->invalidateService->isValid($cachedRequest)) {
      $request = $cachedRequest;
    } else {
      $originalRequest = clone $request;
      $request = $this->divertFetch($request);
      if ($request->getHttpCode() != 200 && ! $context->getIgnoreCache() && ! $request->isPost()) {
        $cachedRequest = $this->cache->expiredGet($request->toHash());
        if ($cachedRequest['found'] == true) {
          return $cachedRequest['data'];
        }
      }
      $this->setRequestCache($originalRequest, $request, $this->cache, $context);
    }
    return $request;
  }

  public function multiFetch(Array $requests, Array $contexts) {
    $rets = array();
    $requestsToProc = array();
    foreach ($requests as $request) {
      list(, $context) = each($contexts);
      if (! ($request instanceof RemoteContentRequest)) {
        throw new RemoteContentException("Invalid request type in remoteContent");
      }
      // determine which requests we can load from cache, and which we have to actually fetch
      if (! $context->getIgnoreCache() && ! $request->isPost() && ($cachedRequest = $this->cache->get($request->toHash())) !== false && $this->invalidateService->isValid($cachedRequest)) {
        $rets[] = $cachedRequest;
      } else {
        $originalRequest = clone $request;
        $requestsToProc[] = $request;
        $originalRequestArray[] = $originalRequest;
      }
    }

    if ($requestsToProc) {
      $newRets = $this->basicFetcher->multiFetchRequest($requestsToProc);
      foreach ($newRets as $request) {
        list(, $originalRequest) = each($originalRequestArray);
        if ($request->getHttpCode() != 200 && ! $context->getIgnoreCache() && ! $request->isPost()) {
          $cachedRequest = $this->cache->expiredGet($request->toHash());
          if ($cachedRequest['found'] == true) {
            $rets[] = $cachedRequest['data'];
          }
        } else {
          $this->setRequestCache($originalRequest, $request, $this->cache, $context);
          $rets[] = $request;
        }
      }
    }
    return $rets;
  }

  public function invalidate(RemoteContentRequest $request) {
    $this->cache->invalidate($request->toHash());
  }

  private function setRequestCache(RemoteContentRequest $originalRequest, RemoteContentRequest $request, Cache $cache, GadgetContext $context) {
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
      $this->invalidateService->markResponse($request);
      $this->cache->set($originalRequest->toHash(), $request, $ttl);
    }
  }

  private function divertFetch(RemoteContentRequest $request) {
    switch ($request->getAuthType()) {
      case RemoteContentRequest::$AUTH_SIGNED:
        $token = $request->getToken();
        $fetcher = $this->signingFetcherFactory->getSigningFetcher($this->basicFetcher, $token);
        return $fetcher->fetchRequest($request);
      case RemoteContentRequest::$AUTH_OAUTH:
        $params = new OAuthRequestParams();
        $token = $request->getToken();
        $fetcher = $this->signingFetcherFactory->getSigningFetcher($this->basicFetcher, $token);
        $oAuthFetcherFactory = new OAuthFetcherFactory($fetcher);
        $oauthFetcher = $oAuthFetcherFactory->getOAuthFetcher($fetcher, $token, $params);
        return $oauthFetcher->fetch($request);
      default:
        return $this->basicFetcher->fetchRequest($request);
    }
  }
}
