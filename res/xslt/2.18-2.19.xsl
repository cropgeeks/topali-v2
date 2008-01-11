<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns="http://www.w3.org/1999/xhtml">

<!-- XSLT Stylesheet to transform Topali 2.18 projects to Version 2.19 -->
<!-- 
Notes:
-->

<!-- Just copy everything -->
<xsl:template match="*">
	<xsl:copy>
		<xsl:for-each select="@*">
			<xsl:copy/>
		</xsl:for-each>
  		<xsl:apply-templates/>
 	</xsl:copy>
</xsl:template>

</xsl:stylesheet>