<!--

    Licensed to the Apache Software Foundation (ASF) under one or more
    contributor license agreements.  See the NOTICE file distributed with
    this work for additional information regarding copyright ownership.
    The ASF licenses this file to You under the Apache License, Version 2.0
    (the "License"); you may not use this file except in compliance with
    the License.  You may obtain a copy of the License at

         http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.

-->
<xsl:stylesheet version="1.0"
 xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

 <xsl:output omit-xml-declaration="yes"/>

    <xsl:template match="node()|@*">
      <xsl:copy>
         <xsl:apply-templates select="node()|@*"/>
      </xsl:copy>
    </xsl:template>

    <!-- This is to remove some entries from -->
    <!-- https://github.com/quarkusio/quarkus/blob/main/independent-projects/enforcer-rules/src/main/resources/enforcer-rules/quarkus-banned-dependencies.xml -->
    <!-- before passing it to Maven enforcer plugin -->
    <xsl:template match="//bannedDependencies/excludes/exclude[text() = 'org.javassist:javassist' or contains(text(), 'org.springframework:spring-')]"/>
</xsl:stylesheet>
