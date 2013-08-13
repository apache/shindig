<?php
namespace apache\shindig\gadgets;

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

class GadgetSpec {
  const DOCTYPE_QUIRKSMODE = "quirksmode";
    
  /**
   * MD5 checksum of the xml's content
   *
   * @var string
   */
  public $checksum;

  // Basic and extended ModulePrefs attributes
  /**
   * @var string
   */
  public $title;
  /**
   * @var string
   */
  public $author;
  /**
   * @var string
   */
  public $authorEmail;
  /**
   * @var string
   */
  public $description;
  /**
   * @var string
   */
  public $directoryTitle;
  /**
   * @var string
   */
  public $screenshot;
  /**
   * @var string
   */
  public $thumbnail;
  /**
   * @var string
   */
  public $titleUrl;
  /**
   * @var string
   */
  public $authorAffiliation;
  /**
   * @var string
   */
  public $authorLocation;
  /**
   * @var string
   */
  public $authorPhoto;
  /**
   * @var string
   */
  public $authorAboutme;
  /**
   * @var string
   */
  public $authorQuote;
  /**
   * @var string
   */
  public $authorLink;
  /**
   * @var string
   */
  public $showStats;
  /**
   * @var string
   */
  public $showInDirectory;
  /**
   * @var string
   */
  public $string;
  /**
   * @var string
   */
  public $width;
  /**
   * @var string
   */
  public $height;
  /**
   * @var string
   */
  public $category;
  /**
   * @var string
   */
  public $category2;
  /**
   * @var string
   */
  public $singleton;
  /**
   * @var string
   */
  public $renderInline;
  /**
   * @var string
   */
  public $scaling;
  /**
   * @var string
   */
  public $scrolling;
  /**
   * @var string
   */
  public $preloads;
  /**
   * @var string
   */
  public $locales;
  /**
   * @var string
   */
  public $icon;
  /**
   * @var string
   */
  public $optionalFeatures;
  /**
   * @var string
   */
  public $requiredFeatures;
  /**
   * @var string
   */
  public $links;
  /**
   * @var string
   */
  public $userPrefs;
  /**
   * @var string
   */
  public $rewrite = null;
  /**
   * @var string
   */
  public $oauth = null;

  // used to track os-templating

  /**
   * @var boolean
   */
  public $templatesRequireLibraries = false;
  /**
   * @var boolean
   */
  public $templatesDisableAutoProcessing = false;
  
  /**
   * @var string
   */
  public $doctype;
  
  /**
   * @var OpenSocialVersion
   */
  public $specificationVersion;
}
