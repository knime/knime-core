<xsl:stylesheet 
      xmlns:xsl='http://www.w3.org/1999/XSL/Transform' version='1.0'>
    
  <!-- same as foo.xsl but doesn't include the param because of a bug -->  
  <xsl:template match="/">
    <out>
      <xsl:apply-templates/>
    </out>
  </xsl:template>
      
  <xsl:template 
      match="@*|*|text()|processing-instruction()">
    <xsl:copy>
      <xsl:apply-templates 
         select="@*|*|text()|processing-instruction()"/>
    </xsl:copy>
  </xsl:template>
</xsl:stylesheet>