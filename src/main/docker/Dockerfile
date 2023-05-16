FROM alfresco/alfresco-content-repository-community:7.4.0.1

ARG TOMCAT_DIR=/usr/local/tomcat

USER root

COPY alfresco-global.properties $TOMCAT_DIR/shared/classes/alfresco-global.properties
COPY dev-log4j.properties $TOMCAT_DIR/shared/classes/alfresco/extension
COPY disable-webscript-caching-context.xml $TOMCAT_DIR/shared/classes/alfresco/extension

# Custom model
#COPY models/*.* $TOMCAT_DIR/shared/classes/alfresco/extension/models/
#COPY custom-model-context.xml $TOMCAT_DIR/shared/classes/alfresco/extension/

# Copy Dockerfile to avoid an error if no license file exists
COPY Dockerfile license/*.* $TOMCAT_DIR/webapps/alfresco/WEB-INF/classes/alfresco/extension/license/