<?xml version='1.0'?>
<xsl:stylesheet  
       xmlns:xsl="http://www.w3.org/1999/XSL/Transform"  version="1.0"> 

  <xsl:import 
     href="../../lib/docbook-xsl-1.67.0/html/docbook.xsl" />

  <xsl:include href="common-customizations.xsl" />


  
  <xsl:param name="html.stylesheet" select="'css/docbook.css'"/> 
  <xsl:param name="make.valid.html"  select="'1'"/>
  <xsl:param name="html.cleanup" select="1" />
 
 
</xsl:stylesheet>  