<?xml version="1.0" encoding="ISO-8859-1" ?>
<!DOCTYPE helpset
  PUBLIC "-//Sun Microsystems Inc.//DTD JavaHelp HelpSet Version 1.0//EN"
         "http://java.sun.com/products/javahelp/helpset_1_0.dtd">

<helpset version="1.0">
   
   <!-- maps -->
   <maps>
     <homeID>intro</homeID>
     <mapref location="help.jhm" />
   </maps>
   
   <!-- views -->
   <view>
      <name>TOC</name>
      <label>Table Of Contents</label>
      <type>javax.help.TOCView</type>
      <data>toc.xml</data>
   </view>
   
   <!-- presentation -->
  <presentation default="true" displayviews="true" displayviewimages="false">
    <name>TOPALi</name>
    <size width="700" height="500" />
    <location x="50" y="50" />
    <title>TOPALi V2 Help</title>
    <image>helpIcon</image>
    <toolbar>
		<helpaction image="backIcon">javax.help.BackAction</helpaction>
		<helpaction image="forwardIcon">javax.help.ForwardAction</helpaction>
		<helpaction image="homeIcon">javax.help.HomeAction</helpaction>
		<helpaction>javax.help.SeparatorAction</helpaction>
		<!--<helpaction image="reloadIcon">javax.help.ReloadAction</helpaction>-->
		<!--<helpaction image="addBookmarkIcon">javax.help.FavoritesAction</helpaction>-->
		<helpaction image="printIcon">javax.help.PrintAction</helpaction>
		<helpaction image="printSetupIcon">javax.help.PrintSetupAction</helpaction>
	</toolbar>
  </presentation>
</helpset>