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

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.shindig.auth.SecurityToken;
import org.apache.shindig.common.util.ImmediateFuture;
import org.apache.shindig.protocol.conversion.BeanConverter;
import org.apache.shindig.protocol.conversion.BeanJsonConverter;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Provider;
import com.google.inject.name.Named;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.Reader;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Default implementation of HandlerRegistry. Bind to appropriately
 * annotated handlers.
 */
public class DefaultHandlerRegistry implements HandlerRegistry {

  private static final Logger logger = Logger.getLogger(DefaultHandlerRegistry.class.getName());

  // Map service - > method -> { handler, ...}
  private final Map<String, Map<String, SortedSet<RestPath>>> serviceMethodPathMap = Maps.newHashMap();
  private final Map<String, RpcInvocationHandler> rpcOperations = Maps.newHashMap();

  private final Injector injector;
  private final BeanJsonConverter beanJsonConverter;
  private final HandlerExecutionListener executionListener;

  /**
   * Creates a dispatcher with the specified handler classes
   *
   * @param injector Used to create instance if handler is a Class
   * @param handlers List of handler instances or classes.
   */
  @Inject
  public DefaultHandlerRegistry(Injector injector,
                                @Named("org.apache.shindig.handlers")Set<Object> handlers,
                                BeanJsonConverter beanJsonConverter,
                                HandlerExecutionListener executionListener) {
    this.injector = injector;
    this.beanJsonConverter = beanJsonConverter;
    this.executionListener = executionListener;
    addHandlers(handlers.toArray());
  }

  /**
   * Get an RPC handler
   */
  public RpcHandler getRpcHandler(JSONObject rpc) {
    try {
      String key = rpc.getString("method");
      RpcInvocationHandler rpcHandler = rpcOperations.get(key);
      if (rpcHandler == null) {
        return new ErrorRpcHandler(new ProtocolException(ResponseError.NOT_IMPLEMENTED,
            "The method " + key + " is not implemented"));
      }
      return new RpcInvocationWrapper(rpcHandler, rpc);
    } catch (JSONException je) {
      return new ErrorRpcHandler(new ProtocolException(ResponseError.BAD_REQUEST,
          "No method requested in RPC"));
    }
  }

  /**
   * Get a REST request handler
   */
  public RestHandler getRestHandler(String path, String method) {
    method = method.toUpperCase();
    if (path.startsWith("/")) {
      path = path.substring(1);
    }
    String[] pathParts = path.split("/");
    Map<String, SortedSet<RestPath>> methods = serviceMethodPathMap.get(pathParts[0]);
    if (methods != null) {
      SortedSet<RestPath> paths = methods.get(method);
      for (RestPath restPath : paths) {
        RestHandler handler = restPath.accept(pathParts);
        if (handler != null) {
          return handler;
        }
      }
    }
    return new ErrorRestHandler(new ProtocolException(ResponseError.NOT_IMPLEMENTED,
        "No service defined for path " + path));
  }

  public Set<String> getSupportedRestServices() {
    Set<String> result = Sets.newTreeSet();
    for (Map<String, SortedSet<RestPath>> methods : serviceMethodPathMap.values()) {
      for (String method : methods.keySet()) {
        for (RestPath path : methods.get(method)) {
          result.add(method + " " + path.operationPath);
        }
      }
    }
    return Collections.unmodifiableSet(result);
  }

  public Set<String> getSupportedRpcServices() {
    return Collections.unmodifiableSet(rpcOperations.keySet());
  }

  /**
   * Adds a custom handler.
   */
  public void addHandlers(Object... handlers) {
    for (final Object handler : handlers) {
      Class<?> handlerType;
      Provider<?> handlerProvider;
      if (handler instanceof Class) {
        handlerType = (Class<?>) handler;
        handlerProvider = injector.getProvider(handlerType);
      } else {
        handlerType = handler.getClass();
        handlerProvider = new Provider<Object>() {
          public Object get() {
            return handler;
          }
        };
      }
      if (!handlerType.isAnnotationPresent(Service.class)) {
        throw new IllegalStateException("Attempt to bind unannotated service implementation " +
            handlerType.getName());
      }
      Service service = (Service) handlerType.getAnnotation(Service.class);

      for (Method m : handlerType.getMethods()) {
        if (m.isAnnotationPresent(Operation.class)) {
          Operation op = m.getAnnotation(Operation.class);
          createRpcHandler(handlerProvider, service, op, m);
          createRestHandler(handlerProvider, service, op, m);
        }
      }
    }
  }

