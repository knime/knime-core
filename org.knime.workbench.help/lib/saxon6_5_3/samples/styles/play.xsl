<xsl:stylesheet 
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.1"
    xmlns:saxon="http://icl.com/saxon" 
    extension-element-prefixes="saxon">

<!-- parameter "dir" must be set from the command line: it represents the output directory -->

<!-- this stylesheet uses "preview" mode to process each scene as it is encountered, avoiding -->
<!-- loading the entire document into memory -->

<saxon:preview mode="preview" elements="SCENE PROLOGUE EPILOGUE"/>

<xsl:variable name="backcolor" select="'#FFFFCC'" />
<xsl:variable name="panelcolor" select="'#88FF88'" />
<xsl:variable name="filesep" select="system-property('file.separator')" />  

<xsl:param name="dir"/>

<xsl:template match="PLAY">
    <xsl:if test="not($dir)">
        <xsl:message terminate="yes">Parameter "dir" has not been set</xsl:message>
    </xsl:if>
    <xsl:document href="{$dir}{$filesep}play.html" method="html">
    <HTML>
    <HEAD><TITLE><xsl:apply-templates select="TITLE"/></TITLE></HEAD>
    <BODY BGCOLOR='{$backcolor}'>
        <CENTER>
            <H1><xsl:value-of select="TITLE"/></H1>
            <H3><xsl:apply-templates select="PLAYSUBT"/></H3>
            <I><xsl:apply-templates select="SCNDESCR"/></I>
        </CENTER><BR/><BR/>
        <TABLE><TR>
            <TD WIDTH='350' VALIGN='TOP' BGCOLOR='{$panelcolor}'>
            <xsl:apply-templates select="PERSONAE"/>
            </TD>
            <TD WIDTH='30'></TD>
            <TD VALIGN='TOP'>
            <xsl:apply-templates select="PROLOGUE | ACT | EPILOGUE"/>
            </TD>
        </TR></TABLE><HR/>
    </BODY></HTML>
    </xsl:document>
</xsl:template>

<xsl:template match="ACT/TITLE">
    <CENTER><H3>
	<xsl:apply-templates/>
    </H3></CENTER>
</xsl:template>

<xsl:template match="PLAYSUBT">
	<xsl:apply-templates/>
</xsl:template>

<xsl:template match="PERSONAE">
	<xsl:apply-templates/>
</xsl:template>

<xsl:template match="PERSONAE/TITLE">
    <CENTER><H3>
	<xsl:apply-templates/>
    </H3></CENTER>	
</xsl:template>

<xsl:template match="PERSONAE/PERSONA">
    <TABLE><TR><TD VALIGN="TOP">
	<xsl:apply-templates/>
    </TD></TR></TABLE>
</xsl:template>

<xsl:template match="PGROUP">
    <TABLE><TR><TD WIDTH="160" VALIGN="TOP">
	<xsl:apply-templates select="PERSONA"/>
	</TD><TD WIDTH="20"></TD>
	<TD VALIGN="BOTTOM"><I>
	<xsl:apply-templates select="GRPDESCR"/>
	</I></TD>
	</TR></TABLE>
</xsl:template>

<xsl:template match="PGROUP/PERSONA">
    <xsl:apply-templates/>
    <BR/>
</xsl:template>

<xsl:template match="PGROUP/GRPDESCR">
    <xsl:apply-templates/>
    <BR/>
</xsl:template>

<xsl:template match="SCNDESCR">
	<xsl:apply-templates/>
</xsl:template>

<xsl:template match="ACT">
    <HR/>
	<xsl:apply-templates/>
    <xsl:if test="position()=last()"><HR/></xsl:if>
</xsl:template>

<xsl:template match="SCENE|PROLOGUE|EPILOGUE">
    <xsl:variable name="NR"><xsl:number count="SCENE|PROLOGUE|EPILOGUE" level="any"/></xsl:variable>
    <A HREF="scene{$NR}.html">
        <xsl:value-of select="saxon:getUserData('TITLE')" />
    </A><BR/>
</xsl:template>

<xsl:template match="SCENE|PROLOGUE|EPILOGUE" mode="preview">
    <xsl:variable name="NR"><xsl:number count="SCENE|PROLOGUE|EPILOGUE" level="any"/></xsl:variable>
    <xsl:variable name="play"><xsl:value-of select="ancestor::PLAY/TITLE"/></xsl:variable>
    <xsl:variable name="act"><xsl:value-of select="ancestor::ACT/TITLE"/></xsl:variable>

    <!-- note that global variables are not available in preview mode -->

    <xsl:variable name="backcolor" select="'#FFFFCC'" />
    <xsl:variable name="filesep" select="system-property('file.separator')" />
    
    <!-- save the TITLE because it will be needed later to create a hyperlink -->

    <xsl:variable name="dummy" select="saxon:setUserData('TITLE', string(TITLE))"/>

    <xsl:document href="{$dir}{$filesep}scene{$NR}.html" method="html">
        <HTML>
        <HEAD>
        <TITLE><xsl:value-of select="concat($play, ' ', $act, ': ', TITLE)"/></TITLE>
        </HEAD>
        <BODY BGCOLOR='{$backcolor}'>
        <P>
            <A HREF="play.html"><xsl:value-of select="$play"/></A><BR/>
            <B><xsl:value-of select="$act"/></B><BR/>
        </P>
        <xsl:apply-templates/>
        </BODY>
        </HTML>
    </xsl:document>
</xsl:template>

<xsl:template match="SCENE/TITLE | PROLOGUE/TITLE | EPILOGUE/TITLE">
    <H1><CENTER>
	<xsl:apply-templates/>
	</CENTER></H1><HR/>
</xsl:template>

<xsl:template match="SPEECH">
    <TABLE><TR>
    <TD WIDTH="160" VALIGN="TOP">
	<xsl:apply-templates select="SPEAKER"/>
    </TD>
    <TD VALIGN="TOP">
    <xsl:apply-templates select="STAGEDIR|LINE"/>
    </TD>
	</TR></TABLE>
</xsl:template>

<xsl:template match="SPEAKER">
    <B>
    <xsl:apply-templates/>
    <xsl:if test="not(position()=last())"><BR/></xsl:if>
    </B>
</xsl:template>

<xsl:template match="SCENE/STAGEDIR">
    <CENTER><H3>
	<xsl:apply-templates/>
	</H3></CENTER>
</xsl:template>

<xsl:template match="SPEECH/STAGEDIR">
    <P><I>
	<xsl:apply-templates/>
	</I></P>
</xsl:template>

<xsl:template match="LINE/STAGEDIR">
     [ <I>
	<xsl:apply-templates/>
	</I> ] 
</xsl:template>

<xsl:template match="SCENE/SUBHEAD">
    <CENTER><H3>
	<xsl:apply-templates/>
	</H3></CENTER>
</xsl:template>

<xsl:template match="SPEECH/SUBHEAD">
    <P><B>
	<xsl:apply-templates/>
	</B></P>
</xsl:template>

<xsl:template match="LINE">
	<xsl:apply-templates/>
	<BR/>
</xsl:template>

</xsl:stylesheet>	
