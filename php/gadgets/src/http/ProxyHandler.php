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

// according to features/core/io.js, this is high on the list of things to scrap
define('UNPARSEABLE_CRUFT', "throw 1; < don't be evil' >");

/**
 * The ProxyHandler class does the actual proxy'ing work. it deals both with
 * GET and POST based input, and peforms a request based on the input, headers and 
 * httpmethod params. It also deals with request signing and verification thru the
 * authz and st (security token) params. 
 *
 */
class ProxyHandler {
	private $context;

	public function __construct($context)
	{
		$this->context = $context;
	}

	/**
	 * Fetches content and returns it in JSON format
	 *
	 * @param string $url the url to fetch
	 * @param GadgetSigner $signer the request signer to use
	 * @param string $method the http method to use (get or post) in making the request
	 */
	public function fetchJson($url, $signer, $method)
	{
		$token = $this->extractAndValidateToken($signer);
		$originalUrl = $this->validateUrl($url);
		$signedUrl = $this->signUrl($originalUrl, $token);
		// Fetch the content and convert it into JSON.
		// TODO: Fetcher needs to handle variety of HTTP methods.
		$result = $this->fetchContent($signedUrl, $method);
		$status = (int) $result->getHttpCode();
		//header("HTTP/1.1 $status", true);
		if ($status == 200) {
			$output = '';
			$json = array($url => array('body' => $result->getResponseContent(), 'rc' => $status));
			$json = json_encode($json);
			$output = UNPARSEABLE_CRUFT . $json;
			$this->setCachingHeaders();
			header("Content-Type: application/json; charset=utf-8", true);
			echo $output;
		} else {
			@ob_end_clean();
			header("HTTP/1.0 404 Not Found", true);
			echo "<html><body><h1>404 - Not Found</h1></body></html>";
		}
		die();
	}

	/**
	 * Fetches the content and returns it as-is using the headers as returned
	 * by the remote host.
	 *
	 * @param string $url the url to retrieve
	 * @param GadgetSigner $signer the GadgetSigner to use
	 * @param string $method either get or post
	 */
	public function fetch($url, $signer, $method)
	{
		$token = $this->extractAndValidateToken($signer);
		$originalUrl = $this->validateUrl($url);
		$signedUrl = $this->signUrl($originalUrl, $token);
		//TODO: Fetcher needs to handle variety of HTTP methods.
		$result = $this->fetchContent($signedUrl, $method);
		// TODO: Fetcher needs to handle variety of HTTP methods.
		$status = (int) $result->getHttpCode();
		if ($status == 200) {
			$headers = explode("\n", $result->getResponseHeaders());
			foreach ($headers as $header) {
				if (strpos($header, ':')) {
					$key = trim(substr($header, 0, strpos($header, ':')));
					$val = trim(substr($header, strpos($header, ':') + 1));
					// filter out headers that would otherwise mess up our output
					if (strcasecmp($key, "Transfer-Encoding") != 0 && strcasecmp($key, "Cache-Control") != 0 && strcasecmp($key, "Expires") != 0 && strcasecmp($key, "Content-Length") != 0) {
						header("$key: $val");
					}
				}
			}
			$this->setCachingHeaders();
			// then echo the content
			echo $result->getResponseContent();
		} else {
			@ob_end_clean();
			header("HTTP/1.0 404 Not Found", true);
			echo "<html><body><h1>404 - Not Found ($status)</h1>";
			echo "</body></html>";
		}
		// make sure the HttpServlet destructor doesn't override ours
		die();
	}