  private void createRestHandler(Provider<?> handlerProvider,
      Service service, Operation op, Method m) {
    // Check for request item subclass constructor
    Class<?> requestItemType = m.getParameterTypes()[0];
    try {
      if (RequestItem.class.isAssignableFrom(requestItemType)) {
        if (requestItemType == RequestItem.class) {
          requestItemType = BaseRequestItem.class;
        }
        Constructor<?> reqItemConstructor =
            requestItemType.getConstructor(Map.class, SecurityToken.class, BeanConverter.class,
                BeanJsonConverter.class);
        String opName = m.getName();
        if (!StringUtils.isEmpty(op.name())) {
          opName = op.name();
        }
        RestInvocationHandler restHandler = new RestInvocationHandler(service, op, m,
            handlerProvider, beanJsonConverter, reqItemConstructor,
            new ExecutionListenerWrapper(service.name(), opName, executionListener));
        String serviceName = service.name();

        Map<String, SortedSet<RestPath>> methods = serviceMethodPathMap.get(serviceName);
        if (methods == null) {
          methods = Maps.newHashMap();
          serviceMethodPathMap.put(serviceName, methods);
        }

        for (String httpMethod : op.httpMethods()) {
          if (!StringUtils.isEmpty(httpMethod)) {
            httpMethod = httpMethod.toUpperCase();
            SortedSet<RestPath> sortedSet = methods.get(httpMethod);
            if (sortedSet == null) {
              sortedSet = Sets.newTreeSet();
              methods.put(httpMethod, sortedSet);
            }

            if (StringUtils.isEmpty(op.path())) {
              sortedSet.add(new RestPath("/" + serviceName +  service.path(), restHandler));
            } else {
              // Use the standard service name and constant prefix as the key
              sortedSet.add(new RestPath("/" + serviceName + op.path(), restHandler));
            }
          }
        }
      }
    } catch (NoSuchMethodException nme) {
      logger.log(Level.INFO, "No REST binding for " + service.name() + "." + m.getName());
    }

  }

  private void createRpcHandler(Provider<?> handlerProvider,
      Service service, Operation op, Method m) {
    // Check for request item subclass constructor
    Class<?> requestItemType = m.getParameterTypes()[0];
    try {
      if (RequestItem.class.isAssignableFrom(requestItemType)) {
        if (requestItemType == RequestItem.class) {
          requestItemType = BaseRequestItem.class;
        }
        Constructor<?> reqItemConstructor =
            requestItemType.getConstructor(JSONObject.class, SecurityToken.class,
                BeanConverter.class,
                BeanJsonConverter.class);
        String opName = m.getName();
        // Use the override if its defined
        if (op.name().length() > 0) {
          opName = op.name();
        }
        RpcInvocationHandler rpcHandler =
            new RpcInvocationHandler(m, handlerProvider, beanJsonConverter, reqItemConstructor,
                new ExecutionListenerWrapper(service.name(), opName, executionListener));
        rpcOperations.put(service.name() + "." + opName, rpcHandler);
      }
    } catch (NoSuchMethodException nme) {
      logger.log(Level.INFO, "No RPC binding for " + service.name() + "." + m.getName());
    }
  }

  /**
   * Utility wrapper for the HandlerExecutionListener
   */
  private static class ExecutionListenerWrapper {
    final String service;
    final String operation;
    final HandlerExecutionListener listener;

    ExecutionListenerWrapper(String service, String operation,
        HandlerExecutionListener listener) {
      this.service = service;
      this.operation = operation;
      this.listener = listener;
    }

    private void executing(RequestItem req) {
      listener.executing(service, operation, req);
    }
  }

  /**
   * Proxy binding for an RPC operation. We allow binding to methods that
   * return non-Future types by wrapping them in ImmediateFuture.
   */
  static final class RpcInvocationHandler  {
    private Method receiver;
    Provider<?> handlerProvider;
    BeanJsonConverter beanJsonConverter;
    Constructor<?> requestItemConstructor;
    ExecutionListenerWrapper listener;

