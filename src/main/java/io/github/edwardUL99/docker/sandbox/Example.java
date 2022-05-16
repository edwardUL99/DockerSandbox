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