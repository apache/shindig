<?php
/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
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
class BasicGadgetSpecFactory implements GadgetSpecFactory {
	
	private $fetcher;

	public function __construct($fetcher)
	{
		$this->fetcher = $fetcher;
	}

	public function getGadgetSpec(GadgetContext $context)
	{
		return $this->getGadgetSpecUri($context->getUrl(), $context->getIgnoreCache());
	}

	/**
	 * Retrieves a gadget specification from the cache or from the Internet.
	 */
	public function getGadgetSpecUri($url, $ignoreCache)
	{
		if ($ignoreCache) {
			return $this->fetchFromWeb($url, true);
		}
		return $this->fetchFromWeb($url, false);
	}

	/**
	 * Retrieves a gadget specification from the Internet, processes its views and
	 * adds it to the cache.
	 */
	private function fetchFromWeb($url, $ignoreCache)
	{
		$remoteContentRequest = new RemoteContentRequest($url);
		$remoteContentRequest->getRequest($url, $ignoreCache);
		$spec = $this->fetcher->fetchRequest($remoteContentRequest);
		$specParser = new GadgetSpecParser();
		$context = new ProxyGadgetContext($url);
		$gadgetSpec = $specParser->parse($spec->getResponseContent(), $context);
		return $gadgetSpec;
	}

}
