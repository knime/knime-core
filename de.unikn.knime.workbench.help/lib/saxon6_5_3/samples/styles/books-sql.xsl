<!-- This stylesheet demonstrates the use of element extensibility with SAXON -->

<xsl:stylesheet
	xmlns:sql="http://icl.com/saxon/extensions/com.icl.saxon.sql.SQLElementFactory"
 	xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0"
	xmlns:saxon="http://icl.com/saxon"
 	extension-element-prefixes="saxon">

<!-- insert your database details here, or supply them in parameters -->
<xsl:param name="driver" select="'sun.jdbc.odbc.JdbcOdbcDriver'"/>
<xsl:param name="database" select="'jdbc:odbc:test'"/>  
<xsl:param name="user"/>
<xsl:param name="password"/>

<!-- This stylesheet writes the book list to a SQL database -->

<xsl:variable name="count" select="0" saxon:assignable="yes"/>

<xsl:template match="BOOKLIST">
    <xsl:if test="not(element-available('sql:connect'))">
        <xsl:message>sql:connect is not available</xsl:message>
    </xsl:if>
    <xsl:message>Connecting to <xsl:value-of select="$database"/>...</xsl:message>
    <sql:connect driver="{$driver}" database="{$database}" 
                 user="{$user}" password="{$password}"
		         xsl:extension-element-prefixes="sql">
	<xsl:fallback>
	    <xsl:message terminate="yes">SQL extensions are not installed</xsl:message>
        </xsl:fallback>
    </sql:connect>
    <xsl:message>Connected...</xsl:message>
    <xsl:apply-templates select="BOOKS"/>
    <xsl:message>Inserted <xsl:value-of select="$count"/> records.</xsl:message>
    <sql:close xsl:extension-element-prefixes="sql">
       <xsl:fallback/>
    </sql:close>
</xsl:template>

<xsl:template match="BOOKS">
    <xsl:for-each select="ITEM">
    	<sql:insert table="book" xsl:extension-element-prefixes="sql">
	    <sql:column name="title" select="TITLE"/>
            <sql:column name="author" select="AUTHOR"/>
            <sql:column name="category" select="@CAT"/>
    	</sql:insert>
	<saxon:assign name="count" select="$count+1"/>
    </xsl:for-each>
</xsl:template>

</xsl:stylesheet>	
