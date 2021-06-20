/*
 * Copyright 2021 Edward Lynch-Milner
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package com.eddy.docker;

import com.eddy.docker.api.Docker;
import com.eddy.docker.api.DockerBuilder;
import com.eddy.docker.api.components.Profile;
import com.eddy.docker.api.components.Result;
import com.eddy.docker.api.components.WorkingDirectory;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * This class provides access to the docker-sandbox API in the api package to abstract the classes of this module into an easy to use interface to allow commands to be
 * run in sandboxed docker containers. An example of compiling and running a c++ program is seen here: (A gcc_compile and gcc_run profile is set up
 * with a GCC image, with compile using user root and run using user sandbox. Our working directory is /home/sandbox)
 * <pre>{@code
 *     DockerSandbox.configure("profiles.json"); // or DockerSandbox.configure(DefaultDocker.Shell.BASH, gcc_compile, gcc_run)
 *     // add environment variables with DockerSandbox.addEnvironmentVariables("VAR1=VALUE", "VAR2=VALUE");
 *     // add bindings:
 *     //   DefaultDocker.Bindings bindings = new DefaultDocker.Bindings();
 *     //   bindings.add("/path/to/host:/path/to/remote");
 *     DockerSandbox.start()
 *
 *     DefaultDocker.Command compile_command = new DefaultDocker.Command("gcc main.c -o main");
 *     Result compile =
 *          DockerSandbox.run("gcc_compile", compile_command, new WorkingDirectory.UploadedFile("main.c", "/path/to/main.c"));
 *
 *     DefaultDocker.Command run_command = new DefaultDocker.Command("./main");
 *     Result run =
 *          DockerSandbox.run("gcc_run", run_command); // notice that this run command will be able to access the compiled main.c from the previous command
 *
 *     To pass stdin in, you can either pass the String or a File in to use a File for stdin.
 * }</pre>
 */
public final class DockerSandbox {
    /**
     * The DefaultDocker client this class is encapsulating
     */
    private static Docker docker;
    /**
     * The current working directory each run call will work with
     */
    private static WorkingDirectory workingDirectory;
    /**
     * The bindings that each run call will use
     */
    private static Docker.Bindings bindings;
    /**
     * A flag to indicate {@link #finish()} is being called by an improper shutdown
     */
    private static boolean improperShutdown = false;
    /**
     * The environment variables that can be shared between each run call
     */
    private static final List<String> envs = new ArrayList<>();
    /**
     * This hook is registered when {@link #start(String)} is called to ensure cleanup if the program is terminated before a call to
     * {@link #finish()} is made
     */
    private static final Thread shutdownHook = new Thread() {
        /**
         * If this thread was constructed using a separate
         * {@code Runnable} run object, then that
         * {@code Runnable} object's {@code run} method is called;
         * otherwise, this method does nothing and returns.
         * <p>
         * Subclasses of {@code Thread} should override this method.
         *
         */
        @Override
        public void run() {
            super.run();
            improperShutdown = true;
            finish();
        }
    };

    /**
     * Configure the system (and the DefaultDocker client) using the provided shell and profiles.
     * See {@link DockerBuilder#withShell(Docker.Shell)} and {@link DockerBuilder#withProfiles(Profile...)} for how it is configured using this method
     * @param shell the shell the docker containers should use
     * @param profiles the profiles to be loaded in
     */
    public static void configure(Docker.Shell shell, Profile...profiles) {
        docker = new DockerBuilder()
                .withShell(shell)
                .withProfiles(profiles)
                .build();

        docker.addProfiles(profiles);
    }

    /**
     * Configures the system (and the DefaultDocker client) using the JSON file.
     * See {@link DockerBuilder#withJSONPath(String)} for info on JSON configuration
     * @param JSONFile the path to the JSON file to load profiles from
     */
    public static void configure(String JSONFile) {
        docker = new DockerBuilder()
                .withJSONPath(JSONFile)
                .build();
    }

    /**
     * Add environment variables to be shared between each run call. They are specified in the form VARIABLE=VALUE.
     * @param envs the list of environment variables to add
     */
    public static void addEnvironmentVariables(String...envs) {
        DockerSandbox.envs.addAll(List.of(envs));
    }

    /**
     * The bindings to use for each run call
     * @param bindings set of bindings to use for run calls
     */
    public static void setBindings(Docker.Bindings bindings) {
        DockerSandbox.bindings = bindings;
    }

    /**
     * Checks if the system has been configured correctly and throws IllegalStateException otherwise
     */
    private static void checkConfiguration() {
        if (docker == null)
            throw new IllegalStateException("DockerSandbox needs to be configured first either by using configure with a list of Profiles" +
                    " or a JSON file containing profiles");
    }

