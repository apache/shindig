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
package org.apache.shindig.protocol;

import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.Futures;


import org.junit.Ignore;

import java.util.Map;
import java.util.concurrent.Future;

import javax.servlet.http.HttpServletResponse;

/**
 * Simple test handler implementation. Can be used standalone or to wrap a mock
 * delegate.
 */
@Ignore
@Service(name = "test", path = "/{someParam}/{someOtherParam}" )
public class TestHandler {

  public static final String GET_RESPONSE = "GET_RESPONSE";
  public static final String CREATE_RESPONSE = "CREATE_RESPONSE";
  public static final String FAILURE_MESSAGE = "FAILURE_MESSAGE";
  public static final String ECHO_PREFIX = "ECHO: ";
  public static final String NO_ARG_RESPONSE = "No arguments from me!";

  public static Map<String,String> REST_RESULTS = ImmutableMap.of(
      "POST", CREATE_RESPONSE, "GET", GET_RESPONSE, "DELETE", FAILURE_MESSAGE);

  private TestHandler mock;

  public TestHandler() {
  }

  public void setMock(TestHandler mock) {
    this.mock = mock;
  }

  @Operation(httpMethods = "GET")
  public Object get(RequestItem req) {
    if (mock != null) {
      return mock.get(req);
    }
    return GET_RESPONSE;
  }

  @Operation(httpMethods = "GET", path = "/overridden/method")
  public Object overridden(RequestItem req) {
    if (mock != null) {
      return mock.get(req);
    }
    return GET_RESPONSE;
  }

  @Operation(name="override.rpcname", httpMethods = "")
  public Object overriddenRpc(RequestItem req) {
    if (mock != null) {
      return mock.get(req);
    }
    return GET_RESPONSE;
  }

  @Operation(httpMethods = {"POST", "PUT"})
  public Future<?> create(RequestItem req) {
    if (mock != null) {
      return mock.create(req);
    }
    return Futures.immediateFuture(CREATE_RESPONSE);
  }

  @Operation(httpMethods = "DELETE")
  public Future<?> futureException(RequestItem req) {
    if (mock != null) {
      return mock.futureException(req);
    }
    return Futures.immediateFailedFuture(new ProtocolException(HttpServletResponse.SC_BAD_REQUEST,
        FAILURE_MESSAGE, new Throwable()));
  }

  @Operation(httpMethods = {})
  public void exception(RequestItem req) {
    if (mock != null) {
      mock.exception(req);
    } else {
      throw new NullPointerException(FAILURE_MESSAGE);
    }
  }

  @Operation(httpMethods = "GET", path = "/echo")
  public String echo(Input input) {
    return ECHO_PREFIX + input.value;
  }

  @Ignore
  public static class Input {
    public String value;
    public void setValue(String value) { this.value = value; }
  }

  @Operation(httpMethods = "")
  public String noArg() {
    return NO_ARG_RESPONSE;
  }
}
