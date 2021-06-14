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

package com.eddy.docker.components;

import com.eddy.docker.Docker;

import java.util.List;
import java.util.Objects;

/**
 * This class represents an execution profile for a docker container to be spun up from. One docker image can have multiple
 * profiles. Profiles are used to configure how to docker container is created based off the profile's image name.
 *
 * Profiles can be configured programmatically or parsed in from a JSON file. {@link Docker#Docker(String)}
 */
public class Profile {
    /**
     * The name of the profile. This is used as a key to lookup the profile in the {@link Docker} class
     */
    private final String profileName;
    /**
     * The name of the docker image this profile is for
     */
    private final String imageName;
    /**
     * The name of the container that will be produced from this profile
     */
    private final String containerName;
    /**
     * The name of the user to run inside the container
     */
    private final String user;
    /**
     * The working directory inside the container
     */
    private final String workingDirectory;
    /**
     * Specified limits for the docker container
     */
    private final Limits limits;
    /**
     * Flag to determine if the container's network should be disabled/enabled
     */
    private final boolean networkDisabled;

    /**
     * Construct a profile with the provided parameters and with default limits
     * @param profileName name of the profile
     * @param imageName name of the docker image
     * @param containerName name of the container that will be produced from this profile
     * @param user the user to run the docker container under
     * @param workingDirectory the working directory to run the container under
     */
    public Profile(String profileName, String imageName, String containerName, String user, String workingDirectory) {
        this(profileName, imageName, containerName, user, workingDirectory, new Limits(), false);
    }

    /**
     * Construct a profile with the provided parameters and with default limits
     * @param profileName name of the profile
     * @param imageName name of the docker image
     * @param containerName name of the container that will be produced from this profile
     * @param user the user to run the docker container under
     * @param workingDirectory the working directory to run the container under
     * @param limits specified limits that the produced container should follow
     */
    public Profile(String profileName, String imageName, String containerName, String user, String workingDirectory, Limits limits,
                   boolean networkDisabled) {
        this.profileName = profileName;
        this.imageName = imageName;
        this.containerName = containerName;
        this.user = user;
        this.workingDirectory = workingDirectory;
        this.limits = limits;
        this.networkDisabled = networkDisabled;
    }

    /**
     * Retrieve this profile's name. The name is used as a lookup by
     * {@link Docker#createContainer(String, Docker.Command, Docker.Bindings, WorkingDirectory, String, List)}
     * to find the profile associated with the provided profile name
     * @return name of this profile
     */
    public String getProfileName() {
        return profileName;
    }

    /**
     * Retrieve the name of the image to create the docker container from
     * @return image name for the container
     */
    public String getImageName() {
        return imageName;
    }

    /**
     * Retrieve the name that the docker container will be running under
     * @return container name
     */
    public String getContainerName() {
        return containerName;
    }

    /**
     * Retrieve the name of the user that will be running inside the docker container
     * @return name of docker user
     */
    public String getUser() {
        return user;
    }

    /**
     * Retrieve the working directory for the docker container
     * @return container working directory
     */
    public String getWorkingDirectory() {
        return workingDirectory;
    }

    /**
     * Retrieve the limits this profile specifies for the container to adhere to
     * @return limits for the container
     */
    public Limits getLimits() {
        return limits;
    }

    /**
     * Returns whether the container should have network disabled or not
     * @return true if disabled, false if enabled
     */
    public boolean isNetworkDisabled() {
        return networkDisabled;
    }

    /**
     * Checks if this limits object is equals to the provided one
     * @param o the object to check
     * @return the equality of the objects
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Profile profile = (Profile) o;
        return networkDisabled == profile.networkDisabled &&
                profileName.equals(profile.profileName) &&
                imageName.equals(profile.imageName) &&
                containerName.equals(profile.containerName) &&
                user.equals(profile.user) &&
                workingDirectory.equals(profile.workingDirectory) &&
                limits.equals(profile.limits);
    }

    /**
     * Retrieve the hashcode for this object
     * @return hashcode
     */
    @Override
    public int hashCode() {
        return Objects.hash(profileName, imageName, containerName, user, workingDirectory, limits, networkDisabled);
    }

    /**
     * This class represents limits that a created docker container must adhere to
     */
    public static class Limits {
        /**
         * The count of CPUs that the docker container can use
         */
        private final Long cpuCount;
        /**
         * The memory the docker container can use in MB
         */
        private final Long memory;
        /**
         * The timeout in seconds for the docker containers to run
         */
        private final Long timeout;
        /**
         * The default for cpuCount
         */
        public static final Long CPU_COUNT_DEFAULT = 4L;
        /**
         * The default for memory
         */
        public static final Long MEMORY_DEFAULT = 64L * 1000000L;
        /**
         * The default for the timeout
         */
        public static final Long TIMEOUT_DEFAULT = 3L;

        /**
         * Create a default Limits object with {@link #CPU_COUNT_DEFAULT}, {@link #MEMORY_DEFAULT} and {@link #TIMEOUT_DEFAULT}
         */
        public Limits() {
            this(CPU_COUNT_DEFAULT, MEMORY_DEFAULT, TIMEOUT_DEFAULT);
        }

        /**
         * Create a Limits object with a specified number of cpus and memory
         * @param cpuCount count of CPUs that the docker container can use
         * @param memory the memory the docker container can use in MB
         * @param timeout the timeout in seconds for the docker containers
         */
        public Limits(Long cpuCount, Long memory, Long timeout) {
            this.cpuCount = cpuCount;
            this.memory = memory * 1000000L;
            this.timeout = timeout;
        }

        /**
         * Retrieve the count of CPUs that the docker container can access
         * @return count of CPUs
         */
        public Long getCpuCount() {
            return cpuCount;
        }

        /**
         * Retrieves the amount of memory that the docker container can access in MB
         * @return docker memory limit in MB
         */
        public Long getMemory() {
            return memory;
        }

        /**
         * Retrieves the timeout in seconds
         * @return timeout in seconds
         */
        public Long getTimeout() {
            return timeout;
        }

        /**
         * Checks if this limits object is equals to the provided one
         * @param o the object to check
         * @return the equality of the objects
         */
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Limits limits = (Limits) o;
            return cpuCount.equals(limits.cpuCount) &&
                    memory.equals(limits.memory) &&
                    timeout.equals(limits.timeout);
        }

        /**
         * Retrieve the hashcode for this object
         * @return hashcode
         */
        @Override
        public int hashCode() {
            return Objects.hash(cpuCount, memory);
        }
    }
}
