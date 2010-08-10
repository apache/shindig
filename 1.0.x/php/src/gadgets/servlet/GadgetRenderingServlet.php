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

require 'src/common/HttpServlet.php';
require 'src/gadgets/GadgetContext.php';
require 'src/gadgets/GadgetServer.php';
require 'src/common/RemoteContentRequest.php';
require 'src/common/RemoteContent.php';
require 'src/common/Cache.php';
require 'src/common/RemoteContentFetcher.php';
require 'src/gadgets/GadgetSpecParser.php';
require 'src/gadgets/Gadget.php';
require 'src/gadgets/GadgetId.php';
require 'src/gadgets/UserPrefs.php';
require 'src/gadgets/Substitutions.php';
require 'src/gadgets/LocaleSpec.php';
require 'src/gadgets/LocaleMessageBundle.php';
require 'src/gadgets/GadgetBlacklist.php';
require 'src/common/Locale.php';
require 'src/gadgets/UserPref.php';
require 'src/gadgets/ViewSpec.php';
require 'src/gadgets/FeatureSpec.php';
require 'src/gadgets/MessageBundleParser.php';
require 'src/gadgets/MessageBundle.php';
require 'src/gadgets/GadgetFeatureRegistry.php';
require 'src/gadgets/GadgetFeatureFactory.php';
require 'src/gadgets/GadgetFeature.php';
require 'src/gadgets/JsLibraryFeatureFactory.php';
require 'src/gadgets/JsLibrary.php';
require 'src/gadgets/HttpUtil.php';
require 'src/gadgets/ContainerConfig.php';
require 'src/common/JsMin.php';
require 'src/common/SecurityTokenDecoder.php';
require 'src/common/SecurityToken.php';
require 'src/common/BlobCrypter.php';
require 'src/gadgets/rewrite/ContentRewriter.php';
require 'src/gadgets/rewrite/ContentRewriteFeature.php';

/**
 * This class deals with the gadget rendering requests (in default config this
 * would be /gadgets/ifr?url=<some gadget's url>). It uses the gadget server and
 * gadget context to render the xml to a valid html file, and outputs it.
 *
 */
class GadgetRenderingServlet extends HttpServlet {
  private $context;

  /**
   * Creates the gadget using the GadgetServer class and calls outputGadget
   *
   */
  public function doGet() {
    try {
      if (empty($_GET['url'])) {
        throw new GadgetException("Missing required parameter: url");
      }
      // GadgetContext builds up all the contextual variables (based on the url or post)
      // plus instances all required classes (feature registry, fetcher, blacklist, etc)
      $this->context = new GadgetContext('GADGET');
      // Unfortunatly we can't do caja content filtering here, hoping we'll have a RPC service
      // or command line caja to use for this at some point
      $gadgetServer = new GadgetServer();
      $gadget = $gadgetServer->processGadget($this->context);
      $this->outputGadget($gadget, $this->context);
    } catch (Exception $e) {
      $this->outputError($e);
    }
  }

  /**
   * If an error occured (Exception) this function echo's the Exception's message
   * and if the config['debug'] is true, shows the debug backtrace in a div
   *
   * @param Exception $e the exception to show
   */
  private function outputError($e) {
    header("HTTP/1.0 400 Bad Request", true, 400);
    echo "<html><body>";
    echo "<h1>Error</h1>";
    echo $e->getMessage();
    if (Config::get('debug')) {
      echo "<p><b>Debug backtrace</b></p><div style='overflow:auto; height:300px; border:1px solid #000000'><pre>";
      print_r(debug_backtrace());
      echo "</pre></div>>";
    }
    echo "</body></html>";
  }

  /**
   * Takes the gadget to output, and depending on its content type calls either outputHtml-
   * or outputUrlGadget
   *
   * @param Gadget $gadget gadget to render
   * @param string $view the view to render (only valid with a html content type)
   */
  private function outputGadget($gadget, $context) {
    $view = HttpUtil::getView($gadget, $context);
    switch ($view->getType()) {
      case 'HTML':
        $this->outputHtmlGadget($gadget, $context, $view);
        break;
      case 'URL':
        $this->outputUrlGadget($gadget, $context, $view);
        break;
    }
  }

