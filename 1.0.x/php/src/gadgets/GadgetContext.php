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

define('DEFAULT_VIEW', 'profile');

/*
 * GadgetContext contains all contextual variables and classes that are relevant for this request,
 * such as url, httpFetcher, feature registry, etc.
 * Server wide variables are stored in config.php
 */
class GadgetContext {
  protected $httpFetcher = null;
  protected $locale = null;
  protected $renderingContext = null;
  protected $registry = null;
  protected $userPrefs = null;
  protected $gadgetId = null;
  protected $view = null;
  protected $moduleId = null;
  protected $url = null;
  protected $cache = null;
  protected $blacklist = null;
  protected $ignoreCache = null;
  protected $forcedJsLibs = null;
  protected $containerConfig = null;
  protected $container = null;
  protected $refreshInterval;

  public function __construct($renderingContext) {
    // Rendering context is set by the calling event handler (either GADGET or CONTAINER)
    $this->setRenderingContext($renderingContext);

    // Request variables
    $this->setIgnoreCache($this->getIgnoreCacheParam());
    $this->setForcedJsLibs($this->getFocedJsLibsParam());
    $this->setUrl($this->getUrlParam());
    $this->setModuleId($this->getModuleIdParam());
    $this->setView($this->getViewParam());
    $this->setContainer($this->getContainerParam());
    $this->setRefreshInterval($this->getRefreshIntervalParam());
    //NOTE All classes are initialized when called (aka lazy loading) because we don't need all of them in every situation
  }

  private function getRefreshIntervalParam() {
    return isset($_GET['refresh']) ? $_GET['refresh'] : Config::get('cache_time');
  }

