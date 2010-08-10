#!/bin/sh

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

#
# This script was orriginally developed under Apache Sling at http://svn.apache.org/repos/asf/sling/trunk/check_staged_release.sh
# and has been modified for Shindig on 7th August 2009
#

STAGING=${1}
DOWNLOAD=${2:-/tmp/shindig-staging}
mkdir ${DOWNLOAD} 2>/dev/null

if [ -z "${STAGING}" -o ! -d "${DOWNLOAD}" ]
then
 echo "Usage: check_staged_release.sh <staging-number> [temp-directory]"
 echo " eg check_staged_release.sh 011 "
 exit
fi

if [ ! -e "${DOWNLOAD}/${STAGING}" ]
then
 echo "################################################################################"
 echo "                           DOWNLOAD STAGED REPOSITORY                           "
 echo "################################################################################"

 if [ `wget --help | grep "no-check-certificate" | wc -l` -eq 1 ]
 then
   CHECK_SSL=--no-check-certificate
 fi

 wget $CHECK_SSL \
  -nv -r -np "--reject=html,txt" "--follow-tags=" \
  -P "${DOWNLOAD}/${STAGING}" -nH "--cut-dirs=3" --ignore-length \
  "http://repository.apache.org/content/repositories/shindig-staging-${STAGING}/org/apache/shindig/"

else
 echo "################################################################################"
 echo "                       USING EXISTING STAGED REPOSITORY                         "
 echo "################################################################################"
 echo "${DOWNLOAD}/${STAGING}"
fi

echo "################################################################################"
echo "                          CHECK SIGNATURES AND DIGESTS                          "
echo "################################################################################"

for i in `find "${DOWNLOAD}/${STAGING}" -type f | grep -v '\.\(asc\|sha1\|md5\)$'`
do
 f=`echo $i | sed 's/\.asc$//'`
 echo "$f"
 gpg --verify $f.asc 2>/dev/null
 if [ "$?" = "0" ]; then CHKSUM="GOOD"; else CHKSUM="BAD!!!!!!!!"; fi
 if [ ! -f "$f.asc" ]; then CHKSUM="----"; fi
 echo "gpg:  ${CHKSUM}"
 if [ "`cat $f.md5 2>/dev/null`" = "`openssl md5 < $f 2>/dev/null`" ]; then CHKSUM="GOOD (`cat $f.md5`)"; else CHKSUM="BAD!!!!!!!!"; fi
 if [ ! -f "$f.md5" ]; then CHKSUM="----"; fi
 echo "md5:  ${CHKSUM}"
 if [ "`cat $f.sha1 2>/dev/null`" = "`openssl sha1 < $f 2>/dev/null`" ]; then CHKSUM="GOOD (`cat $f.sha1`)"; else CHKSUM="BAD!!!!!!!!"; fi
 if [ ! -f "$f.sha1" ]; then CHKSUM="----"; fi
 echo "sha1: ${CHKSUM}"
done

if [ -z "${CHKSUM}" ]; then echo "WARNING: no files found!"; fi

echo "################################################################################"


