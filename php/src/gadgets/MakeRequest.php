<?php
/**
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

require_once 'src/gadgets/MakeRequestOptions.php';

/**
 * Common class for working with remote content requests.
 * Example - sending a request from a gadget context:
 * <code>
 *   $context = new GadgetContext('GADGET');
 *   $params = new MakeRequestOptions('http://www.example.com');
 *   $params->setAuthz('SIGNED')
 *          ->setNoCache(true)
 *          ->setSignViewer(false)
 *          ->setSecurityTokenString($_GET('st'));
 *   $result = $this->makeRequest->fetch($context, $params);
 *   $responseCode = $result->getHttpCode();
 *   $responseText = $result->getResponseContent();
 * </code>
 * More examples can be found in the
 * {@link /php/src/test/gadgets/MakeRequestTest.php} MakeRequest unit tests.
 */

class MakeRequest {
  /*
   * List of disallowed request headers taken from
   * /java/gadgets/src/main/java/org/apache/shindig/gadgets/servlet/HttpRequestHandler.java
   */
  static $BAD_REQUEST_HEADERS = array("HOST", "ACCEPT", "ACCEPT-ENCODING");
  
  /*
   * List of disallowed response headers taken from
   * /java/gadgets/src/main/java/org/apache/shindig/gadgets/servlet/ProxyBase.java
   */
  static $BAD_RESPONSE_HEADERS = array(
    "SET-COOKIE", "CONTENT-LENGTH", "CONTENT-ENCODING", "ETAG", "LAST-MODIFIED",
    "ACCEPT-RANGES", "VARY", "EXPIRES", "DATE", "PRAGMA", "CACHE-CONTROL",
    "TRANSFER-ENCODING", "WWW-AUTHENTICATE");
  
  private $remoteFetcher;

  /**
   * Constructor
   *
   * @param RemoteContentFetcher remoteFetcher A remote content fetcher intended
   *     to override the default fetcher which will be loaded from the config
   *     file.  This allows for injecting a mock into this class for testing.
   */
  public function __construct($remoteFetcher = null) {
    if (isset($remoteFetcher)) {
      $this->remoteFetcher = $remoteFetcher;
    } else {
      $remoteFetcherClass = Config::get('remote_content_fetcher');
      $this->remoteFetcher = new $remoteFetcherClass();
    }
  }

  /**
   * Returns the remote fetcher this instance uses for remote requests.
   *
   * @return RemoteContentFetcher
   */
  public function getRemoteFetcher() {
    return $this->remoteFetcher;
  }

  /**
   * Makes a request for remote data.
   *
   * @param GadgetContext $context Gadget context used to make the request.
   * @param MakeRequestOptions $params Parameter array used to configure the remote request.
   * @return RemoteContentRequest A request/response which has been sent to the target server.
   */
  public function fetch(GadgetContext $context, MakeRequestOptions $params) {

    $signingFetcherFactory = $gadgetSigner = null;
    if ($params->getAuthz() == "SIGNED" || $params->getAuthz() == "OAUTH") {
      $gadgetSigner = Config::get('security_token_signer');
      $gadgetSigner = new $gadgetSigner();
      $signingFetcherFactory = new SigningFetcherFactory(Config::get("private_key_file"));
    }
    $basicRemoteContent = new BasicRemoteContent($this->remoteFetcher, $signingFetcherFactory, $gadgetSigner);
    $request = $this->buildRequest($context, $params, $gadgetSigner);
    $request->getOptions()->ignoreCache = $params->getNoCache();
    $request->getOptions()->viewerSigned = $params->getSignViewer();
    $request->getOptions()->ownerSigned = $params->getSignOwner();
    $result = $basicRemoteContent->fetch($request);
    $status = (int)$result->getHttpCode();
    if ($status == 200) {
      switch ($params->getResponseFormat()) {
        case 'FEED':
          $content = $this->parseFeed($result, $params->getHref(), $params->getNumEntries(), $params->getGetSummaries());
          $result->setResponseContent($content);
          break;
      }
    }
    if (strpos($result->getResponseContent(), '\u')) {
    	$result->setResponseContent($this->decodeUtf8($result->getResponseContent()));
    }

    return $result;
  }

  /**
   * Returns a response header array with invalid headers removed.
   * @param array $headers An associative array of header/value pairs.
   * The reason this cleaning is not automatic is because some consumers of
   * MakeRequest may have need to access the stripped headers before
   * delivering the response to a client.  The reason for removing these headers
   * is also mostly for performance, rather than security.
   *
   * @return array An array with the headers defined in
   *     MakeRequest::$BAD_RESPONSE_HEADERS removed.  The removal is
   *     case-insensitive.
   */
  public function cleanResponseHeaders($headers) {
    return $this->stripInvalidArrayKeys($headers, MakeRequest::$BAD_RESPONSE_HEADERS);
  }

