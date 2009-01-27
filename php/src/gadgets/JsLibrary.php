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

class JsLibrary {
  private $types = array('FILE', 'URL', 'INLINE');
  private $type;
  private $content;
  private $featureName; // used to track what feature this belongs to
  private $loaded = false;

  public function __construct($type, $content, $featureName = '') {
    $this->featureName = $featureName;
    $this->type = $type;
    $this->content = $content;
  }

  public function getType() {
    return $this->type;
  }

  public function getContent() {
    if (! $this->loaded && $this->type == 'FILE') {
      if (Config::get('compress_javascript')) {
        $featureCache = Config::get('feature_cache');
        $featureCache = new $featureCache();
        if (! ($content = $featureCache->get(md5($this->content)))) {
          $content = JsMin::minify(JsLibrary::loadData($this->content, $this->type));
          $featureCache->set(md5($this->content), $content);
          $this->content = $content;
        } else {
          $this->content = $content;
        }
      } else {
        $this->content = JsLibrary::loadData($this->content, $this->type);
      }
      $this->loaded = true;
    }
    return $this->content;
  }

  public function getFeatureName() {
    return $this->featureName;
  }

  public function toString() {
    if ($this->type == 'URL') {
      return "<script src=\"" . $this->getContent() . "\"></script>";
    } else {
      return "\n<script><!--\n" . $this->getContent() . "\n--></script>\n";
    }
  }

  static function create($type, $content, $name) {
    return new JsLibrary($type, $content, $name);
  }

  static private function loadData($name, $type) {
    // we don't really do 'resources', so limiting this to files only
    if ($type == 'FILE') {
      return JsLibrary::loadFile($name);
    }
    return null;
  }

  static private function loadFile($fileName) {
    // this hack prevents loadFile from trying to load .jar resources
    if (empty($fileName) || (strpos($fileName, 'res://') !== false)) {
      return '';
    }
    if (Config::get('debug')) {
      if (! File::exists($fileName)) {
        throw new Exception("JsLibrary file missing: $fileName");
      }
      if (! is_file($fileName)) {
        throw new Exception("JsLibrary file is not a file: $fileName");
      }
      if (! File::readable($fileName)) {
        throw new Exception("JsLibrary file not readable: $fileName");
      }
    }
    if (! ($content = @file_get_contents($fileName))) {
      throw new Exception("JsLibrary error reading file: $fileName");
    }
    return $content;
  }
}
