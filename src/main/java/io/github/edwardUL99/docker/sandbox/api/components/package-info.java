/**
 * This package represents components of the system. A "component" is an input or output of the {@link io.github.edwardUL99.docker.sandbox.api.DockerSandbox}
 * running. For example, for input components, it takes a {@link io.github.edwardUL99.docker.sandbox.api.components.Profile} and a {@link io.github.edwardUL99.docker.sandbox.api.components.WorkingDirectory}
 * and then outputs a Result component, which is the result of executing some command. The {@link io.github.edwardUL99.docker.sandbox.api.components.WorkingDirectory}
 * component can be used as an input component with many different Profiles.
 */
package io.github.edwardUL99.docker.sandbox.api.components;