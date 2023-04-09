#!/bin/bash

IFS=$'\n'

JAVA_OPTS="-Xmx128m"
# activate HotswapAgent when using Trava OpenJDK
# https://github.com/TravaOpenJDK/trava-jdk-11-dcevm/releases
# set JAVA_OPTS=%JAVA_OPTS% -XX:HotswapAgent=fatjar

# set to "dev" to see debug logging, "prod" otherwise
export SPRING_PROFILES_ACTIVE=dev

export ALFRESCO_BASE_PATH=http://localhost:8080
export ALFRESCO_USERNAME=admin
export ALFRESCO_PASSWORD=admin

java $JAVA_OPTS -jar alfresco-node-processor.jar $*
