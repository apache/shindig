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

import org.apache.shindig.config.ContainerConfig;
import org.apache.shindig.protocol.HandlerPreconditions;
import org.apache.shindig.protocol.Operation;
import org.apache.shindig.protocol.ProtocolException;
import org.apache.shindig.protocol.RequestItem;
import org.apache.shindig.protocol.Service;
import org.apache.shindig.social.opensocial.model.MediaItem;
import org.apache.shindig.social.opensocial.spi.CollectionOptionsFactory;
import org.apache.shindig.social.opensocial.spi.MediaItemService;
import org.apache.shindig.social.opensocial.spi.UserId;

import com.google.common.base.Objects;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.inject.Inject;

/**
 * Receives and delegates requests to the OpenSocial MediaItems service.
 *
 * @since 2.0.0
 */
@Service(name = "mediaItems", path = "/{userId}+/{groupId}/{albumId}/{id}+")
public class MediaItemHandler {


  private final MediaItemService service;
  private final ContainerConfig config;
  private final CollectionOptionsFactory collectionOptionsFactory;

  @Inject
  public MediaItemHandler(
      MediaItemService service, ContainerConfig config,
      CollectionOptionsFactory collectionOptionsFactory) {
    this.service = service;
    this.config = config;
    this.collectionOptionsFactory = collectionOptionsFactory;
  }

  /*
    * Handles GET operations.
    *
    * Allowed end-points: /mediaItems/{userId}+/{groupId}/{albumId}/{id}+
    *
    * Examples: /mediaItems/john.doe/@self
    *           /mediaItems/john.doe,jane.doe/@self
    *           /mediaItems/john.doe/@self/album123
    *           /mediaItems/john.doe/@self/album123/1,2,3
    */
  @Operation(httpMethods = "GET")
  public Future<?> get(SocialRequestItem request) throws ProtocolException {
    // Get user, group, album IDs, and MediaItem IDs
    Set<UserId> userIds = request.getUsers();
    Set<String> optionalAlbumIds = ImmutableSet.copyOf(request.getListParameter("albumId"));
    Set<String> optionalMediaItemIds = ImmutableSet.copyOf(getRequestMediaItemIds(request));

    // At least one userId must be specified
    HandlerPreconditions.requireNotEmpty(userIds, "No user ID specified");

    // Get Album ID; null if not provided
    String albumId = null;
    if (optionalAlbumIds.size() == 1) {
      albumId = Iterables.getOnlyElement(optionalAlbumIds);
    } else if (optionalAlbumIds.size() > 1) {
      throw new IllegalArgumentException("Multiple Album IDs not supported");
    }

    // Cannot retrieve by ID if album ID not provided
    if (albumId == null && !optionalMediaItemIds.isEmpty()) {
      throw new IllegalArgumentException("Cannot fetch by MediaItem ID without Album ID");
    }

    // Cannot retrieve by ID or album if multiple user's given
    if (userIds.size() > 1) {
      if (!optionalMediaItemIds.isEmpty()) {
        throw new IllegalArgumentException("Cannot fetch MediaItem by ID for multiple users");
      } else if (albumId != null) {
        throw new IllegalArgumentException("Cannot fetch MediaItem by Album for multiple users");
      }
    }

    // Retrieve by ID(s)
    if (!optionalMediaItemIds.isEmpty()) {
      if (optionalMediaItemIds.size() == 1) {
        return service.getMediaItem(Iterables.getOnlyElement(userIds),
            request.getAppId(), albumId,
            Iterables.getOnlyElement(optionalMediaItemIds),
            request.getFields(), request.getToken());
      } else {
        return service.getMediaItems(Iterables.getOnlyElement(userIds),
            request.getAppId(), albumId, optionalMediaItemIds,
            request.getFields(), collectionOptionsFactory.create(request),
            request.getToken());
      }
    }

    // Retrieve by Album
    if (albumId != null) {
      return service.getMediaItems(Iterables.getOnlyElement(userIds),
          request.getAppId(), albumId, request.getFields(),
          collectionOptionsFactory.create(request), request.getToken());
    }

    // Retrieve by users and groups
    return service.getMediaItems(userIds, request.getGroup(), request
        .getAppId(), request.getFields(),
        collectionOptionsFactory.create(request), request.getToken());
  }

