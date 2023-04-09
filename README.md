# Alfresco Node processor
_Do things with nodes._

A modern, threaded and easily customizable Spring Boot Application that query for nodes in Alfresco and do something with them.
## Build
Java and Maven required

`mvn package -DskipTests -Dlicense.skip=true`

look at the `build.sh` or `build.bat` scripts to build a distribution package.
## Config
Application is configured through these ENV variables, hence `run.sh` and `run.bat` scripts are a convenient way to run the program (default value in parentheses):
- SPRING_PROFILES_ACTIVE (`dev`)
- ALFRESCO_BASE_PATH (`http://localhost:8080`)
- ALFRESCO_USERNAME (`admin`)
- ALFRESCO_PASSWORD (`admin`)
- SEARCH_BATCH_SIZE (`100`)
- QUEUE_SIZE (`1000`)
- CONSUMER_THREADS (`4`)
- CONSUMER_TIMEOUT (`5000`)
## Run
`$ java -jar alfresco-node-processor.jar ./example.json`
## License
Copyright (c) 2023 Saidone

Distributed under the GNU General Public License v3.0
