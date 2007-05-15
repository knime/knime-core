<xsl:transform
 xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
 version="1.0"
>

<xsl:param name="id" select="/*/INDI[1]/@ID"/>

<!-- define keys to allow records to be found by their id -->

<xsl:key name="indi" match="INDI" use="@ID"/>
<xsl:key name="fam" match="FAM" use="@ID"/>


<xsl:template match="/">
    <xsl:variable name="person" select="key('indi', $id)"/>
    <xsl:apply-templates select="$person"/>
</xsl:template>

<xsl:template match="INDI">
	<html>
        <head>
            <xsl:call-template name="css-style"/>
            <xsl:variable name="name">
                <xsl:apply-templates select="NAME"/>
            </xsl:variable>
            <title><xsl:value-of select="$name"/></title>
        </head>

		<!-- choose background color based on gender -->

        <xsl:variable name="color">
            <xsl:choose>
                <xsl:when test="SEX='M'">cyan</xsl:when>
                <xsl:otherwise>pink</xsl:otherwise>
            </xsl:choose>
        </xsl:variable>

        <body bgcolor="{$color}">

            <!-- Show name and parentage -->

            <h1><xsl:apply-templates select="NAME"/></h1>
            <xsl:call-template name="show-parents"/>
            <hr/>

            <table>
            <tr>

                <!-- Show events and attributes -->

                <td width="50%" valign="top">
                    <xsl:call-template name="show-events"/>                        
                </td>
                <td width="20%"/>

                <!-- Show partners, marriages, and children -->

                <td width="30%" valign="top">
                    <xsl:call-template name="show-partners"/>
                </td>
            </tr>
            </table>

            <hr/>

            <!-- Show notes -->

            <xsl:for-each select="NOTE">
                <p class="text"><xsl:apply-templates/></p>
                <xsl:if test="position()=last()"><hr/></xsl:if>
            </xsl:for-each>

        </body>
    </html>
</xsl:template>

<xsl:template name="css-style">
    <style type="text/css">

	H1 {
	    font-family: Verdana, Helvetica, sans-serif;
	    font-size: 18pt;
	    font-weight: bold;
	    color: "#FF0080"
	}

	H2 {
	    font-family: Verdana, Helvetica, sans-serif;
	    font-size: 14pt;
	    font-weight: bold;
	    color: black;
	}

	H3 {
	    font-family: Lucida Sans, Helvetica, sans-serif;
	    font-size: 11pt;
	    font-weight: bold;
	    color: black;
	}

	SPAN.label {
	    font-family: Lucida Sans, Helvetica, sans-serif;
	    font-size: 10pt;
	    font-weight: normal;
        font-style: italic;
	    color: black;
	}

    P,LI,TD {
	    font-family: Lucida Sans, Helvetica, sans-serif;
	    font-size: 10pt;
	    font-weight: normal;
	    color: black;       
	}

    P.text {
	    font-family: Comic Sans MS, Helvetica, sans-serif;
	    font-size: 10pt;
	    font-weight: normal;
	    color: black;       
	}

    </style>
</xsl:template>
   
<xsl:template name="show-parents">
    <xsl:variable name="parents" select="key('fam', FAMC/@REF)"/>
    <xsl:variable name="father" select="key('indi', $parents/HUSB/@REF)"/>
    <xsl:variable name="mother" select="key('indi', $parents/WIFE/@REF)"/>

    <p>
    <xsl:if test="$father">
        <span class="label">Father: </span><xsl:apply-templates select="$father/NAME" mode="link"/>&#xa0;
    </xsl:if>
    <xsl:if test="$mother">
        <span class="label">Mother: </span><xsl:apply-templates select="$mother/NAME" mode="link"/>&#xa0; 
    </xsl:if>
    </p>
</xsl:template>

<xsl:template name="show-events">
    <xsl:for-each select="*">
        <xsl:sort select="substring(DATE, string-length(DATE) - 3)"/>

        <xsl:variable name="event-name">
            <xsl:apply-templates select="." mode="expand"/>
        </xsl:variable>

        <xsl:if test="string($event-name)">
            <h3><xsl:value-of select="$event-name"/></h3>
            <p>
            <xsl:if test="DATE">
                <span class="label">Date: </span><xsl:value-of select="DATE"/><br/>
            </xsl:if>
            <xsl:if test="PLAC">
                <span class="label">Place: </span><xsl:value-of select="PLAC"/><br/>
            </xsl:if>
            </p>
            <xsl:for-each select="NOTE">
                <p class="text"><xsl:apply-templates/></p>
            </xsl:for-each>
        </xsl:if>
    </xsl:for-each>
</xsl:template>

<xsl:template match="BIRT" mode="expand">Birth</xsl:template>
<xsl:template match="DEAT" mode="expand">Death</xsl:template>
<xsl:template match="BURI" mode="expand">Burial</xsl:template>
<xsl:template match="BAPM" mode="expand">Baptism</xsl:template>
<xsl:template match="MARR" mode="expand">Marriage</xsl:template>
<xsl:template match="EVEN" mode="expand"><xsl:value-of select="TYPE"/></xsl:template>
<xsl:template match="*" mode="expand"/>

<xsl:template name="show-partners">
    <xsl:variable name="subject" select="."/>
    <xsl:variable name="partnerships" select="key('fam', FAMS/@REF)"/>
    <xsl:for-each select="$partnerships">
        <xsl:sort select="substring(MARR/DATE, string-length(MARR/DATE) - 3)"/>
        <xsl:variable name="partner" 
            select="key('indi', (HUSB/@REF | WIFE/@REF)[.!=$subject/@ID])"/>

        <xsl:variable name="partner-seq">
            <xsl:choose>
                <xsl:when test="count($subject/FAMS)=1"></xsl:when>
                <xsl:otherwise><xsl:value-of select="position()"/></xsl:otherwise>
            </xsl:choose>
        </xsl:variable>

        <xsl:if test="$partner">
            <h2>Partner <xsl:value-of select="$partner-seq"/></h2>
            <p><xsl:apply-templates select="$partner/NAME" mode="link"/></p>
        </xsl:if>

        <xsl:call-template name="show-events"/>

        <xsl:variable name="children" select="key('indi', CHIL/@REF)"/>
        <xsl:if test="$children">
            <p><span class="label">Children:</span><br/>
            <xsl:for-each select="$children">
                <xsl:sort select="substring(BIRT/DATE, string-length(BIRT/DATE) - 3)"/>
                <xsl:value-of select="substring(BIRT/DATE, string-length(BIRT/DATE) - 3)"/>
                <xsl:text> </xsl:text>
                <xsl:apply-templates select="NAME" mode="link"/><br/>
            </xsl:for-each>
            </p>
        </xsl:if>
    </xsl:for-each>
</xsl:template>

<xsl:template match="NAME" mode="link">
	<a>
		<xsl:attribute name="href">
			<xsl:call-template name="make-href"/>
		</xsl:attribute>
		<xsl:apply-templates/>
	</a>
</xsl:template>

<xsl:template match="S">
    <xsl:text> </xsl:text>
    <b><u><xsl:apply-templates/></u></b>
    <xsl:text> </xsl:text>
</xsl:template>

<xsl:template name="make-href">
	<xsl:value-of select="concat(../@ID, '.html')"/>
</xsl:template>

<xsl:template match="BR"><BR/>
</xsl:template>


</xsl:transform>

