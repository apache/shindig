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

include_once ('src/UserPrefs.php');
include_once ('src/Gadget.php');
include_once ('src/GadgetException.php');
include_once ('src/GadgetServer.php');
include_once ('src/GadgetSpecParser.php');
include_once ('src/GadgetFeatureRegistry.php');
include_once ('src/HttpProcessingOptions.php');
include_once ("src/{$config['remote_content']}.php");

class GadgetRenderingServlet extends HttpServlet {
	
	public function doGet()
	{
		global $config;
		try {
			if (empty($_GET['url'])) {
				throw new GadgetException("Missing required parameter: url");
			}
			$url = trim($_GET['url']);
			$moduleId = isset($_GET['mid']) && is_numeric($_GET['mid']) ? intval($_GET['mid']) : 0;
			$options = new HttpProcessingOptions();
			$httpFetcher = new $config['remote_content']();
			$view = ! empty($_GET['view']) ? $_GET['view'] : DEFAULT_VIEW;
			$gadgetId = new GadgetId($url, $moduleId);
			$prefs = $this->getPrefsFromRequest();
			$locale = $this->getLocaleFromRequest();
			$registry = new GadgetFeatureRegistry($config['features_path']);
			// skipping the contentFilters bit, since there's no way to include caja yet
			// hopefully a rpc service or command line version will be available at some point
			$gadgetServer = new GadgetServer();
			$gadget = $gadgetServer->processGadget($gadgetId, $prefs, $locale, 'GADGET', $options, $httpFetcher, $registry);
			$this->outputGadget($gadget, $view, $options);
		} catch ( Exception $e ) {
			$this->outputError($e);
		}
	}
	private function outputError($e)
	{
		global $config;
		header("HTTP/1.0 400 Bad Request", true, 400);
		echo "<html><body>";
		echo "<h1>Error</h1>";
		echo $e->getMessage();
		if ($config['debug']) {
			echo "<p><b>Debug info:</b></p><div style='overflow:auto; height:300px; border:1px solid #000000'><pre>";
			print_r(debug_backtrace());
			echo "</pre></div>>";
		}
		echo "</body></html>";
	}
	
	private function outputGadget($gadget, $view, $options)
	{
		switch ( $gadget->getContentType()) {
			case 'HTML' :
				$this->outputHtmlGadget($gadget, $view, $options);
				break;
			case 'URL' :
				$this->outputUrlGadget($gadget, $options);
				break;
		}
	}
	
	private function outputHtmlGadget($gadget, $view, $options)
	{
		global $config;
		$this->setContentType("text/html; charset=UTF-8");
		$output = '';
		$output .= "<html><head>";
		// TODO: This is so wrong. (todo copied from java shindig, but i would agree with it :))
		$output .= "<style type=\"text/css\">body,td,div,span,p{font-family:arial,sans-serif;} a {color:#0000cc;}a:visited {color:#551a8b;}a:active {color:#ff0000;}body{margin: 0px;padding: 0px;background-color:white;}</style>";
		$output .= "</head><body>";
		$externJs = "";
		$inlineJs = "";
		$externFmt = "<script src=\"%s\"></script>";
		$forcedLibs = $options->getForcedJsLibs();
		foreach ( $gadget->getJsLibraries() as $library ) {
			$type = $library->getType();
			if ($type == 'URL') {
				// TODO: This case needs to be handled more gracefully by the js
				// servlet. We should probably inline external JS as well.
				$externJs .= sprintf($externFmt, $library->getContent());
			} else if ($type == 'INLINE') {
				$inlineJs .= $library->getContent() . "\n";
			} else {
				// FILE or RESOURCE
				if ($forcedLibs == null) {
					$inlineJs .= $library->getContent() . "\n";
				}
				// otherwise it was already included by options.forceJsLibs.
			}
		}
		// Forced libs first.
		//FIXME this doesnt make any sense to me yet, should make it actually do something :-)
		if (! empty($forcedLibs)) {
			$libs = explode(':', $forcedLibs);
			$output .= sprintf($externFmt, $this->getJsUrl($libs));
		}
		if (strlen($inlineJs) > 0) {
			$output .= "<script><!--\n" . $inlineJs . "\n-->\n</script>";
		}
		if (strlen($externJs) > 0) {
			$output .= $externJs;
		}
		//FIXME quick hack to make it work with the new syndicator.js config, needs a propper implimentation later
		if (!file_exists($config['syndicator_config']) || !is_readable($config['syndicator_config'])) {
			throw new GadgetException("Invalid syndicator config");
		}
		// remove both /* */ and // style comments, they crash the json_decode function
		$contents = preg_replace('/\/\/.*$/m','',preg_replace('@/\\*(?:.|[\\n\\r])*?\\*/@', '', file_get_contents($config['syndicator_config'])));
		$syndData = json_decode($contents, true);
		$output .= '<script>gadgets.config.init('.json_encode($syndData['gadgets.features']).');</script>';
		$gadgetExceptions = array();
		$content = $gadget->getContentData($view);
		if (empty($content)) {
			// unknown view
			$gadgetExceptions[] = "View: '" . $view . "' invalid for gadget: " . $gadget->getId()->getKey();
		}
		if (count($gadgetExceptions)) {
			throw new GadgetException(print_r($gadgetExceptions, true));
		}
		$output .= $content . "\n";
		$output .= "<script>gadgets.util.runOnLoadHandlers();</script>";
		$output .= "</body></html>";
		echo $output;
	}
	
