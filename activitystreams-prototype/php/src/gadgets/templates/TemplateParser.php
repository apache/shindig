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

//TODO support repeat tags on OSML tags, ie this should work: <os:Html repeat="${Bar}" />
//TODO remove the os-templates javascript if all the templates are rendered on the server (saves many Kb's in gadget size)


require_once 'ExpressionParser.php';

class TemplateParser {
  private $dataContext;
  private $templateLibrary;

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

  /**
   * Processes an os-template
   *
   * @param string $template
   */
  public function process(DOMnode &$osTemplate, $dataContext, $templateLibrary) {
    $this->setDataContext($dataContext);
    $this->templateLibrary = $templateLibrary;
    if ($osTemplate instanceof DOMElement) {
      if (($removeNode = $this->parseNode($osTemplate)) !== false) {
        $removeNode->parentNode->removeChild($removeNode);
      }
    }
  }

  /**
   * Sets and initializes the data context to use while processing the template
   *
   * @param array $dataContext
   */
  private function setDataContext($dataContext) {
    $this->dataContext = array();
    $this->dataContext['Top'] = $dataContext;
    $this->dataContext['Cur'] = array();
    $this->dataContext['My'] = array();
    $this->dataContext['Context'] = array('UniqueId' => uniqid());
  }

  public function parseNode(DOMNode &$node) {
    $removeNode = false;
    if ($node instanceof DOMText) {
      if (! $node->isWhitespaceInElementContent() && ! empty($node->nodeValue)) {
        $this->parseNodeText($node);
      }
    } else {
      $tagName = isset($node->tagName) ? $node->tagName : '';
      if (substr($tagName, 0, 3) == 'os:' || substr($tagName, 0, 4) == 'osx:') {
        $removeNode = $this->parseOsmlNode($node);
      } elseif ($this->templateLibrary->hasTemplate($tagName)) {
        // the tag name refers to an existing template (myapp:EmployeeCard type naming)
        // the extra check on the : character is to make sure this is a name spaced custom tag and not some one trying to override basic html tags (br, img, etc)
        $this->parseLibrary($tagName, $node);
      } else {
        $removeNode = $this->parseNodeAttributes($node);
      }
    }
    return is_object($removeNode) ? $removeNode : false;
  }

  /**
   * Misc function that maps the node's attributes to a key => value array
   * and results any expressions to actual values
   *
   * @param DOMElement $node
   * @return array
   */
  private function nodeAttributesToScope(DOMElement &$node) {
    $myContext = array();
    if ($node->hasAttributes()) {
      foreach ($node->attributes as $attr) {
        if (strpos($attr->value, '${') !== false) {
          // attribute value contains an expression
          $expressions = array();
          preg_match_all('/(\$\{)(.*)(\})/imsxU', $attr->value, $expressions);
          for ($i = 0; $i < count($expressions[0]); $i ++) {
            $expression = $expressions[2][$i];
            $myContext[$attr->name] = ExpressionParser::evaluate($expression, $this->dataContext);
          }
        } else {
          // plain old string
          $myContext[$attr->name] = trim($attr->value);
        }
      }
    }
    return $myContext;
  }

  /**
   * Parses the specified template library
   *
   * @param string $tagName
   * @param DOMNode $node
   */
  private function parseLibrary($tagName, DOMNode &$node) {
    // Set the My context based on the node's attributes
    $myContext = $this->nodeAttributesToScope($node);

    // Template call has child nodes, those are params that can be used in a os:Render call, store them
    $oldNodeContext = isset($this->dataContext['_os_render_nodes']) ? $this->dataContext['_os_render_nodes'] : array();
    $this->dataContext['_os_render_nodes'] = array();
    if ($node->childNodes->length) {
      foreach ($node->childNodes as $childNode) {
        if (isset($childNode->tagName) && ! empty($childNode->tagName)) {
          $nodeParam = ($pos = strpos($childNode->tagName, ':')) ? trim(substr($childNode->tagName, $pos + 1)) : trim($childNode->tagName);
          $this->dataContext['_os_render_nodes'][$nodeParam] = $childNode;
        }
      }
    }
    // Parse the template library (store the My scope since this could be a nested call)
    $previousMy = $this->dataContext['My'];
    $this->dataContext['My'] = $myContext;
    $ret = $this->templateLibrary->parseTemplate($tagName, $this);
    $this->dataContext['My'] = $previousMy;
    $this->dataContext['_os_render_nodes'] = $oldNodeContext;
    if ($ret) {
      // And replace the node with the parsed output
      $ownerDocument = $node->ownerDocument;
      foreach ($ret->childNodes as $childNode) {
        $importedNode = $ownerDocument->importNode($childNode, true);
        $importedNode = $node->parentNode->insertBefore($importedNode, $node);
      }
      $node->parentNode->removeChild($node);
    }
  }

