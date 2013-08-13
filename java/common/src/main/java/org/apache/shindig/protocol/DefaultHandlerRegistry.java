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

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.shindig.auth.SecurityToken;
import org.apache.shindig.protocol.conversion.BeanConverter;
import org.apache.shindig.protocol.conversion.BeanJsonConverter;
import org.apache.shindig.protocol.multipart.FormDataItem;

import com.google.common.base.Objects;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.Futures;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Provider;

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

import javax.servlet.http.HttpServletResponse;

/**
 * Default implementation of HandlerRegistry. Bind to appropriately
 * annotated handlers.
 */
public class DefaultHandlerRegistry implements HandlerRegistry {

  private static final Logger LOG = Logger.getLogger(DefaultHandlerRegistry.class.getName());

  // Map service - > method -> { handler, ...}
  private final Map<String, Map<String, SortedSet<RestPath>>> serviceMethodPathMap =
      Maps.newHashMap();
  private final Map<String, RpcInvocationHandler> rpcOperations = Maps.newHashMap();

  private final Injector injector;
  private final BeanJsonConverter beanJsonConverter;
  private final HandlerExecutionListener executionListener;

  /**
   * Creates a dispatcher with the specified handler classes
   *
   * @param injector Used to create instance if handler is a Class
   */
  @Inject
  public DefaultHandlerRegistry(Injector injector,
                                BeanJsonConverter beanJsonConverter,
                                HandlerExecutionListener executionListener) {
    this.injector = injector;
    this.beanJsonConverter = beanJsonConverter;
    this.executionListener = executionListener;
  }

