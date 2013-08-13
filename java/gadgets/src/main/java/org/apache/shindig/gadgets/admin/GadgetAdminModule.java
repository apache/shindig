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
package org.apache.shindig.gadgets.admin;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.shindig.common.logging.i18n.MessageKeys;
import org.apache.shindig.common.util.ResourceLoader;

import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;

/**
 * Module to load the gadget administration information.
 *
 * @version $Id: $
 */
public class GadgetAdminModule extends AbstractModule {

  private static final String GADGET_ADMIN_CONFIG = "config/gadget-admin.json";
  private static final String classname = GadgetAdminModule.class.getName();
  private static final Logger LOG = Logger.getLogger(classname, MessageKeys.MESSAGES);

  @Override
  protected void configure() {
    bind(GadgetAdminStore.class).toProvider(GadgetAdminStoreProvider.class);
  }

  @Singleton
  public static class GadgetAdminStoreProvider implements Provider<GadgetAdminStore> {
    private BasicGadgetAdminStore store;

    @Inject
    public GadgetAdminStoreProvider(BasicGadgetAdminStore store) {
      this.store = store;
      loadStore();
    }

    private void loadStore() {
      try {
        String gadgetAdminString = ResourceLoader.getContent(GADGET_ADMIN_CONFIG);
        this.store.init(gadgetAdminString);
      } catch (Throwable t) {
        if (LOG.isLoggable(Level.WARNING)) {
          LOG.logp(Level.WARNING, classname, "loadStore", MessageKeys.FAILED_TO_INIT,
                  new Object[] { GADGET_ADMIN_CONFIG });
          LOG.log(Level.WARNING, "", t);
        }
      }
    }

    public GadgetAdminStore get() {
      return store;
    }
  }
}
