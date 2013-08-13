<?php
namespace apache\shindig\gadgets\sample;
use apache\shindig\common\RemoteContentRequest;
use apache\shindig\common\sample\BasicRemoteContent;
use apache\shindig\common\Config;
use apache\shindig\gadgets\GadgetContext;

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
 * Basic implementation of a gadget spec factory.
 */
class BasicGadgetSpecFactory {
  /**
   * @var GadgetContext
   */
  private $context;

  /**
   *
   * @param GadgetContext $context
   * @return GadgetSpec
   */
  public function getGadgetSpec(GadgetContext $context) {
    $this->context = $context;
    return $this->getGadgetSpecUri($context->getUrl(), $context->getIgnoreCache());
  }

  /**
   * Retrieves a gadget specification from the cache or from the Internet.
   *
   * @param string $url
   * @param boolean $ignoreCache
   * @return GadgetSpec
   */
  public function getGadgetSpecUri($url, $ignoreCache) {
    return $this->fetchFromWeb($url, $ignoreCache);
  }

  /**
   * Retrieves a gadget specification from the Internet, processes its views and
   * adds it to the cache.
   *
   * @param string $url
   * @param boolean $ignoreCache
   * @return GadgetSpec
   */
  private function fetchFromWeb($url, $ignoreCache) {
    $remoteContentRequest = new RemoteContentRequest($url);
    $remoteContentRequest->getOptions()->ignoreCache = $ignoreCache;
    $remoteContent = new BasicRemoteContent();
    $spec = $remoteContent->fetch($remoteContentRequest);

    $gadgetSpecParserClass = Config::get('gadget_spec_parser');
    $gadgetSpecParser = new $gadgetSpecParserClass();
    $gadgetSpec = $gadgetSpecParser->parse($spec->getResponseContent(), $this->context);
    return $gadgetSpec;
  }
}
