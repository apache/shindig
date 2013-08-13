<?php
namespace apache\shindig\gadgets;
use apache\shindig\common\RemoteContentRequest;
use apache\shindig\common\Config;
use apache\shindig\common\Cache;
use apache\shindig\common\File;

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

/**
 * Class that deals with the processing, loading and dep resolving of the gadget features
 * Features are javascript libraries that provide an API, like 'opensocial' or 'settitle'
 *
 */
class GadgetFeatureRegistry {
  const FEATURE_CONTEXT_GADGET = 'gadget';
  const FEATURE_CONTEXT_CONTAINER = 'container';

  /**
   * @var array
   */
  public $features = array();

  /**
   * @var array
   */
  private $sortedFeatures;

  /**
   *
   * @param miexed $featurePath
   */
  public function __construct($featurePath) {
    if (is_array($featurePath)) {
      foreach ($featurePath as $path) {
        $this->registerFeatures($path);
      }
    } else {
      $this->registerFeatures($featurePath);
    }
    $this->processFeatures();
  }

  /**
   *
   * @param array $features
   * @param GadgetContext $context
   * @param boolean $isGadgetContext
   * @return string
   */
  public function getFeaturesContent($features, GadgetContext $context, $isGadgetContext) {
    $ret = '';
    foreach ($features as $feature) {
      $ret .= $this->getFeatureContent($feature, $context, $isGadgetContext);
    }
    return $ret;
  }

  /**
   *
   * @param string $feature
   * @param GadgetContext $context
   * @param boolean $isGadgetContext
   * @return string
   */
  public function getFeatureContent($feature, GadgetContext $context, $isGadgetContext) {
    if (empty($feature)) return '';
    if (!isset($this->features[$feature])) {
      throw new GadgetException("Invalid feature: ".htmlentities($feature));
    }
    $featureName = $feature;
    $feature = $this->features[$feature];
    $filesContext = $isGadgetContext ? 'gadgetJs' : 'containerJs';
    if (!isset($feature[$filesContext])) {
      // no javascript specified for this context
      return '';
    }
    $ret = '';
    if (Config::get('compress_javascript') && ! isset($_GET['debug'])) {
      $featureCache = Cache::createCache(Config::get('feature_cache'), 'FeatureCache');
      if (($featureContent = $featureCache->get(md5('features:'.$featureName.$isGadgetContext)))) {
        return $featureContent;
      }
    }
    foreach ($feature[$filesContext] as $entry) {
      switch ($entry['type']) {
        case 'URL':
          $request = new RemoteContentRequest($entry['content']);
          $request->getOptions()->ignoreCache = $context->getIgnoreCache();
          $response = $context->getHttpFetcher()->fetch($request);
          if ($response->getHttpCode() == '200') {
            $ret .= $response->getResponseContent()."\n";
          }
          break;
        case 'FILE':
          $file = $feature['basePath'] . '/' . $entry['content'];
          $ret .= file_get_contents($file). "\n";
          break;
        case 'INLINE':
          $ret .= $entry['content'] . "\n";
          break;
      }
    }
    if (Config::get('compress_javascript') && ! isset($_GET['debug'])) {
      $ret = \JsMin::minify($ret);
      $featureCache->set(md5('features:'.$featureName.$isGadgetContext), $ret);
    }
    return $ret;
  }

  /**
   *
   * @param array $needed
   * @param array $resultsFound
   * @param array $resultsMissing
   * @return boolean
   */
  public function resolveFeatures($needed, &$resultsFound, &$resultsMissing) {
    $resultsFound = array();
    $resultsMissing = array();
    $this->addFeatureToResults($resultsFound, $this->features['core']);

    foreach ($needed as $featureName) {
      $feature = isset($this->features[$featureName]) ? $this->features[$featureName] : null;
      if ($feature == null) {
        $resultsMissing[] = $featureName;
      } else {
        $this->addFeatureToResults($resultsFound, $feature);
      }
    }
    return count($resultsMissing) == 0;
  }

  /**
   *
   * @param array $features
   * @param array $sortedFeatures
   */
  public function sortFeatures($features, &$sortedFeatures) {
    if (empty($features)) {
      return;
    }
    $sortedFeatures = array();
    foreach ($this->sortedFeatures as $feature) {
      if (in_array($feature, $features)) {
        $sortedFeatures[] = $feature;
      }
    }
  }

  /**
   *
   * @param array $results
   * @param array $feature
   */
  private function addFeatureToResults(&$results, $feature) {
    if (in_array($feature['name'], $results)) {
      return;
    }
    foreach ($feature['deps'] as $dep) {
      $this->addFeatureToResults($results, $this->features[$dep]);
    }
    if (!in_array($feature['name'], $results)) {
      $results[] = $feature['name'];
    }
  }

  /**
   * Loads the features present in the $featurePath
   *
   * @param string $featurePath path to scan
   */
  private function registerFeatures($featurePath) {
    // Load the features from the shindig/features/features.txt file
    $featuresFile = $featurePath . '/features.txt';
    if (File::exists($featuresFile)) {
      $files = explode("\n", file_get_contents($featuresFile));
      // custom sort, else core.io seems to bubble up before core, which breaks the dep chain order
      usort($files, array($this, 'sortFeaturesFiles'));
      foreach ($files as $file) {
        if (! empty($file) && strpos($file, 'feature.xml') !== false && substr($file, 0, 1) != '#' && substr($file, 0, 2) != '//') {
          $file = realpath($featurePath . '/../' . trim($file));
          $feature = $this->processFile($file);
          $this->features[$feature['name']] = $feature;
        }
      }
    }
  }