  /**
   * Builds a request to retrieve the actual content.
   *
   * @param GadgetContext $context The rendering context.
   * @param MakeRequestOptions $params Options for crafting the request.
   * @param SecurityTokenDecoder $signer A signer needed for signed requests.
   * @return RemoteContentRequest An initialized request object.
   */
  public function buildRequest(GadgetContext $context, MakeRequestOptions $params, SecurityTokenDecoder $signer = null) {
    // Check the protocol requested - curl doesn't really support file://
    // requests but the 'error' should be handled properly
    $protocolSplit = explode('://', $params->getHref(), 2);
    if (count($protocolSplit) < 2) {
      throw new Exception("Invalid protocol specified");
    }
    $protocol = strtoupper($protocolSplit[0]);
    if ($protocol != "HTTP" && $protocol != "HTTPS") {
      throw new Exception("Invalid protocol specified in url: " . htmlentities($protocol));
    }
    $method = $params->getHttpMethod();
    if ($method == 'POST' || $method == 'PUT') {
      // even if postData is an empty string, it will still post
      // (since RemoteContentRquest checks if its false)
      // so the request to POST is still honored
      $request = new RemoteContentRequest($params->getHref(), null, $params->getRequestBody());
    } else {
      $request = new RemoteContentRequest($params->getHref());
    }
    if ($signer) {
      switch ($params->getAuthz()) {
        case 'SIGNED':
          $request->setAuthType(RemoteContentRequest::$AUTH_SIGNED);
          break;
        case 'OAUTH':
          $request->setAuthType(RemoteContentRequest::$AUTH_OAUTH);
          $request->setOAuthRequestParams($params->getOAuthRequestParameters());
          break;
      }
      $st = $params->getSecurityTokenString();
      if ($st === false) {
        throw new Exception("A security token is required for signed requests");
      }
      $token = $context->validateToken($st, $signer);
      $request->setToken($token);
    }

    // Strip invalid request headers.  This limits the utility of the
    // MakeRequest class a little bit, but ensures that none of the invalid
    // headers are present in any request going through this class.
    $headers = $params->getRequestHeadersArray();
    if ($headers !== false) {
      $headers = $this->stripInvalidArrayKeys($headers, MakeRequest::$BAD_REQUEST_HEADERS);
      $params->setRequestHeaders($headers);
    }

    // The request expects headers to be stored as a normal header text blob.
    // ex: Content-Type: application/atom+xml
    //     Accept-Language: en-us
    $formattedHeaders = $params->getFormattedRequestHeaders();
    if ($formattedHeaders !== false) {
      $request->setHeaders($formattedHeaders);
    }
    
    return $request;
  }

  /**
   * Decodes UTF-8 numeric codes (&#xXXXX, or \uXXXX) from a content string.
   * @param string $content The content string to decode.
   * @return string A UTF-8 string where numeric codes have been converted into
   *     their UTF character representations.
   */
  public function decodeUtf8($content) {
    if (preg_match("/&#[xX][0-9a-zA-Z]{2,8};/", $content)) {
      $content = preg_replace("/&#[xX]([0-9a-zA-Z]{2,8});/e", "'&#'.hexdec('$1').';'", $content);
    }
    if (preg_match("/\\\\[uU][0-9a-zA-Z]{2,8}/", $content)) {
      $content = preg_replace("/\\\\[uU]([0-9a-zA-Z]{2,8})/e", "'&#'.hexdec('$1').';'", $content);
    }
    return mb_decode_numericentity($content, array(0x0, 0xFFFF, 0, 0xFFFF), 'UTF-8');
  }

  /**
   * Removes any invalid keys from an array in a case insensitive manner.
   * Used for stripping invalid http headers from a request or response.
   *
   * @param array $target The associative array to check for invalid keys.
   * @param array $invalidKeys An array of keys which are invalid.
   * @return array A copy of $target with any keys matching a value in
   *     $invalidKeys removed.
   */
  private function stripInvalidArrayKeys($target, $invalidKeys) {
    $cleaned = array();
    $upperInvalidKeys = array_map('strtoupper', $invalidKeys);

    foreach ($target as $key => $value) {
      $upperKey = strtoupper($key);
      if (in_array($upperKey, $upperInvalidKeys) === FALSE) {
        $cleaned[$key] = $value;
      }
    }

    return $cleaned;
  }
  
