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

  public static function replace(str:String, from_str:String, to_str:String):String {
    var out_str:String = "";
    var search_ix:Number = 0;
    while (search_ix < str.length) {
      var found_ix:Number = str.indexOf(from_str, search_ix);
      if (found_ix != -1) {
        out_str = out_str.concat(str.substring(search_ix, found_ix)).concat(to_str);
        search_ix = found_ix + from_str.length;
      } else {
        out_str = out_str.concat(str.substring(search_ix));
        search_ix = str.length;
      }
    }
    return out_str;
  }

  public static function esc(str:String):String {
    return replace(str, "\\", "\\\\");
  }

  public static function stripPortIfPresent(str:String):String {
    var col_ix:Number = str.indexOf(":");
    if (col_ix == -1) {
      return str;
    }
    return str.substring(0, col_ix);
  }

  public static function main(swfRoot:MovieClip):Void {
    var escFn:Function = esc;
    var replaceFn:Function = replace;
    
    if (SINGLETON) return;
    SINGLETON = true;
    
    var my_origin:String;

    if (_level0.origin == undefined){
      my_origin = "http://*";
    } else {
      my_origin = _level0.origin;
    }

    var domain:String = stripPortIfPresent(
        my_origin.substr(my_origin.indexOf("//") + 2, my_origin.length));
    
    if (my_origin.substr(0,5)==="http:") {
      security.allowInsecureDomain(domain);
    } else {
      security.allowDomain(domain);
    }

    ExternalInterface.addCallback("setup", { },
        function(rpc_key:String, channel_id:String, role:String) {
      var other_role:String;

      if (role == "INNER") {
        other_role = "OUTER";
      } else {
        other_role = "INNER";
        role = "OUTER";
      }

      var receiving_lc:LocalConnection = new LocalConnection();
      var sending_lc:LocalConnection = new LocalConnection();
      receiving_lc.receiveMessage =
          function(to_origin:String, from_origin:String, in_rpc_key:String, message:String) {
        if ((to_origin === "*" || to_origin === my_origin) && (in_rpc_key == rpc_key)) {
          ExternalInterface.call("gadgets.rpctx.flash._receiveMessage",
              escFn(message), escFn(from_origin), escFn(to_origin));
        }
      };

      ExternalInterface.addCallback("sendMessage_" + channel_id + "_" + rpc_key + "_" + role,
            { }, function(message:String, to_origin:String) {
        if (!to_origin) to_origin = "*";
        var sendId:String =
            replaceFn("channel_" + channel_id + "_" + rpc_key + "_" + other_role, ":", "");
        sending_lc.send(sendId,
            "receiveMessage", to_origin, my_origin, rpc_key, message);
      });
      var recvId:String = replaceFn("channel_" + channel_id + "_" + rpc_key + "_" + role, ":", "");
      receiving_lc.connect(recvId);
    });
    ExternalInterface.call("gadgets.rpctx.flash._ready");
  }
  
  public function Main() {
  }
}
