<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE stylesheet [
<!ENTITY css SYSTEM "style.css">
]>
<!-- Stylesheet for distance descriptions introduced with KNIME 2.10 -->
<xsl:stylesheet version="1.0"
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:t="http://knime.org/missingval/v1.0"
	xmlns="http://www.w3.org/1999/xhtml">
	<xsl:template match="t:distance">
		<!-- <h1> -->

		<xsl:apply-templates select="t:fullDescription/t:p" />

		<xsl:if test="@deprecated = 'true'">
			<h4 class="deprecated">Deprecated</h4>
		</xsl:if>
		

		<xsl:if test="t:fullDescription/t:option">
			<h2>Distance Options</h2>
			<dl>
				<xsl:apply-templates select="t:fullDescription/t:option" />
			</dl>
		</xsl:if>

		<div id="origin-bundle">
			This description is contained in
			<em>
				<xsl:value-of select="osgi-info/@bundle-name" />
			</em>
			provided by
			<em>
				<xsl:value-of select="osgi-info/@bundle-vendor" />
			</em>
			.
		</div>
	</xsl:template>

	<!-- <xsl:template match="t:tab"> -->
	<!-- <div class="group"> -->
	<!-- <div class="groupname"> -->
	<!-- <xsl:value-of select="@name" /> -->
	<!-- </div> -->
	<!-- <dl> -->
	<!-- <xsl:apply-templates /> -->
	<!-- </dl> -->
	<!-- </div> -->
	<!-- </xsl:template> -->

	<xsl:template match="t:option">
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

	<xsl:template match="t:a[starts-with(@t:href, 'node:')]">
		<a
			href="http://127.0.0.1:51176/node/?bundle={/t:distance/osgi-info/@bundle-symbolic-name}&amp;package={/t:knimeNode/osgi-info/@factory-package}&amp;file={substring-after(@href, 'node:')}">
			<xsl:apply-templates />
		</a>
	</xsl:template>

	<xsl:template match="t:a[starts-with(@t:href, 'bundle:')]">
		<a
			href="http://127.0.0.1:51176/bundle/?bundle={/t:distance/osgi-info/@bundle-symbolic-name}&amp;file={substring-after(@href, 'bundle:')}">
			<xsl:apply-templates />
		</a>
	</xsl:template>

	<xsl:template match="*">
		<xsl:element name="{local-name(.)}">
			<xsl:apply-templates select="@*|node()" />
		</xsl:element>
	</xsl:template>

	<xsl:template match="@*">
		<xsl:attribute name="{local-name()}">
            <xsl:value-of select="." />
        </xsl:attribute>
	</xsl:template>

	<xsl:template match="@*|node()" priority="-1">
		<xsl:copy>
			<xsl:apply-templates select="@*|node()" />
		</xsl:copy>
	</xsl:template>
</xsl:stylesheet>