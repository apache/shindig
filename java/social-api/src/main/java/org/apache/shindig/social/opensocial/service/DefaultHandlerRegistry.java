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
package org.apache.shindig.social.opensocial.service;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.shindig.auth.SecurityToken;
import org.apache.shindig.common.util.ImmediateFuture;
import org.apache.shindig.social.ResponseError;
import org.apache.shindig.social.opensocial.spi.SocialSpiException;

import com.google.common.collect.Maps;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.name.Named;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.Reader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Future;

/**
 * Default implementation of HandlerRegistry. Bind to appropriately
 * annotated handlers.
 */
public class DefaultHandlerRegistry implements HandlerRegistry {

  private final Map<String, RestHandler> restOperations = Maps.newHashMap();
  private final Map<String, RpcHandler> rpcOperations = Maps.newHashMap();

  private final Injector injector;

  /**
   * Creates a dispatcher with the specified handler classes
   * @param injector Used to create instance if handler is a Class
   * @param handlers List of handler instances or classes.
   */
  @Inject
  public DefaultHandlerRegistry(Injector injector,
      @Named("org.apache.shindig.handlers") List handlers) {
    this.injector = injector;
    addHandlers(handlers.toArray());
  }

  /**
   * Get an RPC handler
   */
  public RpcHandler getRpcHandler(JSONObject rpc) {
    try {
      String key = rpc.getString("method");
      RpcHandler rpcHandler = rpcOperations.get(key);
      if (rpcHandler == null) {
        return new ErrorRpcHandler(new SocialSpiException(ResponseError.NOT_IMPLEMENTED,
            "The method " + key + " is not implemented"));
      }
      return rpcHandler;
    } catch (JSONException je) {
      return new ErrorRpcHandler(new SocialSpiException(ResponseError.BAD_REQUEST,
            "No method requested in RPC"));
    }
  }

  /**
   * Get a REST request handler
   */
  public RestHandler getRestHandler(String path, String method) {
    method = method.toUpperCase();
    RestHandler restHandler = null;
    String matchPath = path;
    int separatorIndex = matchPath.lastIndexOf("/");
    while (restHandler == null && separatorIndex != -1) {
      restHandler = restOperations.get(method + " " + matchPath);
      matchPath = matchPath.substring(0, separatorIndex);
      separatorIndex = matchPath.lastIndexOf("/");
    }
    if (restHandler == null) {
      return new ErrorRestHandler(new SocialSpiException(ResponseError.NOT_IMPLEMENTED,
            "No service defined for path " + path));
    }
    return restHandler;
  }

  public Set<String> getSupportedRestServices() {
    return Collections.unmodifiableSet(restOperations.keySet());
  }

  public Set<String> getSupportedRpcServices() {
    return Collections.unmodifiableSet(rpcOperations.keySet());
  }

  /**
   * Adds a custom handler.
   */
  public void addHandlers(Object... handlers) {
    for (Object handler : handlers) {
      Class handlerType;
      if (handler instanceof Class) {
        handlerType = (Class) handler;
        handler = injector.getInstance(handlerType);
      } else {
        handlerType = handler.getClass();
      }
      if (!handlerType.isAnnotationPresent(Service.class)) {
        throw new IllegalStateException("Attempt to bind unannotated service implementation " +
            handlerType.getName());
      }
      Service service = (Service) handlerType.getAnnotation(Service.class);
      String serviceName = service.name();

      for (Method m : handlerType.getMethods()) {
        if (m.isAnnotationPresent(Operation.class)) {
          Operation op = m.getAnnotation(Operation.class);
          createRpcHandler(handler, service, m);
          createRestHandler(handler, service, op, m);
        }
      }
    }
  }

  private void createRestHandler(Object handler, Service service, Operation op, Method m) {
    RestHandler restHandler = new RestInvocationHandler(service, op, m, handler);
    String serviceName = service.name();

    for (String httpMethod : op.httpMethods()) {
      if (!StringUtils.isEmpty(httpMethod)) {
        if (StringUtils.isEmpty(op.path())) {
          // Use the standard service name as the key
          restOperations.put(httpMethod.toUpperCase() + " /" + serviceName, restHandler);
        } else {
          // Use the standard service name and constant prefix as the key
          String prefix = op.path().split("\\{")[0];
          restOperations.put(httpMethod.toUpperCase() + " /" + serviceName +
              prefix, restHandler);
        }
      }
    }
  }

