<xsl:transform
 xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
 version="1.0"
>

<!-- This style sheet totals the prices of the books. It is designed to be used
     with the JDOMExample sample application, which adds a VALUE attribute to the data
     in books.xml before doing a transformation -->


<xsl:template match="/">

    <HTML>
    <BODY>
    <p>Total value of books in stock: <xsl:value-of 
            select="format-number(sum(//ITEM/@VALUE),'#0.00')"/></p>
    </BODY>
    </HTML>
</xsl:template>

</xsl:transform>	
