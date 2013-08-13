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

import java.util.List;
import java.util.Set;
import java.util.concurrent.Future;

import com.google.common.base.Objects;
import org.apache.shindig.config.ContainerConfig;
import org.apache.shindig.protocol.HandlerPreconditions;
import org.apache.shindig.protocol.Operation;
import org.apache.shindig.protocol.ProtocolException;
import org.apache.shindig.protocol.RequestItem;
import org.apache.shindig.protocol.Service;
import org.apache.shindig.social.opensocial.model.Album;
import org.apache.shindig.social.opensocial.spi.AlbumService;
import org.apache.shindig.social.opensocial.spi.CollectionOptions;
import org.apache.shindig.social.opensocial.spi.UserId;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.inject.Inject;

/**
 * Receives and delegates requests to the OpenSocial Album service.
 *
 * @since 2.0.0
 */
@Service(name = "albums", path = "/{userId}+/{groupId}/{albumId}+")
public class AlbumHandler {

  private final AlbumService service;
  private final ContainerConfig config;

  @Inject
  public AlbumHandler(AlbumService service, ContainerConfig config) {
    this.service = service;
    this.config = config;
  }

  /*
    * Handles create operations.
    *
    * Allowed end-points: /albums/{userId}/@self
    *
    * Examples: /albums/john.doe/@self
    */
  @Operation(httpMethods = "POST", bodyParam = "album")
  public Future<?> create(SocialRequestItem request) throws ProtocolException {
    // Retrieve userIds and albumIds
    Set<UserId> userIds = request.getUsers();
    List<String> albumIds = request.getListParameter("albumId");

    // Preconditions - exactly one userId specified, no albumIds specified
    HandlerPreconditions.requireNotEmpty(userIds, "No userId specified");
    HandlerPreconditions.requireSingular(userIds, "Multiple userIds not supported");
    HandlerPreconditions.requireEmpty(albumIds, "Cannot specify albumId in create");

    return service.createAlbum(Iterables.getOnlyElement(userIds),
        request.getAppId(),
        request.getTypedParameter("album", Album.class),
        request.getToken());
  }

  /*
    * Handles retrieve operations.
    *
    * Allowed end-points: /albums/{userId}+/{groupId}/{albumId}+
    *
    * Examples: /albums/@me/@self /albums/john.doe/@self/1,2
    * /albums/john.doe,jane.doe/@friends
    */
  @Operation(httpMethods = "GET")
  public Future<?> get(SocialRequestItem request) throws ProtocolException {
    // Get user, group, and album IDs
    Set<UserId> userIds = request.getUsers();
    Set<String> optionalAlbumIds = ImmutableSet.copyOf(request
        .getListParameter("albumId"));

    // At least one userId must be specified
    HandlerPreconditions.requireNotEmpty(userIds, "No userId specified");

    // If multiple userIds specified, albumIds must not be specified
    if (userIds.size() > 1 && !optionalAlbumIds.isEmpty()) {
      throw new IllegalArgumentException("Cannot fetch same albumIds for multiple userIds");
    }

    // Retrieve albums by ID
    if (!optionalAlbumIds.isEmpty()) {
      if (optionalAlbumIds.size() == 1) {
        return service.getAlbum(Iterables.getOnlyElement(userIds),
            request.getAppId(), request.getFields(),
            optionalAlbumIds.iterator().next(), request.getToken());
      } else {
        return service.getAlbums(Iterables.getOnlyElement(userIds),
            request.getAppId(), request.getFields(),
            new CollectionOptions(request), optionalAlbumIds,
            request.getToken());
      }
    }

    // Retrieve albums by group
    return service.getAlbums(userIds, request.getGroup(), request
        .getAppId(), request.getFields(),
        new CollectionOptions(request), request.getToken());
  }

  /*
    * Handles update operations.
    *
    * Allowed end-points: /albums/{userId}/@self/{albumId}
    *
    * Examples: /albums/john.doe/@self/1
    */
  @Operation(httpMethods = "PUT", bodyParam = "album")
  public Future<?> update(SocialRequestItem request) throws ProtocolException {
    // Retrieve userIds and albumIds
    Set<UserId> userIds = request.getUsers();
    List<String> albumIds = request.getListParameter("albumId");

    // Enforce preconditions - exactly one user and one album specified
    HandlerPreconditions.requireNotEmpty(userIds, "No userId specified");
    HandlerPreconditions.requireSingular(userIds, "Multiple userIds not supported");
    HandlerPreconditions.requireNotEmpty(albumIds, "No albumId specified");
    HandlerPreconditions.requireSingular(albumIds, "Multiple albumIds not supported");

    return service.updateAlbum(Iterables.getOnlyElement(userIds),
        request.getAppId(),
        request.getTypedParameter("album", Album.class),
        Iterables.getOnlyElement(albumIds), request.getToken());
  }

  /*
    * Handles delete operations.
    *
    * Allowed end-points: /albums/{userId}/@self/{albumId}
    *
    * Examples: /albums/john.doe/@self/1
    */
  @Operation(httpMethods = "DELETE")
  public Future<?> delete(SocialRequestItem request) throws ProtocolException {
    // Get user and album ID
    Set<UserId> userIds = request.getUsers();
    String albumId = request.getParameter("albumId");

    // Enforce preconditions - userIds must contain exactly one element
    HandlerPreconditions.requireNotEmpty(userIds, "No userId specified");
    HandlerPreconditions.requireSingular(userIds, "Multiple userIds not supported");

    // Service request
    return service.deleteAlbum(Iterables.getOnlyElement(userIds),
        request.getAppId(), albumId, request.getToken());
  }

  /*
    * Retrieves supported fields for the albums service.
    */
  @Operation(httpMethods = "GET", path = "/@supportedFields")
  public List<Object> supportedFields(RequestItem request) {
    String container = Objects.firstNonNull(request.getToken().getContainer(),
        ContainerConfig.DEFAULT_CONTAINER);
    return config.getList(container,
        "${Cur['gadgets.features'].opensocial.supportedFields.album}");
  }
}
