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
import org.apache.shindig.social.opensocial.spi.SocialSpiException;
import org.apache.shindig.social.core.util.ContainerConf;

import com.google.common.collect.ImmutableSet;

import java.util.Collection;
import java.util.Set;
import java.util.concurrent.Future;

public abstract class DataRequestHandler {

  private static final Set<String> GET_SYNONYMS = ImmutableSet.of("get");
  private static final Set<String> CREATE_SYNONYMS = ImmutableSet.of("put", "create");
  private static final Set<String> UPDATE_SYNONYMS = ImmutableSet.of("post", "update");
  private static final Set<String> DELETE_SYNONYMS = ImmutableSet.of("delete");
  protected ContainerConf containerConf;
  
  /**
   * 
   */
  
  public DataRequestHandler(ContainerConf containerConf) {
    this.containerConf = containerConf;
  }
  
  public Future<?> handleItem(RequestItem request) {
    if (request.getOperation() == null) {
      return ImmediateFuture.errorInstance(new SocialSpiException(ResponseError.NOT_IMPLEMENTED,
          "Unserviced operation"));
    }
    String operation = request.getOperation().toLowerCase();
    Future<?> responseItem;
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
        throw new SocialSpiException(ResponseError.NOT_IMPLEMENTED,
            "Unserviced operation " + operation);
      }
    } catch (SocialSpiException spe) {
      return ImmediateFuture.errorInstance(spe);
    } catch (Throwable t) {
      return ImmediateFuture.errorInstance(new SocialSpiException(ResponseError.INTERNAL_ERROR,
          "Unknown error " + t.getMessage(), t));
    }
    return responseItem;
  }

  protected abstract Future<?> handleDelete(RequestItem request)
      throws SocialSpiException;

  protected abstract Future<?> handlePut(RequestItem request)
      throws SocialSpiException;

  protected abstract Future<?> handlePost(RequestItem request)
      throws SocialSpiException;

  protected abstract Future<?> handleGet(RequestItem request)
      throws SocialSpiException;

  /**
   * Utility class for common API call preconditions
   */
  public static class Preconditions {

    public static void requireNotEmpty(Collection<?> coll, String message)
        throws SocialSpiException {
      if (coll.isEmpty()) {
        throw new SocialSpiException(ResponseError.BAD_REQUEST, message);
      }
    }

    public static void requireEmpty(Collection<?> list, String message) throws SocialSpiException {
      if (!list.isEmpty()) {
        throw new SocialSpiException(ResponseError.BAD_REQUEST, message);
      }
    }

    public static void requireSingular(Collection<?> coll, String message)
        throws SocialSpiException {
      if (coll.size() != 1) {
        throw new SocialSpiException(ResponseError.BAD_REQUEST, message);
      }
    }

    public static void requirePlural(Collection<?> coll, String message) throws SocialSpiException {
      if (coll.size() <= 1) {
        throw new SocialSpiException(ResponseError.BAD_REQUEST, message);
      }
    }
  }
}
