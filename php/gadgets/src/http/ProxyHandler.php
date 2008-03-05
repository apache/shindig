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

include_once ("src/{$config['gadget_token']}.php");
include_once ("src/{$config['gadget_signer']}.php");
include_once ("src/HttpProcessingOptions.php");

//TODO make sure this all works, especially the header passthru and post parts

// according to features/core/io.js, this is high on the list of things to scrap
define('UNPARSEABLE_CRUFT', "throw 1; < don't be evil' >");

class ProxyHandler {
	private $fetcher;
	
	public function __construct($fetcher)
	{
		$this->fetcher = $fetcher;
	}
	
	public function fetchJson($signer, $method)
	{
		$token = $this->extractAndValidateToken($signer);
		$url = $_GET['url'];
		$originalUrl = $this->validateUrl($url);
		$signedUrl = $this->signUrl($originalUrl, $token);
		// Fetch the content and convert it into JSON.
		// TODO: Fetcher needs to handle variety of HTTP methods.
		$result = $this->fetchContent($signedUrl, new HttpProcessingOptions(), $method);
		$status = (int)$result->getHttpCode();
		header("HTTP/1.1 $status", true);
		if ($status == 200) {
			$output = '';
			$json = array('body' => $result->getResponseContent(), 'rc' => $status, 'url' => $url);
			$json = json_encode($json);
			$output = UNPARSEABLE_CRUFT . $json;
			$this->setCachingHeaders();
			header("application/json; charset=utf-8");
			header("Content-Disposition", "attachment;filename=p.txt");
			echo $output;
		}
	}
	
	public function fetch($signer, $method)
	{
		$token = $this->extractAndValidateToken($signer);
		$originalUrl = $this->validateUrl($_GET['url']);
		$signedUrl = $this->signUrl($originalUrl, $token);
		//TODO: Fetcher needs to handle variety of HTTP methods.
		$result = $this->fetchContent($signedUrl, new HttpProcessingOptions(), $method);
		// TODO: Fetcher needs to handle variety of HTTP methods.
		$status = (int)$result->getHttpCode();
		header("HTTP/1.1 $status", true);
		if ($status == 200) {
			$headers = explode("\n", $result->getResponseHeaders());
			foreach ( $headers as $header ) {
				header($header);
			}
			$this->setCachingHeaders();
			// then echo the content
			echo $result->getResponseContent();
		}
	}
	
	private function fetchContent($signedUrl, $procOptions, $method)
	{
		//TODO get actual character encoding from the request

		// Extract the request headers from the $_SERVER super-global (this -does- unfortunatly mean that any header that php doesn't understand won't be proxied thru though)
		// if this turns out to be a problem we could add support for HTTP_RAW_HEADERS, but this depends on a php.ini setting, so i'd rather prevent that from being required
		$headers = '';
		foreach ( $_SERVER as $key => $val ) {
			if (substr($key, 0, strlen('HTTP_')) == 'HTTP_') {
				// massage the header key to something a bit more propper (example 'HTTP_ACCEPT_LANGUAGE' becomes 'Accept-Language')
				// TODO: We probably need to test variations as well.
				$key = str_replace(' ', '_', ucwords(strtolower(str_replace('-', ' ', substr($key, strlen('HTTP_'))))));
				if ($key != 'Keep_alive' && $key != 'Connection' && $key != 'Host' && $key != 'Accept' && $key != 'Accept-Encoding') {
					// propper curl header format according to http://www.php.net/manual/en/function.curl-setopt.php#80099
					$headers .= "$key: $val\n";
				}
			}
		}
		if ($method == 'POST') {
			$postData = '';
			$first = true;
			foreach ( $_POST as $key => $val ) {
				if (! $first) {
					$postData .= '&';
				} else {
					$first = false;
				}
				// make sure all the keys and val's are propperly encoded
				$postData .= urlencode(urldecode($key)) . '=' . urlencode(urldecode($val));
			}
			$request = new RemoteContentRequest($signedUrl, $headers, $postData);
			list($request) = $this->fetcher->fetch($request, $procOptions);
		} else {
			$request = new RemoteContentRequest($signedUrl, $headers);
			list($request) = $this->fetcher->fetch($request, $procOptions);
		}
		return $request;
	}
	
	private function setCachingHeaders()
	{
		// TODO: Re-implement caching behavior if appropriate.
		header("Cache-Control", "private; max-age=0");
		header("Expires", time() - 30);
	}
	
	private function validateUrl($url)
	{
		//TODO should really make a PHP version of the URI class and validate in all the locations the java version does
		return $url;
	}
	
	private function extractAndValidateToken($signer)
	{
		if ($signer == null) {
			return null;
		}
		$token = isset($_GET["st"]) ? $_GET["st"] : '';
		return $signer->createToken($token);
	}
	
	private function signUrl($originalUrl, $token)
	{
		if ($token == null || (isset($_GET['authz']) && $_GET['authz'] != 'signed')) {
			return $originalUrl;
		}
		$method = isset($_GET['httpMethod']) ? $_GET['httpMethod'] : 'GET';
		return $token->signUrl($originalUrl, $method);
	}

}