	private function outputUrlGadget($gadget, $options)
	{
		// Preserve existing query string parameters.
		$redirURI = $gadget->getContentHref();
		$queryStr = strpos($redirURI, '?') !== false ? substr($redirURI, strpos($redirURI, '?')) : '';
		$query = $queryStr;
		// TODO: userprefs on the fragment rather than query string
		$query .= $this->getPrefsQueryString($gadget->getUserPrefValues());
		$libs = array();
		$forcedLibs = $options->getForcedJsLibs();
		if ($forcedLibs == null) {
			$reqs = $gadget->getRequires();
			foreach ( $reqs as $key => $val ) {
				$libs[] = $key;
			}
		} else {
			$libs = explode(':', $forcedLibs);
		}
		$query .= $this->appendLibsToQuery($libs);
		// code bugs out with me because of the invalid url syntax since we dont have a URI class to fix it for us
		// this works around that
		if (substr($query, 0, 1) == '&') {
			$query = '?' . substr($query, 1);
		}
		$redirURI .= $query;
		header('Location: ' . $redirURI);
		die();
	}
	
	private function appendLibsToQuery($libs)
	{
		$ret = "&";
		$ret .= LIBS_PARAM_NAME;
		$ret .= "=";
		$ret .= $this->getJsUrl($libs);
		return $ret;
	}
	
	private function getPrefsQueryString($prefVals)
	{
		global $config;
		$ret = '';
		foreach ( $prefVals->getPrefs() as $key => $val ) {
			$ret .= '&';
			$ret .= $config['userpref_param_prefix'];
			$ret .= urlencode($key);
			$ret .= '=';
			$ret .= urlencode($val);
		}
		return $ret;
	}
	
	private function getJsUrl($libs)
	{
		$buf = '';
		if (! is_array($libs) || ! count($libs)) {
			$buf = 'core';
		} else {
			$firstDone = false;
			foreach ( $libs as $lib ) {
				if ($firstDone) {
					$buf .= ':';
				} else {
					$firstDone = true;
				}
				$buf .= $lib;
			}
		}
		//FIXME add sha1 / md5 checksum of all js content as unique key as 'version'
		$buf .= ".js?v=";
		return $buf;
	}
	
	// since we don't have a java style request.locale. we create our own
	// this parses formats like 'en', 'en-us', 'en-us;en-gb' etc
	//TODO should really verify that this really works with all locale strings and that it doesn't trip over priority weight fields
	private function getLocaleFromRequest()
	{
		$language = 'all';
		$country = 'all';
		if (! empty($_SERVER['HTTP_ACCEPT_LANGUAGE'])) {
			$acceptLanguage = explode(';', $_SERVER['HTTP_ACCEPT_LANGUAGE']);
			$acceptLanguage = $acceptLanguage[0];
			if (strpos($acceptLanguage, '-') !== false) {
				$lang = explode('-', $acceptLanguage);
				$language = $lang[0];
				$country = $lang[1];
				if (strpos($country, ',') !== false) {
					$country = explode(',', $country);
					$country = $country[0];
				}
			} else {
				$language = $acceptLanguage;
			}
		
		}
		return new Locale($language, $country);
	}
	
	private function getPrefsFromRequest()
	{
		global $config;
		$prefs = array();
		foreach ( $_GET as $key => $val ) {
			if (substr($key, 0, strlen($config['userpref_param_prefix'])) == $config['userpref_param_prefix']) {
				$name = substr($key, strlen($config['userpref_param_prefix']));
				$prefs[$name] = $val;
			}
		}
		return new UserPrefs($prefs);
	}

}