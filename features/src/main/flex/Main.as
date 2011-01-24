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
    
    var my_origin:String;
    
    if (_level0.origin == undefined){
      my_origin = "http://*";
    } else {
      my_origin = _level0.origin;
    }
    
    var domain:String = my_origin.substr(my_origin.indexOf("//") + 2, my_origin.length);
    
    if (my_origin.substr(0,5)==="http:") {
      security.allowInsecureDomain(domain);
    } else {
      security.allowDomain(domain);
    }
    
    ExternalInterface.addCallback("setup", { }, function(channel_recv_id:String, role:String) {
      if (channel_recv_id.indexOf(":") > -1) {
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
      receiving_lc.receiveMessage = function(to_origin:String, from_origin:String, target_id:String, message:String) {
        if ((to_origin === "*" || to_origin === my_origin) && target_id === channel_recv_id) {
          ExternalInterface.call("gadgets.rpctx.flash._receiveMessage", target_id, message, from_origin, to_origin);
        }
      };

      ExternalInterface.addCallback("sendMessage_" + channel_recv_id, { }, function(message:String, to_origin:String) {
        if (!to_origin) to_origin = "*";
        sending_lc.send(channel_recv_id + "_" + other_role, "receiveMessage", to_origin, my_origin, channel_recv_id, message);
      } );
      receiving_lc.connect(channel_recv_id + "_" + role);
    } );
    ExternalInterface.call("gadgets.rpctx.flash._ready");
  }
  
  public function Main() {
  }
}