  private function parseNodeText(DOMText &$node) {
    if (strpos($node->nodeValue, '${') !== false) {
      $expressions = array();
      preg_match_all('/(\$\{)(.*)(\})/imsxU', $node->wholeText, $expressions);
      for ($i = 0; $i < count($expressions[0]); $i ++) {
        $toReplace = $expressions[0][$i];
        $expression = $expressions[2][$i];
        $expressionResult = ExpressionParser::evaluate($expression, $this->dataContext);
        $stringVal = htmlentities(ExpressionParser::stringValue($expressionResult), ENT_QUOTES, 'UTF-8');
        $node->nodeValue = str_replace($toReplace, $stringVal, $node->nodeValue);
      }
    }
  }

  private function parseNodeAttributes(DOMNode &$node) {
    if ($node->hasAttributes()) {
      foreach ($node->attributes as $attr) {
        if (strpos($attr->value, '${') !== false) {
          $expressions = array();
          preg_match_all('/(\$\{)(.*)(\})/imsxU', $attr->value, $expressions);
          for ($i = 0; $i < count($expressions[0]); $i ++) {
            $toReplace = $expressions[0][$i];
            $expression = $expressions[2][$i];
            $expressionResult = ExpressionParser::evaluate($expression, $this->dataContext);
            switch (strtolower($attr->name)) {

              case 'repeat':
                // Can only loop if the result of the expression was an array
                if (! is_array($expressionResult)) {
                  throw new ExpressionException("Can't repeat on a singular var");
                }
                // Make sure the repeat variable doesn't show up in the cloned nodes (otherwise it would infinit recurse on this->parseNode())
                $node->removeAttribute('repeat');
                // Is a named var requested?
                $variableName = $node->getAttribute('var') ? trim($node->getAttribute('var')) : false;
                // Store the current 'Cur', index and count state, we might be in a nested repeat loop
                $previousCount = isset($this->dataContext['Context']['Count']) ? $this->dataContext['Context']['Count'] : null;
                $previousIndex = isset($this->dataContext['Context']['Index']) ? $this->dataContext['Context']['Index'] : null;
                $previousCur = $this->dataContext['Cur'];
                // For information on the loop context, see http://opensocial-resources.googlecode.com/svn/spec/0.9/OpenSocial-Templating.xml#rfc.section.10.1
                $this->dataContext['Context']['Count'] = count($expressionResult);
                foreach ($expressionResult as $index => $entry) {
                  if ($variableName) {
                    // this is cheating a little since we're not putting it on the top level scope, the variable resolver will check 'Cur' first though so myVar.Something will still resolve correctly
                    $this->dataContext['Cur'][$variableName] = $entry;
                  }
                  $this->dataContext['Cur'] = $entry;
                  $this->dataContext['Context']['Index'] = $index;
                  // Clone this node and it's children
                  $newNode = $node->cloneNode(true);
                  // Append the parsed & expanded node to the parent
                  $newNode = $node->parentNode->insertBefore($newNode, $node);
                  // And parse it (using the global + loop context)
                  $this->parseNode($newNode, true);
                }
                // Restore our previous data context state
                $this->dataContext['Cur'] = $previousCur;
                if ($previousCount) {
                  $this->dataContext['Context']['Count'] = $previousCount;
                } else {
                  unset($this->dataContext['Context']['Count']);
                }
                if ($previousIndex) {
                  $this->dataContext['Context']['Index'] = $previousIndex;
                } else {
                  unset($this->dataContext['Context']['Index']);
                }
                return $node;
                break;

              case 'if':
                if (! $expressionResult) {
                  return $node;
                } else {
                  $node->removeAttribute('if');
                }
                break;

              // These special cases that only apply for certain tag types
              case 'selected':
                if ($node->tagName == 'option') {
                  if ($expressionResult) {
                    $node->setAttribute('selected', 'selected');
                  } else {
                    $node->removeAttribute('selected');
                  }
                } else {
                  throw new ExpressionException("Can only use selected on an option tag");
                }
                break;

              case 'checked':
                if ($node->tagName == 'input') {
                  if ($expressionResult) {
                    $node->setAttribute('checked', 'checked');
                  } else {
                    $node->removeAttribute('checked');
                  }
                } else {
                  throw new ExpressionException("Can only use checked on an input tag");
                }
                break;

              case 'disabled':
                $disabledTags = array('input', 'button',
                    'select', 'textarea');
                if (in_array($node->tagName, $disabledTags)) {
                  if ($expressionResult) {
                    $node->setAttribute('disabled', 'disabled');
                  } else {
                    $node->removeAttribute('disabled');
                  }
                } else {
                  throw new ExpressionException("Can only use disabled on input, button, select and textarea tags");
                }
                break;

              default:
                // On non os-template spec attributes, do a simple str_replace with the evaluated value
                $stringVal = htmlentities(ExpressionParser::stringValue($expressionResult), ENT_QUOTES, 'UTF-8');
                $newAttrVal = str_replace($toReplace, $stringVal, $attr->value);
                $node->setAttribute($attr->name, $newAttrVal);
                break;
            }
          }
        }
      }
    }
    // if a repeat attribute was found, don't recurse on it's child nodes, the repeat handling already did that
    if (isset($node->childNodes) && $node->childNodes->length > 0) {
      $removeNodes = array();
      // recursive loop to all this node's children
      foreach ($node->childNodes as $childNode) {
        if (($removeNode = $this->parseNode($childNode)) !== false) {
          $removeNodes[] = $removeNode;
        }
      }
      if (count($removeNodes)) {
        foreach ($removeNodes as $removeNode) {
          $removeNode->parentNode->removeChild($removeNode);
        }
      }
    }
    return false;
  }

