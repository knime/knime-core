<?xml version="1.0"?> 
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0"
                xmlns:bar="http://apache.org/bar">
  
  <xsl:template match="bar">
    <out>
      <xsl:value-of select="."/>
    </out>
  </xsl:template>
  
  <xsl:template match="text()">
  </xsl:template>  
 
</xsl:stylesheet>
