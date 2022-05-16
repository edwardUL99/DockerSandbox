package io.github.edwardUL99.docker.sandbox.api.impl;

import io.github.edwardUL99.docker.sandbox.api.Docker;
import io.github.edwardUL99.docker.sandbox.api.DockerBuilder;
import io.github.edwardUL99.docker.sandbox.api.DockerSandbox;
import io.github.edwardUL99.docker.sandbox.api.DockerSandboxBuilder;
import io.github.edwardUL99.docker.sandbox.api.components.Profile;

/**
 * Implementation of the sandbox builder
 */
public class DockerSandboxBuilderImpl implements DockerSandboxBuilder {
    /**
     * The sandbox instance being created
     */
    private final DockerSandbox dockerSandbox = new DockerSandboxImpl();
    /**
     * Bindings being initialised
     */
    private final Docker.Bindings bindings = new Docker.Bindings();

    /**
     * Configure the builder with the given shell and profiles
     *
     * @param shell    the shell to configure with
     * @param profiles the profiles to load
     * @return instance of this builder
     */
    @Override
    public DockerSandboxBuilder withShellProfiles(Docker.Shell shell, Profile... profiles) {
        dockerSandbox.configure(shell, profiles);

        return this;
    }

    /**
     * Configures the system (and the Docker client) using the JSON file.
     * See {@link DockerBuilder#withJSONPath(String)} for info on JSON configuration
     *
     * @param JSONFile the path to the JSON file to load profiles from
     * @return instance of this builder
     */
    @Override
    public DockerSandboxBuilder withJson(String JSONFile) {
        dockerSandbox.configure(JSONFile);

        return this;
    }

    /**
     * Add environment variables to be shared between each run call. They are specified in the form VARIABLE=VALUE.
     *
     * @param envs the list of environment variables to add
     * @return instance of this builder
     */
    @Override
    public DockerSandboxBuilder withEnvironmentVariables(String... envs) {
        dockerSandbox.addEnvironmentVariables(envs);

        return this;
    }

    /**
     * The bindings to use for each run call
     *
     * @param bindings set of bindings to use for run calls
     * @return instance of this builder
     */
    @Override
    public DockerSandboxBuilder withBindings(Docker.Bindings bindings) {
        this.bindings.addAll(bindings);

        return this;
    }

    /**
     * Add a binding of local to docker volumes in form "localPath:remotePath"
     *
     * @param binding the binding string
     * @return instance of this builder
     */
    @Override
    public DockerSandboxBuilder withBinding(String binding) {
        this.bindings.addBinding(binding);

        return this;
    }

    /**
     * Build the docker sandbox with the configured properties
     *
     * @return the built sandbox
     */
    @Override
    public DockerSandbox build() {
        dockerSandbox.setBindings(bindings);

        return dockerSandbox;
    }
}
