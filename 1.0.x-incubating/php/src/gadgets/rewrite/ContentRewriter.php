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

class ContentRewriter {

  public function rewriteGadgetView(Gadget $gadget, ViewSpec $gadgetView) {
    // Dont rewrite content if the spec is unavailable
    $requires = $gadget->getRequires();
    if (isset($requires[ContentRewriteFeature::$REWRITE_TAG])) {
      // Store the feature in the spec so we dont keep parsing it
      $rewriteFeature = new ContentRewriteFeature();
      $rewriteFeature->createRewriteFeature($gadget);
    } else {
      return false;
    }
    if (! $rewriteFeature->isRewriteEnabled()) {
      return false;
    }
    if (ContentRewriteFeature::$PROXY_URL != null) {
      $defaultTags = ContentRewriteFeature::defaultHTMLTags();
      $htmlTags = null;
      if (count($rewriteFeature->getTagsParam()) > 0) {
        foreach ($rewriteFeature->getTagsParam() as $tag) {
          if (isset($defaultTags[$tag])) {
            $htmlTags[$tag] = $defaultTags[$tag];
          }
        }
      } else {
        $htmlTags = $defaultTags;
      }
    }
    $gadgetView->setRewrittenContent($this->rewrite($gadgetView->getContent(), $htmlTags, $rewriteFeature->getExcludeParam(), $rewriteFeature->getIncludeParam(), Config::get('web_prefix') . ContentRewriteFeature::$PROXY_URL, $gadget->getId()->getURI(), $rewriteFeature->getTagsParam()));
    return true;
  }

  public function rewriteRequest($request) {
    if (! $request->isPost() && ($this->isCSS($request->getContentType()) || $this->isHTML($request->getContentType()))) {
      if ($this->isCSS($request->getContentType())) {
        $htmlTags = ContentRewriteFeature::styleRegex();
      } else {
        $htmlTags = ContentRewriteFeature::defaultHTMLTags();
      }
      $content = $request->getResponseContent();
      $request->setResponseContent($this->rewrite($content, $htmlTags, '//', '/(.*)/', Config::get('web_prefix') . ContentRewriteFeature::$PROXY_URL, $request->getUrl()));
    }
    return false;
  }

  public function rewrite($content, $htmlTags, $excRegex, $incRegex, $prefix, $gadgetUrl, $tagsParam = null) {
    $original = $content;
    $toReplace = array();
    $toMatch = array();
    if (isset($htmlTags)) {
      foreach ($htmlTags as $rx) {
        $matchTemp = array();
        @preg_match_all($rx, $content, $matchTemp[0]);
        if (! empty($matchTemp[0][0])) {
          $matchTemp[0][1] = $matchTemp[0][2];
          unset($matchTemp[0][2]);
          $toMatch = array_merge($toMatch, $matchTemp);
        }
      }
    }
    $count = 0;
    foreach ($toMatch as $match) {
      $size = count($match[1]);
      for ($i = 0; $i < $size; $i ++) {
        $url = trim($match[1][$i]);
        if (($excRegex != '//' && @preg_match($excRegex, $url) != 0) || ($incRegex != '//' && @preg_match($incRegex, $url) == 0)) {
          continue;
        }
        $toReplace[$count]['original'] = $match[0][$i];
        $toReplace[$count]['url'] = $url;
        if ($this->isRelative($url)) {
          $lPosition = strrpos($gadgetUrl, "/");
          $gadgetUrl = substr($gadgetUrl, 0, $lPosition + 1);
          $url = $gadgetUrl . $url;
        }
        $toReplace[$count]['new'] = $prefix . urlencode($url);
        $count ++;
      }
    }
    if (! empty($toReplace)) {
      foreach ($toReplace as $target) {
        $tagTemp = str_replace($target['url'], $target['new'], $target['original']);
        $content = str_replace($target['original'], $tagTemp, $content);
      }
    } else {
      return $original;
    }
    return $content;
  }

  private function isRelative($url) {
    return ! (strpos($url, "http://") !== false);
  }

  public function isHTML($mime) {
    if ($mime == null) {
      return false;
    }
    return (strpos(strtolower($mime), 'html') !== false);
  }

  public function isCSS($mime) {
    if ($mime == null) {
      return false;
    }
    return (strpos(strtolower($mime), 'css') !== false);
  }

}