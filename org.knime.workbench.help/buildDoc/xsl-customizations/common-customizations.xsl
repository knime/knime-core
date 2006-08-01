<?xml version='1.0'?> 
<xsl:stylesheet  
       xmlns:xsl="http://www.w3.org/1999/XSL/Transform"  version="1.0"> 


 
  <!-- 
    Common settings for all transformations 
    -->
  <xsl:param name="use.extensions" select="'1'" />
  
  <xsl:param name="use.id.as.filename" select="'1'" />

  <xsl:param name="chapter.autolabel" select="'1'"/>
  <xsl:param name="section.autolabel" select="'1'"/>
  <xsl:param name="htmlhelp.autolabel" select="'1'"/>
  
  <xsl:param name="img.src.path" select="'images/'" />
  <xsl:param name="draft.mode" select="'maybe'"/>
  <xsl:param name="draft.watermark.image" select="'images/draft.png'"/>



  <xsl:param name="shade.verbatim" select="'1'"/>

  <xsl:param name="admon.graphics" select="'1'"/>
  <xsl:param name="admon.graphics.path" select="'images/'"/>

  <xsl:param name="paper.type" select="'A4'"/> 


</xsl:stylesheet>
