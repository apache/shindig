/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.shindig.gadgets.http;

import org.apache.shindig.gadgets.JsLibrary;

import java.io.IOException;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Simple servlet serving up JavaScript files by their registered aliases.
 * Used by type=URL gadgets in loading JavaScript resources.
 */
public class JsServlet extends HttpServlet {
  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp)
      throws IOException {
    // Use the last component as filename; prefix is ignored
    String path = req.getServletPath();
    String[] pathComponents = path.split("/");
    String resourceName = pathComponents[pathComponents.length-1];
    if (resourceName.endsWith(".js")) {
      // Lop off the suffix for lookup purposes
      resourceName = resourceName.substring(
          0, resourceName.length() - ".js".length());
    }
    
    String containerParam = req.getParameter("c");
    boolean isContainer =
        (containerParam != null && containerParam.equals("1"));
    String jsData = null;
    if (isContainer) {
      jsData = JsLibrary.getContainerJs(resourceName);
    } else {
      jsData = JsLibrary.getGadgetJs(resourceName);
    }
    
    if (jsData == null) {
      resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
      return;
    }
    
    resp.setContentType("text/javascript");
    resp.setContentLength(jsData.length());
    resp.getOutputStream().write(jsData.getBytes());
  }
}
