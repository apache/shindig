<?php
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
class HttpServlet {
	private $lastModified = false;
	private $contentType = 'text/html';
	private $charset = 'UTF-8';
	public $noHeaders = false;
	private $noCache = false;
	
	/**
	 * Enables output buffering so we can do correct header handling in the destructor
	 *
	 */
	/**
	 * Enables output buffering so we can do correct header handling in the destructor
	 *
	 */
	public function __construct()
	{
		// to do our header magic, we need output buffering on
		ob_start();
	}
	
	/**
	 * Enter description here...
	 * If noHeaders is false, it adds all the correct http/1.1 headers to the request
	 * and deals with modified/expires/e-tags/etc. This makes the server behave more like
	 * a real http server.
	 */
	/**
	 * Enter description here...
	 * If noHeaders is false, it adds all the correct http/1.1 headers to the request
	 * and deals with modified/expires/e-tags/etc. This makes the server behave more like
	 * a real http server.
	 */
	public function __destruct()
	{
		if (! $this->noHeaders) {
			header("Content-Type: $this->contentType; charset={$this->charset}");
			header('Connection: Keep-Alive');
			header('Keep-Alive: timeout=15, max=30');
			header('Accept-Ranges: bytes');
			header('Content-Length: ' . ob_get_length());
			$content = ob_get_clean();
			if ($this->noCache) {
				header("Cache-Control: no-cache, must-revalidate");
				header("Expires: Mon, 26 Jul 1997 05:00:00 GMT");
			} else {
				// attempt at some propper header handling from php
				// this departs a little from the shindig code but it should give is valid http protocol handling
				header('Cache-Control: public,max-age=' . Config::get('cache_time') . ',must-revalidate');
				header("Expires: " . gmdate("D, d M Y H:i:s", time() + Config::get('cache_time')) . " GMT");
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
			}
			echo $content;
		}
	}
	
	/**
	 * Sets the content type of this request (forinstance: text/html or text/javascript, etc) 
	 *
	 * @param string $type content type header to use
	 */
	/**
	 * Sets the content type of this request (forinstance: text/html or text/javascript, etc) 
	 *
	 * @param string $type content type header to use
	 */
	public function setContentType($type)
	{
		$this->contentType = $type;
	}
	
	/**
	 * Returns the current content type 
	 *
	 * @return string content type string
	 */
	/**
	 * Returns the current content type 
	 *
	 * @return string content type string
	 */
	public function getContentType()
	{
		return $this->contentType;
	}
	
	/**
	 * returns the current last modified time stamp
	 *
	 * @return int timestamp
	 */
	/**
	 * returns the current last modified time stamp
	 *
	 * @return int timestamp
	 */
	public function getLastModified()
	{
		return $this->lastModified;
	}
	
	/**
	 * Sets the last modified timestamp. It automaticly checks if this timestamp
	 * is larger then its current timestamp, and if not ignores the call
	 *
	 * @param int $modified timestamp
	 */
	/**
	 * Sets the last modified timestamp. It automaticly checks if this timestamp
	 * is larger then its current timestamp, and if not ignores the call
	 *
	 * @param int $modified timestamp
	 */
	public function setLastModified($modified)
	{
		$this->lastModified = max($this->lastModified, $modified);
	}
	
	/**
	 * Sets the noCache boolean. If its set to true, no-caching headers will be send
	 * (pragma no cache, expiration in the past)
	 *
	 * @param boolean $cache send no-cache headers?
	 */
	/**
	 * Sets the noCache boolean. If its set to true, no-caching headers will be send
	 * (pragma no cache, expiration in the past)
	 *
	 * @param boolean $cache send no-cache headers?
	 */
	public function setNoCache($cache = false)
	{
		$this->noCache = $cache;
	}
	
	/**
	 * returns the noCache boolean
	 *
	 * @return boolean
	 */
	/**
	 * returns the noCache boolean
	 *
	 * @return boolean
	 */
	public function getNoCache()
	{
		return $this->noCache;
	}
}