  /**
   * Function that handles the os: and osx: tags
   *
   * @param DOMNode $node
   */
  private function parseOsmlNode(DOMNode &$node) {
    $tagName = strtolower($node->tagName);
    if (! $this->checkIf($node)) {
      // If the OSML tag contains an if attribute and the expression evaluates to false
      // flag it for removal and don't process it
      return $node;
    }
    switch ($tagName) {

      /****** Control statements ******/

      case 'os:repeat':
        if (! $node->getAttribute('expression')) {
          throw new ExpressionException("Invalid os:Repeat tag, missing expression attribute");
        }
        $expressions = array();
        preg_match_all('/(\$\{)(.*)(\})/imsxU', $node->getAttribute('expression'), $expressions);
        $expression = $expressions[2][0];
        $expressionResult = ExpressionParser::evaluate($expression, $this->dataContext);
        if (! is_array($expressionResult)) {
          throw new ExpressionException("Can't repeat on a singular var");
        }
        // Store the current 'Cur', index and count state, we might be in a nested repeat loop
        $previousCount = isset($this->dataContext['Context']['Count']) ? $this->dataContext['Context']['Count'] : null;
        $previousIndex = isset($this->dataContext['Context']['Index']) ? $this->dataContext['Context']['Index'] : null;
        $previousCur = $this->dataContext['Cur'];
        // Is a named var requested?
        $variableName = $node->getAttribute('var') ? trim($node->getAttribute('var')) : false;
        // For information on the loop context, see http://opensocial-resources.googlecode.com/svn/spec/0.9/OpenSocial-Templating.xml#rfc.section.10.1
        $this->dataContext['Context']['Count'] = count($expressionResult);
        foreach ($expressionResult as $index => $entry) {
          if ($variableName) {
            // this is cheating a little since we're not putting it on the top level scope, the variable resolver will check 'Cur' first though so myVar.Something will still resolve correctly
            $this->dataContext['Cur'][$variableName] = $entry;
          }
          $this->dataContext['Cur'] = $entry;
          $this->dataContext['Context']['Index'] = $index;
          foreach ($node->childNodes as $childNode) {
            $newNode = $childNode->cloneNode(true);
            $newNode = $node->parentNode->insertBefore($newNode, $node);
            $this->parseNode($newNode);
          }
        }
        // Restore our previous data context state
        $this->dataContext['Cur'] = $previousCur;
        if ($previousCount) {
          $this->dataContext['Context']['Count'] = $previousCount;
        } else {
          unset($this->dataContext['Context']['Count']);
        }
        if ($previousIndex) {
          $this->dataContext['Context']['Index'] = $previousIndex;
        } else {
          unset($this->dataContext['Context']['Index']);
        }
        return $node;
        break;

      case 'os:if':
        $expressions = array();
        if (! $node->getAttribute('condition')) {
          throw new ExpressionException("Invalid os:If tag, missing condition attribute");
        }
        preg_match_all('/(\$\{)(.*)(\})/imsxU', $node->getAttribute('condition'), $expressions);
        if (! count($expressions[2])) {
          throw new ExpressionException("Invalid os:If tag, missing condition expression");
        }
        $expression = $expressions[2][0];
        $expressionResult = ExpressionParser::evaluate($expression, $this->dataContext);
        if ($expressionResult) {
          foreach ($node->childNodes as $childNode) {
            $newNode = $childNode->cloneNode(true);
            $this->parseNode($newNode);
            $newNode = $node->parentNode->insertBefore($newNode, $node);
          }
        }
        return $node;
        break;

      /****** OSML tags (os: name space) ******/

      case 'os:name':
        $this->parseLibrary('os:Name', $node);
        break;

      case 'os:badge':
        $this->parseLibrary('os:Badge', $node);
        break;

      case 'os:peopleselector':
        $this->parseLibrary('os:PeopleSelector', $node);
        break;

      case 'os:html':
        if (! $node->getAttribute('code')) {
          throw new ExpressionException("Invalid os:Html tag, missing code attribute");
        }
        preg_match_all('/(\$\{)(.*)(\})/imsxU', $node->getAttribute('code'), $expressions);
        if (count($expressions[2])) {
          $expression = $expressions[2][0];
          $code = ExpressionParser::evaluate($expression, $this->dataContext);
        } else {
          $code = $node->getAttribute('code');
        }
        $node->parentNode->insertBefore($node->ownerDocument->createTextNode($code), $node);

        return $node;
        break;

      case 'os:render':
        if (! ($content = $node->getAttribute('content'))) {
          throw new ExpressionException("os:Render missing attribute: content");
        }
        $content = $node->getAttribute('content');
        if (! isset($this->dataContext['_os_render_nodes'][$content])) {
          throw new ExpressionException("os:Render, Unknown entry: " . htmlentities($content));
        }
        $nodes = $this->dataContext['_os_render_nodes'][$content];
        $ownerDocument = $node->ownerDocument;
        // Only parse the child nodes of the dom tree and not the (myapp:foo) top level element
        foreach ($nodes->childNodes as $childNode) {
          $importedNode = $ownerDocument->importNode($childNode, true);
          $importedNode = $node->parentNode->insertBefore($importedNode, $node);
          $this->parseNode($importedNode);
        }
        return $node;
        break;

      /****** Extension - Tags ******/

      case 'os:flash':
        // handle expressions
        $this->parseNodeAttributes($node);

        // read swf config from attributes
        $swfConfig = array('width' => '100px',
            'height' => '100px', 'play' => 'immediate');
        foreach ($node->attributes as $attr) {
          $swfConfig[$attr->name] = $attr->value;
        }

        // attach security token in the flash var
        $st = 'st=' . $_GET['st'];
        if (array_key_exists('flashvars', $swfConfig)) {
          $swfConfig['flashvars'] = $swfConfig['flashvars'] . '&' . $st;
        } else {
          $swfConfig['flashvars'] = $st;
        }

        // Restrict the content if sanitization is enabled
        $sanitizationEnabled = Config::get('sanitize_views');
        if ($sanitizationEnabled) {
          $swfConfig['allowscriptaccess'] = 'never';
          $swfConfig['swliveconnect'] = 'false';
          $swfConfig['allownetworking'] = 'internal';
        }

        // Generate unique id for this swf
        $ALT_CONTENT_PREFIX = 'os_Flash_alt_';
        $altContentId = uniqid($ALT_CONTENT_PREFIX);

        // Create a div wrapper around the provided alternate content, and add the alternate content to the holder
        $altHolder = $node->ownerDocument->createElement('div');
        $altHolder->setAttribute('id', $altContentId);
        foreach ($node->childNodes as $childNode) {
          $altHolder->appendChild($childNode);
        }
        $node->parentNode->insertBefore($altHolder, $node);

        // Create the call to swfobject in header
        $scriptCode = SwfConfig::buildSwfObjectCall($swfConfig, $altContentId);
        $scriptBlock = $node->ownerDocument->createElement('script');
        $scriptBlock->setAttribute('type', 'text/javascript');
        $node->parentNode->insertBefore($scriptBlock, $node);
        if ($swfConfig['play'] != 'immediate') {
          // Add onclick handler to trigger call to swfobject
          $scriptCode = "function {$altContentId}()\{{$scriptCode};\}";
          $altHolder->setAttribute('onclick', "{$altContentId}()");
        }
        $scriptCodeNode = $node->ownerDocument->createTextNode($scriptCode);
        $scriptBlock->appendChild($scriptCodeNode);
        return $node;
        break;

      case 'osx:navigatetoapp':
        break;

      case 'osx:navigatetoperson':
        break;
    }
    return false;
  }