  /**
   * Outputs a html content type gadget.
   * It creates a html page, with the javascripts from the features inline into the page, plus
   * calls to 'gadgets.config.init' with the container configuration (config/container.js) and
   * 'gadgets.Prefs.setMessages_' with all the substitutions. For external javascripts it adds
   * a <script> tag.
   *
   * @param Gadget $gadget
   * @param GadgetContext $context
   */
  private function outputHtmlGadget($gadget, $context, $view) {
    $content = '';
    $externJs = '';
    $externFmt = "<script src=\"%s\"></script>";
    $forcedLibs = $context->getForcedJsLibs();
    // allow the &libs=.. param to override our forced js libs configuration value
    if (empty($forcedLibs)) {
      $forcedLibs = Config::get('focedJsLibs');
    }
    $this->setContentType("text/html; charset=UTF-8");
    if ($context->getIgnoreCache()) {
      // no cache was requested, set non-caching-headers
      $this->setNoCache(true);
    } elseif (isset($_GET['v'])) {
      // version was given, cache for a long long time (a year)
      $this->setCacheTime(365 * 24 * 60 * 60);
    } else {
      // no version was given, cache for 5 minutes
      $this->setCacheTime(5 * 60);
    }
    // Was a privacy policy header configured? if so set it
    if (Config::get('P3P') != '') {
      header("P3P: " . Config::get('P3P'));
    }
    if (! $view->getQuirks()) {
      $content .= "<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01//EN\" \"http://www.w3.org/TR/html4/strict.dtd\">\n";
    }
    $content .= "<html><head><meta http-equiv=\"Content-Type\" content=\"text/html; charset=utf-8\"/><style type=\"text/css\">" . Config::get('gadget_css') . "</style></head><body>\n";
    // Forced libs first.
    if (! empty($forcedLibs)) {
      $libs = explode(':', $forcedLibs);
      $content .= sprintf($externFmt, Config::get('default_js_prefix') . $this->getJsUrl($libs, $gadget) . "&container=" . $context->getContainer()) . "\n";
    }
    $content .= "<script>\n";

    if (! empty($forcedLibs)) {
      // if some of the feature libraries are externalized (through a browser cachable <script src="/gadgets/js/opensocial-0.7:settitle.js">
      // type url), then we don't want to include dependencies twice, so find the complete features chain, so we can skip over those
      $forcedLibsArray = explode(':', $forcedLibs);
      $registry = $this->context->getRegistry();
      $missing = array();
      $registry->getIncludedFeatures($forcedLibsArray, $forcedLibsArray, $missing);
    }
    foreach ($gadget->getJsLibraries() as $library) {
      $type = $library->getType();
      if ($type == 'URL') {
        // TODO: This case needs to be handled more gracefully by the js
        // servlet. We should probably inline external JS as well.
        $externJs .= sprintf($externFmt, $library->getContent()) . "\n";
        // else check if there are no forcedLibs, or if it wasn't included in their dep chain
      } elseif (empty($forcedLibs) || ! in_array($library->getFeatureName(), $forcedLibsArray)) {
        $content .= $library->getContent();
      }
      // otherwise it was already included by config.forceJsLibs.
    }
    $content .= $this->appendJsConfig($context, $gadget, ! empty($forcedLibs)) . $this->appendMessages($gadget) . $this->appendPreferences($gadget) . $this->appendPreloads($gadget, $context) . "</script>";
    if (strlen($externJs) > 0) {
      $content .= $externJs;
    }
    $gadgetExceptions = array();
    $rewriter = new ContentRewriter();
    if ($rewriter->rewriteGadgetView($gadget, $view)) {
      $content .= $gadget->getSubstitutions()->substitute($view->getRewrittenContent());
    } else {
      $content .= $gadget->getSubstitutions()->substitute($view->getContent());
    }
    if (empty($content)) {
      // Unknown view
      $gadgetExceptions[] = "View: '" . $context->getView() . "' invalid for gadget: " . $gadget->getId()->getKey();
    }
    if (count($gadgetExceptions)) {
      throw new GadgetException(print_r($gadgetExceptions, true));
    }
    $content .= "\n<script>gadgets.util.runOnLoadHandlers();</script></body>\n</html>";
    echo $content;
  }

