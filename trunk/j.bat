@echo off

set lib=web\WEB-INF\lib

set topali=%lib%\
set topali=%topali%;%lib%\axis.jar
set topali=%topali%;%lib%\castor-1.0.4-xml.jar
set topali=%topali%;%lib%\commons-codec-1.3.jar
set topali=%topali%;%lib%\commons-discovery-0.2.jar
set topali=%topali%;%lib%\commons-httpclient-3.0.jar
set topali=%topali%;%lib%\commons-logging-1.0.4.jar
set topali=%topali%;%lib%\doe.jar
set topali=%topali%;%lib%\forester.jar
set topali=%topali%;%lib%\jaxrpc.jar
set topali=%topali%;%lib%\jcommon-1.0.0.jar
set topali=%topali%;%lib%\jfreechart-1.0.1.jar
set topali=%topali%;%lib%\jh.jar
set topali=%topali%;%lib%\office-2.0.jar
set topali=%topali%;%lib%\pal.jar
set topali=%topali%;%lib%\saaj.jar
set topali=%topali%;%lib%\sbrn-commons.jar
set topali=%topali%;%lib%\swing-layout-1.0.jar
set topali=%topali%;%lib%\vamsas.jar
set topali=%topali%;%lib%\xercesImpl.jar

java -Xmx256m -cp .;classes;res;%topali% topali.gui.TOPALi %1