	/**
	 * Both fetch and fetchJson call this function to retrieve the actual content
	 *
	 * @param string $signedUrl the signed url to fetch
	 * @param string $method either get or post
	 * @return the filled in request (RemoteContentRequest)
	 */
	private function fetchContent($signedUrl, $method)
	{
		//TODO get actual character encoding from the request

		// Extract the request headers from the $_SERVER super-global (this -does- unfortunatly mean that any header that php doesn't understand won't be proxied thru though)
		// if this turns out to be a problem we could add support for HTTP_RAW_HEADERS, but this depends on a php.ini setting, so i'd rather prevent that from being required
		$headers = '';
		$context = new GadgetContext('GADGET');
		$requestHeaders = $this->request_headers();
		foreach ($requestHeaders as $key => $val) {
			if ($key != 'Keep-alive' && $key != 'Connection' && $key != 'Host' && $key != 'Accept' && $key != 'Accept-Encoding') {
				// propper curl header format according to http://www.php.net/manual/en/function.curl-setopt.php#80099
				$headers .= "$key: $val\n";
			}
		}
		if ($method == 'POST') {
			$data = isset($_GET['postData']) ? $_GET['postData'] : false;
			if (! $data) {
				$data = isset($_POST['postData']) ? $_POST['postData'] : false;
			}
			$postData = '';
			if ($data) {
				$data = urldecode($data);
				$entries = explode('&', $data);
				foreach ($entries as $entry) {
					$parts = explode('=', $entry);
					// Process only if its a valid value=something pair
					if (count($parts) == 2) {
						$postData .= urlencode($parts[0]) . '=' . urlencode($parts[1]) . '&';
					}
				}
				// chop of the trailing &
				if (strlen($postData)) {
					$postData = substr($postData, 0, strlen($postData) - 1);
				}
			}
			// even if postData is an empty string, it will still post (since RemoteContentRquest checks if its false)
			// so the request to POST is still honored
			$request = new RemoteContentRequest($signedUrl, $headers, $postData);
			$request = $this->context->getHttpFetcher()->fetch($request, $context);
		} else {
			$request = new RemoteContentRequest($signedUrl, $headers);
			$request = $this->context->getHttpFetcher()->fetch($request, $context);
		}
		return $request;
	}

	/**
	 * Sets the caching headers (overwriting anything the remote host set) to force
	 * the browser not to cache this. 
	 *
	 */
	private function setCachingHeaders()
	{
		// TODO: Re-implement caching behavior if appropriate.
		header("Cache-Control: private; max-age=0", true);
		header("Expires: " . gmdate("D, d M Y H:i:s", time() - 3000) . " GMT", true);
	}

	/**
	 * Empty function, should make something practical here some day.
	 * it's function should be to validate the given url if its in
	 * correct http(s):port://location/url format
	 *
	 * @param string $url
	 * @return string the 'validated' url
	 */
	private function validateUrl($url)
	{
		//TODO should really make a PHP version of the URI class and validate in all the locations the java version does
		return $url;
	}

	/**
	 * Extracts the 'st' token from the GET or POST params and calls the
	 * signer to validate the token
	 *
	 * @param GadgetSigner $signer the signer to use (configured in config.php)
	 * @return string the token to use in the signed url
	 */
	private function extractAndValidateToken($signer)
	{
		if ($signer == null) {
			return null;
		}
		$token = isset($_GET["st"]) ? $_GET["st"] : false;
		if ($token) {
			$token = isset($_POST['st']) ? $_POST['st'] : '';
		}
		return $signer->createToken($token);
	}

	/**
	 * Signs a url with the GadgetToken
	 *
	 * @param string $originalUrl
	 * @param GadgetToken $token
	 * @return unknown
	 */
	private function signUrl($originalUrl, $token)
	{
		$authz = isset($_GET['authz']) ? $_GET['authz'] : false;
		if (! $authz) {
			$authz = isset($_POST['authz']) ? $_POST['authz'] : '';
		}
		if ($token == null || $authz != 'signed') {
			return $originalUrl;
		}
		$method = isset($_GET['httpMethod']) ? $_GET['httpMethod'] : false;
		if ($method) {
			$method = isset($_POST['httpMethod']) ? $_POST['httpMethod'] : 'GET';
		}
		return $token->signUrl($originalUrl, $method);
	}

	private function request_headers()
	{
		// Try to use apache's request headers if available
		if (function_exists("apache_request_headers")) {
			if (($headers = apache_request_headers())) {
				return $headers;
			}
		}
		// if that failed, try to create them from the _SERVER superglobal
		$headers = array();
		foreach (array_keys($_SERVER) as $skey) {
			if (substr($skey, 0, 5) == "HTTP_") {
				$headername = str_replace(" ", "-", ucwords(strtolower(str_replace("_", " ", substr($skey, 0, 5)))));
				$headers[$headername] = $_SERVER[$skey];
			}
		}
		return $headers;
	}
}
