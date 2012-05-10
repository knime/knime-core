<?xml version="1.0" encoding="UTF-8"?>

<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
<xsl:template match="style" />
<xsl:template match="h1">
	<xsl:apply-templates /> 
</xsl:template>

	<xsl:template match="h2">
		<xsl:apply-templates /> 
	</xsl:template>
	
		<xsl:template match="h3">
			<xsl:apply-templates /> 
		</xsl:template>
		
			<xsl:template match="h4">
				<xsl:apply-templates /> 
			</xsl:template>		

<xsl:template match="p">
<xsl:apply-templates /> 
</xsl:template>
	
<xsl:template match="ul">
<xsl:for-each select="li">
	<xsl:apply-templates /> 
</xsl:for-each>
</xsl:template>
	
<xsl:template match="ol">
<xsl:for-each select="li">
	<xsl:apply-templates /> 
</xsl:for-each>
</xsl:template>	

</xsl:stylesheet>