  /*
    * Handles DELETE operations.
    *
    * Allowed end-points: /mediaItem/{userId}/@self/{albumId}/{id}
    *
    * Examples: /mediaItems/john.doe/@self/1/2
    */
  @Operation(httpMethods = "DELETE")
  public Future<?> delete(SocialRequestItem request) throws ProtocolException {
    // Get users, Album ID, and MediaItem ID
    Set<UserId> userIds = request.getUsers();
    Set<String> albumIds = ImmutableSet.copyOf(request.getListParameter("albumId"));
    Set<String> mediaItemIds = ImmutableSet.copyOf(getRequestMediaItemIds(request));

    // Exactly one user, Album, and MediaItem must be specified
    HandlerPreconditions.requireNotEmpty(userIds, "No userId specified");
    HandlerPreconditions.requireSingular(userIds, "Exactly one user ID must be specified");
    HandlerPreconditions.requireSingular(albumIds, "Exactly one Album ID must be specified");
    HandlerPreconditions.requireSingular(mediaItemIds, "Exactly one MediaItem ID must be specified");

    // Service request
    return service.deleteMediaItem(Iterables.getOnlyElement(userIds),
        request.getAppId(), Iterables.getOnlyElement(albumIds),
        Iterables.getOnlyElement(mediaItemIds), request.getToken());
  }

  /*
    * Handles POST operations.
    *
    * Allowed end-points: /mediaItems/{userId}/@self/{albumId}
    *
    * Examples: /mediaItems/john.doe/@self/1
    */
  @Operation(httpMethods = "POST", bodyParam = "data")
  public Future<?> create(SocialRequestItem request) throws ProtocolException {
    // Retrieve userIds and albumIds
    Set<UserId> userIds = request.getUsers();
    Set<String> albumIds = ImmutableSet.copyOf(request.getListParameter("albumId"));

    // Exactly one user and Album must be specified
    HandlerPreconditions.requireNotEmpty(userIds, "No userId specified");
    HandlerPreconditions.requireSingular(userIds, "Exactly one user ID must be specified");
    HandlerPreconditions.requireSingular(albumIds, "Exactly one Album ID must be specified");

    // Service request
    return service.createMediaItem(Iterables.getOnlyElement(userIds),
        request.getAppId(), Iterables.getOnlyElement(albumIds),
        getRequestMediaItem(request),
        request.getToken());
  }

  /*
    * Handles PUT operations.
    *
    * Allowed end-points: /mediaItems/{userId}/@self/{albumId}/{id}
    *
    * Examples: /mediaItems/john.doe/@self/1/2
    */
  @Operation(httpMethods = "PUT", bodyParam = "data")
  public Future<?> update(SocialRequestItem request) throws ProtocolException {
    // Retrieve userIds, albumIds, and mediaItemIds
    Set<UserId> userIds = request.getUsers();
    Set<String> albumIds = ImmutableSet.copyOf(request.getListParameter("albumId"));
    Set<String> mediaItemIds = ImmutableSet.copyOf(getRequestMediaItemIds(request));

    // Exactly one user, Album, and MediaItem must be specified
    HandlerPreconditions.requireNotEmpty(userIds, "No userId specified");
    HandlerPreconditions.requireSingular(userIds, "Exactly one user ID must be specified");
    HandlerPreconditions.requireSingular(albumIds, "Exactly one Album ID must be specified");
    HandlerPreconditions.requireSingular(mediaItemIds, "Exactly one MediaItem ID must be specified");

    // Service request
    return service.updateMediaItem(Iterables.getOnlyElement(userIds),
        request.getAppId(), Iterables.getOnlyElement(albumIds),
        Iterables.getOnlyElement(mediaItemIds),
        getRequestMediaItem(request),
        request.getToken());
  }

  @Operation(httpMethods = "GET", path = "/@supportedFields")
  public List<Object> supportedFields(RequestItem request) {
    // TODO: Would be nice if name in config matched name of service.
    String container = Objects.firstNonNull(request.getToken().getContainer(),
        ContainerConfig.DEFAULT_CONTAINER);
    return config.getList(container,
        "${Cur['gadgets.features'].opensocial.supportedFields.mediaItem}");
  }

  protected List<String> getRequestMediaItemIds(SocialRequestItem request) {
    List<String> ids = request.getListParameter("id");
    if (ids.isEmpty()) {
      ids = request.getListParameter("mediaItemId");
    }
    return ids;
  }

  protected MediaItem getRequestMediaItem(SocialRequestItem request) {
    // 'data' missing is ok, but then 'mediaItem' must exist.
    // 'data' or 'mediaItem' invalid will lead to errors.
    MediaItem result = request.getOptionalTypedParameter("data", MediaItem.class);
    if (result == null) {
      result = request.getTypedParameter("mediaItem", MediaItem.class);
    }
    return result;
  }
}
