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
/**
<h1>The Social API package</h1>
<p>Shared API classes and interfaces used by both the back end (spi)
and front end (service).</p>
<p>The Social Jar consists of a core set of packages that contain
the lower level implementation of the component (o.a.s.social.core), and
a set interface layers (o.a.s.social.opensocial) that represent areas of
the component that are more outward facing. In addition there is a
sample package which contains sample implementations of the service and
spi.</p>
<h2>Opensocial</h2>
<p>This package contains classes that are outward facing, there are
4 main areas.
<ul>
  <li><b>model</b> the model interfaces underlying the Opensocial implementation</li>
  <li><b>oath</b> the OAuth interfaces used inside the Opensocial implementation</li>
  <li><b>service</b> the HTTP facing service interface</li>
  <li><b>spi</b> the Service Provider Interfaces</li>
</ul>
*/

package org.apache.shindig.social;
