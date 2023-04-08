@echo off

SET DIST_DIR=alfresco-node-processor
IF EXIST %DIST_DIR% RMDIR /S /Q %DIST_DIR%
MKDIR %DIST_DIR%\log
CALL mvn package -DskipTests -Dlicense.skip=true
COPY target\alfresco-node-processor.jar %DIST_DIR%
COPY run.bat %DIST_DIR%
COPY run.sh %DIST_DIR%