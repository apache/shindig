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

import org.apache.shindig.protocol.HandlerPreconditions;
import org.apache.shindig.protocol.Operation;
import org.apache.shindig.protocol.ProtocolException;
import org.apache.shindig.protocol.Service;
import org.apache.shindig.social.opensocial.model.Message;
import org.apache.shindig.social.opensocial.model.MessageCollection;
import org.apache.shindig.social.opensocial.spi.CollectionOptionsFactory;
import org.apache.shindig.social.opensocial.spi.CollectionOptions;
import org.apache.shindig.social.opensocial.spi.MessageService;
import org.apache.shindig.social.opensocial.spi.UserId;

import java.util.List;
import java.util.Set;
import java.util.concurrent.Future;

import javax.servlet.http.HttpServletResponse;

import com.google.inject.Inject;

/**
 * RPC/REST handler for all Messages requests
 */
@Service(name = "messages", path="/{userId}+/{msgCollId}/{messageIds}+")
public class MessageHandler {

  private final MessageService service;
  private final CollectionOptionsFactory collectionOptionsFactory;

  @Inject
  public MessageHandler(MessageService service, CollectionOptionsFactory collectionOptionsFactory) {
    this.service = service;
    this.collectionOptionsFactory = collectionOptionsFactory;
  }

  @Operation(httpMethods = "DELETE")
  public Future<?> delete(SocialRequestItem request) throws ProtocolException {

    Set<UserId> userIds = request.getUsers();
    String msgCollId = request.getParameter("msgCollId");
    List<String> messageIds = request.getListParameter("messageIds");

    HandlerPreconditions.requireNotEmpty(userIds, "No userId specified");
    HandlerPreconditions.requireSingular(userIds, "Multiple userIds not supported");

    if (msgCollId == null) {
      throw new ProtocolException(HttpServletResponse.SC_BAD_REQUEST,
          "A message collection is required");
    }

    UserId user = request.getUsers().iterator().next();

    if (messageIds == null || messageIds.isEmpty()) {
      // MessageIds may be null if the complete collection should be deleted
      return service.deleteMessageCollection(user, msgCollId, request.getToken());
    }
    // Delete specific messages
    return service.deleteMessages(user, msgCollId, messageIds, request.getToken());
  }


  @Operation(httpMethods = "GET")
  public Future<?> get(SocialRequestItem request) throws ProtocolException {

    Set<UserId> userIds = request.getUsers();
    String msgCollId = request.getParameter("msgCollId");
    List<String> messageIds = request.getListParameter("messageIds");

    CollectionOptions options = collectionOptionsFactory.create(request);

    HandlerPreconditions.requireNotEmpty(userIds, "No userId specified");
    HandlerPreconditions.requireSingular(userIds, "Multiple userIds not supported");

    UserId user = request.getUsers().iterator().next();

    if (msgCollId == null) {
      // No message collection specified, return list of message collections
      Set<String> fields = request.getFields(MessageCollection.Field.ALL_FIELDS);
      return service.getMessageCollections(user, fields, options, request.getToken());
    }
    // If messageIds are specified return them, otherwise return entries in the given collection.
    Set<String> fields = request.getFields(Message.Field.ALL_FIELDS);
    return service.getMessages(user, msgCollId, fields, messageIds, options, request.getToken());
  }

  /**
   * Creates a new message collection or message
   */
  @Operation(httpMethods = "POST", bodyParam = "entity")
  public Future<?> create(SocialRequestItem request) throws ProtocolException {

    Set<UserId> userIds = request.getUsers();
    String msgCollId = request.getParameter("msgCollId");
    List<String> messageIds = request.getListParameter("messageIds");

    HandlerPreconditions.requireNotEmpty(userIds, "No userId specified");
    HandlerPreconditions.requireSingular(userIds, "Multiple userIds not supported");

    UserId user = request.getUsers().iterator().next();


    if (msgCollId == null) {
      // Request to create a new message collection
      MessageCollection msgCollection = request.getTypedParameter("entity", MessageCollection.class);

      return service.createMessageCollection(user, msgCollection, request.getToken());
    }

    // A message collection has been specified, allow for posting

    HandlerPreconditions.requireEmpty(messageIds,"Message IDs not allowed here, use PUT instead");

    Message message = request.getTypedParameter("entity", Message.class);
    HandlerPreconditions.requireNotEmpty(message.getRecipients(), "No recipients specified");

    return service.createMessage(userIds.iterator().next(), request.getAppId(), msgCollId, message,
        request.getToken());
  }

  /**
   * Handles modifying a message or a message collection.
   */
  @Operation(httpMethods = "PUT", bodyParam = "entity")
  public Future<?> modify(SocialRequestItem request) throws ProtocolException {

    Set<UserId> userIds = request.getUsers();
    String msgCollId = request.getParameter("msgCollId");
    List<String> messageIds = request.getListParameter("messageIds");

    HandlerPreconditions.requireNotEmpty(userIds, "No userId specified");
    HandlerPreconditions.requireSingular(userIds, "Multiple userIds not supported");

    UserId user = request.getUsers().iterator().next();

    if (msgCollId == null) {
      throw new ProtocolException(HttpServletResponse.SC_BAD_REQUEST,
          "A message collection is required");
    }

    if (messageIds.isEmpty()) {
      // No message IDs specified, this is a PUT to a message collection
      MessageCollection msgCollection = request.getTypedParameter("entity", MessageCollection.class);
      if (msgCollection == null) {
        throw new ProtocolException(HttpServletResponse.SC_BAD_REQUEST,
            "cannot parse message collection");
      }

      // TODO, do more validation.

      return service.modifyMessageCollection(user, msgCollection, request.getToken());
    }

    HandlerPreconditions.requireSingular(messageIds, "Only one messageId at a time");

    Message message = request.getTypedParameter("entity", Message.class);
    // TODO, do more validation.

    if (message == null || message.getId() == null) {
      throw new ProtocolException(HttpServletResponse.SC_BAD_REQUEST,
        "cannot parse message or missing ID");
    }

    return service.modifyMessage(user, msgCollId, messageIds.get(0), message, request.getToken());
  }

}
