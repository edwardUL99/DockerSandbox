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

## Build instructions
To build the JAR file, run the command:
```bash
mvn clean install -DskipTests
```

To run with tests, you need to build the images in sample-images with gcc-docker as the tag name and java-docker

The resulting JAR file will be output to the target directory 

## Usage
The main class to interact with the library is com.eddy.docker.DockerSandbox. The following is an example of how to run
and compile a C program:
```java
import com.eddy.docker.Docker;
import com.eddy.docker.DockerSandbox;
import com.eddy.docker.components.Result;
import com.eddy.docker.components.WorkingDirectory;

public class Example {
    public static void main(String[] args) {
        DockerSandbox.configure("profiles.json"); // see profiles.json in the root of the project for the example file
        // Or you can do DockerSandbox.configure(Docker.Shell.SH or Docker.Shell.BASH, profiles)
        DockerSandbox.start("/home/sandbox");
        try {
            Docker.Bindings bindings = new Docker.Bindings();
            bindings.addBinding("/path/to/local:/path/to/remote");
            DockerSandbox.setBindings(bindings);

            DockerSandbox.addEnvironmentVariables("VAR1=VALUE1", "VAR2=VALUE2");

            Docker.Command command = new Docker.Command("gcc main.c -o main");
            Result result = DockerSandbox.run("gcc-docker", command,
                    new WorkingDirectory.UploadedFile("main.c", "/path/to/main.c"));

            // do something with result

            command = new Docker.Command("./main");
            result = DockerSandbox.run("gcc-docker", command, "Stdin Input"); // notice how this run command uses the compiled file from the previous execution
            // but you don't have to re-upload it as generated files from the previous call
            // are shared

            // do something with result

            // the call to finish in the finally block will free any resources such as created files on the host machine in the working directory
        } catch (Exception ex) {
            ex.printStackTrace();
            DockerSandbox.onException(); // clean up any created containers that didn't get removed
        } finally {
            DockerSandbox.finish(); // ensure all resources are freed
        }
    }
}
```
It is very important in the case of the inner exception to call `DockerSandbox.onException()`. An exception may have been
thrown after a docker container got created but before it got removed. This will lead to name conflict errors the next time
you create a container with the same name.

Similarly, it is important to have the `DockerSandbox.finish()` call in the finally block since that will clean up any files produced
by the containers on the host machine.

## Installation
The simplest way to install this JAR is to build the JAR and add it to your CLASSPATH
