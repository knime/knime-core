<?xml version="1.0"?>

<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
  <xsl:output method="html" indent="yes"/>
    
  <xsl:template match="/">
    <xsl:apply-templates/>
  </xsl:template>

  <xsl:template match="s1">
    <html>
      <head><title><xsl:value-of select="@title"/></title>
      <xsl:comment>HTML generated using <xsl:text/>
          <xsl:value-of select="system-property('xsl:vendor')"/>
      </xsl:comment>
      </head>
      <body  bgcolor="#ffffff" text="#000000">
        <xsl:apply-templates select="s2"/>
      </body>
    </html>
  </xsl:template>

  <xsl:template match="s2">
    <table width="100%" border="0" cellspacing="0" cellpadding="4">
      <tr>
        <td bgcolor="#006699">
          <font color="#ffffff" size="+1">
            <b><xsl:value-of select="@title"/></b>
          </font>
        </td>
      </tr>
    </table>
    <xsl:apply-templates/>
    <br/>
  </xsl:template>

  <xsl:template match="p">
    <p><xsl:apply-templates/></p>
  </xsl:template>

  <xsl:template match="note">
    <table border="0" width="100%">
      <tr>
        <td width="20">&#160;</td>
        <td bgcolor="#88aacc">
          <font size="-1"><i>NOTE: <xsl:apply-templates/></i></font>
        </td>
        <td width="20">&#160;</td>
      </tr>
    </table>
  </xsl:template>
  
  <xsl:template match="ul">
    <ul><xsl:apply-templates/></ul>
  </xsl:template>

  <xsl:template match="ol">
    <ol><xsl:apply-templates/></ol>
  </xsl:template>
  
  <xsl:template match="gloss">
    <dl><xsl:apply-templates/></dl>
  </xsl:template>
   <!-- <term> contains a single-word, multi-word or symbolic 
       designation which is regarded as a technical term. --> 
  <xsl:template match="term">
    <dfn><xsl:apply-templates/></dfn>
  </xsl:template>

  <xsl:template match="label" priority="1">
    <dt><xsl:apply-templates/></dt>
  </xsl:template>

  <xsl:template match="item" priority="2">
    <dd>
      <xsl:apply-templates/>
    </dd>
  </xsl:template>

  <xsl:template match="table">
    <p align="center"><table border="0"><xsl:apply-templates/></table></p>
  </xsl:template>

  <xsl:template match="source">
    <table border="0" width="100%">
      <tr>
        <td width="20">&#160;</td>
        <td bgcolor="#88aacc"><pre><xsl:apply-templates/></pre></td>
        <td width="20">&#160;</td>
      </tr>
    </table>
  </xsl:template>

  <xsl:template match="li">
    <li><xsl:apply-templates/></li>
  </xsl:template>

  <xsl:template match="tr">
    <tr><xsl:apply-templates/></tr>
  </xsl:template>

  <xsl:template match="th">
    <td bgcolor="#006699" align="center">
      <font color="#ffffff"><b><xsl:apply-templates/></b></font>
    </td>
  </xsl:template>

  <xsl:template match="td">
    <td bgcolor="#88aacc"><xsl:apply-templates/>&#160;</td>
  </xsl:template>

  <xsl:template match="tn">
    <td>&#160;</td>
  </xsl:template>

  <xsl:template match="em">
    <b><xsl:apply-templates/></b>
  </xsl:template>

  <xsl:template match="ref">
    <i><xsl:apply-templates/></i>
  </xsl:template>

  <xsl:template match="code">
    <code><xsl:apply-templates/></code>
  </xsl:template>

  <xsl:template match="br">
    <br/>
  </xsl:template>


  <xsl:template match="jump">
    <a href="{@href}" target="_top"><xsl:apply-templates/></a>
  </xsl:template>  

  <xsl:template match="anchor">
    <a name="{@id}"> </a>
  </xsl:template>

  <xsl:template match="img">
    <img src="{@src}" align="right" border="0" vspace="4" hspace="4"/>
  </xsl:template>
  
</xsl:stylesheet>