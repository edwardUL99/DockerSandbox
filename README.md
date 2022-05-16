DockerSandbox
==
DockerSandbox is a Java library that allows code/commands to be executed in an isolated Docker container. It provides
a means of sand-boxing the execution of code onto the docker machine rather than running it on the host machine.

While, usually, when a Docker container shuts down, any files that were not persisted to the host machine are lost,
and thus, you would not be able to share the files between different containers. The solution to this is using Docker volumes
but with this, there is the issue of extra manual setup and cleanup after executing code.

This library helps abstract all this overhead setup to provide what feels like a single machine running accepting multiple
commands in one execution (through multiple calls to DockerSandbox.run). These calls to run don't all need to be made to the
same docker image either, but the files can still be shared (within reason).

## Requirements
- Java JDK 11 (tested with OpenJDK 11.0.1)
- Docker Engine (tested with Docker engine version 20.10.6)
- Unix machine (will look into other platform compatibility soon)
- Maven 3.6.3 to build

## Installation
The simplest way to install is to add the dependency to your pom.xml. The coordinates are as follows:
```xml
<artifactId>io.github.edwardUL99</artifactId>
<groupId>docker-sandbox</groupId>
<version>0.3.0</version>
```

## Build instructions
To build the JAR file, run the command:
```bash
mvn clean install -DskipTests
```

To run with tests, you need to build the images in sample-images with gcc-docker as the tag name and java-docker

The resulting JAR file will be output to the target directory 

## Usage
The main class to interact with the library is DockerSandbox. The following is an example of how to run
and compile a C program:

```java
package io.github.edwardUL99.docker.sandbox;

import io.github.edwardUL99.docker.sandbox.api.Docker;
import io.github.edwardUL99.docker.sandbox.api.DockerSandbox;
import io.github.edwardUL99.docker.sandbox.api.components.Result;
import io.github.edwardUL99.docker.sandbox.api.components.WorkingDirectory;

public class Example {
    public static void main(String[] args) {
        DockerSandbox sandbox = DockerSandbox.builder()
                .withJson("profiles.json") // see profiles.json in the root of the project for the example file
                // Or you can do withShellProfiles(Docker.Shell.SH or Docker.Shell.BASH, profiles)
                .withBinding("/path/to/local:/path/to/remote")
                .withEnvironmentVariables("VAR1=VALUE1", "VAR2=VALUE2")
                .build();

        sandbox.start("/home/sandbox");

        try {
            Docker.Command command = new Docker.Command("gcc main.c -o main");
            Result result = sandbox.run("gcc_compile", command,
                    new WorkingDirectory.UploadedFile("main.c", "/path/to/main.c"));

            // do something with result

            command = new Docker.Command("./main");
            result = sandbox.run("gcc_run", command, "Stdin Input"); // notice how this run command uses the compiled file from the previous execution
            // but you don't have to re-upload it as generated files from the previous call
            // are shared

            // do something with result

            // the call to finish in the finally block will free any resources such as created files on the host machine in the working directory
        } catch (Exception ex) {
            ex.printStackTrace();
            sandbox.cleanup(); // clean up any created containers that didn't get removed
        } finally {
            sandbox.finish(); // ensure all resources are freed
        }
    }
}
```
It is very important in the case of the inner exception to call `DockerSandbox.onException()`. An exception may have been
thrown after a docker container got created but before it got removed. This will lead to name conflict errors the next time
you create a container with the same name.

Similarly, it is important to have the `DockerSandbox.finish()` call in the finally block since that will clean up any files produced
by the containers on the host machine.

## Configuration
The most configuration can be obtained by using a JSON file passed into `DockerSandbox.configure(String filename)`. The JSON
file contains the following properties:
```json
{
  "profiles": [
    {
      "name": "profile_name",
      "image": "image_name[:version]",
      "container-name": "name-of-created-container",
      "user": "sandbox",
      "limits": {
        "cpuCount": 4,
        "memory": 64,
        "timeout": 3
      },
      "networkDisabled": true
    }   
  ],
  "docker_host": "unix:///var/run/docker.sock",
  "shell": "bash",
  "docker_config": "/path/to/config/directory",
  "docker_tls_verify": "default-value-if-left-out"    
}
```

Profiles can have multiple profiles defined. If "limits" is left out, a default limits is used. The limits are defined as
follows:
- cpuCount: The number of CPUs the container should use
- memory: The max amount of memory in MB the container should use
- timeout: The max length of time in seconds container execution should be waited for

The only accepted "shell" values at the moment are: [bash, sh].

If you don't want to configure with JSON (at the moment, this way, you cannot set docker_host, docker_config 
or docker_tls_verify), you can use `DockerSandbox.configure(Shell, Profile...)`.

The profile in the above JSON file can be constructed as:
```
Profile profile = new Profile("profile_name", "image_name[:version]", "name-of-created-container", "sandbox", "/home/sandbox",
    new Profile.Limits(4L, 64L, 3L), true);
DockerSandbox.configure(Docker.Shell.BASH, profile);
```

## Release Information

**Note:** As of release 0.3.0, the working-directory parameter in Profile has been removed.
Instead, the working directory passed into `DockerSandbox.start(String workingDirectory)` is used.

**Note:** As of release 1.0.0, the library consists of a package `api` and a single interface `DockerSandbox` which
represents the API to create and run Docker sandboxes. Instances can be built using the builder returned by `DockerSandbox.builder()`.
The only interface intended to be interacted with directly with in using this library is the `DockerSandbox` interface and
any objects returned by the sandbox, such a result. `DockerSandbox` takes and returns objects of types defined in the `api` package,
but only these objects should be used in the context of `DockerSandbox`. The package has also changed from `com.eddy.docker` to
`io.github.edwardUL99.docker.sandbox`

This means, if you were using releases before 1.0.0, after upgrading, you will have to fix package imports to reflect the
new `api` package and new package names
