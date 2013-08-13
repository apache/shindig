<?php
namespace apache\shindig\gadgets\servlet;
use apache\shindig\gadgets\GadgetException;
use apache\shindig\common\HttpServlet;
use apache\shindig\common\Config;
use apache\shindig\gadgets\Gadget;

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

class GadgetRenderingServlet extends HttpServlet {
  /**
   *
   * @var GadgetContext
   */
  protected $context;

  /**
   * @throws GadgetException
   */
  public function doGet() {
    try {
      if (empty($_GET['url'])) {
        throw new GadgetException("Missing required parameter: url");
      }
      $contextClass = Config::get('gadget_context_class');
      $this->context = new $contextClass('GADGET');
      $gadgetSigner = Config::get('security_token_signer');
      $gadgetSigner = new $gadgetSigner();
      try {
        $token = $this->context->extractAndValidateToken($gadgetSigner);
      } catch (\Exception $e) {
        // no token given, this is a fatal error if 'render_token_required' is set to true
        if (Config::get('render_token_required')) {
          $this->showError($e);
        } else {
          $token = '';
        }
      }
      $factoryClass = Config::get('gadget_factory_class');
      $gadgetSpecFactory = new $factoryClass($this->context, $token);
      $gadget = $gadgetSpecFactory->createGadget();
      $this->setCachingHeaders();
      $this->renderGadget($gadget);
    } catch (\Exception $e) {
      $this->showError($e);
    }
  }

  /**
   *
   * @param Gadget $gadget
   * @throws GadgetException
   */
  protected function renderGadget(Gadget $gadget) {
    $view = $gadget->getView($this->context->getView());
    $renderClasses = Config::get('gadget_renderer');

    foreach ($renderClasses as $renderClass => $constraints) {
      // if current view meets the configurated renderer constraints
      // render the gadget and stop checking
      if ($this->checkConstraints($view, $constraints)) {
        $gadgetRenderer = new $renderClass($this->context);
        $gadgetRenderer->renderGadget($gadget, $view);
        return;
      }
    }

    throw new GadgetException("Invalid view type");   
  }

  /**
   * checks if the current view meets the given gadget renderer constraints
   *
   * constraint format:
   * 
   * array(
   *   attributeName => expectedValue or boolean to indicate if the attribute is 
   *                      required or not
   * )
   *
   * @param array $view
   * @param array $constraints
   * @return boolean
   */
  public function checkConstraints($view, $constraints) {
    foreach ($constraints as $attribute => $expected) {
      if ($expected === false && isset($view[$attribute]) && $view[$attribute]) {
        return false;
      } else if ($expected === true && !(isset($view[$attribute]) && $view[$attribute])) {
        return false;
      } else if (! is_bool($expected) && $view[$attribute] !== $expected) {
        return false;
      }
    }
    return true;
  }

  /**
   * 
   */
  protected function setCachingHeaders() {
    $this->setContentType("text/html; charset=UTF-8");
    if ($this->context->getIgnoreCache()) {
      // no cache was requested, set non-caching-headers
      $this->setNoCache(true);
    } elseif (isset($_GET['v'])) {
      // version was given, cache for a long long time (a year)
      $this->setCacheTime(365 * 24 * 60 * 60);
    } else {
      // no version was given, cache for 5 minutes
      $this->setCacheTime(5 * 60);
    }
  }

  /**
   *
   * @param Exception $e
   */
  protected function showError($e) {
    header("HTTP/1.0 400 Bad Request", true, 400);
    echo "<html><body>";
    echo "<h1>Error</h1>";
    echo $e->getMessage();
    if (Config::get('debug')) {
      echo "<p><b>Debug backtrace</b></p><div style='overflow:auto; height:300px; border:1px solid #000000'><pre>";
      print_r(debug_backtrace());
      echo "</pre></div>>";
    }
    echo "</body></html>";
    die();
  }
}
