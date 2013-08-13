<?php
namespace apache\shindig\gadgets;
use apache\shindig\common\Config;
use apache\shindig\common\Cache;
use apache\shindig\common\sample\BasicSecurityToken;

/*
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

/*
 * GadgetContext contains all contextual variables and classes that are relevant for this request,
 * such as url, httpFetcher, feature registry, etc.
 * Server wide variables are stored in config.php
 */
class GadgetContext {
  const DEFAULT_VIEW = 'profile';
  /**
   *
   * @var RemoteContentFetcher
   */
  protected $httpFetcher = null;
  /**
   *
   * @var array
   */
  protected $locale = null;
  /**
   *
   * @var string
   */
  protected $renderingContext = null;
  /**
   *
   * @var GadgetFeatureRegistry
   */
  protected $registry = null;
  /**
   *
   * @var string
   */
  protected $view = null;
  /**
   *
   * @var string
   */
  protected $moduleId = null;
  /**
   *
   * @var string
   */
  protected $url = null;
  /**
   *
   * @var array
   */
  protected $cache = null;
  /**
   *
   * @var GadgetBlacklist
   */
  protected $blacklist = null;
  /**
   *
   * @var boolean
   */
  protected $ignoreCache = null;
  /**
   *
   * @var string
   */
  protected $forcedJsLibs = null;
  /**
   *
   * @var array
   */
  protected $containerConfig = null;
  /**
   *
   * @var string
   */
  protected $container = null;

  /**
   *
   * @var string
   */
  protected $rawXml = null;

  /**
   *
   * @var int
   */
  protected $refreshInterval;

  /**
   *
   * @param string $renderingContext
   */
  public function __construct($renderingContext) {
    // Rendering context is set by the calling event handler (either GADGET or CONTAINER)
    $this->setRenderingContext($renderingContext);

    // Request variables
    $this->setIgnoreCache($this->getIgnoreCacheParam());
    $this->setForcedJsLibs($this->getForcedJsLibsParam());
    $this->setUrl($this->getUrlParam());
    $this->setRawXml($this->getRawXmlParam());
    $this->setModuleId($this->getModuleIdParam());
    $this->setView($this->getViewParam());
    $this->setContainer($this->getContainerParam());
    $this->setRefreshInterval($this->getRefreshIntervalParam());
    //NOTE All classes are initialized when called (aka lazy loading) because we don't need all of them in every situation
  }

  /**
   *
   * @return int
   */
  protected function getRefreshIntervalParam() {
    return isset($_GET['refresh']) ? $_GET['refresh'] : Config::get('default_refresh_interval');
  }

  /**
   *
   * @return string
   */
  protected function getContainerParam() {
    $container = 'default';
    if (! empty($_GET['container'])) {
      $container = $_GET['container'];
    } elseif (! empty($_POST['container'])) {
      $container = $_POST['container'];
      //FIXME The paramater used to be called 'synd' & is scheduled for removal
    } elseif (! empty($_GET['synd'])) {
      $container = $_GET['synd'];
    } elseif (! empty($_POST['synd'])) {
      $container = $_POST['synd'];
    }
    return $container;
  }

  /**
   *
   * @return boolean
   */
  protected function getIgnoreCacheParam() {
    // Support both the old Orkut style &bpc and new standard style &nocache= params
    return (isset($_GET['nocache']) && intval($_GET['nocache']) == 1) || (isset($_GET['bpc']) && intval($_GET['bpc']) == 1);
  }

  /**
   *
   * @return string
   */
  protected function getForcedJsLibsParam() {
    return isset($_GET['libs']) ? trim($_GET['libs']) : null;
  }

  /**
   *
   * @return string
   */
  protected function getUrlParam() {
    if (! empty($_GET['url'])) {
      return $_GET['url'];
    } elseif (! empty($_POST['url'])) {
      return $_POST['url'];
    }
    return null;
  }

  /**
   *
   * @return string
   */
  protected function getRawXmlParam() {
    if (! empty($_GET['rawxml'])) {
      return $_GET['rawxml'];
    } elseif (! empty($_POST['rawxml'])) {
      return $_POST['rawxml'];
    }
    return null;
  }

  /**
   *
   * @return int
   */
  protected function getModuleIdParam() {
    return isset($_GET['mid']) && is_numeric($_GET['mid']) ? intval($_GET['mid']) : 0;
  }

  /**
   *
   * @return string
   */
  protected function getViewParam() {
    return ! empty($_GET['view']) ? $_GET['view'] : self::DEFAULT_VIEW;
  }

  /**
   *
   * @return GadgetBlacklist
   */
  protected function instanceBlacklist() {
    $blackListClass = Config::get('blacklist_class');
    if (! empty($blackListClass)) {
      return new $blackListClass();
    } else {
      return null;
    }
  }

  /**
   *
   * @return RemoteContentFetcher
   */
  protected function instanceHttpFetcher() {
    $remoteContent = Config::get('remote_content');
    return new $remoteContent();
  }

  /**
   *
   * @return GadgetFeatureRegistry 
   */
  protected function instanceRegistry() {
    // feature parsing is very resource intensive so by caching the result this saves upto 30% of the processing time
    $featureCache = Cache::createCache(Config::get('feature_cache'), 'FeatureCache');
    $key = md5(implode(',', Config::get('features_path')));
    if (! ($registry = $featureCache->get($key))) {
      $registry = new GadgetFeatureRegistry(Config::get('features_path'));
      $featureCache->set($key, $registry);
    }
    return $registry;
  }

