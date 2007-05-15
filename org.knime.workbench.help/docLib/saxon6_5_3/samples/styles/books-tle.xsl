
<HTML xsl:version="1.0"
      xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
    <xsl:comment xmlns:Date="/java.util.Date">Generated at <xsl:value-of
                                       select="Date:toString(Date:new())"/></xsl:comment>
    <BODY LEFTMARGIN="100"  >
        <xsl:attribute name="LEFTMARGIN">120</xsl:attribute>
 

    <ul>
    <xsl:for-each select="//ITEM">
        <li><xsl:value-of select="TITLE"/></li>
    </xsl:for-each>
    </ul>

    </BODY>
    </HTML>

