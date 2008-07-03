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
package org.apache.shindig.social.opensocial;

import com.google.inject.AbstractModule;

import org.apache.shindig.social.opensocial.model.Activity;
import org.apache.shindig.social.opensocial.model.ActivityImpl;
import org.apache.shindig.social.opensocial.model.Address;
import org.apache.shindig.social.opensocial.model.AddressImpl;
import org.apache.shindig.social.opensocial.model.BodyType;
import org.apache.shindig.social.opensocial.model.BodyTypeImpl;
import org.apache.shindig.social.opensocial.model.Email;
import org.apache.shindig.social.opensocial.model.EmailImpl;
import org.apache.shindig.social.opensocial.model.MediaItem;
import org.apache.shindig.social.opensocial.model.MediaItemImpl;
import org.apache.shindig.social.opensocial.model.Message;
import org.apache.shindig.social.opensocial.model.MessageImpl;
import org.apache.shindig.social.opensocial.model.Name;
import org.apache.shindig.social.opensocial.model.NameImpl;
import org.apache.shindig.social.opensocial.model.Organization;
import org.apache.shindig.social.opensocial.model.OrganizationImpl;
import org.apache.shindig.social.opensocial.model.Person;
import org.apache.shindig.social.opensocial.model.PersonImpl;
import org.apache.shindig.social.opensocial.model.Phone;
import org.apache.shindig.social.opensocial.model.PhoneImpl;
import org.apache.shindig.social.opensocial.model.Url;
import org.apache.shindig.social.opensocial.model.UrlImpl;

/**
 * Module for binding standard model implementations
 */
public class DefaultModelGuiceModule extends AbstractModule {

  protected void configure() {
    bind(Activity.class).to(ActivityImpl.class);
    bind(Address.class).to(AddressImpl.class);
    bind(BodyType.class).to(BodyTypeImpl.class);
    bind(Email.class).to(EmailImpl.class);
    bind(MediaItem.class).to(MediaItemImpl.class);
    bind(Message.class).to(MessageImpl.class);
    bind(Name.class).to(NameImpl.class);
    bind(Organization.class).to(OrganizationImpl.class);
    bind(Person.class).to(PersonImpl.class);
    bind(Phone.class).to(PhoneImpl.class);
    bind(Url.class).to(UrlImpl.class);
  }
}