  /**
   * gets core features and sorts features
   */
  private function processFeatures() {
    $sortedFeatures = array();
    // Topologically sort all features according to their dependency
    $features = array();
    foreach ($this->features as $feature) {
      $features[] = $feature['name'];
    }
    $reverseDeps = array();
    foreach ($features as $feature) {
      $reverseDeps[$feature] = array();
    }
    $depCount = array();
    foreach ($features as $feature) {
      $deps = $this->features[$feature]['deps'];
      $deps = array_uintersect($deps, $features, "strcasecmp");
      $depCount[$feature] = count($deps);
      foreach ($deps as $dep) {
        $reverseDeps[$dep][] = $feature;
      }
    }
    while (! empty($depCount)) {
      $fail = true;
      foreach ($depCount as $feature => $count) {
        if ($count != 0) continue;
        $fail = false;
        $sortedFeatures[] = $feature;
        foreach ($reverseDeps[$feature] as $reverseDep) {
          $depCount[$reverseDep] -= 1;
        }
        unset($depCount[$feature]);
      }
      if ($fail && ! empty($depCount)) {
        throw new GadgetException("Sorting feature dependence failed: it contains ring!");
      }
    }
    $this->sortedFeatures = $sortedFeatures;
  }

  /**
   * Loads the feature's xml content
   *
   * @param string $file
   * @return array
   */
  private function processFile($file) {
    $feature = null;
    if (!empty($file) && File::exists($file)) {
      if (($content = file_get_contents($file))) {
        $feature = $this->parse($content, dirname($file));
      }
    }
    return $feature;
  }

  /**
   * Parses the feature's XML content
   *
   * @param string $content
   * @param string $path
   * @return array
   */
  protected function parse($content, $path) {
    $doc = simplexml_load_string($content);
    $feature = array();
    $feature['deps'] = array();
    $feature['basePath'] = $path;
    if (! isset($doc->name)) {
      throw new GadgetException('Invalid name in feature: ' . $path);
    }
    $feature['name'] = trim($doc->name);
    if ($doc->gadget) {
      foreach ($doc->gadget as $gadget) {
        $this->processContext($feature, $gadget, self::FEATURE_CONTEXT_GADGET);
      }
    } else if ($doc->all) {
      foreach ($doc->all as $container) {
        $this->processContext($feature, $container, self::FEATURE_CONTEXT_GADGET);
      }
    }
    if ($doc->container) {
      foreach ($doc->container as $container) {
        $this->processContext($feature, $container, self::FEATURE_CONTEXT_CONTAINER);
      }
    } else if ($doc->all) {
      foreach ($doc->all as $container) {
        $this->processContext($feature, $container, self::FEATURE_CONTEXT_CONTAINER);
      }
    }
    foreach ($doc->dependency as $dependency) {
      $feature['deps'][trim($dependency)] = trim($dependency);
    }
    return $feature;
  }

  /**
   * Processes the feature's entries
   *
   * @param array $feature
   * @param string $context
   * @param string $envContext container or gadget
   */
  private function processContext(&$feature, $context, $envContext) {
    foreach ($context->script as $script) {
      $attributes = $script->attributes();
      if (! isset($attributes['src'])) {
        // inline content
        $type = 'INLINE';
        $content = (string)$script;
      } else {
        $content = trim($attributes['src']);
        $url = parse_url($content);

        if (! isset($url['scheme']) || ! isset($url['path']) || ! isset($url['host'])) {
          $type = 'FILE';
          $content = $content;
        } else {
          $type = false;
          switch ($url['scheme']) {
            case 'res':
              $type = 'URL';
              $scheme = (! isset($_SERVER['HTTPS']) || $_SERVER['HTTPS'] != "on") ? 'http' : 'https';
              $content = $scheme . '://' . (Config::get('http_host') ? Config::get('http_host') : $_SERVER['HTTP_HOST']) . '/gadgets/resources/' . $url['host'] . $url['path'];
              break;
            case 'http':
            case 'https':
              $type = 'URL';
              break;
            default:
              $type = 'FILE';
              $content = $content;
          }
        }
      }

      if (! $type) {
        continue;
      }

      $library = array('type' => $type, 'content' => $content);
      if ($library != null) {
        switch ($envContext) {
          case self::FEATURE_CONTEXT_GADGET:
            $feature['gadgetJs'][] = $library;
            break;
          case self::FEATURE_CONTEXT_CONTAINER:
            $feature['containerJs'][] = $library;
            break;
        }
      }
    }
  }

  /**
   *
   * @param string $feature1
   * @param string $feature2
   * @return boolean
   */
  private function sortFeaturesFiles($feature1, $feature2) {
    $feature1 = basename(str_replace('/feature.xml', '', $feature1));
    $feature2 = basename(str_replace('/feature.xml', '', $feature2));
    if ($feature1 == $feature2) {
      return 0;
    }
    return ($feature1 < $feature2) ? - 1 : 1;
  }
}
