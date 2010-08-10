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


/**
 * Misc class to parse and clear libxml2 based errors and returns a formatted and explanatory
 * error string.
 */
class XmlError {

  static public function getErrors($xml = false) {
    $errors = libxml_get_errors();
    $ret = '';
    if ($xml) {
      $xml = explode("\n", $xml);
    }
    foreach ($errors as $error) {
      $ret .= self::parseXmlError($error, $xml);
    }
    libxml_clear_errors();
    return $ret;
  }

  static public function parseXmlError($error, $xml) {
    if ($xml) {
      $ret = $xml[$error->line - 1] . "\n";
      $ret .= str_repeat('-', $error->column) . "^\n";
    }
    switch ($error->level) {
      case LIBXML_ERR_WARNING:
        $ret .= "Warning $error->code: ";
        break;
      case LIBXML_ERR_ERROR:
        $ret .= "Error $error->code: ";
        break;
      case LIBXML_ERR_FATAL:
        $ret .= "Fatal Error $error->code: ";
        break;
    }
    $ret .= trim($error->message) . "\n  Line: $error->line" . "\n  Column: $error->column\n\n";
    return $ret;
  }

  /**
   * Generic misc debugging function to dump a node's structure as plain text xml
   *
   * @param DOMElement $node
   * @param string $function
   */
  public function dumpNode($node, $function) {
    $doc = new DOMDocument(null, 'utf-8');
    $doc->preserveWhiteSpace = true;
    $doc->formatOutput = false;
    $doc->strictErrorChecking = false;
    $doc->recover = false;
    $doc->resolveExternals = false;
    if (! $newNode = @$doc->importNode($node, false)) {
      echo "[Invalid node, dump failed]<br><br>";
      return;
    }
    $doc->appendChild($newNode);
    echo "<b>$function (" . get_class($node) . "):</b><br>" . htmlentities(str_replace('<?xml version="" encoding="utf-8"?>', '', $doc->saveXML()) . "\n") . "<br><br>";
  }
}
