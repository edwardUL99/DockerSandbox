/**
 * This package represents components of the system. A "component" is an input or output of the {@link com.eddy.docker.DockerSandbox}
 * running. For example, for input components, it takes a {@link com.eddy.docker.api.components.Profile} and a {@link com.eddy.docker.api.components.WorkingDirectory}
 * and then outputs a Result component, which is the result of executing some command. The {@link com.eddy.docker.api.components.WorkingDirectory}
 * component can be used as an input component with many different Profiles.
 */
package com.eddy.docker.api.components;