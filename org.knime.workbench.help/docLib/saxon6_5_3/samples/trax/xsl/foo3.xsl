<?xml version="1.0"?> 
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
  
  <xsl:template match="out">
    <out>
      <xsl:apply-templates/>
    </out>
  </xsl:template>
  
  <xsl:template match="text()">
    <some-text><xsl:value-of select="."/></some-text>
  </xsl:template>  
   
</xsl:stylesheet>
