#! /bin/bash

IFS=$'\n'

JAVA=java
JAVA_OPTS="-Xmx256m"
# activate HotswapAgent when using Trava OpenJDK
# https://github.com/TravaOpenJDK/trava-jdk-11-dcevm/releases
# set JAVA_OPTS=%JAVA_OPTS% -XX:HotswapAgent=fatjar

# set to "dev" to see debug logging, "prod" otherwise
export SPRING_PROFILES_ACTIVE=dev

# search batch size
export SEARCH_BATCH_SIZE=1000

# RW for read/write
export MODE=RO

export ALFRESCO_BASE_PATH=http://localhost:8080
export ALFRESCO_USERNAME=admin
export ALFRESCO_PASSWORD=admin

$JAVA $JAVA_OPTS -jar alfresco-node-processor.jar