  /**
   * Misc function that checks if the OSML tag $node has an if attribute, returns
   * true if the expression is true or no if attribute is set
   *
   * @param DOMElement $node
   */
  private function checkIf(DOMElement &$node) {
    if (($if = $node->getAttribute('if'))) {
      $expressions = array();
      preg_match_all('/(\$\{)(.*)(\})/imsxU', $if, $expressions);
      if (! count($expressions[2])) {
        throw new ExpressionException("Invalid os:If tag, missing condition expression");
      }
      $expression = $expressions[2][0];
      $expressionResult = ExpressionParser::evaluate($expression, $this->dataContext);
      return $expressionResult ? true : false;
    }
    return true;
  }
}

class SwfConfig {
  public static $FLASH_VER = '9.0.115';
  public static $PARAMS = array('loop', 'menu', 'quality', 'scale', 'salign', 'wmode', 'bgcolor',
      'swliveconnect', 'flashvars', 'devicefont', 'allowscriptaccess', 'seamlesstabbing',
      'allowfullscreen', 'allownetworking');
  public static $ATTRS = array('id', 'name', 'styleclass', 'align');

  public static function buildSwfObjectCall($swfConfig, $altContentId, $flashVars = 'null') {
    $params = SwfConfig::buildJsObj($swfConfig, SwfConfig::$PARAMS);
    $attrs = SwfConfig::buildJsObj($swfConfig, SwfConfig::$ATTRS);
    $flashVersion = SwfConfig::$FLASH_VER;
    $swfObject = "swfobject.embedSWF(\"{$swfConfig['swf']}\", \"{$altContentId}\", \"{$swfConfig['width']}\", \"{$swfConfig['height']}\", \"{$flashVersion}\", null, {$flashVars}, {$params}, {$attrs});";
    return $swfObject;
  }

  private static function buildJsObj($swfConfig, $keymap) {
    $arr = array();
    foreach ($swfConfig as $key => $value) {
      if (in_array($key, $keymap)) {
        $arr[] = "{$key}:\"{$value}\"";
      }
    }
    $output = implode(",", $arr);
    $output = '{' . $output . '}';
    return $output;
  }
}
