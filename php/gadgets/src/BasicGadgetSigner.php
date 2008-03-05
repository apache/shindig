<?
/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 * 
 */

/*
 * A GadgetSigner implementation that just provides dummy data to satisfy
 * tests and API calls. Do not use this for any security applications.
 */


class BasicGadgetSigner extends GadgetSigner {
	private $timeToLive;
	
	/**
	 * Creates a signer with 24 hour token expiry
	 */
	public function __construct($timeToLive = false)
	{
		$this->timeToLive = $timeToLive ? $timeToLive : 24 * 60 * 60 * 1000;
	}
	
	/**
	 * {@inheritDoc}
	 * This implementation only validates non-empty tokens. Empty tokens
	 * are considered to always be valid.
	 */
	public function createToken($stringToken)
	{
		if ($stringToken != null && ! empty($stringToken)) {
			$parts = explode('$', $stringToken);
			if (count($parts) != 2) {
				throw new GadgetException("Invalid token format.");
			}
			$expiry = intval($parts[1]);
			$currentTimeMillis = time() * 1000;
			if ($expiry < $currentTimeMillis) {
				throw new GadgetException("Expired token.");
			}
		}
		return new BasicGadgetToken($stringToken);
	}
}