    private RpcInvocationHandler(Method receiver,
                                 Provider<?> handlerProvider,
                                 BeanJsonConverter beanJsonConverter,
                                 Constructor<?> reqItemConstructor,
                                 ExecutionListenerWrapper listener) {
      this.receiver = receiver;
      this.handlerProvider = handlerProvider;
      this.beanJsonConverter = beanJsonConverter;
      this.requestItemConstructor = reqItemConstructor;
      this.listener = listener;
    }

    public Future<?> execute(JSONObject rpc, SecurityToken token, BeanConverter converter) {
      try {
        RequestItem item;
        if (rpc.has("params")) {
          item = (RequestItem) requestItemConstructor.newInstance(
              (JSONObject) rpc.get("params"), token, converter, beanJsonConverter);
        } else {
          item = (RequestItem) requestItemConstructor.newInstance(new JSONObject(), token,
              converter, beanJsonConverter);
        }
        listener.executing(item);
        Object result = receiver.invoke(handlerProvider.get(), item);
        if (result instanceof Future) {
          return (Future<?>) result;
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
   * Encapsulate the dispatch of a single RPC
   */
  static final class RpcInvocationWrapper implements RpcHandler {

    final RpcInvocationHandler handler;
    final JSONObject rpc;

    RpcInvocationWrapper(RpcInvocationHandler handler, JSONObject rpc) {
      this.handler = handler;
      this.rpc = rpc;
    }

    public Future<?> execute(SecurityToken st, BeanConverter converter) {
      return handler.execute(rpc, st, converter);
    }
  }

  /**
   * Proxy binding for a REST operation. We allow binding to methods that
   * return non-Future types by wrapping them in ImmediateFuture.
   */
  static final class RestInvocationHandler  {
    final Method receiver;
    final Provider<?> handlerProvider;
    final Service service;
    final Operation operation;
    final String[] expectedUrl;
    final BeanJsonConverter beanJsonConverter;
    final Constructor<?> requestItemConstructor;
    final ExecutionListenerWrapper listener;

    private RestInvocationHandler(Service service,
        Operation operation,
        Method receiver,
        Provider<?> handlerProvider,
        BeanJsonConverter beanJsonConverter,
        Constructor<?> requestItemConstructor,
        ExecutionListenerWrapper listener) {
      this.service = service;
      this.operation = operation;
      this.receiver = receiver;
      this.handlerProvider = handlerProvider;
      expectedUrl = service.path().split("/");
      this.beanJsonConverter = beanJsonConverter;
      this.requestItemConstructor = requestItemConstructor;
      this.listener = listener;
    }

    public Future<?> execute(Map<String, String[]> parameters, Reader body,
                             SecurityToken token, BeanConverter converter) {
      try {
        // bind the body contents if available
        if (body != null) {
          parameters.put(operation.bodyParam(), new String[]{IOUtils.toString(body)});
        }
        RequestItem item = (RequestItem) requestItemConstructor.newInstance(parameters, token,
            converter, beanJsonConverter);
        listener.executing(item);
        Object result = receiver.invoke(handlerProvider.get(), item);
        if (result instanceof Future) {
          return (Future<?>) result;
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
   * Encapsulate the dispatch of a single REST call.
   * Augment the executed parameters with those extracted from the path
   */
  static class RestInvocationWrapper implements RestHandler {
    RestInvocationHandler handler;
    Map<String, String[]> pathParams;

    RestInvocationWrapper(Map<String, String[]> pathParams, RestInvocationHandler handler) {
      this.pathParams = pathParams;
      this.handler = handler;
    }

    public Future<?> execute(Map<String, String[]> parameters, Reader body,
                             SecurityToken token, BeanConverter converter) {
      pathParams.putAll(parameters);
      return handler.execute(pathParams, body, token, converter);
    }
  }


  /**
   * Standard REST handler to wrap errors
   */
  private static final class ErrorRestHandler implements RestHandler {

    private ProtocolException error;

    public ErrorRestHandler(ProtocolException error) {
      this.error = error;
    }

    public Future<?> execute(Map<String, String[]> parameters, Reader body,
                          SecurityToken token, BeanConverter converter) {
      return ImmediateFuture.errorInstance(error);
    }
  }

  /**
   * Standard RPC handler to wrap errors
   */
  private static final class ErrorRpcHandler implements RpcHandler {

    private ProtocolException error;

    public ErrorRpcHandler(ProtocolException error) {
      this.error = error;
    }

    public Future<?> execute(SecurityToken token, BeanConverter converter) {
      return ImmediateFuture.errorInstance(error);
    }
  }

  /**
   * Path matching and parameter extraction for REST.
   */
  static class RestPath implements Comparable<RestPath> {

    enum PartType {
      CONST, SINGULAR_PARAM, PLURAL_PARAM
    }

    class Part {
      String partName;
      PartType type;
      Part(String partName, PartType type) {
        this.partName = partName;
        this.type = type;
      }
    }

    final  String operationPath;
    final  List<Part> parts;
    final RestInvocationHandler handler;
    final int constCount;
    final int lastConstIndex;

    public RestPath(String path, RestInvocationHandler handler) {
      int tmpConstCount = 0;
      int tmpConstIndex = -1;
      this.operationPath = path;
      String[] partArr = path.substring(1).split("/");
      parts = Lists.newArrayList();
      for (int i = 0; i < partArr.length; i++) {
        String part = partArr[i];
        if (part.startsWith("{")) {
          if (part.endsWith("}+")) {
            parts.add(new Part(part.substring(1, part.length() - 2), PartType.PLURAL_PARAM));
          } else if (part.endsWith("}")) {
            parts.add(new Part(part.substring(1, part.length() - 1), PartType.SINGULAR_PARAM));
          } else {
            throw new IllegalStateException("Invalid REST path part format " + part);
          }
        } else {
          parts.add(new Part(part, PartType.CONST));
          tmpConstCount++;
          tmpConstIndex = i;
        }
      }
      constCount = tmpConstCount;
      lastConstIndex = tmpConstIndex;
      this.handler = handler;
    }

    /**
     * See if this Rest path is a match for the requested path
     * Requested path is offset by 1 as it includes service name
     * @return A handler with the path parameters decoded, null if not a match for the path
     */
    public RestInvocationWrapper accept(String[] requestPathParts) {
      if (constCount > 0) {
        if (lastConstIndex >= requestPathParts.length) {
          // Last required constant match is not possible with
          // this request
          return null;
        }
        for (int i = 0; i <= lastConstIndex; i++) {
          if (parts.get(i).type == PartType.CONST &&
              !parts.get(i).partName.equals(requestPathParts[i])) {
            // Constant part does not match request
            return null;
          }
        }
      }

      // All constant parts matched, extract the parameters
      Map<String, String[]> parsedParams = Maps.newHashMap();
      for (int i = 0; i < Math.min(requestPathParts.length, parts.size()); i++) {
        if (parts.get(i).type == PartType.SINGULAR_PARAM) {
          if (requestPathParts[i].indexOf(',') != -1) {
            throw new ProtocolException(ResponseError.BAD_REQUEST,
                "Cannot expect plural value " + requestPathParts[i]
                    + " for singular field " + parts.get(i) + " for path " + operationPath);
          }
          parsedParams.put(parts.get(i).partName, new String[]{requestPathParts[i]});
        } else if (parts.get(i).type == PartType.PLURAL_PARAM) {
          parsedParams.put(parts.get(i).partName, requestPathParts[i].split(","));
        }
      }
      return new RestInvocationWrapper(parsedParams, handler);
    }

    /**
     * Rank based on the number of consant parts they accept, where the constant parts occur
     * and lexical ordering.
     */
    public int compareTo(RestPath other) {
      // Rank first by number of constant elements in the path
      int result = other.constCount - this.constCount;
      if (result == 0) {
        // Rank second by the position of the last constant element
        // (lower index is better)
        result = this.lastConstIndex - other.lastConstIndex;
      }
      if (result == 0) {
        // Rank lastly by lexical order
        result = this.operationPath.compareTo(other.operationPath);
      }
      return result;
    }
  }
}
