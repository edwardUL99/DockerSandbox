package io.github.edwardUL99.docker.sandbox.api;

import io.github.edwardUL99.docker.sandbox.api.components.Profile;
import io.github.edwardUL99.docker.sandbox.api.components.Result;
import io.github.edwardUL99.docker.sandbox.api.components.WorkingDirectory;
import io.github.edwardUL99.docker.sandbox.api.impl.DockerSandboxBuilderImpl;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * This interface provides the main docker-sandbox API in the api package to abstract the classes of this module into
 * an easy to use interface to allow commands to be run in sandboxed docker containers.
 */
public interface DockerSandbox {
    /**
     * Configure the system (and the Docker client) using the provided shell and profiles.
     * See {@link DockerBuilder#withShell(Docker.Shell)} and {@link DockerBuilder#withProfiles(Profile...)} for how it is configured using this method
     * @param shell the shell the docker containers should use
     * @param profiles the profiles to be loaded in
     */
    void configure(Docker.Shell shell, Profile...profiles);

    /**
     * Configures the system (and the Docker client) using the JSON file.
     * See {@link DockerBuilder#withJSONPath(String)} for info on JSON configuration
     * @param JSONFile the path to the JSON file to load profiles from
     */
    void configure(String JSONFile);

    /**
     * Add environment variables to be shared between each run call. They are specified in the form VARIABLE=VALUE.
     * @param envs the list of environment variables to add
     */
    void addEnvironmentVariables(String...envs);

    /**
     * The bindings to use for each run call
     * @param bindings set of bindings to use for run calls
     */
    void setBindings(Docker.Bindings bindings);

    /**
     * Run the command with the specified profile, command and uploaded files with no stdin.
     * See {@link #run(String, Docker.Command, String, WorkingDirectory.UploadedFile...)}
     * @param profile the profile to use
     * @param command the command to execute
     * @param uploadedFiles the files to upload to the working directory if any
     * @return the result of the run call
     */
    Result run(String profile, Docker.Command command, WorkingDirectory.UploadedFile...uploadedFiles);

    /**
     * Run the command with the specified profile, command and uploaded files with a file as stdin.
     * See {@link #run(String, Docker.Command, String, WorkingDirectory.UploadedFile...)}
     * @param profile the profile to use
     * @param command the command to execute
     * @param stdin the file to read the contents from and use as stdin
     * @param uploadedFiles the files to upload to the working directory if any
     * @return the result of the run call
     * @throws IOException if the file fails to be read
     */
    Result run(String profile, Docker.Command command, File stdin, WorkingDirectory.UploadedFile...uploadedFiles) throws IOException;

    /**
     * Run the command with the specified profile, command and uploaded files with String stdin.
     * {@link #configure(Docker.Shell, Profile...)} or {@link #configure(String)} and {@link #start(String)} needs to be called first or else
     * an IllegalStateException will be thrown.
     *
     * @param profile the profile to use
     * @param command the command to execute
     * @param stdin the standard input String to send to the docker container
     * @param uploadedFiles the files to upload to the working directory if any
     * @return the result of the run call
     */
    Result run(String profile, Docker.Command command, String stdin, WorkingDirectory.UploadedFile...uploadedFiles);

    /**
     * Starts the system and readies it for calls to run. This call should have a subsequent call to {@link #finish()} after
     * you are finished with the run calls to ensure that resources are released.
     *
     * A shutdown hook is added to force a call to {@link #finish()} if the program is terminated before {@link #finish()}
     * is called (e.g. by Ctrl-C).
     *
     * @param workingDirectory the path of the working directory on the docker container
     */
    void start(String workingDirectory);

    /**
     * Finishes the system by releasing any resources and resetting this class. The configure and start methods will need
     * to be called again after this call.
     *
     * This method removes the shutdown hook added by {@link #start(String)} if it is called normally and not by Ctrl-C
     */
    void finish();

    /**
     * This method should be called in a catch block to ensure that any created containers left behind by a run call (which
     * may have thrown the exception) are removed to avoid name conflict exceptions
     */
    void cleanup();

    /**
     * Retrieve the working directory object being used
     * @return working directory
     */
    WorkingDirectory getWorkingDirectory();

    /**
     * Retrieve the bindings being used
     * @return bindings
     */
    Docker.Bindings getBindings();

    /**
     * Get the docker client being used
     * @return docker client
     */
    Docker getDocker();

    /**
     * Get the list of environment variables being used
     * @return list of environment variables
     */
    List<String> getEnvs();

    /**
     * Retrieve a builder to build a DockerSandboxBuilder instance to build a sandbox instance
     * @return the builder instance
     */
    static DockerSandboxBuilder builder() {
        return new DockerSandboxBuilderImpl();
    }
}
