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

import com.google.inject.ImplementedBy;

import org.apache.shindig.auth.SecurityToken;
import org.apache.shindig.social.opensocial.model.Message;
import org.apache.shindig.social.opensocial.model.MessageCollection;
import org.apache.shindig.protocol.ProtocolException;
import org.apache.shindig.protocol.RestfulCollection;

import java.util.Set;
import java.util.List;
import java.util.concurrent.Future;

import javax.servlet.http.HttpServletResponse;

/**
 * The MessageService interface defines the service provider interface to post messages to
 * the underlying SNS.
 */
@ImplementedBy(MessageService.NotImplementedMessageService.class)
public interface MessageService {

  /**
   * Returns a list of message collections corresponding to the given user
   *
   * @param userId   The User to fetch for
   * @param fields   The fields to fetch for the message collections
   * @param options  Pagination, etal
   * @param token    Given security token for this request
   * @return a collection of message collections.
   * @throws ProtocolException when invalid parameters are given
   */
  Future<RestfulCollection<MessageCollection>> getMessageCollections(UserId userId,
     Set<String> fields, CollectionOptions options, SecurityToken token) throws ProtocolException;

  /**
   * Creates a new message collection for the given arguments
   *
   * @param userId  The userId to create the message collection for
   * @param msgCollection A message collection that is to be created
   * @param token  A security token for this request
   *
   * @return Data for the message collection that is created
   * @throws ProtocolException when invalid parameters are given or not implemented
   */
  Future<MessageCollection> createMessageCollection(UserId userId, MessageCollection msgCollection, SecurityToken token)  throws ProtocolException;

  /**
   * Modifies/Updates a message collection for the given arguments
   *
   * @param userId  The userId to modify the message collection for
   * @param msgCollection Data for the message collection to be modified
   * @param token  A security token for this request
   *
   * @throws ProtocolException when invalid parameters are given or not implemented
   */

  Future<Void> modifyMessageCollection(UserId userId, MessageCollection msgCollection, SecurityToken token) throws ProtocolException;

  /**
   * Deletes a message collection for the given arguments
   *
   * @param userId  The userId to create the message collection for
   * @param msgCollId Data for the message collection to be modified
   * @param token  A security token for this request
   *
   * @throws ProtocolException when invalid parameters are given, the message collection does not exist or not implemented
   * @return            Future<Void>
   */

  Future<Void> deleteMessageCollection(UserId userId, String msgCollId, SecurityToken token) throws ProtocolException;


  /**
   * Returns a list of messages that correspond to the passed in data
   *
   * @param userId      The User to fetch for
   * @param msgCollId   The message Collection ID to fetch from, default @all
   * @param fields      The fields to fetch for the messages
   * @param msgIds      An explicit set of message ids to fetch
   * @param options     Options to control the fetch
   * @param token       Given security token for this request
   * @return a collection of messages
   * @throws ProtocolException when invalid parameters are given
   */
  Future<RestfulCollection<Message>> getMessages(UserId userId, String msgCollId,
      Set<String> fields, List<String> msgIds, CollectionOptions options, SecurityToken token) throws ProtocolException;


  /**
   * Posts a message to the user's specified message collection, to be sent to the set of recipients specified in
   * the message.
   *
   * @param userId      The user posting the message.
   * @param appId       The app id
   * @param msgCollId   The message collection Id to post to, default @outbox
   * @param message     The message to post
   * @param token       A valid security token @return a response item containing any errors/
   * @return Void Future
   * @throws ProtocolException when invalid parameters are given
   */
  Future<Void> createMessage(UserId userId, String appId, String msgCollId, Message message,
                             SecurityToken token) throws ProtocolException;

  /**
   * Deletes a set of messages for a given user/message collection
   * @param userId      The User to delete for
   * @param msgCollId   The Message Collection ID to delete from, default @all
   * @param ids         List of IDs to delete
   * @param token       Given Security Token for this request
   * @return            Future<Void>
   * @throws ProtocolException
   */
  Future<Void> deleteMessages(UserId userId, String msgCollId, List<String> ids,
      SecurityToken token) throws ProtocolException;


  /**
   * Modifies/Updates a specific message with new data
   * @param userId      The User to modify for
   * @param msgCollId   The Message Collection ID to modify from, default @all
   * @param messageId   The messageId to modify
   * @param message     The message details to modify
   * @param token       Given Security Token for this request
   * @return            Future<Void>
   * @throws ProtocolException for invalid parameters or missing messages or users
   */

  Future<Void> modifyMessage(UserId userId, String msgCollId, String messageId, Message message, SecurityToken token)
      throws ProtocolException;

  public static class NotImplementedMessageService implements MessageService {
    public Future<RestfulCollection<MessageCollection>> getMessageCollections(UserId userId,
       Set<String> fields, CollectionOptions options, SecurityToken token) throws ProtocolException {
      throw new ProtocolException(HttpServletResponse.SC_BAD_REQUEST, "Not Implemented");
    }

    public Future<MessageCollection> createMessageCollection(UserId userId, MessageCollection msgCollection, SecurityToken token)  throws ProtocolException {
      throw new ProtocolException(HttpServletResponse.SC_BAD_REQUEST, "Not Implemented");
    }

    public Future<Void> modifyMessageCollection(UserId userId, MessageCollection msgCollection, SecurityToken token) throws ProtocolException {
      throw new ProtocolException(HttpServletResponse.SC_BAD_REQUEST, "Not Implemented");
    }

    public Future<Void> deleteMessageCollection(UserId userId, String msgCollId, SecurityToken token) throws ProtocolException {
      throw new ProtocolException(HttpServletResponse.SC_BAD_REQUEST, "Not Implemented");
    }

    public Future<RestfulCollection<Message>> getMessages(UserId userId, String msgCollId,
        Set<String> fields, List<String> msgIds, CollectionOptions options, SecurityToken token) throws ProtocolException {
      throw new ProtocolException(HttpServletResponse.SC_BAD_REQUEST, "Not Implemented");
    }

    public Future<Void> createMessage(UserId userId, String appId, String msgCollId, Message message,
                               SecurityToken token) throws ProtocolException {
      throw new ProtocolException(HttpServletResponse.SC_BAD_REQUEST, "Not Implemented");
    }

    public Future<Void> deleteMessages(UserId userId, String msgCollId, List<String> ids,
        SecurityToken token) throws ProtocolException {
      throw new ProtocolException(HttpServletResponse.SC_BAD_REQUEST, "Not Implemented");
    }

    public Future<Void> modifyMessage(UserId userId, String msgCollId, String messageId, Message message, SecurityToken token)
        throws ProtocolException {
      throw new ProtocolException(HttpServletResponse.SC_BAD_REQUEST, "Not Implemented");
    }
  }
}
