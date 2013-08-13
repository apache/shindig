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
package org.apache.shindig.social.opensocial.spi;

import java.util.Set;
import java.util.concurrent.Future;

import com.google.inject.ImplementedBy;
import org.apache.shindig.auth.SecurityToken;
import org.apache.shindig.protocol.ProtocolException;
import org.apache.shindig.protocol.RestfulCollection;
import org.apache.shindig.social.opensocial.model.Album;

import javax.servlet.http.HttpServletResponse;

/**
 * The AlbumService interface defines the service provider interface for
 * creating, retrieving, updating, and deleting OpenSocial albums.
 *
 * @since 2.0.0
 */
@ImplementedBy(AlbumService.NotImplementedAlbumService.class)
public interface AlbumService {

  /*
    * Retrieves a single album for the given user with the given album ID.
    *
    * @param userId  Identifies the person to retrieve the album from
    * @param appId    Identifies the application to retrieve the album from
    * @param fields  Indicates the fields to return.  Empty set implies all
    * @param albumId  Identifies the album to retrieve
    * @param token    A valid SecurityToken
    *
    * @return a response item with the requested album
    */
  Future<Album> getAlbum(UserId userId, String appId, Set<String> fields,
                         String albumId, SecurityToken token) throws ProtocolException;

  /*
    * Retrieves albums for the given user with the given album IDs.
    *
    * @param userId  Identifies the person to retrieve albums for
    * @param appId    Identifies the application to retrieve albums from
    * @param fields  The fields to return; empty set implies all
    * @param options  The sorting/filtering/pagination options
    * @param albumIds  The set of album ids to fetch
    * @param token    A valid SecurityToken
    *
    * @return a response item with requested albums
    */
  Future<RestfulCollection<Album>> getAlbums(UserId userId, String appId,
                                             Set<String> fields, CollectionOptions options,
                                             Set<String> albumIds, SecurityToken token) throws ProtocolException;

  /*
    * Retrieves albums for the given user and group.
    *
    * @param userIds  Identifies the users to retrieve albums from
    * @param groupId  Identifies the group to retrieve albums from
    * @param appId    Identifies the application to retrieve albums from
    * @param fields   The fields to return.  Empty set implies all
    * @param options  The sorting/filtering/pagination options
    * @param token    A valid SecurityToken
    *
    * @return a response item with the requested albums
    */
  Future<RestfulCollection<Album>> getAlbums(Set<UserId> userIds,
                                             GroupId groupId, String appId, Set<String> fields,
                                             CollectionOptions options, SecurityToken token)
      throws ProtocolException;

  /*
    * Deletes a single album for the given user with the given album ID.
    *
    * @param userId   Identifies the user to delete the album from
    * @param appId    Identifies the application to delete the album from
    * @param albumId  Identifies the album to delete
    * @param token    A valid SecurityToken
    *
    * @return a response item containing any errors
    */
  Future<Void> deleteAlbum(UserId userId, String appId, String albumId,
                           SecurityToken token) throws ProtocolException;

  /*
    * Creates an album for the given user.
    *
    * @param userId   Identifies the user to create the album for
    * @param appId    Identifies the application to create the album in
    * @param album    The album to create
    * @param token    A valid SecurityToken
    *
    * @return a response containing any errors
    */
  Future<Void> createAlbum(UserId userId, String appId, Album album,
                           SecurityToken token) throws ProtocolException;

  /*
    * Updates an album for the given user.  The album ID specified in the REST
    * end-point is used, even if the album also defines an ID.
    *
    * @param userId   Identifies the user to update the album for
    * @param appId    Identifies the application to update the album in
    * @param album    Defines the updated album
    * @param albumId  Identifies the ID of the album to update
    * @param token    A valid SecurityToken
    *
    * @return a response containing any errors
    */
  Future<Void> updateAlbum(UserId userId, String appId, Album album,
                           String albumId, SecurityToken token) throws ProtocolException;

  public static class NotImplementedAlbumService implements AlbumService {
    public Future<Album> getAlbum(UserId userId, String appId, Set<String> fields, String albumId, SecurityToken token) throws ProtocolException {
      throw new ProtocolException(HttpServletResponse.SC_BAD_REQUEST, "Not Implemented");
    }

    public Future<RestfulCollection<Album>> getAlbums(UserId userId, String appId, Set<String> fields, CollectionOptions options, Set<String> albumIds, SecurityToken token) throws ProtocolException {
      throw new ProtocolException(HttpServletResponse.SC_BAD_REQUEST, "Not Implemented");
    }

    public Future<RestfulCollection<Album>> getAlbums(Set<UserId> userIds, GroupId groupId, String appId, Set<String> fields, CollectionOptions options, SecurityToken token) throws ProtocolException {
      throw new ProtocolException(HttpServletResponse.SC_BAD_REQUEST, "Not Implemented");
    }

    public Future<Void> deleteAlbum(UserId userId, String appId, String albumId, SecurityToken token) throws ProtocolException {
      throw new ProtocolException(HttpServletResponse.SC_BAD_REQUEST, "Not Implemented");
    }

    public Future<Void> createAlbum(UserId userId, String appId, Album album, SecurityToken token) throws ProtocolException {
      throw new ProtocolException(HttpServletResponse.SC_BAD_REQUEST, "Not Implemented");
    }

    public Future<Void> updateAlbum(UserId userId, String appId, Album album, String albumId, SecurityToken token) throws ProtocolException {
      throw new ProtocolException(HttpServletResponse.SC_BAD_REQUEST, "Not Implemented");
    }
  }
}
