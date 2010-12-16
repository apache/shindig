/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

import flash.external.ExternalInterface;
import System.security;

/**
 * XPC Flash Based Transport
 * Original design by evn@google.com (Eduardo Vela)
 */
class Main {
  private static var SINGLETON:Boolean = false;
  public static function main(swfRoot:MovieClip):Void {
    if (SINGLETON) return;
    SINGLETON = true;
    
    var origin:String;
    
    if (_level0.origin == undefined){
      origin = "http://*";
    } else {
      origin = _level0.origin;
    }
    
    var domain:String = origin.substr(origin.indexOf("//") + 2, origin.length);
    
    if (origin.substr(0,5)==="http:") {
      security.allowInsecureDomain(domain);
    } else {
      security.allowDomain(domain);
    }
    
    ExternalInterface.addCallback("setup", { }, function(this_channel:String, role:String) {
      if (this_channel.indexOf(":") > -1) {
        return;
      }

      var other_role:String;
      
      if (role == "INNER") {
        other_role = "OUTER";
      } else {
        role = "OUTER";
        other_role = "INNER";
      }

      var receiving_lc:LocalConnection = new LocalConnection();
      var sending_lc:LocalConnection = new LocalConnection();
      receiving_lc.receiveMessage = function(to:String, from:String, channel:String, message:String) {
        if ((to === "*" || to === origin) && channel === this_channel) {
          ExternalInterface.call("gadgets.rpctx.flash.receiveMessage", channel, message, from, to);
        }
      }

      ExternalInterface.addCallback("sendMessage_"+this_channel, { }, function(message:String, to:String) {
        if (!to) to = "*";
        sending_lc.send(this_channel + "_" + other_role, "receiveMessage", to, origin, this_channel, message);
      } );
      receiving_lc.connect(this_channel + "_" + role);
    } );
    ExternalInterface.call("ready");
  }
  
  public function Main() {
  }
}
