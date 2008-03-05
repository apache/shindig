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
 * Basic remote content fetcher, uses curl_multi to fetch multiple resources at the same time
 */

class BasicRemoteContentFetcher extends remoteContentFetcher {
	private $requests = array();
	
	public function addRequests($requests)
	{
		foreach ( $request as $request ) {
			$this->addRequest($request);
		}
	}
	
	public function addRequest(remoteContentRequest $request)
	{
		$url = $request->getUrl();
		if (empty($url)) {
			throw new remoteContentException("Invalid or empty url specified in remoteContentFetcher");
		}
		$this->requests[] = $request;
	}
	
	public function fetchRequests()
	{
		$ret = array();
		$mh = curl_multi_init();
		// Setup each request based on its options
		foreach ( $this->requests as $request ) {
			$request->handle = curl_init();
			curl_setopt($request->handle, CURLOPT_URL, $request->getUrl());
			curl_setopt($request->handle, CURLOPT_FOLLOWLOCATION, 1);
			curl_setopt($request->handle, CURLOPT_RETURNTRANSFER, 1);
			curl_setopt($request->handle, CURLOPT_HEADER, 1);
			if ($request->hasHeaders()) {
				curl_setopt($request->handle, CURLOPT_HTTPHEADER, array($request->getHeaders()));
			}
			if ($request->isPost()) {
				curl_setopt($request->handle, CURLOPT_POST, 1);
				curl_setopt($request->handle, CURLOPT_POSTFIELDS, $request->getPostBody());
			}
			curl_multi_add_handle($mh, $request->handle);
		}
		// Execute the multi fetch
		$running = null;
		do {
			curl_multi_exec($mh, $running);
		} while ( $running > 0 );
		
		// Done, close handles
		foreach ( $this->requests as $key => $request ) {
			$content = curl_multi_getcontent($request->handle);
			$body = substr($content, strpos($content, "\r\n\r\n") + 4);
			$header = substr($content, 0, strpos($content, "\r\n\r\n"));
			$httpCode = curl_getinfo($request->handle, CURLINFO_HTTP_CODE);
			$contentType = curl_getinfo($request->handle, CURLINFO_CONTENT_TYPE);
			$request->setHttpCode($httpCode);
			$request->setContentType($contentType);
			$request->setResponseHeaders($header);
			$request->setResponseContent($body);
			$request->setResponseSize(strlen($content));
			curl_multi_remove_handle($mh, $request->handle);
			unset($request->handle);
		}
		curl_multi_close($mh);
		
		$ret = $this->requests;
		// empty our requests queue
		$this->requests = array();
		return $ret;
	}
	
	public function pendingRequests()
	{
		return count($this->requests);
	}
}
