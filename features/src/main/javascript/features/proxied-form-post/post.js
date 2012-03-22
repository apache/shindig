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
 * @fileoverview This provides the ability to upload a file through the shindig
 *         proxy by posting a form element.
 */

(function () {
  var config,
      iframe,
      work,
      workQ = [],
      workTimout;

  /**
   * @param {Object} configuration Configuration settings.
   * @private
   */
  function init(configuration) {
    config = configuration['core.io'] || {};
  }
  gadgets.config.register('core.io', {
    "jsonProxyUrl": gadgets.config.NonEmptyStringValidator
  }, init);

  // IE and FF3.6 only
  function getIFrame() {
    if (!iframe) {
      var container = gadgets.util.createElement('div');
      container.innerHTML =
          '<iframe name="os-xhrframe"'
        + ' style="position:absolute;left:1px;top:1px;height:1px;width:1px;visibility:hidden"'
        + ' onload="gadgets.io.proxiedMultipartFormPostCB_();"></iframe>';
      gadgets.util.getBodyElement().appendChild(iframe = container.firstChild);
    }
    return iframe;
  }

  gadgets.io.proxiedMultipartFormPostCB_ = function(event) {
    if (!work) {
      return;
    }

    try {
      var doc = iframe.contentDocument || (iframe.contentWindow && iframe.contentWindow.document) || iframe.document,
          data = doc.getElementsByTagName('textarea')[0].value;
    } catch (e) {}
    var xhrobj = {
      readyState: 4,
      status: data ? 200 : 500,
      responseText: data ? data : 'Unknown error.'
    };
    work.form.setAttribute('action', work.url);

    gadgets.io.processResponse_.call(null, work.url, work.onresult, work.params, xhrobj);
    work = 0;
    if (workQ.length) {
      work = workQ.shift();
      work.form.submit();
    }
  };

  /**
   * Posts a form through the proxy to a remote service.
   *
   * @param {Element} form The form element to be posted. This form element must
   *           include the action attribute for where you want the data to be posted.
   * @param {Object} params The request options. Similar to gadgets.io.makeRequest
   * @param {function} onresult The callback to process the success or failure of
   *           the post.  Similar to gadgets.io.makeRequest's callback param.
   * @param {function=} opt_onprogress The callback to call with progress updates.
   *           This callback may not be called if the browser does not
   *           support the api. Please note that this only reflects the progress of
   *           uploading to the shindig proxy and does not account for progress of
   *           the request from the shindig proxy to the remote server.
   *           This callback takes 2 arguments:
   *             {Event} event The progress event.
   *             {function} abort Function to call to abort the post.
   * @param {FormData=} opt_formdata The FormData object to post. If provided, this
   *           object will be used as the data to post the provided form and the form
   *           element provided should contain the action attribute of where to post
   *           the form. If ommitted and the browser supports it, a FormData object
   *           will be created from the provided form element.
   */
  gadgets.io.proxiedMultipartFormPost = function (form, params, onresult, onprogress, formdata) {
    params = params || {};

    var auth, signOwner,
        signViewer = signOwner = true,
        st = shindig.auth.getSecurityToken(),
        url = form.getAttribute('action'),
        contentType = 'multipart/form-data',
        headers = params['HEADERS'] || (params['HEADERS'] = {}),
        urlParams = gadgets.util.getUrlParameters();

    if (params['AUTHORIZATION'] && params['AUTHORIZATION'] !== 'NONE') {
      auth = params['AUTHORIZATION'].toLowerCase();
    }
    // Include owner information?
    if (typeof params['OWNER_SIGNED'] !== 'undefined') {
      signOwner = params['OWNER_SIGNED'];
    }
    // Include viewer information?
    if (typeof params['VIEWER_SIGNED'] !== 'undefined') {
      signViewer = params['VIEWER_SIGNED'];
    }

    if (!url) {
      throw new Error('Form missing action attribute.');
    }
    if (!st) {
      throw new Error('Something went wrong, security token is unavailable.');
    }

    form.setAttribute('enctype', headers['Content-Type'] = contentType);

    // Info that the proxy endpoint needs.
    var query = {
      'MPFP': 1, // This will force an alternate route in the makeRequest proxy endpoint
      'url': url,
      'httpMethod': 'POST',
      'headers': gadgets.io.encodeValues(headers, false),
      'authz': auth || '',
      'st': st,
      'contentType': params['CONTENT_TYPE'] || 'TEXT',
      'signOwner': signOwner,
      'signViewer': signViewer,
      // should we bypass gadget spec cache (e.g. to read OAuth provider URLs)
      'bypassSpecCache': gadgets.util.getUrlParameters()['nocache'] || '',
      'getFullHeaders': !!params['GET_FULL_HEADERS']
    };

    delete params['OAUTH_RECEIVED_CALLBACK'];
    // OAuth goodies
    if (auth === 'oauth' || auth === 'signed' || auth === 'oauth2') {
      // Just copy the OAuth parameters into the req to the server
      for (var opt in params) {
        if (params.hasOwnProperty(opt)) {
          if (opt.indexOf('OAUTH_') === 0 || opt === 'code') {
            query[opt] = params[opt];
          }
        }
      }
    }

    var proxyUrl = config['jsonProxyUrl'].replace('%host%', document.location.host)
      + '?' + gadgets.io.encodeValues(query);

    if (window.FormData) {
      var xhr = new XMLHttpRequest(),
          data = formdata || new FormData(form);

      if (xhr.upload) {
        xhr.upload.onprogress = function(event) {
          onprogress.call(null, event, xhr.abort);
        };
      }
      xhr.onreadystatechange = gadgets.util.makeClosure(
        null, gadgets.io.processResponse_, url, onresult, params, xhr
      );
      xhr.open("POST", proxyUrl);
      xhr.send(data);
    } else {
      // IE and FF3.6 only
      proxyUrl += '&iframe=1';
      form.setAttribute('action', proxyUrl);
      form.setAttribute('target', getIFrame().name);
      form.setAttribute('method', 'POST');

      // This transport can only support 1 request at a time, so we serialize
      // them.
      var job = {
        form: form,
        onresult: onresult,
        params: params,
        url: url
      };
      if (work) {
        workQ.push(job);
      } else {
        work = job;
        form.submit();
      }
    }
  };

})();