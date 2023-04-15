#!/bin/bash

JAVA_OPTS="-Xmx128m -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=0.0.0.0:9000"
# activate HotswapAgent when using Trava OpenJDK
# https://github.com/TravaOpenJDK/trava-jdk-11-dcevm/releases
# JAVA_OPTS="$JAVA_OPTS -XX:HotswapAgent=fatjar"

# set to "dev" to see debug logging, "prod" otherwise
export SPRING_PROFILES_ACTIVE=dev

# Alfresco config
export ALFRESCO_BASE_PATH=http://localhost:8080
export ALFRESCO_USERNAME=admin
export ALFRESCO_PASSWORD=admin

# application parameters
#export SEARCH_BATCH_SIZE=100
#export QUEUE_SIZE=1000
#export CONSUMER_THREADS=4
#export CONSUMER_TIMEOUT=5000

java $JAVA_OPTS -jar alfresco-node-processor.jar $*
