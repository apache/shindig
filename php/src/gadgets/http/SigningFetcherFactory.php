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
 * Produces Signing content fetchers for input tokens.
 */
class SigningFetcherFactory {
    private $keyName;
    private $privateKey;

	/**
	 * Produces a signing fetcher that will sign requests and delegate actual
	 * network retrieval to the {@code networkFetcher}
	 *
	 * @param networkFetcher The fetcher that will be doing actual work.
	 * @param token The gadget token used for extracting signing parameters.
	 * @return The signing fetcher.
	 * @throws GadgetException
	 */
	public function getSigningFetcher($networkFetcher, $token)
	{
		return SigningFetcher::makeFromB64PrivateKey($networkFetcher, $token, $this->keyName, $this->privateKey);
	}

	/**
	 * @param keyFile The file containing your private key for signing requests.
	 */
	public function __construct($keyFile = null)
	{
		$this->keyName = 'http://'.$_SERVER["HTTP_HOST"].Config::get('web_prefix').'/public.crt';
		if (! empty($keyFile)) {
			$privateKey = null;
			try {
				// check if the converted from PKCS8 key is in cache, if not, convert it
				$cache = Config::get('data_cache');
				$cache = new $cache();
				if (($cachedKey = $cache->get(md5("RSA_PRIVATE_KEY_" . $this->keyName))) !== false) {
					$rsa_private_key = $cachedKey;
				} else {
					if (! $rsa_private_key = @file_get_contents($keyFile)) {
						throw new Exception("Could not read keyfile ($keyFile), check the file name and permission");
					}
					// TODO: sending NULL as a second param to openssl_pkey_get_private works?
					$phrase = Config::get('private_key_phrase') != '' ? (Config::get('private_key_phrase')) : null;
					if (($privateKey = @openssl_pkey_get_private($rsa_private_key, $phrase)) == false) {
						//TODO: double check if can input keyfile -inform PEM
						if (! $in = @tempnam(sys_get_temp_dir(), "RSA_PRIVATE_KEY_")) {
							throw new Exception("Could not create temporary file");
						}
						if (! @file_put_contents($in, base64_decode($rsa_private_key))) {
							throw new Exception("Could not write to temporary file");
						}
						if (! $out = @tempnam(sys_get_temp_dir(), "RSA_PRIVATE_KEY_")) {
							throw new Exception("Could not create temporary file");
						}
						exec("openssl pkcs8 -inform DER -outform PEM -out " . $out . " -nocrypt -in " . $in);
						if (! $rsa_private_key = @file_get_contents($out)) {
							throw new Exception("Could not read temporary file");
						}
					}
					$cache->set(md5("RSA_PRIVATE_KEY_" . $this->keyName), $rsa_private_key);
				}
			} catch (Exception $e) {
				throw new Exception("Error loading private key: " . $e);
			}
			$this->privateKey = $rsa_private_key;
		}
	}
}
