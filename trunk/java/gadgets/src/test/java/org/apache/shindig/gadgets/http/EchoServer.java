/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.shindig.gadgets.http;

import org.apache.commons.lang3.StringUtils;
import org.mortbay.jetty.servlet.ServletHolder;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * A server that echoes back whatever you send to it.
 */
public class EchoServer extends FakeHttpServer {

  public static final String STATUS_PARAM = "status";
  public static final String BODY_PARAM = "body";
  public static final String HEADER_PARAM = "header";

  @Override
  protected void addServlets() throws Exception {
    ServletHolder servletHolder = new ServletHolder(new EchoServlet());
    context.addServlet(servletHolder, "/*");
  }

  private static class EchoServlet extends HttpServlet {

    protected EchoServlet() {
    }

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp)
        throws ServletException, IOException {
      handleEcho(req, resp);
    }

    private void handleEcho(HttpServletRequest req, HttpServletResponse resp)
        throws IOException {
      int code = HttpServletResponse.SC_OK;
      if (req.getParameter(STATUS_PARAM) != null) {
        code = Integer.parseInt(req.getParameter(STATUS_PARAM));
      }
      resp.setStatus(code);

      String[] headers = req.getParameterValues(HEADER_PARAM);
      if (headers != null) {
        for (String header : headers) {
          String[] nameAndValue = StringUtils.splitPreserveAllTokens(header, "=", 2);
          resp.setHeader(nameAndValue[0], nameAndValue[1]);
        }
      }

      resp.setHeader("X-Method", req.getMethod());

      String body = req.getParameter(BODY_PARAM);
      if (body != null) {
        resp.getWriter().print(body);
      } else {
        resp.setHeader("Content-Type", "application/octet-stream");

        // Read the input stream into memory
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        InputStream is = req.getInputStream();
        byte[] buf = new byte[1024];
        int len;
        while ((len = is.read(buf)) > 0) {
          baos.write(buf, 0, len);
        }

        // Echo the bytes back to the output stream
        OutputStream os = resp.getOutputStream();
        ByteArrayInputStream bais = new ByteArrayInputStream(
            baos.toByteArray());
        while ((len = bais.read(buf)) > 0) {
          os.write(buf, 0, len);
        }
      }
    }
  }

}
