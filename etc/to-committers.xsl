<?xml version="1.0" encoding="UTF-8"?>

<!--
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
-->

<!-- ====================================================================== -->
<!-- XSL to extract developers from a Maven pom.xml                         -->
<!-- ====================================================================== -->
<xsl:stylesheet version="1.1"
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns="http://maven.apache.org/POM/4.0.0"
    xmlns:mvn="http://maven.apache.org/POM/4.0.0" exclude-result-prefixes="mvn">

  <xsl:output method="text" encoding="UTF-8"/>

  <xsl:template match="/">
    <xsl:text>The following people have commit access to the Shindig sources.
Note that this is not a full list of Shindig's authors, however --
for that, you'd need to look over the log messages to see all the
patch contributors.

If you have a question or comment, it's probably best to mail
dev@shindig.apache.org, rather than mailing any of these
people directly.

Blanket commit access:&#xa;</xsl:text>
    <xsl:apply-templates select="mvn:project/mvn:developers/mvn:developer" />
  </xsl:template>

  <xsl:template match="mvn:developer">
    <xsl:text>&#xa;&#x9;</xsl:text>
    <xsl:call-template name="leftPad">
      <xsl:with-param name="input" select="mvn:id"/>
      <xsl:with-param name="maxSize" select="number(10)"/>
    </xsl:call-template>
    <xsl:text>&#x9;</xsl:text>
    <xsl:call-template name="rightPad">
      <xsl:with-param name="input" select="mvn:name"/>
      <xsl:with-param name="maxSize" select="number(20)"/>
    </xsl:call-template>
    <xsl:call-template name="rightPad">
      <xsl:with-param name="input" select="mvn:email"/>
      <xsl:with-param name="maxSize" select="number(25)"/>
    </xsl:call-template>
    <xsl:call-template name="rightPad">
      <xsl:with-param name="input" select="normalize-space(mvn:roles)"/>
      <xsl:with-param name="maxSize" select="number(10)"/>
    </xsl:call-template>
  </xsl:template>

  <!-- String Utilities -->

  <xsl:template name="rightPad">
    <xsl:param name="input" select="string('')"/>
    <xsl:param name="maxSize" select="0"/>
    <xsl:variable name="diff" select="$maxSize - string-length($input)" />
    <xsl:choose>
      <xsl:when test="$diff &lt; 0" >
        <xsl:value-of select="$input"/>
      </xsl:when>
      <xsl:otherwise>
        <xsl:value-of select="$input"/>
        <xsl:call-template name="space"><xsl:with-param name="repeat" select="$diff"/></xsl:call-template>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <xsl:template name="space">
    <xsl:param name="repeat">0</xsl:param>
    <xsl:param name="fillchar" select="' '"/>
    <xsl:if test="number($repeat) >= 1">
      <xsl:call-template name="space">
        <xsl:with-param name="repeat" select="$repeat - 1"/>
        <xsl:with-param name="fillchar" select="$fillchar"/>
      </xsl:call-template>
      <xsl:value-of select="$fillchar"/>
    </xsl:if>
  </xsl:template>

  <xsl:template name="leftPad">
    <xsl:param name="input" select="string('')"/>
    <xsl:param name="maxSize" select="0"/>
    <xsl:variable name="diff" select="$maxSize - string-length($input)" />
    <xsl:choose>
      <xsl:when test="$diff &lt; 0" >
        <xsl:value-of select="$input"/>
      </xsl:when>
      <xsl:otherwise>
        <xsl:call-template name="space"><xsl:with-param name="repeat" select="$diff"/></xsl:call-template>
        <xsl:value-of select="$input"/>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>
</xsl:stylesheet>