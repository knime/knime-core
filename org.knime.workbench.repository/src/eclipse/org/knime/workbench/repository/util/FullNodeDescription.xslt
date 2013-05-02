<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE stylesheet [
<!ENTITY css SYSTEM "style.css">
]>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
    <xsl:param name="css" />
    <xsl:template match="knimeNode">
        <html>
            <head>
                <title>
                    Node description for
                    <xsl:value-of select="name" />
                </title>
                <style type="text/css"><xsl:value-of select="$css" /></style>
            </head>
            <body>
                <h1>
                    <xsl:value-of select="name" />
                </h1>
                
                <xsl:if test="@deprecated = 'true'">
                    <h4 class="deprecated">Deprecated</h4>
                </xsl:if>
                <p>
                    <xsl:apply-templates select="fullDescription/intro/node()" />
                </p>

                <xsl:if test="fullDescription/option">
                    <h2>Dialog Options</h2>
                    <dl>
                        <xsl:apply-templates select="fullDescription/option" />
                    </dl>
                </xsl:if>

                <xsl:if test="fullDescription/tab">
                    <h2>Dialog Options</h2>
                    <xsl:apply-templates select="fullDescription/tab" />
                </xsl:if>


                <xsl:apply-templates select="interactiveView" />
                <xsl:apply-templates select="ports" />
                <xsl:apply-templates select="views" />

                <div id="origin-bundle">
                    This node is contained in <em><xsl:value-of select="osgi-info/@bundle-name" /></em>
                    provided by <em><xsl:value-of select="osgi-info/@bundle-vendor" /></em>.
                </div>
            </body>
        </html>
    </xsl:template>
    
    <xsl:template match="tab">
        <div class="group">
            <div class="groupname">
                <xsl:value-of select="@name" />
            </div>
            <dl>
                <xsl:apply-templates />
            </dl>
        </div>
    </xsl:template>

    <xsl:template match="option">
        <dt>
            <xsl:value-of select="@name" />
            <xsl:if test="@optional = 'true'">
                <span style="font-style: normal; font-weight: normal;"> (optional)</span>
            </xsl:if>            
        </dt>
        <dd>
            <xsl:apply-templates select="node()" />
        </dd>
    </xsl:template>

    <xsl:template match="interactiveView">
        <h2><xsl:value-of select="@name" /></h2>
        <div>
            <xsl:apply-templates />
        </div>
    </xsl:template>

    <xsl:template match="views[view]">
        <h2>Views</h2>
        <dl>
            <xsl:for-each select="view">
                <xsl:sort select="@index" />
                <dt>
                    <xsl:value-of select="@name" />
                </dt>
                <dd>
                    <xsl:apply-templates />
                </dd>
            </xsl:for-each>
        </dl>
    </xsl:template>


    <xsl:template match="ports">
        <h2>Ports</h2>
        <dl>
            <xsl:if test="dataIn|inPort">
                <div class="group">
                    <div class="groupname">Input Ports</div>
                    <table>
                        <xsl:for-each select="dataIn|inPort">
                            <xsl:sort select="@index" />
                            <tr>
                                <td class="dt">
                                    <xsl:value-of select="@index" />
                                </td>
                                <td>
                                    <xsl:apply-templates />
                                    <xsl:if test="@optional = 'true'"> (optional)</xsl:if>
                                </td>
                            </tr>
                        </xsl:for-each>
                    </table>
                </div>
            </xsl:if>
            <xsl:if test="dataOut|outPort">
                <div class="group">
                    <div class="groupname">Output Ports</div>
                    <table>
                        <xsl:for-each select="dataOut|outPort">
                            <xsl:sort select="@index" />
                            <tr>
                                <td class="dt">
                                    <xsl:value-of select="@index" />
                                </td>
                                <td>
                                    <xsl:apply-templates />
                                </td>
                            </tr>
                        </xsl:for-each>
                    </table>
                </div>
            </xsl:if>
        </dl>
    </xsl:template>


    <xsl:template match="intro/table">
        <table class="introtable">
            <xsl:apply-templates />
        </table>
    </xsl:template>

    <xsl:template match="a[starts-with(@href, 'node:')]">
        <a href="http://127.0.0.1:51176/node/?bundle={/knimeNode/osgi-info/@bundle-symbolic-name}&amp;package={/knimeNode/osgi-info/@factory-package}&amp;file={substring-after(@href, 'node:')}">
        <xsl:apply-templates />
        </a>
    </xsl:template>

    <xsl:template match="a[starts-with(@href, 'bundle:')]">
        <a href="http://127.0.0.1:51176/bundle/?bundle={/knimeNode/osgi-info/@bundle-symbolic-name}&amp;file={substring-after(@href, 'bundle:')}">
        <xsl:apply-templates />
        </a>
    </xsl:template>


    <xsl:template match="@*|node()" priority="-1" mode="copy">
        <xsl:copy>
            <xsl:apply-templates select="@*|node()" mode="copy" />
        </xsl:copy>
    </xsl:template>


    <xsl:template match="@*|node()" priority="-1">
        <xsl:copy>
            <xsl:apply-templates select="@*|node()" />
        </xsl:copy>
    </xsl:template>

</xsl:stylesheet>