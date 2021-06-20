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

package com.eddy.docker.api;

import com.eddy.docker.DockerSandbox;
import com.eddy.docker.api.impl.DefaultDocker;
import com.github.dockerjava.api.DockerClient;
import com.eddy.docker.api.components.Profile;
import com.eddy.docker.api.components.Result;
import com.eddy.docker.api.components.WorkingDirectory;

import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * This interface defines the API that abstracts client access to the DefaultDocker API through {@link DockerClient}.
 *
 * It provides all the necessary commands required for running sandbox docker programs.
 * It can be used as-is, but it is recommended to use the API wrapper {@link DockerSandbox} which abstracts any setup and execution
 * steps that this class requires. The {@link DockerSandbox} is the most stable means of using this API since any changes
 * (particularly breaking changes) made here would be automatically made to the DockerSandbox class, meaning that any existing methods
 * (i.e. not new ones) your code is using will compile and still run as documented even if the Docker API has changed in the background
 *
 * @since 0.5.0
 */
public interface Docker {
    /**
     * Add the list of profiles to this class. It uses {@link Profile#getProfileName()} as the lookup name
     * @param profiles the list of profiles to add
     */
    void addProfiles(Profile...profiles);

    /**
     * Create a volume with the specified name
     * @param volumeName the name of the volume to create
     * @since 0.5.0, the return type is void to be consistent with {@link #removeVolume(String)}
     */
    void createVolume(String volumeName);

    /**
     * Remove the docker volume with the provided name
     * @param volumeName the name of the docker volume to remove
     */
    void removeVolume(String volumeName);

    /**
     * Copies the provided tar to the remote path on the specified container
     * @param containerId the id of the container
     * @param remotePath the path to the directory the tar should be extracted on
     * @param tarStream the input stream to the tar. This is not a TarArchiveInputStream since tar decompression is done by DockerClient
     */
    void copyTarToContainer(String containerId, String remotePath, FileInputStream tarStream);

    /**
     * Create a container and retrieve the ID of the created container.
     *
     * <b>Warning: </b> if using stdin, you have to ensure that the command will in fact use stdin or else the call
     * to getResult will return a Result with {@link Result#isTimedOut()} returning true
     *
     * @param profileName the name of the profile to look up. If a profile cannot be found, IllegalArgumentException will be thrown
     * @param command the command to run on the container
     * @param bindings any bindings for the container
     * @param workingDirectory the working directory for the container to work with
     * @param stdin the stdin input as a String. If Stdin is not required, pass in null or ""
     * @param envs environment variables to be set inside the docker container
     * @return the ID of the created container
     */
    String createContainer(String profileName, Command command, Bindings bindings,
                                                   WorkingDirectory workingDirectory, String stdin, List<String> envs);

    /**
     * Start the container with the provided ID
     * @param containerId the ID of the container to start
     */
    void startContainer(String containerId);

    /**
     * Remove the container with the provided container ID. This forces removal
     * @param containerId the ID of the container to remove
     */
    void removeContainer(String containerId);

    /**
     * Retrieve the result of a started docker container. I.e., {@link #startContainer(String)} needs to be called first.
     * {@link #removeContainer(String)} should be called afterwards
     * @param containerId the ID of the container to start
     * @return the result of execution
     */
    Result getResult(String containerId);

    /**
     * This class represents a command that can be executed on the container
     */
    class Command extends ArrayList<String> {
        /**
         * Construct a command object with the provided command
         * @param command the command to execute
         */
        public Command(String command) {
            this.add(command);
        }
    }

    /**
     * This class represents a list of bindings for volumes on the docker container and host machine.
     * A binding is defined as a String in the same way you would define one on the command line:
     * "/path/on/local":"/path/on/docker"
     */
    class Bindings extends ArrayList<String> {
        /**
         * Add a binding to the current bindings
         * @param binding the binding to add
         * @return this so you can chain the creation
         */
        public Bindings addBinding(String binding) {
            add(binding);
            return this;
        }
    }

    /**
     * This method instantiates a WorkingDirectory object, opens it and then returns it.
     * @param workingDirectory the path to mount the WorkingDirectory onto
     * @return the WorkingDirectory object to use for containers
     */
    WorkingDirectory open(String workingDirectory);

    /**
     * Retrieve the shell that the client has been configured with
     * @return the shell the client is configured to use
     */
    Shell getShell();

    /**
     * Get the profiles that this client is configured with
     * @return profiles contained by the client
     */
    List<Profile> getProfiles();

    /**
     * This method removes any containers that were running and were started by this class. This should be called if an exception
     * occurred that may result in docker containers not being removed correctly which may result in conflict name errors.
     */
    void cleanupContainers();

    /**
     * This enum represents shells that the DefaultDocker class supports
     */
    enum Shell {
        SH,
        BASH
    }
}
