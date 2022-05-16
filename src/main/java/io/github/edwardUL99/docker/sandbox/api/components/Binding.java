package io.github.edwardUL99.docker.sandbox.api.components;

/**
 * This class represents a Binding of local to remote directories
 */
public class Binding {
    /**
     * The local path
     */
    private final String local;
    /**
     * The remote path
     */
    private final String remote;

    /**
     * Construct a binding
     * @param local the local binding
     * @param remote the remote binding
     */
    public Binding(String local, String remote) {
        this.local = local;
        this.remote = remote;
    }

    /**
     * Get the local path
     * @return local path
     */
    public String getLocal() {
        return local;
    }

    /**
     * Get the remote path
     * @return remote path
     */
    public String getRemote() {
        return remote;
    }

    /**
     * Return the binding as a local-path:remote-path string
     * @return string representation
     */
    public String toString() {
        return local + ":" + remote;
    }

    /**
     * Create a binding from "local-path:remote-path" string
     * @param binding the binding string
     * @return the binding object
     */
    public static Binding fromString(String binding) {
        String[] paths = binding.split(":");

        if (paths.length != 2)
            throw new IllegalArgumentException(binding + " not formatted correctly as local-path:remote-path");

        return new Binding(paths[0], paths[1]);
    }
}
