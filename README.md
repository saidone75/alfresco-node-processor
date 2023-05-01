# Alfresco Node processor
_Do things with nodes._

A modern, threaded and easily customizable Spring Boot Application that - given a means for collecting nodes - do something with them.

Think about this as a template for your application.

Pull requests are welcome!
## Customize
If none of the predefined Collectors/Processors meet your needs, simply write your own by extending the abstract ones. Just inject the required handler (e.g., NodesApi) and override the relevant methods.
### Collecting nodes
### Processing nodes
```java
@Component
@Slf4j
public class LogNodeNameProcessor extends AbstractNodeProcessor {

    @Autowired
    private NodesApi nodesApi;

    @Override
    public void processNode(String nodeId, Config config) {
        var node = Objects.requireNonNull(nodesApi.getNode(nodeId, null, null, null).getBody()).getEntry();
        log.debug("node name --> {}", node.getName());
    }

}
```
## Build
Java and Maven required

`mvn package -DskipTests -Dlicense.skip=true`

look at the `build.sh` or `build.bat` scripts for creating a convenient distribution package.
## Application config
Application is configured through these ENV variables, hence `run.sh` and `run.bat` scripts are a convenient way to run the program (default value in parentheses):
- SPRING_PROFILES_ACTIVE (`dev`)
- ALFRESCO_BASE_PATH (`http://localhost:8080`)
- ALFRESCO_USERNAME (`admin`)
- ALFRESCO_PASSWORD (`admin`)
- SEARCH_BATCH_SIZE (`100`)
- QUEUE_SIZE (`1000`)
- CONSUMER_THREADS (`4`)
- CONSUMER_TIMEOUT (`5000`)
## Testing
For integration tests just change configuration and point it to an existing Alfresco installation, or use `alfresco.(sh|bat)` script to start it with docker.
## Run
`$ java -jar alfresco-node-processor.jar ./example.json` or use the provided `run.sh` and `run.bat` scripts.
## License
Copyright (c) 2023 Saidone

Distributed under the GNU General Public License v3.0
