<?xml version="1.0" encoding="utf-8"?>
<!-- XSLT transformation to remove namespace prefixes and attributes -->
<xsl:stylesheet version="1.0"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

    <!-- XSLT transformations work by matching parts of the input document against templates and determining the
         output for each match based on the template contents. `xsl:element` produces a new element node,
         `xsl:attribute` produces a new attribute node. -->

    <!--  matches any element node (text nodes, comments, processing instructions, attribute nodes, ...)  -->
    <xsl:template match="*">
        <!--  output element with only its local name part (omitting namespace prefix)    -->
        <!-- `create-element` dynamically creates a new output element. The element's name should be
                the local name of the current node. The "current node" is the node best matched by a template. -->
        <xsl:element name="{local-name(current())}">
            <!-- `apply-templates` processes the children of the context node. With `select` we can process only a
                    subset of children. Here, we select attribute nodes (`@*`) and any other child node (`node()`) -->
            <xsl:apply-templates select="@* | node()" />
        </xsl:element>
    </xsl:template>

    <!--  matches any attribute node  -->
    <xsl:template match="@*">
        <!-- output an attribute node, but with only the local name (omitting namespace prefix) -->
        <xsl:attribute name="{local-name(current())}">
            <!-- output the value of the attribute  -->
            <xsl:value-of select="current()" />
        </xsl:attribute>
    </xsl:template>
</xsl:stylesheet>
