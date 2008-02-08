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

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Handles RPC metadata requests.
 */
public class RpcServlet extends HttpServlet {
  private CrossServletState state;
  private static final int MAX_REQUEST_SIZE = 1024 * 128;
  private static final Logger logger
      = Logger.getLogger("org.apache.shindig.gadgets");

  @Override
  protected void doPost(HttpServletRequest request,
      HttpServletResponse response) throws IOException {
    int length = request.getContentLength();
    if (length <= 0) {
      logger.info("No Content-Length specified.");
      response.setStatus(HttpServletResponse.SC_LENGTH_REQUIRED);
      return;
    }
    if (length > MAX_REQUEST_SIZE) {
      logger.info("Request size too large: " + length);
      response.setStatus(HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE);
      return;
    }

    ServletInputStream reader = request.getInputStream();
    ByteArrayOutputStream os = new ByteArrayOutputStream();
    byte[] buf = new byte[1024 * 8];
    int read = 0;

    while ((read = reader.read(buf, 0, buf.length)) > 0) {
      os.write(buf, 0, read);
      if (os.size() > length) {
        // Bad request, we're leaving now.
        logger.info("Wrong size. Length: " + length + " real: " + os.size());
        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        return;
      }
    }

    try {
      String encoding = request.getCharacterEncoding();
      if (encoding == null) {
        encoding = "UTF-8";
      }
      String postBody = new String(os.toByteArray(), encoding);
      JsonRpcRequest req = new JsonRpcRequest(postBody);
      JSONObject out = req.process(state);
      response.setStatus(HttpServletResponse.SC_OK);
      response.getWriter().write(out.toString());
    } catch (UnsupportedEncodingException e) {
      response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
      response.getWriter().write("Unsupported input character set");
      logger.log(Level.INFO, e.getMessage(), e);
    } catch (RpcException e) {
      response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
      response.getWriter().write(e.getMessage());
      logger.log(Level.INFO, e.getMessage(), e);
    }
  }

  @Override
  public void init(ServletConfig config) throws ServletException {
    state = CrossServletState.get(config);
  }
}