    /**
     * Checks if {@link #start(String)} has been called yet and throws IllegalStateException if not
     */
    private static void checkStarted() {
        if (workingDirectory == null)
            throw new IllegalStateException("DockerSandbox needs to have start called first");
    }

    /**
     * Run the command with the specified profile, command and uploaded files with no stdin.
     * See {@link #run(String, Docker.Command, String, WorkingDirectory.UploadedFile...)}
     * @param profile the profile to use
     * @param command the command to execute
     * @param uploadedFiles the files to upload to the working directory if any
     * @return the result of the run call
     */
    public static Result run(String profile, Docker.Command command, WorkingDirectory.UploadedFile...uploadedFiles) {
        return run(profile, command, "", uploadedFiles);
    }

    /**
     * Read the specified file into a string
     * @param file the file to read
     * @return the file contents
     * @throws IOException if an error occurs
     */
    private static String readFile(File file) throws IOException {
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            StringBuilder contents = new StringBuilder();

            String line;
            while ((line = reader.readLine()) != null)
                contents.append(line).append("\n");

            return contents.toString();
        }
    }

    /**
     * Run the command with the specified profile, command and uploaded files with a file as stdin.
     * See {@link #run(String, Docker.Command, String, WorkingDirectory.UploadedFile...)}
     * @param profile the profile to use
     * @param command the command to execute
     * @param stdin the file to read the contents from and use as stdin
     * @param uploadedFiles the files to upload to the working directory if any
     * @return the result of the run call
     * @throws IOException if the file failes to be read
     */
    public static Result run(String profile, Docker.Command command, File stdin, WorkingDirectory.UploadedFile...uploadedFiles) throws IOException {
        return run(profile, command, readFile(stdin), uploadedFiles);
    }

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
    public static Result run(String profile, Docker.Command command, String stdin, WorkingDirectory.UploadedFile...uploadedFiles) {
        checkConfiguration();
        checkStarted();

        if (bindings == null)
            bindings = new Docker.Bindings();

        String response = docker.createContainer(profile, command, bindings, workingDirectory, stdin, envs);

        if (uploadedFiles.length > 0)
            workingDirectory.addFiles(response, uploadedFiles); // add the files to the container

        docker.startContainer(response);
        Result result = docker.getResult(response);
        docker.removeContainer(response);

        return result;
    }

    /**
     * Starts the system and readies it for calls to run. This call should have a subsequent call to {@link #finish()} after
     * you are finished with the run calls to ensure that resources are released.
     *
     * A shutdown hook is added to force a call to {@link #finish()} if the program is terminated before {@link #finish()}
     * is called (e.g. by Ctrl-C).
     *
     * @param workingDirectory the path of the working directory on the docker container
     */
    public static void start(String workingDirectory) {
        checkConfiguration();

        if (DockerSandbox.workingDirectory != null && !DockerSandbox.workingDirectory.isClosed())
            throw new IllegalStateException("You must close the previous working directory before calling this one");

        DockerSandbox.workingDirectory = docker.open(workingDirectory);

        Runtime.getRuntime().addShutdownHook(shutdownHook);
    }

    /**
     * Finishes the system by releasing any resources and resetting this class. The configure and start methods will need
     * to be called again after this call.
     *
     * This method removes the shutdown hook added by {@link #start(String)} if it is called normally and not by Ctrl-C
     */
    public static void finish() {
        try {
            if (workingDirectory != null)
                workingDirectory.close();
        } catch (IOException ex) {
            ex.printStackTrace();
        }

        if (!improperShutdown)
            Runtime.getRuntime().removeShutdownHook(shutdownHook);

        envs.clear();
        bindings = null;
        workingDirectory = null;
        docker = null;
    }

    /**
     * Retrieve the working directory object being used
     * @return working directory
     */
    protected static WorkingDirectory getWorkingDirectory() {
        return workingDirectory;
    }

    /**
     * Retrieve the bindings being used
     * @return bindings
     */
    protected static Docker.Bindings getBindings() {
        return bindings;
    }

    /**
     * Get the docker client being used
     * @return docker client
     */
    protected static Docker getDocker() {
        return docker;
    }

    /**
     * Get the list of environment variables being used
     * @return list of environment variables
     */
    protected static List<String> getEnvs() {
        return envs;
    }

    /**
     * This method should be called in a catch block to ensure that any created containers left behind by a run call (which
     * may have thrown the exception) are removed to avoid name conflict exceptions
     */
    public static void onException() {
        if (docker != null)
            docker.cleanupContainers();
    }
}
