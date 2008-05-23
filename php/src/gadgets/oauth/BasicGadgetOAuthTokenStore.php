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

class BasicGadgetOAuthTokenStore extends GadgetOAuthTokenStore {
	
	/** default location for consumer keys and secrets */
	private $OAUTH_CONFIG = "../config/oauth.json";
	private $CONSUMER_SECRET_KEY = "consumer_secret";
	private $CONSUMER_KEY_KEY = "consumer_key";
	private $KEY_TYPE_KEY = "key_type";

	public function __construct($store)
	{
		parent::__construct($store);
	}

	public function initFromConfigFile($fetcher)
	{
		// Read our consumer keys and secrets from config/oauth.js
		// This actually involves fetching gadget specs
		try {
			$oauthConfigStr = file_get_contents($this->OAUTH_CONFIG);
			
			// remove all comments because this confuses the json parser
			// note: the json parser also crashes on trailing ,'s in records so please don't use them
			$contents = preg_replace('@/\\*(?:.|[\\n\\r])*?\\*/@', '', $oauthConfigStr);
			$oauthConfig = json_decode($contents, true);
			foreach ($oauthConfig as $gadgetUri => $value) {
				$this->storeProviderInfos($fetcher, $gadgetUri);
				$this->storeConsumerInfos($gadgetUri, $value);
			}
		} catch (Exception $e) {
			throw new GadgetException($e);
		}
	}

	private function storeProviderInfos($fetcher, $gadgetUri)
	{
		$cache = Config::get('data_cache');
		$cache = new $cache();
		
		// determine which requests we can load from cache, and which we have to actually fetch
		if (($cachedRequest = $cache->get(md5($gadgetUri))) !== false) {
			$gadget = $cachedRequest;
		} else {
			$remoteContentRequest = new RemoteContentRequest($gadgetUri);
			$remoteContentRequest->getRequest($gadgetUri, false);
			$response = $fetcher->fetchRequest($remoteContentRequest);
			$context = new ProxyGadgetContext($gadgetUri);
			$spec = new GadgetSpecParser();
			$gadget = $spec->parse($response->getResponseContent(), $context);
			$cache->set(md5($gadgetUri), $gadget);
		}
		parent::storeServiceInfoFromGadgetSpec($gadgetUri, $gadget);
	}

	private function storeConsumerInfos($gadgetUri, $oauthConfig)
	{
		foreach ($oauthConfig as $key => $value) {
			$serviceName = $key;
			$consumerInfo = $value;
			$this->storeConsumerInfo($gadgetUri, $serviceName, $consumerInfo);
		}
	}

	private function storeConsumerInfo($gadgetUri, $serviceName, $consumerInfo)
	{
		$consumerSecret = $consumerInfo[$this->CONSUMER_SECRET_KEY];
		$consumerKey = $consumerInfo[$this->CONSUMER_KEY_KEY];
		$keyTypeStr = $consumerInfo[$this->KEY_TYPE_KEY];
		$keyType = 'HMAC_SYMMETRIC';
		
		if ($keyTypeStr == "RSA_PRIVATE") {
			$keyType = 'RSA_PRIVATE';
			// check if the converted from PKCS8 key is in cache, if not, convert it
			$cache = Config::get('data_cache');
			$cache = new $cache();
			
			if (($cachedRequest = $cache->get(md5("RSA_KEY_" . $serviceName))) !== false) {
				$consumerSecret = $cachedRequest;
			} else {
				$in = tempnam(sys_get_temp_dir(), "RSA_KEY");
				file_put_contents($in, base64_decode($consumerInfo[$this->CONSUMER_SECRET_KEY]));
				$out = tempnam(sys_get_temp_dir(), "RSA_KEY");
				exec("openssl pkcs8 -inform DER -outform PEM -out " . $out . " -nocrypt -in " . $in);
				$consumerSecret = file_get_contents($out);
				$cache->set(md5("RSA_KEY_" . $serviceName), $consumerSecret);
			}
		}
		
		$kas = new ConsumerKeyAndSecret($consumerKey, $consumerSecret, $keyType);
		$this->storeConsumerKeyAndSecret($gadgetUri, $serviceName, $kas);
	}

}
