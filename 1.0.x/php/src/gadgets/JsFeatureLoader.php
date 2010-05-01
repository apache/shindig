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

class JsFeatureLoader {

  public function loadFeatures($path, $registry) {
    $alreadyRegistered = array();
    $registeredFeatures = array();
    $installedFeatures = array();
    $this->loadFiles($path, $installedFeatures);
    
    // This ensures that we register everything in the right order.
    foreach ($installedFeatures as $feature) {
      $registeredFeature = $this->register($registry, $feature, $alreadyRegistered, $installedFeatures, $registeredFeatures);
      if ($registeredFeature != null) {
        $registeredFeatures[] = $registeredFeature;
      }
    }
    return $registeredFeatures;
  }

  private function sortFeaturesFiles($feature1, $feature2) {
    $feature1 = basename(str_replace('/feature.xml', '', $feature1));
    $feature2 = basename(str_replace('/feature.xml', '', $feature2));
    if ($feature1 == $feature2) {
      return 0;
    }
    return ($feature1 < $feature2) ? - 1 : 1;
  }

  private function loadFiles($path, &$features) {
    $featuresFile = $path . '/features.txt';
    if (File::exists($featuresFile)) {
      $files = explode("\n", file_get_contents($featuresFile));
      // custom sort, else core.io seems to bubble up before core, which breaks the dep chain order
      usort($files, array($this, 'sortFeaturesFiles'));
      foreach ($files as $file) {
        if (! empty($file) && strpos($file, 'feature.xml') !== false && substr($file, 0, 1) != '#' && substr($file, 0, 2) != '//') {
          $file = realpath($path . '/../' . trim($file));
          $feature = $this->processFile($file);
          $features[$feature->name] = $feature;
        }
      }
    }
  }

  private function processFile($file) {
    $feature = null;
    if (File::exists($file)) {
      if (($content = @file_get_contents($file))) {
        $feature = $this->parse($content, dirname($file));
      }
    }
    return $feature;
  }

  private function register(&$registry, $feature, &$alreadyRegistered, $installedFeatures, &$registeredFeatures) {
    if (in_array($feature->name, $alreadyRegistered)) {
      return null;
    }
    foreach ($feature->deps as $dep) {
      if (isset($installedFeatures[$dep]) && ! in_array($dep, $alreadyRegistered)) {
        $registeredFeature = $this->register($registry, $installedFeatures[$dep], $alreadyRegistered, $installedFeatures, $registeredFeatures);
        if ($registeredFeature != null) {
          // add dependency to list of loaded features
          $registeredFeatures[] = $registeredFeature;
        }
      }
      // Note: if a depency is missing, it is simply not loaded. There is no check for that here
    }
    $factory = new JsLibraryFeatureFactory($feature->gadgetJs, $feature->containerJs);
    $alreadyRegistered[] = $feature->name;
    return $registry->register($feature->name, $feature->deps, $factory);
  }

  private function parse($content, $path) {
    $doc = simplexml_load_string($content);
    $feature = new ParsedFeature();
    $feature->basePath = $path;
    if (! isset($doc->name)) {
      throw new GadgetException('Invalid name in feature: ' . $path);
    }
    $feature->name = trim($doc->name);
    foreach ($doc->gadget as $gadget) {
      $feature = $this->processContext($feature, $gadget, false);
    }
    foreach ($doc->container as $container) {
      $feature = $this->processContext($feature, $container, true);
    }
    foreach ($doc->dependency as $dependency) {
      $feature->deps[] = trim($dependency);
    }
    return $feature;
  }

  private function processContext(&$feature, $context, $isContainer) {
    foreach ($context->script as $script) {
      $attributes = $script->attributes();
      if (! isset($attributes['src'])) {
        // inline content
        $type = 'INLINE';
        $content = (string)$script;
      } else {
        $content = trim($attributes['src']);
        if (strtolower(substr($content, 0, strlen("http://"))) == "http://") {
          $type = 'URL';
        } elseif (strtolower(substr($content, 0, strlen("//"))) == "//") {
          $type = 'URL';
        } else {
          // as before, we skip over the resource parts since we dont support them
          $type = 'FILE';
          $content = $feature->basePath . '/' . $content;
        }
      }
      $library = JsLibrary::create($type, $content, $feature->name);
      if ($library != null) {
        if ($isContainer) {
          $feature->containerJs[] = $library;
        } else {
          $feature->gadgetJs[] = $library;
        }
      }
    }
    return $feature;
  }
}

class ParsedFeature {
  public $name = "";
  public $basePath = "";
  public $containerJs = array();
  public $gadgetJs = array();
  public $deps = array();
}