  /**
   *
   * @return array
   */
  protected function instanceLocale() {
    // Get language and country params, try the GET params first, if their not set try the POST, else use 'all' as default
    $language = ! empty($_GET['lang']) ? $_GET['lang'] : (! empty($_POST['lang']) ? $_POST['lang'] : 'all');
    $country = ! empty($_GET['country']) ? $_GET['country'] : (! empty($_POST['country']) ? $_POST['country'] : 'all');
    return array('lang' => strtolower($language), 'country' => strtoupper($country));
  }

  /**
   *
   * @return ContainerConfig
   */
  protected function instanceContainerConfig() {
    $containerConfigClass = Config::get('container_config_class');
    return new $containerConfigClass(Config::get('container_path'));
  }

  /**
   *
   * @return string
   */
  public function getContainer() {
    return $this->container;
  }

  /**
   *
   * @return ContainerConfig
   */
  public function getContainerConfig() {
    if ($this->containerConfig == null) {
      $this->containerConfig = $this->instanceContainerConfig();
    }
    return $this->containerConfig;
  }

  /**
   *
   * @return int
   */
  public function getModuleId() {
    return $this->moduleId;
  }

  /**
   *
   * @return GadgetFeatureRegistry
   */
  public function getRegistry() {
    if ($this->registry == null) {
      $this->setRegistry($this->instanceRegistry());
    }
    return $this->registry;
  }

  /**
   *
   * @return string
   */
  public function getUrl() {
    return $this->url;
  }

  /**
   *
   * @return string
   */
  public function getRawXml() {
    return $this->rawXml;
  }

  /**
   *
   * @return string
   */
  public function getView() {
    return $this->view;
  }

  /**
   *
   * @param int $interval
   */
  public function setRefreshInterval($interval) {
    $this->refreshInterval = $interval;
  }

  /**
   *
   * @param string $container
   */
  public function setContainer($container) {
    $this->container = $container;
  }

  /**
   *
   * @param array $containerConfig
   */
  public function setContainerConfig($containerConfig) {
    $this->containerConfig = $containerConfig;
  }

  /**
   *
   * @param GadgetBlacklist $blacklist
   */
  public function setBlacklist($blacklist) {
    $this->blacklist = $blacklist;
  }

  /**
   *
   * @param array $cache
   */
  public function setCache($cache) {
    $this->cache = $cache;
  }

  /**
   *
   * @param RemoteContentFetcher $httpFetcher
   */
  public function setHttpFetcher($httpFetcher) {
    $this->httpFetcher = $httpFetcher;
  }

  /**
   *
   * @param array $locale
   */
  public function setLocale($locale) {
    $this->locale = $locale;
  }

  /**
   *
   * @param int $moduleId
   */
  public function setModuleId($moduleId) {
    $this->moduleId = $moduleId;
  }

  /**
   *
   * @param GadgetFeatureRegistry $registry
   */
  public function setRegistry($registry) {
    $this->registry = $registry;
  }

  /**
   *
   * @param string $renderingContext
   */
  public function setRenderingContext($renderingContext) {
    $this->renderingContext = $renderingContext;
  }

  /**
   *
   * @param string $url
   */
  public function setUrl($url) {
    $this->url = $url;
  }

  /**
   * @param string $rawXml
   */
  public function setRawXml($rawXml) {
    $this->rawXml = $rawXml;
  }

  /**
   *
   * @param string $view
   */
  public function setView($view) {
    $this->view = $view;
  }

  /**
   *
   * @param boolean $ignoreCache
   */
  public function setIgnoreCache($ignoreCache) {
    $this->ignoreCache = $ignoreCache;
  }

  /**
   *
   * @param string $forcedJsLibs
   */
  public function setForcedJsLibs($forcedJsLibs) {
    $this->forcedJsLibs = $forcedJsLibs;
  }

  /**
   *
   * @return int
   */
  public function getRefreshInterval() {
    return $this->refreshInterval;
  }

  /**
   *
   * @return boolean
   */
  public function getIgnoreCache() {
    return $this->ignoreCache;
  }

  /**
   *
   * @return string
   */
  public function getForcedJsLibs() {
    return $this->forcedJsLibs;
  }

  /**
   *
   * @return GadgetBlacklist
   */
  public function getBlacklist() {
    if ($this->blacklist == null) {
      $this->setBlacklist($this->instanceBlacklist());
    }
    return $this->blacklist;
  }

  /**
   *
   * @return string
   */
  public function getRenderingContext() {
    return $this->renderingContext;
  }

  /**
   *
   * @return RemoteContentFetcher
   */
  public function getHttpFetcher() {
    if ($this->httpFetcher == null) {
      $this->setHttpFetcher($this->instanceHttpFetcher());
    }
    return $this->httpFetcher;
  }

  /**
   *
   * @return array
   */
  public function getLocale() {
    if ($this->locale == null) {
      $this->setLocale($this->instanceLocale());
    }
    return $this->locale;
  }

  /**
   * Extracts the 'st' token from the GET or POST params and calls the
   * signer to validate the token
   *
   * @param SecurityTokenDecoder $signer the signer to use (configured in config.php)
   * @return SecurityToken An object representation of the token data.
   */
  public function extractAndValidateToken($signer) {
    if ($signer == null) {
      return null;
    }

    $token = BasicSecurityToken::getTokenStringFromRequest();

    return $this->validateToken($token, $signer);
  }

  /**
   * Validates a passed-in token.
   * 
   * @param string $token A urlencoded base64 encoded security token.
   * @param SecurityTokenDecoder $signer The signer to use (configured in config.php)
   * @return SecurityToken An object representation of the token data.
   */
  public function validateToken($token, $signer) {
    if (empty($token)) {
      throw new \Exception("Missing or invalid security token");
    }
    return $signer->createToken($token);
  }
}
