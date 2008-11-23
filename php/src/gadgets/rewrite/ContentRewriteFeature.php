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

class ContentRewriteFeature {
  public static $REWRITE_TAG = "content-rewrite";
  public static $INCLUDE_URLS = "include-urls";
  public static $EXCLUDE_URLS = "exclude-urls";
  public static $INCLUDE_TAGS = "include-tags";
  public static $PROXY_URL = "/gadgets/proxy?url=";
  private $includeAll = false;
  private $includeNone = false;
  private $includeParam;
  private $excludeParam;
  private $tagsParam;

  public function createRewriteFeature(Gadget $gadget) {
    $requires = $gadget->getRequires();
    if (! isset($requires[ContentRewriteFeature::$REWRITE_TAG])) {
      return;
    }
    $rewriteFeature = $requires[ContentRewriteFeature::$REWRITE_TAG];
    $rewriteParams = $rewriteFeature->getParams();
    if (isset($rewriteParams[ContentRewriteFeature::$INCLUDE_URLS])) {
      $this->includeParam = $this->normalizeParam($rewriteParams[ContentRewriteFeature::$INCLUDE_URLS], '//');
    } else {
      $this->includeParam = '//';
    }
    if (isset($rewriteParams[ContentRewriteFeature::$EXCLUDE_URLS])) {
      $this->excludeParam = $this->normalizeParam($rewriteParams[ContentRewriteFeature::$EXCLUDE_URLS], '//');
    } else {
      $this->excludeParam = '//';
    }
    if (isset($rewriteParams[ContentRewriteFeature::$INCLUDE_TAGS])) {
      $this->tagsParam = $rewriteParams[ContentRewriteFeature::$INCLUDE_TAGS];
      $this->tagsParam = explode(',', $this->tagsParam);
    } else {
      $this->tagsParam = array();
    }
    if ($this->excludeParam == '.*' || $this->includeParam == null) {
      $this->includeNone = true;
    }
    if ($this->includeParam == '.*' || $this->excludeParam == null) {
      $this->includeAll = true;
    }
  }

  public function createDefaultRewriteFeature(Gadget $gadget) {
    $this->includeParam = '/.*/';
    $this->includeAll = true;
  }

  public function normalizeParam($paramValue, $defaultVal) {
    if (empty($paramValue)) {
      return $defaultVal;
    }
    if ($paramValue{0} != '/') {
      return '/' . $paramValue . '/';
    } else {
      return $paramValue;
    }
  }

  public function isRewriteEnabled() {
    return ! $this->includeNone;
  }

  public function shouldRewriteURL($url) {
    if ($this->includeNone) {
      return false;
    } else if ($this->includeAll) {
      return true;
    } else if (preg_match($this->includeParam, $url) != 0) {
      return ($this->excludeParam != null && preg_match($this->excludeParam, $url) != 0);
    }
    return false;
  }

  public function shouldRewriteTag($tag) {
    if ($tag != null) {
      return in_array(strtolower($tag), $this->tagsParam);
    }
    return false;
  }

  public static function defaultHTMLTags() {
    return array('img' => '/\<img[^\>]*?src\=(\'|\")(.*?)\1/', 
        'link' => '/\<link[^\>]*?href\=(\'|\")(.*?)\1/', 
        'embed' => '/\<embed[^\>]*?src\=(\'|\")(.*?)\1/', 
        'script' => '/\<script[^\>]*?src\=(\'|\")(.*?)\1/', 
        'style' => '/url\(\s*(\'|\"|)([^\'\"]*?)(\'|\"|)\s*\)/');
  }

  public static function styleRegex() {
    return array('css' => '/url\(\s*(\'|\"|)([^\'\"]*?)(\'|\"|)\s*\)/');
  }

  public function isTagIncluded($tag) {
    if (empty($this->tagsParam)) {
      return false;
    }
    return in_array($tag, $this->getTagsParam());
  }

  public function getExcludeParam() {
    return $this->excludeParam;
  }

  public function getIncludeParam() {
    return $this->includeParam;
  }

  public function getIncludeAll() {
    return $this->includeAll;
  }

  public function getIncludeNone() {
    return $this->includeNone;
  }

  public function getTagsParam() {
    return $this->tagsParam;
  }

  public function setExcludeParam($excludeParam) {
    $this->excludeParam = $excludeParam;
  }

  public function setIncludeParam($includeParam) {
    $this->includeParam = $includeParam;
  }

  public function setIncludeAll($includeAll) {
    $this->includeAll = $includeAll;
  }

  public function setIncludeNone($includeNone) {
    $this->includeNone = $includeNone;
  }

  public function setTagsParam($tagsParam) {
    $this->tagsParam = $tagsParam;
  }
}