  private void createRpcHandler(Object handler, Service service, Method m) {
    RpcHandler rpcHandler = new RpcInvocationHandler(m, handler);
    String defaultName = service.name() + "." + m.getName();
    rpcOperations.put(defaultName, rpcHandler);
  }

  /**
   * Proxy binding for an RPC operation. We allow binding to methods that
   * return non-Future types by wrapping them in ImmediateFuture.
   */
  private static final class RpcInvocationHandler implements RpcHandler {
    private Method receiver;
    Object handlerInstance;

    private RpcInvocationHandler(Method receiver, Object handlerInstance) {
      this.receiver = receiver;
      this.handlerInstance =  handlerInstance;
    }

    public Future execute(JSONObject rpc, SecurityToken token, BeanConverter converter) {
      try {
        RequestItem item;
        if (rpc.has("params")) {
          item = new BaseRequestItem((JSONObject)rpc.get("params"), token, converter);
        } else {
          item = new BaseRequestItem(new JSONObject(), token, converter);
        }
        Object result = receiver.invoke(handlerInstance, item);
        if (result instanceof Future) {
          return (Future)result;
        }
        return ImmediateFuture.newInstance(result);
      } catch (InvocationTargetException ite) {
        // Unwrap these
        return ImmediateFuture.errorInstance(ite.getTargetException());
      } catch (Exception e) {
        return ImmediateFuture.errorInstance(e);
      }
    }
  }

  /**
   * Proxy binding for a REST operation. We allow binding to methods that
   * return non-Future types by wrapping them in ImmediateFuture.
   */
  static final class RestInvocationHandler implements RestHandler {
    Method receiver;
    Object handlerInstance;
    Service service;
    Operation operation;
    final String[] expectedUrl;

    private RestInvocationHandler(Service service, Operation operation,
                                  Method receiver, Object handlerInstance) {
      this.service = service;
      this.operation = operation;
      this.receiver = receiver;
      this.handlerInstance =  handlerInstance;
      expectedUrl = service.path().split("/");
    }

    public Future<?> execute(String path, Map<String, String[]> parameters, Reader body,
                          SecurityToken token, BeanConverter converter) {
      // Create a mutable copy.
      parameters = new HashMap(parameters);
      try {
        // bind the body contents if available
        if (body != null) {
          parameters.put(operation.bodyParam(), new String[]{IOUtils.toString(body)});
        }

        extractPathParameters(parameters, path);

        RequestItem item = new BaseRequestItem(parameters, token, converter);
        Object result = receiver.invoke(handlerInstance, item);
        if (result instanceof Future) {
          return (Future) result;
        }
        return ImmediateFuture.newInstance(result);
      } catch (InvocationTargetException ite) {
        // Unwrap these
        return ImmediateFuture.errorInstance(ite.getTargetException());
      } catch (Exception e) {
        return ImmediateFuture.errorInstance(e);
      }
    }

    private void extractPathParameters(Map<String, String[]> parameters, String path) {
      String[] actualUrl = path.split("/");

      for (int i = 1; i < actualUrl.length; i++) {
        String actualPart = actualUrl[i];
        String expectedPart = expectedUrl[i - 1];
        // Extract named parameters from the path
        if (expectedPart.startsWith("{")) {
          if (expectedPart.endsWith("}+")) {
            // The param can be a repeated field. Use ',' as default separator
            parameters.put(expectedPart.substring(1, expectedPart.length() - 2),
                actualPart.split(","));
          } else {
            if (actualPart.indexOf(',') != -1) {
              throw new SocialSpiException(ResponseError.BAD_REQUEST,
                  "Cannot expect plural value " + actualPart
                      + " for singular field " + expectedPart + " in " + service.path());
            }
            parameters.put(expectedPart.substring(1, expectedPart.length() - 1),
                new String[]{actualPart});
          }
        }
      }
    }
  }


  /**
   * Standard REST handler to wrap errors
   */
  private static final class ErrorRestHandler implements RestHandler {

    private SocialSpiException error;

    public ErrorRestHandler(SocialSpiException error) {
      this.error = error;
    }

    public Future execute(String path, Map parameters, Reader body,
                          SecurityToken token, BeanConverter converter) {
      return ImmediateFuture.errorInstance(error);
    }
  }

  /**
   * Standard RPC handler to wrap errors
   */
  private static final class ErrorRpcHandler implements RpcHandler {

    private SocialSpiException error;

    public ErrorRpcHandler(SocialSpiException error) {
      this.error = error;
    }

    public Future execute(JSONObject rpc, SecurityToken token, BeanConverter converter) {
      return ImmediateFuture.errorInstance(error);
    }
  }
}
