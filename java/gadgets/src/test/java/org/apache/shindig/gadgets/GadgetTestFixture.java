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

package org.apache.shindig.gadgets;


import org.apache.shindig.gadgets.http.CrossServletState;
import org.apache.shindig.gadgets.http.ProxyHandler;
import org.apache.shindig.gadgets.spec.GadgetSpec;
import org.apache.shindig.gadgets.spec.MessageBundle;
import org.apache.shindig.util.BlobCrypterException;

import java.util.Set;
import java.util.concurrent.Executors;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class GadgetTestFixture extends EasyMockTestCase {
  public final HttpServletRequest request = mock(HttpServletRequest.class, true);
  public final HttpServletResponse response = mock(HttpServletResponse.class, true);
  public final GadgetServer gadgetServer;
  public final ProxyHandler proxyHandler;
  public final RemoteContentFetcher fetcher = mock(RemoteContentFetcher.class, true);
  @SuppressWarnings(value="unchecked")
  public final DataFetcher<GadgetSpec> specFetcher = mock(DataFetcher.class, true);
  @SuppressWarnings(value="unchecked")
  public final DataFetcher<MessageBundle> bundleFetcher
      = mock(DataFetcher.class);
  public final GadgetBlacklist blacklist = mock(GadgetBlacklist.class, true);
  public GadgetFeatureRegistry registry;
  public SyndicatorConfig syndicatorConfig;
  public final CrossServletState state = new CrossServletState() {
    @Override
    public GadgetServer getGadgetServer() {
      return gadgetServer;
    }

    @Override
    public GadgetSigner getGadgetSigner() {
      return new GadgetSigner() {
        public GadgetToken createToken(String tokenString)
            throws GadgetException {
          try {
            return new BasicGadgetToken("owner", "viewer", "app", "domain");
          } catch (BlobCrypterException e) {
            throw new GadgetException(GadgetException.Code.INVALID_GADGET_TOKEN, e);
          }
        }
      };
    }

    @Override
    public String getJsUrl(Set<String> libs, GadgetContext context) {
      StringBuilder bs = new StringBuilder();
      boolean first = false;
      for (String lib : libs) {
        if (!first) {
          first = true;
        } else {
          bs.append(':');
        }
        bs.append(lib);
      }
      return bs.toString();
    }

    @Override
    public String getIframeUrl(Gadget gadget) {
      return "";
    }

    @Override
    public void init(ServletContext config) {

    }

    @Override
    public RemoteContentFetcher makeSigningFetcher(RemoteContentFetcher fetcher, GadgetToken token) {
      // Real implementations should use their own key, probably pulled from
      // disk rather than hardcoded in the source.
      final String PRIVATE_KEY_TEXT =
        "MIICdgIBADANBgkqhkiG9w0BAQEFAASCAmAwggJcAgEAAoGBALRiMLAh9iimur8V" +
        "A7qVvdqxevEuUkW4K+2KdMXmnQbG9Aa7k7eBjK1S+0LYmVjPKlJGNXHDGuy5Fw/d" +
        "7rjVJ0BLB+ubPK8iA/Tw3hLQgXMRRGRXXCn8ikfuQfjUS1uZSatdLB81mydBETlJ" +
        "hI6GH4twrbDJCR2Bwy/XWXgqgGRzAgMBAAECgYBYWVtleUzavkbrPjy0T5FMou8H" +
        "X9u2AC2ry8vD/l7cqedtwMPp9k7TubgNFo+NGvKsl2ynyprOZR1xjQ7WgrgVB+mm" +
        "uScOM/5HVceFuGRDhYTCObE+y1kxRloNYXnx3ei1zbeYLPCHdhxRYW7T0qcynNmw" +
        "rn05/KO2RLjgQNalsQJBANeA3Q4Nugqy4QBUCEC09SqylT2K9FrrItqL2QKc9v0Z" +
        "zO2uwllCbg0dwpVuYPYXYvikNHHg+aCWF+VXsb9rpPsCQQDWR9TT4ORdzoj+Nccn" +
        "qkMsDmzt0EfNaAOwHOmVJ2RVBspPcxt5iN4HI7HNeG6U5YsFBb+/GZbgfBT3kpNG" +
        "WPTpAkBI+gFhjfJvRw38n3g/+UeAkwMI2TJQS4n8+hid0uus3/zOjDySH3XHCUno" +
        "cn1xOJAyZODBo47E+67R4jV1/gzbAkEAklJaspRPXP877NssM5nAZMU0/O/NGCZ+" +
        "3jPgDUno6WbJn5cqm8MqWhW1xGkImgRk+fkDBquiq4gPiT898jusgQJAd5Zrr6Q8" +
        "AO/0isr/3aa6O6NLQxISLKcPDk2NOccAfS/xOtfOz4sJYM3+Bs4Io9+dZGSDCA54" +
        "Lw03eHTNQghS0A==";
      final String PRIVATE_KEY_NAME = "shindig-insecure-key";
      return SigningFetcher.makeFromB64PrivateKey(
          fetcher, token, PRIVATE_KEY_NAME, PRIVATE_KEY_TEXT);
    }
  };

  public GadgetTestFixture() {
    GadgetServerConfig config = new GadgetServerConfig();
    config.setExecutor(Executors.newSingleThreadExecutor());
    config.setGadgetSpecFetcher(specFetcher);
    config.setMessageBundleFetcher(bundleFetcher);
    config.setContentFetcher(fetcher);
    try {
      registry = new GadgetFeatureRegistry(null, fetcher);
    } catch (Exception e) {
      e.printStackTrace();
      fail("Failed to create feature registry");
    }

    try {
      syndicatorConfig = new SyndicatorConfig(null);
    } catch (Exception e) {
      e.printStackTrace();
      fail("Failed to create syndicator config");
    }

    config.setSyndicatorConfig(syndicatorConfig);
    config.setFeatureRegistry(registry);
    config.setGadgetBlacklist(blacklist);
    gadgetServer = new GadgetServer(config);
    proxyHandler = new ProxyHandler(fetcher);
  }
}