  /**
   * Output's a URL content type gadget, it adds libs=<list:of:js:libraries>.js and user preferences
   * to the href url, and redirects the browser to it
   *
   * @param Gadget $gadget
   */
  private function outputUrlGadget($gadget, $context, $view) {
    // Preserve existing query string parameters.
    $redirURI = $view->getHref();
    $queryStr = strpos($redirURI, '?') !== false ? substr($redirURI, strpos($redirURI, '?')) : '';
    $query = $queryStr;
    // TODO: userprefs on the fragment rather than query string
    $query .= $this->getPrefsQueryString($gadget->getUserPrefValues());
    $libs = array();
    $forcedLibs = Config::get('focedJsLibs');
    if ($forcedLibs == null) {
      $reqs = $gadget->getRequires();
      foreach ($reqs as $key => $val) {
        $libs[] = $key;
      }
    } else {
      $libs = explode(':', $forcedLibs);
    }
    $query .= $this->appendLibsToQuery($libs, $gadget);
    // code bugs out with me because of the invalid url syntax since we dont have a URI class to fix it for us
    // this works around that
    if (substr($query, 0, 1) == '&') {
      $query = '?' . substr($query, 1);
    }
    $redirURI .= $query;
    header('Location: ' . $redirURI);
    die();
  }

  /**
   * Returns the requested libs (from getjsUrl) with the libs_param_name prepended
   * ie: in libs=core:caja:etc.js format
   *
   * @param string $libs the libraries
   * @param Gadget $gadget
   * @return string the libs=... string to append to the redirection url
   */
  private function appendLibsToQuery($libs, $gadget) {
    $ret = "&";
    $ret .= Config::get('libs_param_name');
    $ret .= "=";
    $ret .= $this->getJsUrl($libs, $gadget);
    return $ret;
  }

  /**
   * Returns the user preferences in &up_<name>=<val> format
   *
   * @param array $libs array of features this gadget requires
   * @param Gadget $gadget
   * @return string the up_<name>=<val> string to use in the redirection url
   */
  private function getPrefsQueryString($prefVals) {
    $ret = '';
    foreach ($prefVals->getPrefs() as $key => $val) {
      $ret .= '&';
      $ret .= Config::get('userpref_param_prefix');
      $ret .= urlencode($key);
      $ret .= '=';
      $ret .= urlencode($val);
    }
    return $ret;
  }

  /**
   * generates the library string (core:caja:etc.js) including a checksum of all the
   * javascript content (?v=<md5 of js>) for cache busting
   *
   * @param string $libs
   * @param Gadget $gadget
   * @return string the list of libraries in core:caja:etc.js?v=checksum> format
   */
  private function getJsUrl($libs, $gadget) {
    $buf = '';
    if (! is_array($libs) || ! count($libs)) {
      $buf = 'core';
    } else {
      $firstDone = false;
      foreach ($libs as $lib) {
        if ($firstDone) {
          $buf .= ':';
        } else {
          $firstDone = true;
        }
        $buf .= $lib;
      }
    }
    $cache = Config::get('feature_cache');
    $cache = new $cache();
    if (($md5 = $cache->get(md5('getJsUrlMD5'))) === false) {
      $registry = $this->context->getRegistry();
      $features = $registry->getAllFeatures();
      // Build a version string from the md5() checksum of all included javascript
      // to ensure the client always has the right version
      $inlineJs = '';
      foreach ($features as $feature) {
        $library = $feature->getFeature();
        $libs = $library->getLibraries($this->context->getRenderingContext());
        foreach ($libs as $lib) {
          $inlineJs .= $lib->getContent();
        }
      }
      $md5 = md5($inlineJs);
      $cache->set(md5('getJsUrlMD5'), $md5);
    }
    $buf .= ".js?v=" . $md5;
    return $buf;
  }

  private function appendJsConfig($context, $gadget, $hasForcedLibs) {
    $container = $context->getContainer();
    $containerConfig = $context->getContainerConfig();
    //TODO some day we should parse the forcedLibs too, and include their config selectivly as well
    // for now we just include everything if forced libs is set.
    if ($hasForcedLibs) {
      $gadgetConfig = $containerConfig->getConfig($container, 'gadgets.features');
    } else {
      $gadgetConfig = array();
      $featureConfig = $containerConfig->getConfig($container, 'gadgets.features');
      foreach ($gadget->getJsLibraries() as $library) {
        $feature = $library->getFeatureName();
        if (! isset($gadgetConfig[$feature]) && ! empty($featureConfig[$feature])) {
          $gadgetConfig[$feature] = $featureConfig[$feature];
        }
      }
    }

    // Add gadgets.util support. This is calculated dynamically based on request inputs.
    // See java/org/apache/shindig/gadgets/render/RenderingContentRewriter.java for reference.
    $requires = array();
    foreach ($gadget->getRequires() as $feature) {
      $requires[$feature->name] = new EmptyClass();
    }
    $gadgetConfig['core.util'] = $requires;

    return "gadgets.config.init(" . json_encode($gadgetConfig) . ");\n";
  }

