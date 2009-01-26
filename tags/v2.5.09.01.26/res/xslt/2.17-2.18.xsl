<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns="http://www.w3.org/1999/xhtml">

<!-- XSLT Stylesheet to transform Topali 2.16 and 2.17 projects to Version 2.18 -->
<!-- 
Notes:
* Substitution Model choice will be ignored (set to default)
* ModelGenerator results will be ignored
-->

<!-- Ignore Model nodes -->
<xsl:template match="model">
</xsl:template>


<!--  Ignore Modelgenerator results -->
<xsl:template match='results[@xsi:type="MGResult"]'>
</xsl:template>


<!-- And just copy the rest -->
<xsl:template match="*">
	<xsl:copy>
		<xsl:for-each select="@*">
			<xsl:copy/>
		</xsl:for-each>
  		<xsl:apply-templates/>
 	</xsl:copy>
</xsl:template>

</xsl:stylesheet>