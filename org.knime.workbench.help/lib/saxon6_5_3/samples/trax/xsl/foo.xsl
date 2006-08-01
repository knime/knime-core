<xsl:stylesheet 
      xmlns:xsl='http://www.w3.org/1999/XSL/Transform' version='1.0'
      xmlns:bar="http://apache.org/bar"
      exclude-result-prefixes="bar">
      
  <xsl:include href="inc1/inc1.xsl"/>
      
  <xsl:param name="a-param">default param value</xsl:param>
  
  <xsl:template match="bar:element">
    <bar>
      <param-val>
        <xsl:value-of select="$a-param"/><xsl:text>, </xsl:text>
        <xsl:value-of select="$my-var"/>
      </param-val>
      <data><xsl:apply-templates/></data>
    </bar>
  </xsl:template>
      
  <xsl:template 
      match="@*|*|text()|processing-instruction()">
    <xsl:copy>
      <xsl:apply-templates 
         select="@*|*|text()|processing-instruction()"/>
    </xsl:copy>
  </xsl:template>
</xsl:stylesheet>