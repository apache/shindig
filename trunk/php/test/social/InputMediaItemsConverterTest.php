<?php
namespace apache\shindig\test\social;
use apache\shindig\social\converters\InputMediaItemsConverter;

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

/**
 * InputAtomConverter test case.
 */
class InputMediaItemsConverterTest extends \PHPUnit_Framework_TestCase {

  /**
   * @var InputMediaItemsConverter
   */
  private $inputConverter;

  /**
   * Prepares the environment before running a test.
   */
  protected function setUp() {
    parent::setUp();
    $this->inputConverter = new InputMediaItemsConverter();
  }

  /**
   * Cleans up the environment after running a test.
   */
  protected function tearDown() {
    $this->inputConverter = null;
    parent::tearDown();
  }

  public function testConvertAtom() {
    $xml = '<entry xmlns="http://www.w3.org/2005/Atom">
              <content type="application/xml">
                <mediaItem xmlns="http://ns.opensocial.org/2008/opensocial">
                  <id>11223344</id>
                  <thumbnailUrl>http://www.libpng.org/pub/png/img_png/pngnow.png</thumbnailUrl>
                  <mimeType>image/png</mimeType>
                  <type>image</type>
                  <url>http://www.libpng.org/pub/png/img_png/pngnow.png</url>
                  <albumId>44332211</albumId>
                </mediaItem>
              </content>
              <title/>
              <updated>2003-12-13T18:30:02Z</updated>
              <author><url>example.org:55443322</url></author>
              <id>urn:guid:example.org:11223344</id>
            </entry>';
    $mediaItem = $this->inputConverter->convertAtom($xml);
    $this->assertEquals('11223344', $mediaItem['id']);
    $this->assertEquals('http://www.libpng.org/pub/png/img_png/pngnow.png', $mediaItem['thumbnailUrl']);
    $this->assertEquals('44332211', $mediaItem['albumId']);
    $this->assertEquals('http://www.libpng.org/pub/png/img_png/pngnow.png', $mediaItem['url']);
    $this->assertEquals('image/png', $mediaItem['mimeType']);
  }

  public function testConvertJson() {
    $json = '{ "id" : "11223344",
               "thumbnailUrl" : "http://www.libpng.org/pub/png/img_png/pngnow.png",
               "mimeType" : "image/png",
               "type" : "image",
               "url" : "http://www.libpng.org/pub/png/img_png/pngnow.png",
               "albumId" : "44332211"
             }';
    $mediaItem = $this->inputConverter->convertJson($json);
    $this->assertEquals('11223344', $mediaItem['id']);
    $this->assertEquals('http://www.libpng.org/pub/png/img_png/pngnow.png', $mediaItem['thumbnailUrl']);
    $this->assertEquals('44332211', $mediaItem['albumId']);
    $this->assertEquals('http://www.libpng.org/pub/png/img_png/pngnow.png', $mediaItem['url']);
    $this->assertEquals('image/png', $mediaItem['mimeType']);
  }

  public function testConvertXml() {
    $xml = '<?xml version="1.0" encoding="UTF-8"?>
            <mediaItem xmlns="http://ns.opensocial.org/2008/opensocial">
              <id>11223344</id>
              <thumbnailUrl>http://www.libpng.org/pub/png/img_png/pngnow.png</thumbnailUrl>
              <mimeType>image/png</mimeType>
              <type>image</type>
              <url>http://www.libpng.org/pub/png/img_png/pngnow.png</url>
              <albumId>44332211</albumId>
            </mediaItem>';
    $mediaItem = $this->inputConverter->convertXml($xml);
    $this->assertEquals('11223344', $mediaItem['id']);
    $this->assertEquals('http://www.libpng.org/pub/png/img_png/pngnow.png', $mediaItem['thumbnailUrl']);
    $this->assertEquals('44332211', $mediaItem['albumId']);
    $this->assertEquals('http://www.libpng.org/pub/png/img_png/pngnow.png', $mediaItem['url']);
    $this->assertEquals('image/png', $mediaItem['mimeType']);
  }
}
