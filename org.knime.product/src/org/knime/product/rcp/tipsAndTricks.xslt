<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
    <xsl:param name="cssUrl" />
    <xsl:param name="bgColor" />
    <xsl:param name="linkBase" />

    <xsl:template match="/">
        <html>
            <head>
                <meta http-equiv="content-type" content="text/html; charset=UTF-8"></meta>
                <style type="text/css">
                    @import url("<xsl:value-of select="$cssUrl" />");
                    body { background-color: <xsl:value-of select="$bgColor" /> };
                </style>
            </head>
            <body>
                <xsl:apply-templates select="//div[@class='contentWrapper']" />
                <xsl:apply-templates select="//div[@id='block-views-news-block_2']//div[@class='views-field-title']/span[@class='field-content']" />
                <xsl:apply-templates select="//div[@id='block-views-events-block_2']//div[@class='views-field-title']/span[@class='field-content']" />
            </body>
        </html>
    </xsl:template>

    <xsl:template match="div[@class='contentWrapper']">
        <xsl:apply-templates />
    </xsl:template>

    <xsl:template match="span[@class='field-content']">
        <div id="news-entry">
            <xsl:apply-templates />
        </div>
    </xsl:template>

    <xsl:template match="a[not(starts-with(@href, 'http'))]">
        <a href="{$linkBase}{@href}"><xsl:apply-templates /></a>
    </xsl:template>

    <xsl:template match="style" />    
    <xsl:template match="@*|node()" priority="-1">
        <xsl:copy>
            <xsl:apply-templates select="@*|node()"/>
        </xsl:copy>
    </xsl:template>
</xsl:stylesheet>