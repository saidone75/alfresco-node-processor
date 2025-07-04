# Alfresco Node Processor
_Giro... vedo nodi... faccio cose..._

_Do things with nodes._

[![License: GPL v3](https://img.shields.io/badge/License-GPLv3-blue.svg)](https://www.gnu.org/licenses/gpl-3.0)
[![Javadoc](https://img.shields.io/badge/Javadoc-API-blue.svg)](https://saidone75.github.io/alfresco-node-processor/javadoc/)
![Java CI](https://github.com/saidone75/alfresco-node-processor/actions/workflows/build.yml/badge.svg)
![CodeQL](https://github.com/saidone75/alfresco-node-processor/actions/workflows/codeql.yml/badge.svg)

A modern, threaded and easily customizable Spring Boot Application that - given a means for collecting nodes - do something with them.

Think about this as a template for your application.

Pull requests are welcome!
## Features
- `QueryNodeCollector` collect nodes with Alfresco FTS queries
- `NodeListCollector` reads node IDs from a file
- `NodeTreeCollector` walks the repository tree
- `DeleteNodeProcessor` deletes or trashes nodes
- `MoveNodeProcessor` relocates nodes under a new parent
- `AddAspectsAndSetPropertiesProcessor` adds aspects and properties
- `SetPermissionsProcessor` applies permissions and inheritance
- `DownloadNodeProcessor` saves node content and metadata to the filesystem
- `ChainingNodeProcessor` executes multiple processors sequentially
- Queue based architecture with configurable consumer threads
- Easily extensible by implementing `AbstractNodeCollector` and `AbstractNodeProcessor`

## Customize
If none of the predefined Collectors/Processors meet your needs, simply write your own by extending the abstract ones. Just inject the required handlers (e.g., NodesApi) and override the relevant methods.
### Collecting nodes
#### QueryNodeCollector
The QueryNodeCollector takes an Alfresco FTS query, execute it on a separate thread and feed the queue:
```json
"collector": {
  "name": "QueryNodeCollector",
  "args": {
    "query": "PATH:'/app:company_home/*' AND TYPE:'cm:folder'"
  }
}
```
the default page size for search is `100` and can be modified by passing an additional argument to the collector:
```json
"batch-size": 1000
```
#### NodeListCollector
The NodeListCollector takes an input file containing a list of node-id with each id on a separate line, e.g.:
```
e72b6596-ec2e-4279-b490-3a03b119d8de
d78c0036-15c0-43cf-89e4-cd198d14b626
1a7ecc34-de06-45ed-85c0-76f8355f3724
```
and the path of the file need to be specified in the config:
```json
  "collector": {
      "name": "NodeListCollector",
      "args": {
        "node-list-file": "/tmp/node-ids.txt"
      }
  }
```
#### NodeTreeCollector
Iteratively walk the tree starting from a folder node given either its id or its repository path:
```json
"collector": {
  "name": "NodeTreeCollector",
  "args": {
    "path": "/"
  }
}
```
The collector automatically descends into folders.
The default page size for listNodeChildren is `100` and can be modified by passing an additional argument to the collector:
```json
"batch-size": 200
```
### Processing nodes
#### DeleteNodeProcessor
Delete the collected nodes, set the `permanent` flag to true if you want to delete the nodes directly rather than move them into the trashcan:
```json
"processor": {
  "name": "DeleteNodeProcessor",
  "args": {
    "permanent": true
  }
}
```     
#### AddAspectsAndSetPropertiesProcessor
Add a list of aspects and apply a map of properties to the collected nodes:
```json
"processor": {
  "name": "AddAspectsAndSetPropertiesProcessor",
  "args": {
    "properties": {
      "cm:publisher": "saidone",
      "cm:contributor": "saidone"
    },
    "aspects": [
      "cm:dublincore"
    ]
  }
}
```
#### SetPermissionsProcessor
Apply a list of permissions and set inheritance flag to the collected nodes:
```json
"processor": {
  "name": "SetPermissionsProcessor",
  "args": {
    "permissions": {
      "isInheritanceEnabled": false,
      "locallySet": [
        {
          "authorityId": "GROUP_EVERYONE",
          "name": "Collaborator",
          "accessStatus": "ALLOWED"
        }
      ]
    }
  }
}
```
#### MoveNodeProcessor
Move collected nodes to a new folder identified either by its node-id or by the repository path:
```json
"processor": {
  "name": "MoveNodeProcessor",
  "args": {
    "target-parent-id": "e72b6596-ec2e-4279-b490-3a03b119d8de"
  }
}
```
#### DownloadNodeProcessor
Download node content and metadata to a local directory in a format compatible with bulk import:
```json
"processor": {
  "name": "DownloadNodeProcessor",
  "args": {
    "output-dir": "/tmp/export"
  }
}
```
#### ChainingNodeProcessor
Execute a list of processors sequentially on each node:
```json
"processor": {
  "name": "ChainingNodeProcessor",
  "args": {
    "processors": [
      { "name": "LogNodeNameProcessor" },
      { "name": "VoidProcessor" }
    ]
  }
}
```
#### Custom processors
Custom processors can be easily created by extending the AbstractNodeProcessor and overriding the `processNode` method:
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
## Application global config
Global configuration is stored in `config/application.yml` file, the relevant parameters are:

| Parameter/env variable | Key in `application.yml` | Default value | Purpose |
|------------------------|--------------------------|---------------|--------------------------------------------------|
| ALFRESCO_BASE_PATH     | `content.service.url` | http://localhost:8080 | scheme, host and port of the Alfresco server |
| ALFRESCO_USERNAME      | `content.service.security.basicAuth.username` | admin | Alfresco user |
| ALFRESCO_PASSWORD      | `content.service.security.basicAuth.password` | admin | password for the Alfresco user |
| QUEUE_SIZE             | `application.queue-size` | 1000 | size of the node-uuid queue |
| CONSUMER_THREADS       | `application.consumer-threads` | 4 | number of consumers that are executed simultaneously |
| CONSUMER_TIMEOUT       | `application.consumer-timeout` | 5000 | milliseconds after which a consumer gives up waiting for data in the queue |
| READ_ONLY              | `application.read-only` | true | when true, mutating operations on nodes are skipped |
## Testing
For integration tests just change configuration and point it to an existing Alfresco installation, or use `alfresco.(sh|bat)` script to start it with docker.
## Run
`$ java -jar anp.jar -c example-log-node-name.json`
## Further documentation

See [Javadoc](https://saidone75.github.io/alfresco-node-processor/)

## License
Copyright (c) 2023-2025 Saidone

Distributed under the GNU General Public License v3.0