  /**
   * Add handlers to the registry
   * @param handlers
   */
  public void addHandlers(Set<Object> handlers) {
    for (final Object handler : handlers) {
      Class<?> handlerType;
      Provider<?> handlerProvider;
      if (handler instanceof Class<?>) {
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
      Preconditions.checkState(handlerType.isAnnotationPresent(Service.class),
          "Attempt to bind unannotated service implementation %s",handlerType.getName());

      Service service = handlerType.getAnnotation(Service.class);

      for (Method m : handlerType.getMethods()) {
        if (m.isAnnotationPresent(Operation.class)) {
          Operation op = m.getAnnotation(Operation.class);
          createRpcHandler(handlerProvider, service, op, m);
          createRestHandler(handlerProvider, service, op, m);
        }
      }
    }
  }

  /**
   * Get an RPC handler
   */
  public RpcHandler getRpcHandler(JSONObject rpc) {
    try {
      String key = rpc.getString("method");
      RpcInvocationHandler rpcHandler = rpcOperations.get(key);
      if (rpcHandler == null) {
        return new ErrorRpcHandler(new ProtocolException(HttpServletResponse.SC_NOT_IMPLEMENTED,
            "The method " + key + " is not implemented"));
      }
      return new RpcInvocationWrapper(rpcHandler, rpc);
    } catch (JSONException je) {
      return new ErrorRpcHandler(new ProtocolException(HttpServletResponse.SC_BAD_REQUEST,
          "No method requested in RPC"));
    }
  }

  /**
   * Get a REST request handler
   */
  public RestHandler getRestHandler(String path, String method) {
    method = method.toUpperCase();
    if (path != null) {
      if (path.startsWith("/")) {
        path = path.substring(1);
      }
      String[] pathParts = StringUtils.splitPreserveAllTokens(path, '/');
      Map<String, SortedSet<RestPath>> methods = serviceMethodPathMap.get(pathParts[0]);
      if (methods != null) {
        SortedSet<RestPath> paths = methods.get(method);
        if (paths != null) {
          for (RestPath restPath : paths) {
            RestHandler handler = restPath.accept(pathParts);
            if (handler != null) {
              return handler;
            }
          }
        }
      }
    }
    return new ErrorRestHandler(new ProtocolException(HttpServletResponse.SC_NOT_IMPLEMENTED,
        "No service defined for path " + path));
  }

  public Set<String> getSupportedRestServices() {
    Set<String> result = Sets.newTreeSet();
    for (Map<String, SortedSet<RestPath>> methods : serviceMethodPathMap.values()) {
      for (Map.Entry<String, SortedSet<RestPath>> method : methods.entrySet()) {
        for (RestPath path : method.getValue()) {
          result.add(method.getKey() + ' ' + path.operationPath);
        }
      }
    }
    return Collections.unmodifiableSet(result);
  }

  public Set<String> getSupportedRpcServices() {
    return Collections.unmodifiableSet(rpcOperations.keySet());
  }

  private void createRestHandler(Provider<?> handlerProvider,
      Service service, Operation op, Method m) {
    try {
      MethodCaller methodCaller = new MethodCaller(m, true);
      String opName = m.getName();
      if (!Strings.isNullOrEmpty(op.name())) {
        opName = op.name();
      }
      RestInvocationHandler restHandler = new RestInvocationHandler(op, methodCaller,
          handlerProvider, beanJsonConverter,
          new ExecutionListenerWrapper(service.name(), opName, executionListener));
      String serviceName = service.name();

      Map<String, SortedSet<RestPath>> methods = serviceMethodPathMap.get(serviceName);
      if (methods == null) {
        methods = Maps.newHashMap();
        serviceMethodPathMap.put(serviceName, methods);
      }

      for (String httpMethod : op.httpMethods()) {
        if (!Strings.isNullOrEmpty(httpMethod)) {
          httpMethod = httpMethod.toUpperCase();
          SortedSet<RestPath> sortedSet = methods.get(httpMethod);
          if (sortedSet == null) {
            sortedSet = Sets.newTreeSet();
            methods.put(httpMethod, sortedSet);
          }

          if (Strings.isNullOrEmpty(op.path())) {
            sortedSet.add(new RestPath('/' + serviceName +  service.path(), restHandler));
          } else {
            // Use the standard service name and constant prefix as the key
            sortedSet.add(new RestPath('/' + serviceName + op.path(), restHandler));
          }
        }
      }
    } catch (NoSuchMethodException nme) {
      LOG.log(Level.INFO, "No REST binding for " + service.name() + '.' + m.getName());
    }

  }

  private void createRpcHandler(Provider<?> handlerProvider, Service service,
      Operation op, Method m) {
    try {
      MethodCaller methodCaller = new MethodCaller(m, false);

      String opName = m.getName();
      // Use the override if its defined
      if (op.name().length() > 0) {
        opName = op.name();
      }
      RpcInvocationHandler rpcHandler =
          new RpcInvocationHandler(methodCaller, handlerProvider, beanJsonConverter,
              new ExecutionListenerWrapper(service.name(), opName, executionListener));
      rpcOperations.put(service.name() + '.' + opName, rpcHandler);
    } catch (NoSuchMethodException nme) {
      LOG.log(Level.INFO, "No RPC binding for " + service.name() + '.' + m.getName());
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

    private void executed(RequestItem req) {
      listener.executed(service, operation, req);
    }
  }


  /**
   * Proxy binding for an RPC operation. We allow binding to methods that
   * return non-Future types by wrapping them in ImmediateFuture.
   */
  static final class RpcInvocationHandler  {

    private Provider<?> handlerProvider;
    private BeanJsonConverter beanJsonConverter;
    private ExecutionListenerWrapper listener;
    private MethodCaller methodCaller;

    private RpcInvocationHandler(MethodCaller methodCaller,
                                 Provider<?> handlerProvider,
                                 BeanJsonConverter beanJsonConverter,
                                 ExecutionListenerWrapper listener) {
      this.handlerProvider = handlerProvider;
      this.beanJsonConverter = beanJsonConverter;
      this.listener = listener;
      this.methodCaller = methodCaller;
    }

    public Future<?> execute(JSONObject rpc, Map<String, FormDataItem> formItems,
        SecurityToken token, BeanConverter converter) {
      RequestItem item;
      try {
        JSONObject params = rpc.has("params") ? (JSONObject)rpc.get("params") : new JSONObject();
        item = methodCaller.getRpcRequestItem(params, formItems, token, beanJsonConverter);
      } catch (Exception e) {
        return Futures.immediateFailedFuture(e);
      }

      try {
        listener.executing(item);
        return methodCaller.call(handlerProvider.get(), item);
      } catch (Exception e) {
        return Futures.immediateFailedFuture(e);
      } finally {
        listener.executed(item);
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

    public Future<?> execute(Map<String, FormDataItem> formItems, SecurityToken st,
        BeanConverter converter) {
      return handler.execute(rpc, formItems, st, converter);
    }
  }

  /**
   * Proxy binding for a REST operation. We allow binding to methods that
   * return non-Future types by wrapping them in ImmediateFuture.
   */
  static final class RestInvocationHandler  {
    final Provider<?> handlerProvider;
    final Operation operation;
    final BeanJsonConverter beanJsonConverter;
    final ExecutionListenerWrapper listener;
    final MethodCaller methodCaller;

    private RestInvocationHandler(Operation operation,
        MethodCaller methodCaller,
        Provider<?> handlerProvider,
        BeanJsonConverter beanJsonConverter,
        ExecutionListenerWrapper listener) {
      this.operation = operation;
      this.handlerProvider = handlerProvider;
      this.beanJsonConverter = beanJsonConverter;
      this.listener = listener;
      this.methodCaller = methodCaller;
    }

    public Future<?> execute(Map<String, String[]> parameters, Reader body,
                             SecurityToken token, BeanConverter converter) {

      RequestItem item;
      try {
        // bind the body contents if available
        if (body != null) {
          parameters.put(operation.bodyParam(), new String[]{IOUtils.toString(body)});
        }
        item = methodCaller.getRestRequestItem(parameters, token, converter, beanJsonConverter);
      } catch (Exception e) {
        return Futures.immediateFailedFuture(e);
      }

      try {
        listener.executing(item);
        return methodCaller.call(handlerProvider.get(), item);
      } catch (Exception e) {
        return Futures.immediateFailedFuture(e);
      } finally {
        listener.executed(item);
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
   * Calls methods annotated with {@link Operation} and appropriately translates
   * RequestItem to the actual input class of the method.
   */
  private static class MethodCaller {
    /** Type of object to create for this method, or null if takes no args */
    private Class<?> inputClass;

    /** Constructors for request item class that will be used */
    private final Constructor<?> restRequestItemConstructor;
    private final Constructor<?> rpcRequestItemConstructor;

    /** The method */
    private final Method method;

    /**
     * Create information needed to call a method
     * @param method The method
     * @param isRest True if REST method (affects which RequestItem constructor to return)
     * @throws NoSuchMethodException
     */
    public MethodCaller(Method method, boolean isRest) throws NoSuchMethodException {
      this.method = method;

      inputClass = method.getParameterTypes().length > 0 ? method.getParameterTypes()[0] : null;

      // Methods that need RequestItem interface should automatically get a BaseRequestItem
      if (RequestItem.class.equals(inputClass)) {
        inputClass = BaseRequestItem.class;
      }
      boolean inputIsRequestItem = (inputClass != null) &&
          RequestItem.class.isAssignableFrom(inputClass);

      Class<?> requestItemType = inputIsRequestItem ? inputClass : BaseRequestItem.class;

      restRequestItemConstructor = requestItemType.getConstructor(Map.class,
          SecurityToken.class, BeanConverter.class, BeanJsonConverter.class);
      rpcRequestItemConstructor = requestItemType.getConstructor(JSONObject.class,
          Map.class, SecurityToken.class, BeanConverter.class, BeanJsonConverter.class);
    }

    public RequestItem getRestRequestItem(Map<String, String[]> params, SecurityToken token,
        BeanConverter converter, BeanJsonConverter jsonConverter) {
      return getRequestItem(params, token, converter, jsonConverter, restRequestItemConstructor);
    }

    public RequestItem getRpcRequestItem(JSONObject params, Map<String, FormDataItem> formItems,
        SecurityToken token, BeanJsonConverter converter) {
      return getRequestItem(params, formItems, token, converter, converter, rpcRequestItemConstructor);
    }

    private RequestItem getRequestItem(Object params, Map<String, FormDataItem> formItems,
        SecurityToken token, BeanConverter converter, BeanJsonConverter jsonConverter,
        Constructor<?> constructor) {
      try {
        return (RequestItem) constructor.newInstance(params, formItems, token,  converter,
            jsonConverter);
      } catch (InstantiationException e) {
        throw new RuntimeException(e);
      } catch (IllegalAccessException e) {
        throw new RuntimeException(e);
      } catch (InvocationTargetException e) {
        throw new RuntimeException(e);
      }
    }

    private RequestItem getRequestItem(Object params, SecurityToken token, BeanConverter converter,
        BeanJsonConverter jsonConverter, Constructor<?> constructor) {
      try {
        return (RequestItem) constructor.newInstance(params, token,  converter, jsonConverter);
      } catch (InstantiationException e) {
        throw new RuntimeException(e);
      } catch (IllegalAccessException e) {
        throw new RuntimeException(e);
      } catch (InvocationTargetException e) {
        throw new RuntimeException(e);
      }
    }

    public Future<?> call(Object handler, RequestItem item) {
      try {
        Object result;
        if (inputClass == null) {
          result = method.invoke(handler);
        } else if (RequestItem.class.isAssignableFrom(inputClass)) {
          result = method.invoke(handler, item);
        } else {
          result = method.invoke(handler, item.getTypedRequest(inputClass));
        }

        if (result instanceof Future<?>) {
          return (Future<?>) result;
        }
        return Futures.immediateFuture(result);
      } catch (IllegalAccessException e) {
        return Futures.immediateFailedFuture(e);
      } catch (InvocationTargetException e) {
        // Unwrap the internal exception
        return Futures.immediateFailedFuture(e.getTargetException());
      }
    }
  }

  /**
   * Standard REST handler to wrap errors
   */
  private static final class ErrorRestHandler implements RestHandler {

    private final ProtocolException error;

    public ErrorRestHandler(ProtocolException error) {
      this.error = error;
    }

    public Future<?> execute(Map<String, String[]> parameters, Reader body,
                          SecurityToken token, BeanConverter converter) {
      return Futures.immediateFailedFuture(error);
    }
  }

  /**
   * Standard RPC handler to wrap errors
   */
  private static final class ErrorRpcHandler implements RpcHandler {

    private final ProtocolException error;

    public ErrorRpcHandler(ProtocolException error) {
      this.error = error;
    }

    public Future<?> execute(Map<String, FormDataItem> formItems, SecurityToken token,
        BeanConverter converter) {
      return Futures.immediateFailedFuture(error);
    }
  }

  /**
   * Path matching and parameter extraction for REST.
   */
  static class RestPath implements Comparable<RestPath> {

    enum PartType {
      CONST, SINGULAR_PARAM, PLURAL_PARAM
    }

    static class Part {
      String partName;
      PartType type;
      Part(String partName, PartType type) {
        this.partName = partName;
        this.type = type;
      }
    }

    final String operationPath;
    final List<Part> parts;
    final RestInvocationHandler handler;
    final int constCount;
    final int lastConstIndex;

    public RestPath(String path, RestInvocationHandler handler) {
      int tmpConstCount = 0;
      int tmpConstIndex = -1;
      this.operationPath = path;
      String[] partArr = StringUtils.split(path.substring(1), '/');
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
            throw new ProtocolException(HttpServletResponse.SC_BAD_REQUEST,
                "Cannot expect plural value " + requestPathParts[i]
                    + " for singular field " + parts.get(i) + " for path " + operationPath);
          }
          parsedParams.put(parts.get(i).partName, new String[]{requestPathParts[i]});
        } else if (parts.get(i).type == PartType.PLURAL_PARAM) {
          parsedParams.put(parts.get(i).partName, StringUtils.splitPreserveAllTokens(requestPathParts[i], ','));
        }
      }
      return new RestInvocationWrapper(parsedParams, handler);
    }

    @Override
    public boolean equals(Object other) {
      if (other instanceof RestPath) {
        RestPath that = (RestPath)other;
        return (this.constCount == that.constCount &&
            this.lastConstIndex == that.lastConstIndex &&
            Objects.equal(this.operationPath, that.operationPath));
      }
      return false;
    }

    @Override
    public int hashCode() {
      return this.constCount ^ this.lastConstIndex ^ operationPath.hashCode();
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
