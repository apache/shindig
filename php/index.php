<?php
/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
?><?php require_once 'config.php' ?>
<html>
    <script type="text/javascript" src="http://o.aolcdn.com/dojo/0.4.2/dojo.js"></script>
    <script type="text/javascript">
        // thanks to http://www.tagneto.org/blogcode/xframe/ui2.html
        dojo.addOnLoad(function() {
            var lastHash = '';
            setInterval(function() {
                var frame = navigator.userAgent.indexOf("Safari") != -1 ? frames["comframe"] : document.getElementById("comframe").contentWindow;
                var hash = String(frame.location.hash);
                if (hash != lastHash) {
                    lastHash = hash;
                    match = (/__os_iframe_height_([\d]+)__/.exec(hash))
                    if (match) {
                        document.getElementById("comframe").style.height = match[1] + 'px';
                    }
                }
            }, 200);
        });
    </script>
<body bgcolor="black">
    <iframe src="<?php echo "http://" . SITE_HOST ?>/comframe.php" id="comframe" scrolling="auto" frameborder="0" style="border:0;padding:0;margin:0;overflow:auto;background:white"/>
</body>
</html>
