<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
    <xsl:param name="cssUrl" />
    <xsl:param name="bgColor" />
    <xsl:param name="linkBase" />

    <xsl:template match="/">
        <html>
            <head>
                <base href="{$linkBase}" />
                <meta http-equiv="content-type" content="text/html; charset=UTF-8"></meta>
                <style type="text/css">
                    @import url("<xsl:value-of select="$cssUrl" />");
                    body { background-color: <xsl:value-of select="$bgColor" /> };
                </style>
            </head>
            <body>
                <xsl:apply-templates select="//div[@class='contentWrapper']" />
                <xsl:apply-templates select="//div[@id='knime-client-news']" />
            </body>
        </html>
    </xsl:template>

    <xsl:template match="div[@class='contentWrapper']">
        <xsl:apply-templates />
    </xsl:template>

    <xsl:template match="div[@id='knime-client-news']">
        <div id="news-entry">
            <xsl:apply-templates />
        </div>
    </xsl:template>

    <xsl:template match="style" />    
    <xsl:template match="@*|node()" priority="-1">
        <xsl:copy>
            <xsl:apply-templates select="@*|node()"/>
        </xsl:copy>
    </xsl:template>
</xsl:stylesheet>