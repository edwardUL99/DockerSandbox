package io.github.edwardUL99.docker.sandbox.api;

import io.github.edwardUL99.docker.sandbox.api.components.Profile;

/**
 * This interface represents a builder for building a docker sandbox instance
 */
public interface DockerSandboxBuilder {
    /**
     * Configure the builder with the given shell and profiles
     * @param shell the shell to configure with
     * @param profiles the profiles to load
     * @return instance of this builder
     */
    DockerSandboxBuilder withShellProfiles(Docker.Shell shell, Profile...profiles);

    /**
     * Configures the system (and the Docker client) using the JSON file.
     * See {@link DockerBuilder#withJSONPath(String)} for info on JSON configuration
     * @param JSONFile the path to the JSON file to load profiles from
     * @return instance of this builder
     */
    DockerSandboxBuilder withJson(String JSONFile);

    /**
     * Add environment variables to be shared between each run call. They are specified in the form VARIABLE=VALUE.
     * @param envs the list of environment variables to add
     * @return instance of this builder
     */
    DockerSandboxBuilder withEnvironmentVariables(String...envs);

    /**
     * The bindings to use for each run call
     * @param bindings set of bindings to use for run calls
     * @return instance of this builder
     */
    DockerSandboxBuilder withBindings(Docker.Bindings bindings);

    /**
     * Add a binding of local to docker volumes in form "localPath:remotePath"
     * @param binding the binding string
     * @return instance of this builder
     */
    DockerSandboxBuilder withBinding(String binding);

    /**
     * Build the docker sandbox with the configured properties
     * @return the built sandbox
     */
    DockerSandbox build();
}
