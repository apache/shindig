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
  private $signingFetcher;
  private $oauthFetcher;

  public function __construct($context, $signingFetcher = null, $oauthFetcher = null) {
    $this->context = $context;
    $this->signingFetcher = $signingFetcher;
    $this->oauthFetcher = $oauthFetcher;
  }

  /**
   * Fetches content and returns it in JSON format
   *
   * @param string $url the url to fetch
   * @param GadgetSigner $signer the request signer to use
   * @param string $method the http method to use (get or post) in making the request
   */
  public function fetchJson($url, $signer, $method) {
    try {
      $token = $this->context->extractAndValidateToken($signer);
    } catch (Exception $e) {
      $token = '';
      // no token given, safe to ignore
    }
    $url = $this->validateUrl($url);
    // Fetch the content and convert it into JSON.
    // TODO: Fetcher needs to handle variety of HTTP methods.
    $result = $this->fetchContentDivert($url, $method, $signer);
    if (! isset($result)) {
      //OAuthFetcher only
      $metadata = $this->oauthFetcher->getResponseMetadata();
      $json = array($url => $metadata);
      $json = json_encode($json);
      $output = UNPARSEABLE_CRUFT . $json;
      $this->setCachingHeaders();
      header("Content-Type: application/json; charset=utf-8", true);
      echo $output;
      die();
    }
    $status = (int)$result->getHttpCode();
    //header("HTTP/1.1 $status", true);
    if ($status == 200) {
      $output = '';
      if (isset($_REQUEST['contentType']) && $_REQUEST['contentType'] == 'FEED') {
        require 'external/Zend/Feed.php';
        $numEntries = $_REQUEST['numEntries'];
        $getSummaries = ! empty($_REQUEST['getSummaries']) && $_REQUEST['getSummaries'] != 'false' ? true : false;
        $channel = array();
        $request = new RemoteContentRequest($url);
        $request = $this->context->getHttpFetcher()->fetch($request, $this->context);
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
      } else {
        $resp = $result->getResponseContent();
      }
      $json = array($url => array('body' => $resp, 'rc' => $status));
      $json = json_encode($json);
      $output = UNPARSEABLE_CRUFT . $json;
      $this->setCachingHeaders();
      //	header("Content-Type: application/json; charset=utf-8", true);
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
  public function fetch($url, $signer, $method) {
    $url = $this->validateUrl($url);
    //TODO: Fetcher needs to handle variety of HTTP methods.
    $result = $this->fetchContent($url, $method);
    // TODO: Fetcher needs to handle variety of HTTP methods.
    $status = (int)$result->getHttpCode();
    if ($status == 200) {
      $headers = explode("\n", $result->getResponseHeaders());
      $isShockwaveFlash = false;
      foreach ($headers as $header) {
        if (strpos($header, ':')) {
          $key = trim(substr($header, 0, strpos($header, ':')));
          $val = trim(substr($header, strpos($header, ':') + 1));
          // filter out headers that would otherwise mess up our output
          if (strcasecmp($key, "Transfer-Encoding") != 0 && strcasecmp($key, "Cache-Control") != 0 && strcasecmp($key, "Expires") != 0 && strcasecmp($key, "Content-Length") != 0 && strcasecmp($key, "ETag") != 0) {
            header("$key: $val");
          }
          if ($key == 'Content-Type' && $val == 'application/x-shockwave-flash') {
            // We're skipping the content disposition header for flash due to an issue with Flash player 10
            // This does make some sites a higher value phishing target, but this can be mitigated by
            // additional referer checks.
            $isShockwaveFlash = true;
          }
        }
      }
      if (!$isShockwaveFlash) {
        header('Content-Disposition: attachment;filename=p.txt');
      }
      $etag = md5($result->getResponseContent());
      $lastModified = $result->getResponseHeader('Last-Modified') != null ? $result->getResponseHeader('Last-Modified') : gmdate('D, d M Y H:i:s', $result->getCreated()) . ' GMT';
      $notModified = false;
      // If HTTP_PRAGMA | HTTP_CACHE_CONTROL == no-cache, the browser wants to do a 'forced reload'
      if (! isset($_SERVER['HTTP_PRAGMA']) || ! strstr(strtolower($_SERVER['HTTP_PRAGMA']), 'no-cache') && (! isset($_SERVER['HTTP_CACHE_CONTROL']) || ! strstr(strtolower($_SERVER['HTTP_CACHE_CONTROL']), 'no-cache'))) {
        if (isset($_SERVER['HTTP_IF_NONE_MATCH']) && $_SERVER['HTTP_IF_NONE_MATCH'] == $etag) {
          // if e-tag's match, set not modified, and no need to check the if-modified-since headers
          $notModified = true;
        } elseif (isset($_SERVER['HTTP_IF_MODIFIED_SINCE']) && $lastModified && ! isset($_SERVER['HTTP_IF_NONE_MATCH'])) {
          $if_modified_since = strtotime($_SERVER['HTTP_IF_MODIFIED_SINCE']);
          // Use the request's Last-Modified, otherwise fall back on our internal time keeping (the time the request was created)
          $lastModified = strtotime($lastModified);
          if ($lastModified <= $if_modified_since) {
            $notModified = true;
          }
        }
      }
      $this->setCachingHeaders($etag, $this->context->getRefreshInterval(), $lastModified);
      // If the cached file time is within the refreshInterval params value and the ETag match, return not-modified
      if ($notModified) {
        header('HTTP/1.0 304 Not Modified', true);
        header('Content-Length: 0', true);
      } else {
        // then echo the content
        echo $result->getResponseContent();
      }
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
   * @param string $url the url to fetch
   * @param string $method either get or post
   * @return the filled in request (RemoteContentRequest)
   */
  private function fetchContent($url, $method) {
    //TODO get actual character encoding from the request

    // Check the protocol requested - curl doesn't really support file://
    // requests but the 'error' should be handled properly
    $protocolSplit = explode('://', $url, 2);
    if (count($protocolSplit) < 2) {
      throw new Exception("Invalid protocol specified");
    } else {
      $protocol = strtoupper($protocolSplit[0]);
      if ($protocol != "HTTP" && $protocol != "HTTPS" && $protocol != "FTP") {
        throw new Exception("Invalid protocol specified in url ($protocol)");
      }
    }

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
        $entries = explode('&', $data);
        foreach ($entries as $entry) {
          $entry = urldecode($entry);
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
      $request = new RemoteContentRequest($url, $headers, $postData);
      $request = $this->context->getHttpFetcher()->fetch($request, $context);
    } else {
      $request = new RemoteContentRequest($url, $headers);
      $request = $this->context->getHttpFetcher()->fetch($request, $context);
    }
    return $request;
  }

  private function fetchContentDivert($url, $method, $signer) {
    $authz = isset($_GET['authz']) ? $_GET['authz'] : (isset($_POST['authz']) ? $_POST['authz'] : '');
    $token = $this->context->extractAndValidateToken($signer);
    switch (strtoupper($authz)) {
      case 'SIGNED':
        $fetcher = $this->signingFetcher->getSigningFetcher(new BasicRemoteContentFetcher(), $token);
        return $fetcher->fetch($url, $method);
      case 'OAUTH':
        $params = new OAuthRequestParams();
        $fetcher = $this->signingFetcher->getSigningFetcher(new BasicRemoteContentFetcher(), $token);
        $oAuthFetcherFactory = new OAuthFetcherFactory($fetcher);
        $this->oauthFetcher = $oAuthFetcherFactory->getOAuthFetcher($fetcher, $token, $params);
        $request = new RemoteContentRequest($url);
        $request->createRemoteContentRequestWithUri($url);
        return $this->oauthFetcher->fetch($request);
      case 'NONE':
      default:
        return $this->fetchContent($url, $method);
    }
  }

  public function setContentFetcher($contentFetcherFactory) {
    $this->contentFetcherFactory = $contentFetcherFactory;
  }

  /**
   * Sets the caching headers (overwriting anything the remote host set) to force
   * the browser not to cache this.
   *
   */
  private function setCachingHeaders($etag = false, $maxAge = false, $lastModified = false) {
    if ($etag) {
      header("ETag: $etag");
    }
    if ($lastModified) {
      header("Last-Modified: $lastModified");
    }
    $expires = $maxAge !== false ? time() + $maxAge : time() - 3000;
    $public = $maxAge ? 'public' : 'private';
    $maxAge = $maxAge === false ? '0' : $maxAge;
    header("Cache-Control: {$public}; max-age={$maxAge}", true);
    header("Expires: " . gmdate("D, d M Y H:i:s", $expires) . " GMT", true);
  }

  /**
   * Empty function, should make something practical here some day.
   * it's function should be to validate the given url if its in
   * correct http(s):port://location/url format
   *
   * @param string $url
   * @return string the 'validated' url
   */
  private function validateUrl($url) {
    if (! @parse_url($url)) {
      throw new Exception("Invalid Url");
    } else {
      return $url;
    }
  }

  private function request_headers() {
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
