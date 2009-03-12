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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;
import com.google.common.collect.ImmutableMap;

import junit.framework.TestCase;

import org.apache.shindig.common.testing.FakeGadgetToken;
import org.apache.shindig.common.util.ImmediateFuture;
import org.apache.shindig.social.core.model.MessageImpl;
import org.apache.shindig.social.opensocial.model.Message;
import org.apache.shindig.social.opensocial.spi.MessageService;
import org.apache.shindig.social.opensocial.spi.SocialSpiException;
import org.apache.shindig.social.opensocial.spi.UserId;
import org.apache.shindig.protocol.conversion.BeanJsonConverter;
import org.apache.shindig.protocol.*;
import org.easymock.classextension.EasyMock;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

public class MessageHandlerTest extends TestCase {

  private MessageService messageService;
  private MessageHandler handler;
  private BeanJsonConverter converter;
  private FakeGadgetToken token;
  private UserId sender;
  private List<String> recipients;
  protected HandlerRegistry registry;


  @Override
  protected void setUp() throws Exception {
    token = new FakeGadgetToken();
    messageService = EasyMock.createMock(MessageService.class);
    messageService = EasyMock.createMock(MessageService.class);
    converter = EasyMock.createMock(BeanJsonConverter.class);
    sender = new UserId(UserId.Type.userId, "message.sender");
    recipients = ImmutableList.of("second.recipient", "first.recipient");

    handler = new MessageHandler(messageService);
    registry = new DefaultHandlerRegistry(null, converter,
        new HandlerExecutionListener.NoOpHandlerExecutionListener());
    registry.addHandlers(Sets.<Object>newHashSet(handler));
  }

  public void testPostMessage() 
      throws SocialSpiException, InterruptedException, ExecutionException {
    MessageImpl message = new MessageImpl("A message body", "A title", Message.Type.PRIVATE_MESSAGE);
    message.setRecipients(recipients);

    EasyMock.expect(converter.convertToObject(null, Message.class)).andReturn(message);
    EasyMock.expect(messageService.createMessage(sender, "messageHandlerTest", "@outbox", message,
        token)).andReturn(ImmediateFuture.newInstance((Void) null));

    EasyMock.replay(messageService, converter);

    RestHandler operation = registry.getRestHandler("/messages/" + sender.getUserId() + "/@outbox", "POST");
    Map<String,String[]> params = ImmutableMap.of(RequestItem.APP_ID, new String[]{"messageHandlerTest"});

    operation.execute(params,null, token, converter).get();
    EasyMock.verify(converter, messageService);
  }
}
