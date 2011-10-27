<?php
namespace apache\shindig\gadgets\templates;

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
 * Misc class that holds the template information, an inline template
 * will only contain a text blob (stored as parsed $dom node), however
 * external and OSML library templates can also container script and
 * style blocks
 *
 */
class TemplateLibraryEntry {
  public $dom;
  public $style = array();
  public $script = array();

  public function __construct($dom = false) {
    $this->dom = $dom;
  }

  /**
   * Adds a javascript blob to this template
   *
   * @param unknown_type $script
   */
  public function addScript(TemplateLibraryContent $script) {
    $this->script[] = $script;
  }

  /**
   * Adds a style blob to this template
   *
   * @param unknown_type $style
   */
  public function addStyle(TemplateLibraryContent $style) {
    $this->style[] = $style;
  }

  /**
   * Returns the (combined, in inclusion  order) script text blob, or
   * false if there's no javascript for this template
   *
   * @return javascript string or false
   */
  public function getScript() {
    $ret = '';
    foreach ($this->script as $script) {
      if (! $script->included) {
        $ret .= $script->content . "\n";
      }
    }
    return ! empty($ret) ? $ret : false;
  }

  /**
   * Returns the (combined, in inclusion  order) stylesheet text blob, or
   * false if there's no style sheet associated with this template
   *
   * @return javascript string or false
   */
  public function getStyle() {
    $ret = '';
    foreach ($this->style as $style) {
      if (! $style->included) {
        $ret .= $style->content . "\n";
      }
    }
    return ! empty($ret) ? $ret : false;
  }
}