  private function getContainerParam() {
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

  private function getIgnoreCacheParam() {
    // Support both the old Orkut style &bpc and new standard style &nocache= params
    return (isset($_GET['nocache']) && intval($_GET['nocache']) == 1) || (isset($_GET['bpc']) && intval($_GET['bpc']) == 1);
  }

  private function getFocedJsLibsParam() {
    return isset($_GET['libs']) ? trim($_GET['libs']) : null;
  }

  private function getUrlParam() {
    if (! empty($_GET['url'])) {
      return $_GET['url'];
    } elseif (! empty($_POST['url'])) {
      return $_POST['url'];
    }
    return null;
  }

  private function getModuleIdParam() {
    return isset($_GET['mid']) && is_numeric($_GET['mid']) ? intval($_GET['mid']) : 0;
  }

  private function getViewParam() {
    return ! empty($_GET['view']) ? $_GET['view'] : DEFAULT_VIEW;
  }

  private function instanceBlacklist() {
    $blackListClass = Config::get('blacklist_class');
    if (! empty($blackListClass)) {
      return new $blackListClass();
    } else {
      return null;
    }
  }

  private function instanceUserPrefs() {
    $prefs = array();
    $userPrefParamPrefix = Config::get('userpref_param_prefix');
    foreach ($_GET as $key => $val) {
      if (substr($key, 0, strlen($userPrefParamPrefix)) == $userPrefParamPrefix) {
        $name = substr($key, strlen($userPrefParamPrefix));
        $prefs[$name] = $val;
      }
    }
    return new UserPrefs($prefs);
  }

  private function instanceGadgetId($url, $moduleId) {
    return new GadgetId($url, $moduleId);
  }

  private function instanceHttpFetcher() {
    $remoteContent = Config::get('remote_content');
    return new $remoteContent();
  }

  private function instanceRegistry() {
    // Profiling showed 40% of the processing time was spend in the feature registry
    // So by caching this and making it a one time initialization, we almost double the performance
    $featureCache = Config::get('feature_cache');
    $featureCache = new $featureCache();
    if (! ($registry = $featureCache->get(md5(Config::get('features_path'))))) {
      $registry = new GadgetFeatureRegistry(Config::get('features_path'));
      $featureCache->set(md5(Config::get('features_path')), $registry);
    }
    return $registry;
  }

  private function instanceLocale() {
    // Get language and country params, try the GET params first, if their not set try the POST, else use 'all' as default
    $language = ! empty($_GET['lang']) ? $_GET['lang'] : (! empty($_POST['lang']) ? $_POST['lang'] : 'all');
    $country = ! empty($_GET['country']) ? $_GET['country'] : (! empty($_POST['country']) ? $_POST['country'] : 'all');
    return new Locale($language, $country);
  }

  private function instanceContainerConfig() {
    return new ContainerConfig(Config::get('container_path'));
  }

  public function getContainer() {
    return $this->container;
  }

  public function getContainerConfig() {
    if ($this->containerConfig == null) {
      $this->containerConfig = $this->instanceContainerConfig();
    }
    return $this->containerConfig;
  }

  public function getGadgetId() {
    if ($this->gadgetId == null) {
      $this->setGadgetId($this->instanceGadgetId($this->getUrl(), $this->getModuleId()));
    }
    return $this->gadgetId;
  }

  public function getModuleId() {
    return $this->moduleId;
  }

  public function getRegistry() {
    if ($this->registry == null) {
      $this->setRegistry($this->instanceRegistry());
    }
    return $this->registry;
  }

  public function getUrl() {
    return $this->url;
  }

  public function getUserPrefs() {
    if ($this->userPrefs == null) {
      $this->setUserPrefs($this->instanceUserPrefs());
    }
    return $this->userPrefs;
  }

  public function getView() {
    return $this->view;
  }

  public function setRefreshInterval($interval) {
    $this->refreshInterval = $interval;
  }

  public function setContainer($container) {
    $this->container = $container;
  }

  public function setContainerConfig($containerConfig) {
    $this->containerConfig = $containerConfig;
  }

  public function setBlacklist($blacklist) {
    $this->blacklist = $blacklist;
  }

  public function setCache($cache) {
    $this->cache = $cache;
  }

  public function setGadgetId($gadgetId) {
    $this->gadgetId = $gadgetId;
  }

  public function setHttpFetcher($httpFetcher) {
    $this->httpFetcher = $httpFetcher;
  }

  public function setLocale($locale) {
    $this->locale = $locale;
  }

  public function setModuleId($moduleId) {
    $this->moduleId = $moduleId;
  }

  public function setRegistry($registry) {
    $this->registry = $registry;
  }

  public function setRenderingContext($renderingContext) {
    $this->renderingContext = $renderingContext;
  }

  public function setUrl($url) {
    $this->url = $url;
  }

  public function setUserPrefs($userPrefs) {
    $this->userPrefs = $userPrefs;
  }

  public function setView($view) {
    $this->view = $view;
  }

  public function setIgnoreCache($ignoreCache) {
    $this->ignoreCache = $ignoreCache;
  }

  public function setForcedJsLibs($forcedJsLibs) {
    $this->forcedJsLibs = $forcedJsLibs;
  }

  public function getRefreshInterval() {
    return $this->refreshInterval;
  }

  public function getIgnoreCache() {
    return $this->ignoreCache;
  }

  public function getForcedJsLibs() {
    return $this->forcedJsLibs;
  }

  public function getBlacklist() {
    if ($this->blacklist == null) {
      $this->setBlacklist($this->instanceBlacklist());
    }
    return $this->blacklist;
  }

  public function getRenderingContext() {
    return $this->renderingContext;
  }

  public function getHttpFetcher() {
    if ($this->httpFetcher == null) {
      $this->setHttpFetcher($this->instanceHttpFetcher());
    }
    return $this->httpFetcher;
  }

  public function getLocale() {
    if ($this->locale == null) {
      $this->setLocale($this->instanceLocale());
    }
    return $this->locale;
  }

  public function getFeatureRegistry() {
    return $this->registry;
  }

  /**
   * Extracts the 'st' token from the GET or POST params and calls the
   * signer to validate the token
   *
   * @param GadgetSigner $signer the signer to use (configured in config.php)
   * @return string the token to use in the signed url
   */
  public function extractAndValidateToken($signer) {
    if ($signer == null) {
      return null;
    }
    $token = isset($_GET["st"]) ? $_GET["st"] : '';
    if (! isset($token) || $token == '') {
      $token = isset($_POST['st']) ? $_POST['st'] : '';
    }
    if (count(explode(':', $token)) != 6) {
      $token = urldecode(base64_decode($token));
    }
    return $signer->createToken($token);
  }
}
