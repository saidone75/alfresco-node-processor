@echo off

set JAVA_OPTS=-Xms256m -Xmx256m -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=0.0.0.0:9000
:: activate HotswapAgent when using Trava OpenJDK
:: https://github.com/TravaOpenJDK/trava-jdk-11-dcevm/releases
:: set JAVA_OPTS=%JAVA_OPTS% -XX:HotswapAgent=fatjar

:: set to "dev" to see debug logging, "prod" otherwise
set SPRING_PROFILES_ACTIVE=prod

set ALFRESCO_BASE_PATH=http://localhost:8080
set MODE=ro

:: use mvn for running application without package it
:: mvn spring-boot:run -Dspring-boot.run.jvmArguments="%JAVA_OPTS%"
java %JAVA_OPTS% -jar alfresco-node-processor.jar