/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.apache.shindig.gadgets.servlet;

import com.google.inject.Inject;
import com.google.inject.name.Named;

import org.apache.commons.io.IOUtils;
import org.apache.shindig.gadgets.DefaultGadgetSpecFactory;
import org.apache.shindig.gadgets.GadgetContext;
import org.apache.shindig.gadgets.GadgetException;
import org.apache.shindig.gadgets.GadgetException.Code;
import org.apache.shindig.gadgets.http.HttpRequest;
import org.apache.shindig.gadgets.http.HttpResponse;
import org.apache.shindig.gadgets.http.RequestPipeline;

import java.io.IOException;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;

/**
 * Html pages acceleration servlet.
 * It uses the rewriter pipeline to improve speed of loading html.
 * Internally it create a fake gadget spec and uses the Gadget rendering servlet.
 * Only html content is being rewritten, other content is passed as is.
 * (It uses the rawxml tag to pass spec, so require DefaultGadgetSpecFactory)
 */
public class HtmlAccelServlet extends GadgetRenderingServlet {

  public static final String URL_PARAM_NAME = "url";
  /**
   * Use ACCEL_GADGET parameter to check for AccelServlet during rendering.
   */
  public static final String ACCEL_GADGET_PARAM_NAME = "accelGadget";
  /** 
   * Use the next value with '==' operation when checking for ACCEL_GADGET,
   * That will prevent spoofing it using url params.
   */
  public static final String ACCEL_GADGET_PARAM_VALUE = "true";
  public static final String CONTENT_TYPE = "Content-Type";
  public static final String HTML_CONTENT = "text/html";
  public static final String CONTENT_ENCODING = "Content-Encoding";
  public static final String CONTENT_LENGTH = "Content-Length";

  /** Fake spec to wrap the html data */
  private static final String FAKE_SPEC_TPL = 
      "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
      + "<Module>\n"
      + "  <ModulePrefs title=\"Apache Shindig Accelerator\"\n"
      + "               author=\"Apache Shindig\"\n"
      + "               author_email=\"no-reply@gmail.com\">\n"
      + "    <Require feature=\"core.none\"/>\n"
      + "    <Optional feature=\"content-rewrite\">\n"
      + "      <Param name=\"include-urls\">.*</Param>\n"
      + "    </Optional>\n"
      + "  </ModulePrefs>\n"
      + "  <Content type=\"html\">\n"
      + "    <![CDATA[%s]]>\n" 
      + "  </Content>\n"
      + "</Module>\n";

  private RequestPipeline requestPipeline;
  private Map<String, String> addedServletParams = null;
  
  @Inject
  public void setRequestPipeline(RequestPipeline pipeline) {
    this.requestPipeline = pipeline;
  }
  
  @Inject(optional = true)
  public void setAddedServletParams(
      @Named("shindig.accelerate.added-params") Map<String, String> params) {
    this.addedServletParams = params;
  }
  
  public static boolean isAccel(GadgetContext context) {
    return context.getParameter(HtmlAccelServlet.ACCEL_GADGET_PARAM_NAME) ==
      HtmlAccelServlet.ACCEL_GADGET_PARAM_VALUE;
  }

  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp)
      throws IOException {

    // Get data to accelerate:
    HttpResponse data;
    HttpGadgetContext context = new HttpGadgetContext(req);
    try {
      data = fetch(context);
    } catch (GadgetException e) {
      resp.sendError(HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
      return;
    }
    
    // No such page:
    if (data == null) {
      resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Error fetching data");
      return;
    }
    
    // If not html, just return data
    if (!isHtmlContent(data)) {
      respondVerbatim(data, resp);
      return;
    }
    
    // For html, use the gadgetServlet for rewrite
    String content = data.getResponseAsString();

    // Create fake spec wrapper for the html:
    // (Note that the content can be big so don't use the limited String.Format)
    final String spec = createFakeSpec(content);
    
    // wrap the request with the added params
    HttpServletRequestWrapper reqWrapper = new HttpServletRequestWrapper(req) {
      @Override
      public String getParameter(String name) {
        // Mark this as an accelerate page spec
        // (The code that check for that field have to use the defined constant,
        //  hence the use of == )
        if (name == ACCEL_GADGET_PARAM_NAME) {
          return ACCEL_GADGET_PARAM_VALUE;
        }
        // Pass the spec using rawxml (require DefaultGadgetSpecFactory):
        if (name == DefaultGadgetSpecFactory.RAW_GADGETSPEC_XML_PARAM_NAME) {
          return spec;
        }
        // Allow overriding extra params (i.e. container)
        if (addedServletParams != null && addedServletParams.containsKey(name)) {
          return addedServletParams.get(name);
        }
        return super.getParameter(name);
      }
    };
    
    // Call the gadget renderer to rewrite content:
    super.doGet(reqWrapper,resp);
  }

  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
    doGet(req,resp);
  }
  
  protected boolean isHtmlContent(HttpResponse data) {
    return data.getHeader(CONTENT_TYPE) != null &&
           data.getHeader(CONTENT_TYPE).contains(HTML_CONTENT);
  }

  private HttpResponse fetch(HttpGadgetContext context) throws GadgetException {

    if (context.getUrl() == null) {
      throw new GadgetException(Code.INVALID_PARAMETER, "Missing url paramater");
    }

    HttpRequest request = new HttpRequest(context.getUrl())
        .setIgnoreCache(context.getIgnoreCache())
        .setContainer(context.getContainer());

    HttpResponse results = requestPipeline.execute(request);
    return results;
  }
  
  private void respondVerbatim(HttpResponse results, HttpServletResponse response) 
      throws IOException {
    
    for (Map.Entry<String, String> entry : results.getHeaders().entries()) {
      // Encoding such as gzip was already stripped from data
      if (!CONTENT_ENCODING.equals(entry.getKey()) &&
          !CONTENT_LENGTH.equals(entry.getKey())) {
        response.setHeader(entry.getKey(), entry.getValue());
      }
    }
    response.setStatus(results.getHttpStatusCode());
    IOUtils.copy(results.getResponse(), response.getOutputStream());
  }

  private String createFakeSpec(String content) {
    // need to rescape CDATA: ( each "]]>" and "<![CDATA[" should be "]]><![CDATA[")
    // TODO: just xml escape this and remove the CDATA section
    String data = content.replace("]]>", "<![CDATA[")
      .replace("<![CDATA[", "]]><![CDATA[");
    return String.format(FAKE_SPEC_TPL, data);   
  }
}
