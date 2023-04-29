#!/bin/bash

DIST_DIR=alfresco-node-processor

if [ -e $DIST_DIR ]; then rm -rf $DIST_DIR; fi
mkdir -p $DIST_DIR/log
mvn package -DskipTests -Dlicense.skip=true
cp target/alfresco-node-processor.jar $DIST_DIR
cp src/main/resources/example*.json $DIST_DIR
cp run.bat $DIST_DIR
cp run.sh $DIST_DIR
chmod +x $DIST_DIR/run.sh