  /**
   * Handles (RSS & Atom) Type.FEED parsing using Zend's feed parser
   *
   * @return response string, either a json encoded feed structure or an error message
   */
  private function parseFeed($result, $url, $numEntries = 3, $getSummaries = false) {
    require 'external/Zend/Feed.php';
    $channel = array();
    if ((int)$result->getHttpCode() == 200) {
      $content = $result->getResponseContent();
      try {
        $feed = Zend_Feed::importString($content);
        if ($feed instanceof Zend_Feed_Rss) {
          // Try get author
          if ($feed->author()) {
            $author = $feed->author();
          } else {
            if ($feed->creator()) {
              $author = $feed->creator();
            } else {
              $author = null;
            }
          }
          // Loop over each channel item and store relevant data
          $counter = 0;
          $channel['Entry'] = array();
          foreach ($feed as $item) {
            if ($counter >= $numEntries) {
              break;
            }
            $_entry = array();
            $_entry['Title'] = $item->title();
            $_entry['Link'] = $item->link();
            if (!is_string($_entry['Link']) && isset($_entry['Link'][1]) && $_entry['Link'][1] instanceof DOMElement) {
            	$_entry['Link'] = $_entry['Link'][1]->getAttribute('href');
            }
            if ($getSummaries && $item->description()) {
              $_entry['Summary'] = $item->description();
            }
            $date = 0;
            if ($item->date()) {
              $date = strtotime($item->date());
            } else {
              if ($item->pubDate()) {
                $date = strtotime($item->pubDate());
              }
            }
            $_entry['Date'] = $date;
            $channel['Entry'][] = $_entry;
            // Remember author if first found
            if (empty($author) && $item->author()) {
              $author = $item->author();
            } else if ($item->creator()) {
              $author = $item->creator();
            }
            $counter ++;
          }
          $channel['Title'] = $feed->title();
          $channel['URL'] = $url;
          $channel['Description'] = $feed->description();
          if ($feed->link()) {
            if (is_array($feed->link())) {
              foreach ($feed->link() as $_link) {
                if ($_link->nodeValue) $channel['Link'] = $_link->nodeValue;
              }
            } else {
              $channel['Link'] = $feed->link();
            }
          }
          if ($author != null) {
            $channel['Author'] = $author;
          }
        } elseif ($feed instanceof Zend_Feed_Atom) {
          // Try get author
          if ($feed->author()) {
            if ($feed->author->name()) {
              $author = $feed->author->name();
            } else if ($feed->author->email()) {
              $author = $feed->author->email();
            } else {
              $author = $feed->author();
            }
          } else {
            $author = null;
          }
          // Loop over each entries and store relevant data
          $counter = 0;
          $channel['Entry'] = array();
          foreach ($feed as $entry) {
            if ($counter >= $numEntries) {
              break;
            }
            $_entry = array();
            $_entry['Title'] = $entry->title();
            // get Link if rel="alternate"
            if ($entry->link('alternate')) {
              $_entry['Link'] = $entry->link('alternate');
            } else {
              // if there's no alternate, pick the one without "rel" attribtue
              $_links = $entry->link;
              if (is_array($_links)) {
                foreach ($_links as $_link) {
                  if (empty($_link['rel'])) {
                    $_entry['Link'] = $_link['href'];
                    break;
                  }
                }
              } else {
                $_entry['Link'] = $_links['href'];
              }
            }
            if ($getSummaries && $entry->summary()) {
              $_entry['Summary'] = $entry->summary();
            }
            $date = 0;
            if ($entry->updated()) {
              $date = strtotime($entry->updated());
            } else {
              if ($entry->published()) {
                $date = strtotime($entry->published());
              }
            }
            $_entry['Date'] = $date;
            $channel['Entry'][] = $_entry;
            // Remember author if first found
            if (empty($author) && $entry->author()) {
              if ($entry->author->name()) {
                $author = $entry->author->name();
              } else if ($entry->author->email()) {
                $author = $entry->author->email();
              } else {
                $author = $entry->author();
              }
            } elseif (empty($author)) {
              $author = null;
            }
            $counter ++;
          }
          $channel['Title'] = $feed->title();
          $channel['URL'] = $url;
          $channel['Description'] = $feed->subtitle();
          // get Link if rel="alternate"
          if ($feed->link('alternate')) {
            $channel['Link'] = $feed->link('alternate');
          } else {
            // if there's no alternate, pick the one without "rel" attribtue
            $_links = $feed->link;
            if (is_array($_links)) {
              foreach ($_links as $_link) {
                if (empty($_link['rel'])) {
                  $channel['Link'] = $_link['href'];
                  break;
                }
              }
            } else {
              $channel['Link'] = $_links['href'];
            }
          }
          if (! empty($author)) {
            $channel['Author'] = $author;
          }
        } else {
          throw new Exception('Invalid feed type');
        }
        $resp = json_encode($channel);
      } catch (Zend_Feed_Exception $e) {
        $resp = 'Error parsing feed: ' . $e->getMessage();
      }
    } else {
      // feed import failed
      $resp = "Error fetching feed, response code: " . $result->getHttpCode();
    }
    return $resp;
  }
}
