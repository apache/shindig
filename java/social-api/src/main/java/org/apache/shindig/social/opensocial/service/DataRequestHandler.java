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
package org.apache.shindig.social.opensocial.service;

import org.apache.shindig.common.util.ImmediateFuture;
import org.apache.shindig.social.ResponseError;
import org.apache.shindig.social.ResponseItem;

import com.google.common.collect.Sets;

import java.util.Collection;
import java.util.Set;
import java.util.concurrent.Future;

public abstract class DataRequestHandler {

  private static final Set<String> GET_SYNONYMS = Sets.newHashSet("get");
  private static final Set<String> CREATE_SYNONYMS = Sets.newHashSet("put", "create");
  private static final Set<String> UPDATE_SYNONYMS = Sets.newHashSet("post", "update");
  private static final Set<String> DELETE_SYNONYMS = Sets.newHashSet("delete", "delete");

  public Future<? extends ResponseItem> handleItem(RequestItem request) {
    if (request.getOperation() == null) {
      return error(ResponseError.NOT_IMPLEMENTED, "Unserviced operation ");
    }
    String operation = request.getOperation().toLowerCase();
    Future<? extends ResponseItem> responseItem;
    try {
      if (GET_SYNONYMS.contains(operation)) {
        responseItem = handleGet(request);
      } else if (UPDATE_SYNONYMS.contains(operation)) {
        responseItem = handlePost(request);
      } else if (CREATE_SYNONYMS.contains(operation)) {
        responseItem = handlePut(request);
      } else if (DELETE_SYNONYMS.contains(operation)) {
        responseItem = handleDelete(request);
      } else {
        return error(ResponseError.NOT_IMPLEMENTED, "Unserviced operation " + operation);
      }
    } catch (IllegalArgumentException iae) {
      // Upconvert IllegalArgumentExceptions to BAD_REQUEST.
      return error(ResponseError.BAD_REQUEST, iae.getMessage());
    } catch (Throwable t) {
      return error(ResponseError.INTERNAL_ERROR, "Unknown error " + t.getMessage());
    }
    return responseItem;
  }

  protected abstract Future<? extends ResponseItem> handleDelete(RequestItem request);

  protected abstract Future<? extends ResponseItem> handlePut(RequestItem request);

  protected abstract Future<? extends ResponseItem> handlePost(RequestItem request);

  protected abstract Future<? extends ResponseItem> handleGet(RequestItem request);

  /**
   * Create standard error messages as futures
   */
  protected Future<? extends ResponseItem> error(ResponseError type, String message) {
    return ImmediateFuture.newInstance(new ResponseItem(type, message));
  }

  /**
   * Utility class for common API call preconditions
   */
  public static class Preconditions {

    public static void requireNotEmpty(Collection<?> coll, String message) {
      if (coll.isEmpty()) {
        throw new IllegalArgumentException(message);
      }
    }

    public static void requireEmpty(Collection<?> list, String message) {
      if (!list.isEmpty()) {
        throw new IllegalArgumentException(message);
      }
    }

    public static void requireSingular(Collection<?> coll, String message) {
      if (coll.size() != 1) {
        throw new IllegalArgumentException(message);
      }
    }

    public static void requirePlural(Collection<?> coll, String message) {
      if (coll.size() <= 1) {
        throw new IllegalArgumentException(message);
      }
    }
  }
}
