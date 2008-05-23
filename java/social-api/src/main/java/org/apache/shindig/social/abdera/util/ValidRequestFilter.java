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
package org.apache.shindig.social.abdera.util;

import org.apache.abdera.protocol.server.Filter;
import org.apache.abdera.protocol.server.FilterChain;
import org.apache.abdera.protocol.server.ProviderHelper;
import org.apache.abdera.protocol.server.RequestContext;
import org.apache.abdera.protocol.server.ResponseContext;


public class ValidRequestFilter implements Filter {
  public final static String FORMAT_FIELD = "format";
  private final static String INVALID_FORMAT =
    "Invalid format parameter. only atom/json are supported";

  public enum Format {
    JSON("json"), ATOM("atom");

    private final String displayValue;

    private Format(String displayValue) {
      this.displayValue = displayValue;
    }

    public String getDisplayValue() {
      return displayValue;
    }
  }

  public ResponseContext filter(RequestContext request, FilterChain chain) {
    Format format = getFormatTypeFromRequest(request);
    if (format == null) {
      return ProviderHelper.badrequest(request, INVALID_FORMAT);
    }
    request.setAttribute(FORMAT_FIELD, format);
    return chain.next(request);
  }

  /**
   * Returns the format (jsoc or atom) from the RequestContext's URL parameters.
   *
   * @param request Abdera's RequestContext
   * @return The format and default to Format.JSON.
   */
  public static Format getFormatTypeFromRequest(RequestContext request) {
    // TODO: should gracefully handle introspection if format param is missing.
    String format = request.getTarget().getParameter(FORMAT_FIELD);

    if (format == null || format.equals(Format.JSON.getDisplayValue())) {
      return Format.JSON;
    } else if (format.equals(Format.ATOM.getDisplayValue())) {
      return Format.ATOM;
    } else {
      return null;
    }
  }
}
