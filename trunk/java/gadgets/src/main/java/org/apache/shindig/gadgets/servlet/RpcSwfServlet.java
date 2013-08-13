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
package org.apache.shindig.gadgets.servlet;

import com.google.inject.Inject;
import com.google.inject.name.Named;

import org.apache.commons.io.IOUtils;
import org.apache.shindig.common.servlet.HttpUtil;
import org.apache.shindig.common.util.HashUtil;
import org.apache.shindig.common.util.ResourceLoader;
import org.apache.shindig.gadgets.uri.UriCommon;

import java.io.IOException;
import java.io.InputStream;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Trivial servlet that does precisely one thing: serves the SWF needed for gadgets.rpc's
 * Flash-based transport.
 */
public class RpcSwfServlet extends HttpServlet {
  private static final String SWF_RESOURCE_NAME = "files/xpc.swf";
  private static final int ONE_YEAR_IN_SEC = 365 * 24 * 60 * 60;
  private static final int DEFAULT_SWF_TTL = 24 * 60 * 60;

  private final byte[] swfBytes;
  private final String hash;
  private int defaultSwfTtl = DEFAULT_SWF_TTL;

  public RpcSwfServlet() {
    this(SWF_RESOURCE_NAME);
  }

  public RpcSwfServlet(String swfResource) {
    try {
      InputStream is = ResourceLoader.openResource(swfResource);
      if (is == null) {
        throw new RuntimeException("Failed to locate Flash SWF");
      }
      this.swfBytes = IOUtils.toByteArray(is);
      this.hash = HashUtil.checksum(swfBytes);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Inject(optional = true)
  public void setDefaultRpcSwfTtl(@Named("shindig.rpc.swf.defaultTtl") Integer defaultTtl) {
    defaultSwfTtl = defaultTtl;
  }

  public String getSwfHash() {
    return hash;
  }

  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
    resp.setStatus(HttpServletResponse.SC_OK);

    // Similar versioning method to other APIs, implemented more compactly.
    String v = req.getParameter(UriCommon.Param.VERSION.getKey());
    if (v != null && v.equals(hash)) {
      HttpUtil.setCachingHeaders(resp, ONE_YEAR_IN_SEC, true);
    } else {
      HttpUtil.setCachingHeaders(resp, defaultSwfTtl, true);
    }

    resp.setHeader("Content-Type", "application/x-shockwave-flash");

    resp.getOutputStream().write(swfBytes);
  }
}
