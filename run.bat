@echo off

set JAVA_OPTS=-Xms128m -Xmx128m -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=0.0.0.0:9000
:: activate HotswapAgent when using Trava OpenJDK
:: https://github.com/TravaOpenJDK/trava-jdk-11-dcevm/releases
:: set JAVA_OPTS=%JAVA_OPTS% -XX:HotswapAgent=fatjar

:: set to "dev" to see debug logging, "prod" otherwise
set SPRING_PROFILES_ACTIVE=dev

:: Alfresco config
set ALFRESCO_BASE_PATH=http://localhost:8080
set ALFRESCO_USERNAME=admin
set ALFRESCO_PASSWORD=admin

:: application parameters
:: set SEARCH_BATCH_SIZE=100
:: set QUEUE_SIZE=1000
:: set CONSUMER_THREADS=4
:: set CONSUMER_TIMEOUT=5000

:: use mvn for running application without packaging it
:: mvn spring-boot:run -Dspring-boot.run.jvmArguments="%JAVA_OPTS%"
java %JAVA_OPTS% -jar alfresco-node-processor.jar %1