  private function appendMessages($gadget) {
    $msgs = '';
    if ($gadget->getMessageBundle()) {
      $bundle = $gadget->getMessageBundle();
      $msgs = json_encode($bundle->getMessages());
    }
    return "gadgets.Prefs.setMessages_($msgs);\n";
  }

  private function appendPreferences(Gadget $gadget) {
    $prefs = json_encode($gadget->getUserPrefValues()->getPrefs());
    return "gadgets.Prefs.setDefaultPrefs_($prefs);\n";
  }

  /**
   * Appends data from <Preload> elements to make them available to
   * gadgets.io.
   *
   * @param gadget
   */
  private function appendPreloads(Gadget $gadget, GadgetContext $context) {
    $resp = Array();
    $gadgetSigner = Config::get('security_token_signer');
    $gadgetSigner = new $gadgetSigner();
    $token = '';
    try {
      $token = $context->extractAndValidateToken($gadgetSigner);
    } catch (Exception $e) {
      $token = '';
      // no token given, safe to ignore
    }
    $unsignedRequests = $unsignedContexts = Array();
    $signedRequests = Array();
    foreach ($gadget->getPreloads() as $preload) {
      try {
        if (($preload->getAuth() == Auth::$NONE || $token != null) && (count($preload->getViews()) == 0 || in_array($context->getView(), $preload->getViews()))) {
          $request = new RemoteContentRequest($preload->getHref());
          $request->createRemoteContentRequestWithUri($preload->getHref());
          $request->getOptions()->ownerSigned = $preload->isSignOwner();
          $request->getOptions()->viewerSigned = $preload->isSignViewer();
          switch (strtoupper(trim($preload->getAuth()))) {
            case "NONE":
              //						Unify all unsigned requests to one single multi request
              $unsignedRequests[] = $request;
              $unsignedContexts[] = $context;
              break;
            case "SIGNED":
              //						Unify all signed requests to one single multi request
              $signingFetcherFactory = new SigningFetcherFactory(Config::get("private_key_file"));
              $fetcher = $signingFetcherFactory->getSigningFetcher(new BasicRemoteContentFetcher(), $token);
              $req = $fetcher->signRequest($preload->getHref(), $request->getMethod());
              $req->setNotSignedUri($preload->getHref());
              $signedRequests[] = $req;
              break;
            default:
              @ob_end_clean();
              header("HTTP/1.0 500 Internal Server Error", true);
              echo "<html><body><h1>" . "500 - Internal Server Error" . "</h1></body></html>";
              die();
          }
        }
      } catch (Exception $e) {
        throw new Exception($e);
      }
    }
    if (count($unsignedRequests)) {
      try {
        $brc = new BasicRemoteContent();
        $responses = $brc->multiFetch($unsignedRequests, $unsignedContexts);
        foreach ($responses as $response) {
          $resp[$response->getUrl()] = array(
              'body' => $response->getResponseContent(),
              'rc' => $response->getHttpCode());
        }
      } catch (Exception $e) {
        throw new Exception($e);
      }
    }
    if (count($signedRequests)) {
      try {
        $fetcher = $signingFetcherFactory->getSigningFetcher(new BasicRemoteContentFetcher(), $token);
        $responses = $fetcher->multiFetchRequest($signedRequests);
        foreach ($responses as $response) {
          $resp[$response->getNotSignedUrl()] = array(
              'body' => $response->getResponseContent(),
              'rc' => $response->getHttpCode());
        }
      } catch (Exception $e) {
        throw new Exception($e);
      }
    }
    $resp = count($resp) ? json_encode($resp) : "{}";
    return "gadgets.io.preloaded_ = " . $resp . ";\n";
  }
}

/*
 * An empty class for generate "{}" code in JavaScript.
 */
class EmptyClass {
}
