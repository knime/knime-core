<xsl:transform
 xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
 version="1.0"
>

<xsl:import href="person.xsl"/>

<!-- Change the way hyperlinks are generated -->

<xsl:template name="make-href">
    <xsl:variable name="apos">'</xsl:variable>
	<xsl:value-of select="concat('Javascript:refresh(', $apos, ../@ID, $apos, ')')"/>
</xsl:template>

<!-- Suppress the generation of a CSS stylesheet -->

<xsl:template name="css-style"/>

</xsl:transform>
