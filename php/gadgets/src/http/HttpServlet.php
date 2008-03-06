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
 * This is a somewhat liberal interpretation of the HttpServlet class
 * Mixed with some essentials to make propper http header handling
 * happen in php.
 */

class ServletException extends Exception {
}

class HttpServlet {
	private $lastModified = false;
	private $contentType = 'text/html';
	private $charset = 'UTF-8';
	public  $noHeaders = false;
	
	public function __construct()
	{
		// to do our header magic, we need output buffering on
		ob_start();
	}
	
	public function __destruct()
	{
		global $config;
		if (!$this->noHeaders) {
			// attempt at some propper header handling from php
			// this departs a little from the shindig code but it should give is valid http protocol handling
			header("Content-Type: $this->contentType; charset={$this->charset}");
			header('Connection: Keep-Alive');
			header('Keep-Alive: timeout=15, max=30');
			header('Accept-Ranges: bytes');
			header('Content-Length: ' . ob_get_length());
			header('Cache-Control: public,max-age=' . $config['cache_time'] . ',must-revalidate');
			header("Expires: " . gmdate("D, d M Y H:i:s", time() + $config['cache_time']) . " GMT");
			$content = ob_get_clean();
			// Obey browsers (or proxy's) request to send a fresh copy if we recieve a no-cache pragma or cache-control request
			if (! isset($_SERVER['HTTP_PRAGMA']) || ! strstr(strtolower($_SERVER['HTTP_PRAGMA']), 'no-cache') && (! isset($_SERVER['HTTP_CACHE_CONTROL']) || ! strstr(strtolower($_SERVER['HTTP_CACHE_CONTROL']), 'no-cache'))) {
				// If the browser send us a E-TAG check if it matches (sha1 sum of content), if so send a not modified header instead of content
				$etag = sha1($content);
				if (isset($_SERVER['HTTP_IF_NONE_MATCH']) && $_SERVER['HTTP_IF_NONE_MATCH'] == $etag) {
					header("ETag: \"$etag\"");
					if ($this->lastModified) {
						header('Last-Modified: ' . gmdate('D, d M Y H:i:s', $this->lastModified));
					}
					header("HTTP/1.1 304 Not Modified");
					header('Content-Length: 0');
					die();
				}
				header("ETag: \"$etag\"");
				// If no etag is present, then check if maybe this browser supports if_modified_since tags,
				// check it against our lastModified (if it's set)
				if (isset($_SERVER['HTTP_IF_MODIFIED_SINCE']) && $this->lastModified && ! isset($_SERVER['HTTP_IF_NONE_MATCH'])) {
					$if_modified_since = strtotime($_SERVER['HTTP_IF_MODIFIED_SINCE']);
					if ($this->lastModified <= $if_modified_since) {
						header('Last-Modified: ' . gmdate('D, d M Y H:i:s', $this->lastModified));
						header("HTTP/1.1 304 Not Modified");
						header('Content-Length: 0');
						die();
					}
				}
				if ($this->lastModified) {
					header('Last-Modified: ' . gmdate('D, d M Y H:i:s', $this->lastModified));
				}
			}
			echo $content;
		}
	}
	
	public function setContentType($type)
	{
		$this->contentType = $type;
	}
	
	public function getContentType()
	{
		return $this->contentType;
	}
	
	public function getLastModified()
	{
		return $this->lastModified;
	}
	
	public function setLastModified($modified)
	{
		$this->lastModified = max($this->lastModified, $modified);
	}
	
	public function error($msg)
	{
		@ob_end_clean();
		header("HTTP/1.0 400 Bad Request", true);
		echo "<html><body><h1>400 - $msg</h1></body></html>";
	}
}