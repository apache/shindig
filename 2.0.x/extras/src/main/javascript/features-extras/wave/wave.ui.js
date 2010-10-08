/**
 * Copyright 2010 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

/**
 * @fileoverview UI extension to the basic wave gadget API.
 *
 * wave.ui allows gadgets to get the look and feel of other
 * wave elements.
 *
 * Example: turn a link into a wave button
 * <pre>
 *   <a id="butTest" href="#" onclick="alert('hi!')">Text</a>
 *
 *   <script>
 *     wave.ui.makeButton(document.getElementById('butTest'));
 *   </script>
 * </pre>
 */

if (typeof wave == "undefined") {
  wave = {};
};

if (typeof wave.ui == "undefined") {
/**
 * @namespace This namespace defines methods for creating a wave
 * look & feel inside a gadget.
 */
  wave.ui = {};
};

wave.ui.BASE = 'http://wave-api.appspot.com/public/';

wave.ui.cssLoaded = false;

/**
 * Loads a CSS with Wave-like styles into the gadget, including font
 * properties, link properties, and the properties for the wave-styled
 * button, dialog, and frame.
 *
 * @export
 */
wave.ui.loadCss = function() {
  if (wave.ui.cssLoaded) {
    return;
  }
  wave.ui.cssLoaded = true;
  var fileref = document.createElement("link")
  fileref.setAttribute("rel", "stylesheet")
  fileref.setAttribute("type", "text/css")
  fileref.setAttribute("href", wave.ui.BASE + "wave.ui.css")
  document.getElementsByTagName("head")[0].appendChild(fileref)
}

/**
 * Converts the passed in target into a wave-styled button.
 *
 * @param {Element} target element to turn into a button. The target should be
 * an anchor element.
 * @export
 */
wave.ui.makeButton = function(target) {
  wave.ui.loadCss();
  target.innerHTML = '<span>' + target.innerHTML + '</span>';
  target.className += ' wavebutton';
};

/**
 * Converts the passed in target into a wave-styled dialog.
 *
 * For now it only creates a centered box. The close button in the upper right
 * corner will be default do nothing.
 *
 * @param {Element} target element to turn into a dialog. The target should be
 * a div.
 * @param {string} title
 * @export
 */
wave.ui.makeDialog = function(target, title, onclick) {
  wave.ui.loadCss();

  var body = target.innerHTML;
  target.innerHTML = '';

  var headDiv = document.createElement('div');
  headDiv.className = 'wavedialoghead';

  var span = document.createElement('span');

  var closeDiv = document.createElement('div');
  closeDiv.className = 'wavedialogclose';
  function closeFunction() {
    target.style.display = 'none';
  }
  closeDiv.onclick = onclick || closeFunction;

  span.appendChild(closeDiv);
  span.appendChild(document.createTextNode(title));

  headDiv.appendChild(span);
  target.appendChild(headDiv);

  var bodyDiv = document.createElement('div');
  bodyDiv.className = 'wavedialogbody';
  bodyDiv.innerHTML = body;
  target.appendChild(bodyDiv);
  target.className += ' wavedialog';
};

/**
 * Converts the passed in target into a wave-styled frame.
 *
 * @param {Element} target element to turn into a frame. The target should be
 * a div.
 * @export
 */
wave.ui.makeFrame = function(target) {
  wave.ui.loadCss();
  target.innerHTML = '<div class="waveboxhead"><span>&nbsp;</span></div>' +
      '<div class="waveboxbody">' + target.innerHTML + '</div>';
  target.className += ' wavebox';
};
