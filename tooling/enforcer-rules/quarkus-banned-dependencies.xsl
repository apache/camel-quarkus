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
