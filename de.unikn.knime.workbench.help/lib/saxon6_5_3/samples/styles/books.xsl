<xsl:transform
 xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
 version="1.0"
 xmlns:saxon="http://icl.com/saxon"
 exclude-result-prefixes="saxon"
>

<!-- This style sheet displays the books.xml file. It is designed to exercise a variety   -->
<!-- of features of the SAXON XSL language and does not do everything in the simplest way -->

<xsl:key name="authkey" match="ITEM" use="AUTHOR"/>
<xsl:key name="codekey" match="CATEGORY" use="@CODE"/>
<xsl:decimal-format name="comma" decimal-separator="," grouping-separator="."/>

<xsl:include href="books-attsets.xsl"/>


<xsl:variable name="bgcolor">x00ffff</xsl:variable>
<xsl:variable name="fontcolor" select="'xff0080'"/>
<xsl:variable name="categories" select="//CATEGORY"/>
<xsl:variable name="now" xmlns:Date="/java.util.Date">
    <xsl:value-of select="Date:toString(Date:new())"/>
</xsl:variable>

<xsl:param name="top-author">Bonner</xsl:param>

<xsl:preserve-space elements="AUTHOR TITLE"/>



<xsl:template match="/">

    <HTML>

    <xsl:comment>Generated at <xsl:value-of select="$now"/> using <xsl:value-of select="system-property('xsl:vendor')"/></xsl:comment>

    <xsl:call-template name="header">
        <xsl:with-param name="title" select="'Book List'"/>
    </xsl:call-template>
    <BODY LEFTMARGIN="100" BGCOLOR="{$bgcolor}" xsl:use-attribute-sets="attset1">
        <xsl:attribute name="LEFTMARGIN">120</xsl:attribute>
    <FONT COLOR="{$fontcolor}">
    <xsl:element name="div" use-attribute-sets="attset2">
    <xsl:apply-templates/>
    </xsl:element>
    </FONT>
    </BODY>
    </HTML>
</xsl:template>

<xsl:template name="header">
    <xsl:param name="title" select="'Default Title'"/>
    <HEAD>
    <TITLE><xsl:value-of select="$title"/></TITLE></HEAD>
</xsl:template>

<xsl:template match="BOOKLIST">

    <H2>List of categories</H2>
    <xsl:apply-templates select="$categories">
        <xsl:sort select="@DESC" order="descending"/>
        <xsl:sort select="@CODE" order="descending"/>
        <xsl:with-param name="p2"><i>35</i></xsl:with-param>
    </xsl:apply-templates>

    <H2>This week's top author is <xsl:value-of select="$top-author"/></H2>
    <xsl:variable name="top-authors-books" select="key('authkey', $top-author)"/>
    
    <p>We stock the following <xsl:value-of select="count($top-authors-books)"/> books by this author:</p>
    <ul>
    <xsl:for-each select="$top-authors-books">
        <li><xsl:value-of select="TITLE"/></li>
    </xsl:for-each>
    </ul>
    
    <p>But this one is the first</p>
    <xsl:for-each select="$top-authors-books[1]">
        <p><xsl:value-of select="TITLE"/> costing $<xsl:value-of select="PRICE"/></p>
    </xsl:for-each>


    <p>This author has written books in the following categories:</p>
    <ul>
    <xsl:for-each select="key('codekey', $top-authors-books/@CAT)/@DESC">
        <li><xsl:value-of select="."/></li>
    </xsl:for-each>
    </ul>

    <p>The average price of these books is: <xsl:text/>
        <xsl:value-of select="format-number(
                                 sum($top-authors-books/PRICE)
                                   div count($top-authors-books),
                                     '$####.00')"/>
    </p>


    <H2>A complete list of books, grouped by author</H2>
    <xsl:apply-templates select="child :: BOOKS" mode="by-author"/>

    <H2>A complete list of books, grouped by category</H2>
    <xsl:apply-templates select="BOOKS" mode="by-category"/>

</xsl:template>   

<xsl:template match="BOOKS" mode="by-author">
    <div xsl:extension-element-prefixes="saxon">
    <saxon:group select="ITEM" group-by="AUTHOR">
    <xsl:sort select="AUTHOR" order="ascending"/>
    <xsl:sort select="TITLE" order="ascending"/>
    <h3>AUTHOR: <xsl:value-of select="AUTHOR"/>
         (<xsl:value-of select="position()"/> of <xsl:value-of select="last()"/>)</h3>
        <TABLE>
        <saxon:item>
            <TR><TD WIDTH="100" VALIGN="TOP"><xsl:number format="i"/></TD>
                <TD>
                TITLE: <xsl:value-of select="TITLE"/><BR/>
                CATEGORY: <xsl:value-of select="id(@CAT)/@DESC" />
                        (<xsl:value-of select="@CAT" />)
                </TD></TR>
        </saxon:item>
        </TABLE><HR/>
    </saxon:group>
    </div>
</xsl:template>

<xsl:template match="BOOKS" mode="by-category">
    <div xsl:extension-element-prefixes="saxon">
    <saxon:group select="ITEM" group-by="@CAT">
    <xsl:sort select="id(@CAT)/@DESC" order="ascending"/>
    <xsl:sort select="TITLE" order="ascending"/>
    <h3>CATEGORY: <xsl:value-of select="id(@CAT)/@DESC" /></h3>
        <OL>
        <saxon:item>
            <LI>AUTHOR: <xsl:value-of select="AUTHOR"/><BR/>
                TITLE: <xsl:value-of select="TITLE"/></LI>                
        </saxon:item>
        </OL><HR/>
    </saxon:group>
    </div>
</xsl:template>

<xsl:template match="CATEGORY" >
    <H4>CATEGORY <xsl:number value="position()" format="I"/></H4>
    <TABLE>
    <xsl:for-each select="@*[position()&lt;120]">
        <TR>
        <TD><xsl:value-of select="position()"/></TD>
        <TD><xsl:value-of select="name(.)"/></TD>
        <TD><xsl:value-of select="."/></TD>
        </TR>
    </xsl:for-each>
    </TABLE>
    <HR/>
</xsl:template>

<xsl:template match="@*">
<xsl:copy/>
</xsl:template>

<xsl:template match="processing-instruction()">
<p>Default PI template called</p>
Name = <xsl:value-of select="name()"/>
Value = <xsl:value-of select="."/>
Copy: <xsl:copy/>
</xsl:template>

<xsl:template match="processing-instruction('testpi1')" priority="2">
<p>PI template for testpi1 called:
number (any) = <xsl:number level="any"/>
number (single) = <xsl:number level="single"/>
number (multiple) = <xsl:number level="multiple" count="node()"/>
</p>
</xsl:template>

<xsl:template match="id('C')" priority="93.7">
Found category C!
</xsl:template>

</xsl:transform>	
