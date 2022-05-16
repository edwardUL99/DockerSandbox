package io.github.edwardUL99.docker.sandbox.api;

import io.github.edwardUL99.docker.sandbox.api.components.Profile;
import io.github.edwardUL99.docker.sandbox.api.impl.DefaultDocker;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * This class provides a Builder for constructing a Docker object to access the Docker API for running sandbox containers.
 * @since 0.5.0
 */
public class DockerBuilder {
    /**
     * The list of profiles being used by the builder
     */
    private final List<Profile> profiles = new ArrayList<>();
    /**
     * The shell being used (SH is default)
     */
    private Docker.Shell shell = Docker.Shell.SH;
    /**
     * The path to the JSON file being used by the builder
     */
    private String jsonPath = "";
    /**
     * The host of the docker socket
     */
    private String host;

    /**
     * Build the Docker client with the specified profiles.
     * If {@link #withJSONPath(String)} has been called, any previously added profiles will be cleared.
     * By default, if withProfiles isn't called and withJSONPath isn't either, a client is constructed with o profiles
     * and can be added to afterwards. However, if withJSONPath is called, it takes priority over profiles
     * @param profiles the profiles to add
     * @return this instance for chaining
     */
    public DockerBuilder withProfiles(Profile...profiles) {
        this.profiles.addAll(Arrays.asList(profiles));
        jsonPath = "";
        return this;
    }

    /**
     * Build the Docker client with the specified shell
     * @param shell the shell to set
     * @return this instance for chaining
     */
    public DockerBuilder withShell(Docker.Shell shell) {
        this.shell = shell;
        return this;
    }

    /**
     * Build the docker client with the specified docker host
     * @param host the docker host
     * @return this instance for chaining
     */
    public DockerBuilder withHost(String host) {
        this.host = host;
        return this;
    }

    /**
     * Build the Docker client with a path to a JSON configuration file.
     * See profiles.json for an example.
     * The shell variable should be retrieved from here. JSON is the most configurable option
     * If {@link #withProfiles(Profile...)} is called, any previously set filename is set to an empty string
     * @param jsonPath the path to the JSON file
     * @return this instance for chaining
     */
    public DockerBuilder withJSONPath(String jsonPath) {
        this.jsonPath = jsonPath;
        this.profiles.clear();
        return this;
    }

    /**
     * Builds the Docker instance based off the provided configuration
     * @return the built docker instance
     */
    public Docker build() {
        if (jsonPath != null && !jsonPath.isEmpty()) {
            return new DefaultDockerImpl(jsonPath);
        } else {
            Docker docker = (host == null) ? new DefaultDockerImpl(shell):new DefaultDockerImpl(shell, host);

            Profile[] profilesArr = new Profile[profiles.size()];
            docker.addProfiles(profiles.toArray(profilesArr));

            return docker;
        }
    }

    /**
     * Extended to provide access to the protected constructors in the DefaultDocker implementation class
     */
    private static class DefaultDockerImpl extends DefaultDocker {
        /**
         * Construct a docker client with default Host being "unix:///var/run/docker.sock" and no profiles loaded. Profiles
         * would have to be added using {@link #addProfiles(Profile...)}
         * @param shell the shell the docker containers should run under
         */
        private DefaultDockerImpl(Shell shell) {
            super(shell);
        }

        /**
         * Construct a docker client with the provided shell and host and no profiles loaded. Profiles
         * would have to be added using {@link #addProfiles(Profile...)}
         *
         * @param shell the shell the docker containers should run under
         * @param host  the host of the docker socket
         */
        private DefaultDockerImpl(Shell shell, String host) {
            super(shell, host);
        }

        /**
         * Create a docker object from a JSON file specified by filename. See profiles.json for an example.
         * The shell variable should be retrieved from here.
         * @param filename the name of the JSON file to load from
         */
        private DefaultDockerImpl(String filename) {
            super(filename);
        }
    }
}