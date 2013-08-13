<?php
namespace apache\shindig\test\social;
use apache\shindig\social\converters\InputAlbumsConverter;

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
 * InputAlbumsConverter test case.
 */
class InputAlbumsConverterTest extends \PHPUnit_Framework_TestCase {

  /**
   * @var InputAlbumsConverter
   */
  private $inputConverter;

  /**
   * Prepares the environment before running a test.
   */
  protected function setUp() {
    parent::setUp();
    $this->inputConverter = new InputAlbumsConverter();
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
              <album xmlns="http://ns.opensocial.org/2008/opensocial">
                <id>44332211</id>
                <thumbnailUrl>http://www.libpng.org/pub/png/img_png/pngnow.png</thumbnailUrl>
                <caption>Example Album</caption>
                <description>This is an example album, and this text is an example description</description>
                <location>
                  <latitude>0</latitude>
                  <longitude>0</longitude>
                </location>
                <ownerId>example.org:55443322</ownerId>
              </album>
            </content>
            <title/>
            <updated>2003-12-13T18:30:02Z</updated>
            <author><url>example.org:55443322</url></author>
            <id>urn:guid:example.org:44332211</id>
            </entry>';
    $album = $this->inputConverter->convertAtom($xml);
    $this->assertEquals('44332211', $album['id']);
    $this->assertEquals('http://www.libpng.org/pub/png/img_png/pngnow.png', $album['thumbnailUrl']);
    $this->assertEquals('This is an example album, and this text is an example description', $album['description']);
    $this->assertEquals('Example Album', $album['title']);
    $this->assertEquals('example.org:55443322', $album['ownerId']);
    $this->assertFalse(empty($album['location']));
    $this->assertEquals(0, $album['location']['latitude']);
    $this->assertEquals(0, $album['location']['longitude']);
  }

  public function testConvertJson() {
    $json = '{ "id": "albumId",
               "title": "The album title.",
               "location": {"latitude": 100.0, "longitude": 200.0}
    }';
    $album = $this->inputConverter->convertJson($json);
    $this->assertEquals('albumId', $album['id']);
    $this->assertEquals('The album title.', $album['title']);
    $this->assertFalse(empty($album['location']));
    $this->assertEquals(100.0, $album['location']['latitude']);
    $this->assertEquals(200.0, $album['location']['longitude']);
  }

  public function testConvertXml() {
    $xml = '<?xml version="1.0" encoding="UTF-8"?>
            <album xmlns="http://ns.opensocial.org/2008/opensocial">
            <id>44332211</id>
            <thumbnailUrl>http://www.libpng.org/pub/png/img_png/pngnow.png</thumbnailUrl>
            <caption>Example Album</caption>
            <description>This is an example album, and this text is an example description</description>
            <location>
               <latitude>0</latitude>
               <longitude>0</longitude>
            </location>
            <ownerId>example.org:55443322</ownerId>
            </album>';
    $album = $this->inputConverter->convertXml($xml);
    $this->assertEquals('44332211', $album['id']);
    $this->assertEquals('http://www.libpng.org/pub/png/img_png/pngnow.png', $album['thumbnailUrl']);
    $this->assertEquals('This is an example album, and this text is an example description', $album['description']);
    $this->assertEquals('Example Album', $album['title']);
    $this->assertEquals('example.org:55443322', $album['ownerId']);
    $this->assertFalse(empty($album['location']));
    $this->assertEquals(0, $album['location']['latitude']);
    $this->assertEquals(0, $album['location']['longitude']);
  }
}
