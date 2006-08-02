<?xml version='1.0'?>
<xsl:stylesheet  
       xmlns:xsl="http://www.w3.org/1999/XSL/Transform"  version="1.0"> 

  <xsl:import 
     href="../../lib/docbook-xsl-1.67.0/eclipse/eclipse.xsl" />

  <xsl:include href="common-customizations.xsl" />



  <xsl:param name="html.stylesheet" select="'css/eclipse.css'"/> 

  <xsl:param name="make.valid.html"  select="'1'"/>
  <xsl:param name="html.cleanup" select="1" />

  <xsl:param name="chunk.fast" select="'1'"/>
  <xsl:param name="chunk.section.depth" select="'3'"/>
  <xsl:param name="chunk.first.sections" select="'1'"/>
  
  <xsl:param name="suppress.navigation" select="'1'"/>
  <xsl:param name="navig.graphics" select="'1'" />
  <xsl:param name="navig.graphics.path">images/</xsl:param>
  <xsl:param name="navig.showtitles">0</xsl:param>
  
<!-- 
   These are unused, as the plugin.xml is edited manually !
  <xsl:param name="eclipse.plugin.name"></xsl:param>
  <xsl:param name="eclipse.plugin.id">org.knime.workbench.help</xsl:param>
  <xsl:param name="eclipse.plugin.provider">Chair for Applied Computer Science, University of Konstanz</xsl:param>
-->

  <xsl:param name="eclipse.autolabel" select="0" />
  
  <xsl:param name="root.filename" select="'index'"/>

  <xsl:param name="manifest.in.base.dir" select="1" />
  
</xsl:stylesheet>  