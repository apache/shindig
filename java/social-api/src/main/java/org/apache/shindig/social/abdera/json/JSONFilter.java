/*
* Licensed to the Apache Software Foundation (ASF) under one or more
* contributor license agreements.  The ASF licenses this file to You
* under the Apache License, Version 2.0 (the "License"); you may not
* use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*     http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.  For additional information regarding
* copyright in this work, please see the NOTICE file in the top level
* directory of this distribution.
*/
package org.apache.shindig.social.abdera.json;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;

import org.apache.abdera.Abdera;
import org.apache.abdera.model.Document;
import org.apache.abdera.model.Element;
import org.apache.abdera.protocol.server.Filter;
import org.apache.abdera.protocol.server.FilterChain;
import org.apache.abdera.protocol.server.ProviderHelper;
import org.apache.abdera.protocol.server.RequestContext;
import org.apache.abdera.protocol.server.ResponseContext;
import org.apache.abdera.protocol.server.context.ResponseContextWrapper;
import org.apache.abdera.writer.Writer;

/**
 * TODO: This file is copied and modified from Abdera code as we needed 
 * functionality different from the Abdera Json writer code base.
 * This file definitely needs cleanup and heavy refactoring
 *
 * Filter implementation that will convert an Atom document returned by
 * the server into a JSON document if the request specifies a higher
 * preference value for JSON or explicitly requests JSON by including
 * a format=json querystring parameter
 */
public class JSONFilter
  implements Filter {

  public ResponseContext filter(
    RequestContext request,
    FilterChain chain) {
      ResponseContext resp = chain.next(request);
      String format = request.getParameter("format");
      if (format != null && format.equalsIgnoreCase("atom")) {
        return resp;
      }
      // if there is no content, it could be either due to some error such as
      // 404 or, there is no content to be translated into json. return
      // TODO verify this claim
      if (resp.getContentType() == null) {
        return resp;
      }

      return jsonPreferred(request,resp.getContentType().toString()) ||
        format == null || format.equalsIgnoreCase("json") ?
        new JsonResponseContext(resp,request.getAbdera()) :
        resp;
  }

  private boolean jsonPreferred(RequestContext request, String type) {
    return ProviderHelper.isPreferred(
      request,
      "application/json",
      type);
  }

  private class JsonResponseContext
    extends ResponseContextWrapper {

    private final Abdera abdera;

    public JsonResponseContext(
      ResponseContext response,
      Abdera abdera) {
        super(response);
        setContentType("application/json");
        this.abdera = abdera;
    }

    @Override
    public void writeTo(
      OutputStream out,
      Writer writer) {
      try {
        toJson(out,writer);
      } catch (Exception se) {
        if (se instanceof RuntimeException)
          throw (RuntimeException)se;
        throw new RuntimeException(se);
      }
    }

    @Override
    public void writeTo(
      OutputStream out) {
      try {
        toJson(out,null);
      } catch (Exception se) {
        if (se instanceof RuntimeException)
          throw (RuntimeException)se;
        throw new RuntimeException(se);
      }
    }

    private void toJson(OutputStream aout,Writer writer) throws Exception {
      Document<Element> doc = null;
      try {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        if (writer == null)
          super.writeTo(out);
        else
          super.writeTo(out,writer);
        ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
        doc = abdera.getParser().parse(in);
      } catch (Exception e) {}
      if (doc != null) {
        doc.writeTo("json",aout);
      } else {
        throw new RuntimeException(
          "There was an error serializing the entry to JSON");
      }
    }
  }
}
