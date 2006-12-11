<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">

<xsl:attribute-set name="attset1" use-attribute-sets="attset2">
    <xsl:attribute name="LEFTMARGIN">150</xsl:attribute>
    <xsl:attribute name="RIGHTMARGIN">150</xsl:attribute>
    <xsl:attribute name="TOPMARGIN">190</xsl:attribute>
</xsl:attribute-set>

<xsl:variable name="x">attsets.xsl</xsl:variable>

<xsl:attribute-set name="attset2">
    <xsl:attribute name="TOPMARGIN">180</xsl:attribute>
    <xsl:attribute name="BOTTOMMARGIN">200</xsl:attribute>
</xsl:attribute-set>

</xsl:stylesheet>
