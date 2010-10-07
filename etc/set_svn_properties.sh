#!/bin/bash

# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
#  http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.

# This script will set the proper svn properties on all the files in the tree
# It pretty much requires a gnu compatible xargs (for the -r flag).  Running
# on Linux is probably the best option or on Windows with cygwin.

# Note: use the following line if you want to remove svn:keywords
#for ext in java php xml xsl xsd wsdl properties txt htm* css js ; do find . -path '*/.svn' -prune -o  -name "*.$ext" -print0 | grep -v '.svn' | xargs -0  -r  svn propdel  svn:keywords ; done

# Note: use the following line to automatically apply svn ignore 
#svn propset svn:ignore -F etc/svn-ignores .
#svn propset svn:ignore -F etc/svn-ignores features
#svn propset svn:ignore -F etc/svn-ignores java
#svn propset svn:ignore -F etc/svn-ignores java/common
#svn propset svn:ignore -F etc/svn-ignores java/gadgets
#svn propset svn:ignore -F etc/svn-ignores java/social-api
#svn propset svn:ignore -F etc/svn-ignores java/server
#svn propset svn:ignore -F etc/svn-ignores java/samples
#svn propset svn:ignore -F etc/svn-ignores php

# Language files
find . -name "*.java" | grep -v '.svn' | xargs -n 1 svn propset svn:eol-style native

find . -name "*.php" | grep -v '.svn' | xargs -n 1 svn propset svn:eol-style native

find . -name "*.properties" | grep -v '.svn' | xargs -n 1 svn propset svn:eol-style native
find . -name "*.properties" | grep -v '.svn' | xargs -n 1 svn propset svn:mime-type text/plain

# XML files
find . -name "*.xml" | grep -v '.svn' | xargs -n 1 svn propset svn:eol-style native
find . -name "*.xml" | grep -v '.svn' | xargs -n 1 svn propset svn:mime-type text/xml

find . -name "*.xsl" | grep -v '.svn' | xargs -n 1 svn propset svn:eol-style native
find . -name "*.xsl" | grep -v '.svn' | xargs -n 1 svn propset svn:mime-type text/xml

find . -name "*.xsd" | grep -v '.svn' | xargs -n 1 svn propset svn:eol-style native
find . -name "*.xsd" | grep -v '.svn' | xargs -n 1 svn propset svn:mime-type text/xml

find . -name "*.wsdl" | grep -v '.svn' | xargs -n 1 svn propset svn:eol-style native
find . -name "*.wsdl" | grep -v '.svn' | xargs -n 1 svn propset svn:mime-type text/xml

find . -name "*.wsdd" | grep -v '.svn' | xargs -n 1 svn propset svn:eol-style native
find . -name "*.wsdd" | grep -v '.svn' | xargs -n 1 svn propset svn:mime-type text/xml

# HTML files
find . -name "*.htm*" | grep -v '.svn' | xargs -n 1 svn propset svn:eol-style native
find . -name "*.htm*" | grep -v '.svn' | xargs -n 1 svn propset svn:mime-type text/html

find . -name "*.css" | grep -v '.svn' | xargs -n 1 svn propset svn:eol-style native
find . -name "*.css" | grep -v '.svn' | xargs -n 1 svn propset svn:mime-type text/css

find . -name "*.js" | grep -v '.svn' | xargs -n 1 svn propset svn:eol-style native
find . -name "*.js" | grep -v '.svn' | xargs -n 1 svn propset svn:mime-type text/javascript

# Image files
find . -name "*.png" | grep -v '.svn' | xargs -n 1 svn propset svn:mime-type image/png
find . -name "*.gif" | grep -v '.svn' | xargs -n 1 svn propset svn:mime-type image/gif
find . -name "*.jpg" | grep -v '.svn' | xargs -n 1 svn propset svn:mime-type image/jpeg
find . -name "*.jpeg" | grep -v '.svn' | xargs -n 1 svn propset svn:mime-type image/jpeg

# Executable files
find . -name "*.sh" | grep -v '.svn' | xargs -n 1 svn propset svn:eol-style native
find . -name "*.sh" | grep -v '.svn' | xargs -n 1 svn propset svn:mime-type text/plain
find . -name "*.sh" | grep -v '.svn' | xargs -n 1 svn propset svn:executable ""

find . -name "*.bat" | grep -v '.svn' | xargs -n 1 svn propset svn:eol-style native
find . -name "*.bat" | grep -v '.svn' | xargs -n 1 svn propset svn:mime-type text/plain
find . -name "*.bat" | grep -v '.svn' | xargs -n 1 svn propset svn:executable ""

find . -name "*.cmd" | grep -v '.svn' | xargs -n 1 svn propset svn:eol-style native
find . -name "*.cmd" | grep -v '.svn' | xargs -n 1 svn propset svn:mime-type text/plain
find . -name "*.cmd" | grep -v '.svn' | xargs -n 1 svn propset svn:executable ""

# Maven site files
find . -name "*.apt" | grep -v '.svn' | xargs -n 1 svn propset svn:eol-style native
find . -name "*.apt" | grep -v '.svn' | xargs -n 1 svn propset svn:mime-type text/plain

find . -name "*.fml" | grep -v '.svn' | xargs -n 1 svn propset svn:eol-style native
find . -name "*.fml" | grep -v '.svn' | xargs -n 1 svn propset svn:mime-type text/xml

find . -name "*.xdoc" | grep -v '.svn' | xargs -n 1 svn propset svn:eol-style native
find . -name "*.xdoc" | grep -v '.svn' | xargs -n 1 svn propset svn:mime-type text/xml

# Other files
find . -name "*.txt" | grep -v '.svn' | xargs -n 1 svn propset svn:eol-style native
find . -name "*.txt" | grep -v '.svn' | xargs -n 1 svn propset svn:mime-type text/plain

find . -name "README*" | grep -v '.svn' | xargs -n 1 svn propset svn:eol-style native
find . -name "README*" | grep -v '.svn' | xargs -n 1 svn propset svn:mime-type text/plain

find . -name "LICENSE*" | grep -v '.svn' | xargs -n 1 svn propset svn:eol-style native
find . -name "LICENSE*" | grep -v '.svn' | xargs -n 1 svn propset svn:mime-type text/plain

find . -name "NOTICE*" | grep -v '.svn' | xargs -n 1 svn propset svn:eol-style native
find . -name "NOTICE*" | grep -v '.svn' | xargs -n 1 svn propset svn:mime-type text/plain

find . -name "KEYS*" | grep -v '.svn' | xargs -n 1 svn propset svn:eol-style native
find . -name "KEYS*" | grep -v '.svn' | xargs -n 1 svn propset svn:mime-type text/plain

find . -name "INSTALL*" | grep -v '.svn' | xargs -n 1 svn propset svn:eol-style native
find . -name "INSTALL*" | grep -v '.svn' | xargs -n 1 svn propset svn:mime-type text/plain

find . -name "UPGRADING*" | grep -v '.svn' | xargs -n 1 svn propset svn:eol-style native
find . -name "UPGRADING*" | grep -v '.svn' | xargs -n 1 svn propset svn:mime-type text/plain

find . -name "COMMITTERS*" | grep -v '.svn' | xargs -n 1 svn propset svn:eol-style native
find . -name "COMMITTERS*" | grep -v '.svn' | xargs -n 1 svn propset svn:mime-type text/plain

find . -name "BUILD-JAVA*" | grep -v '.svn' | xargs -n 1 svn propset svn:eol-style native
find . -name "BUILD-JAVA*" | grep -v '.svn' | xargs -n 1 svn propset svn:mime-type text/plain
