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

package io.github.edwardUL99.docker.sandbox.api.impl;

import com.github.dockerjava.api.exception.NotModifiedException;
import io.github.edwardUL99.docker.sandbox.api.Docker;
import io.github.edwardUL99.docker.sandbox.api.Utils;
import io.github.edwardUL99.docker.sandbox.api.components.Binding;
import io.github.edwardUL99.docker.sandbox.api.components.Profile;
import io.github.edwardUL99.docker.sandbox.api.components.Result;
import io.github.edwardUL99.docker.sandbox.api.components.WorkingDirectory;
import io.github.edwardUL99.docker.sandbox.api.exceptions.DockerException;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.command.*;
import com.github.dockerjava.api.model.*;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.DockerClientConfig;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.*;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 *  This is the default implementation of the Docker client which should provide all the features required.
 */
public class DefaultDocker implements Docker {
    /**
     * The client this class will be using to access DefaultDocker
     */
    private DockerClient dockerClient;
    /**
     * The list of container IDs that have been created through this class
     */
    private final List<String> createdContainers = new ArrayList<>();
    /**
     * The profiles that have been loaded in from a JSON file or added programmatically by {@link #addProfiles(Profile...)}
     */
    private final Map<String, Profile> profiles = new HashMap<>();
    /**
     * This map is used to keep track of the profiles being used by a container. It is a mapping of container ID to profile
     */
    private final Map<String, Profile> usedProfiles = new HashMap<>();
    /**
     * The shell the docker containers should have commands run with
     */
    private Shell shell;
    /**
     * This variable stores a command to allow attaching to a container to feed stdin into the container if it is required.
     * If not required, this will be null
     */
    private AttachContainerCmd attachContainerCmd;
    /**
     * The date time returned by docker inspect if the container has not finished/unknown
     */
    private static final String UNKNOWN_DATE = "0001-01-01T00:00:00Z";
    /**
     * The socket for the unix docker container
     */
    private static final String UNIX_DOCKER_SOCK = "unix:///var/run/docker.sock";

    /**
     * Construct a docker client with default Host being "unix:///var/run/docker.sock" and no profiles loaded. Profiles
     * would have to be added using {@link #addProfiles(Profile...)}
     * @param shell the shell the docker containers should run under
     */
    protected DefaultDocker(Shell shell) {
        this(shell, UNIX_DOCKER_SOCK);
    }

    /**
     * Construct a docker client with the provided shell and host and no profiles loaded. Profiles
     * would have to be added using {@link #addProfiles(Profile...)}
     * @param shell the shell the docker containers should run under
     * @param host the host of the docker socket
     */
    protected DefaultDocker(Shell shell, String host) {
        configure(null, host);
        this.shell = shell;
    }

    /**
     * Create a docker object from a JSON file specified by filename. See profiles.json for an example.
     * The shell variable should be retrieved from here. JSON is the most configurable option over {@link #DefaultDocker(Shell)}
     * @param filename the name of the JSON file to load from
     */
    protected DefaultDocker(String filename) {
        if (filename != null) {
            try {
                configureFromJSON(filename);
            } catch (IOException ex) {
                throw new DockerException("Failed to read the JSON file: " + filename + " provided", ex);
            } catch (ParseException ex) {
                throw new DockerException("Failed to parse the JSON file: " + filename + " provided", ex);
            }
        } else {
            throw new IllegalStateException("A JSON file must be provided to this constructor");
        }
    }

    /**
     * Configures the profiles from the provided array of JSON profile objects
     * @param profiles array of JSON profile objects
     */
    private void configureProfiles(JSONArray profiles) {
        List<Profile> profilesList = Utils.profilesFromJSON(profiles);

        for (Profile profile : profilesList) {
            this.profiles.put(profile.getProfileName(), profile);
        }
    }

    /**
     * Configure DefaultDocker from the JSON file
     * @param filename the name of the JSON file
     * @throws IOException if an error occurs
     * @throws ParseException if the JSON cannot be parsed
     */
    @SuppressWarnings("unchecked")
    private void configureFromJSON(String filename) throws IOException, ParseException {
        JSONParser parser = new JSONParser();
        FileReader reader = new FileReader(filename);

        JSONObject json = (JSONObject)parser.parse(reader);
        JSONArray profiles = (JSONArray)json.get("profiles");

        if (profiles != null)
            configureProfiles(profiles);

        String shell = (String)json.getOrDefault("shell", "sh");
        shell = shell.toLowerCase();

        boolean shellFound = false;
        for (Shell sh : Shell.values()) {
            if (sh.toString().equalsIgnoreCase(shell)) {
                shellFound = true;
                this.shell = sh;
                break;
            }
        }

        if (!shellFound)
            throw new IllegalArgumentException("Shell: " + shell + " provided in " + filename + " not recognised");

        String dockerHost = (String)json.getOrDefault("docker_host", UNIX_DOCKER_SOCK);

        DefaultDockerClientConfig config = DefaultDockerClientConfig.createDefaultConfigBuilder()
                .withDockerHost(dockerHost)
                .withDockerConfig((String)json.getOrDefault("docker_config", DefaultDockerClientConfig.DOCKER_CONFIG))
                .withDockerTlsVerify((String)json.getOrDefault("docker_tls_verify", DefaultDockerClientConfig.DOCKER_TLS_VERIFY))
                .build();

        configure(config, dockerHost);
    }

    /**
     * Configures the DockerClient
     * @param config config to pass into the docker client, leave null if not needed
     * @param host the hostname of the Docker container
     * @since 0.4.1
     */
    private void configure(DockerClientConfig config, String host) {
        PrintStream err = System.err;
        PrintStream noOp = new PrintStream(OutputStream.nullOutputStream());
        System.setErr(new PrintStream(noOp)); // avoid SLF4J warnings
        DockerClientBuilder builder = config == null ? DockerClientBuilder.getInstance():DockerClientBuilder.getInstance(config);
        dockerClient = builder.withDockerHttpClient(new ApacheDockerHttpClient.Builder()
                .dockerHost(URI.create(host))
                .build()
        ).build();
        System.setErr(err);
        noOp.close();
    }

    /**
     * Add the list of profiles to this class. It uses {@link Profile#getProfileName()} as the lookup name
     * @param profiles the list of profiles to add
     */
    @Override
    public void addProfiles(Profile...profiles) {
        for (Profile profile : profiles) {
            this.profiles.put(profile.getProfileName(), profile);
        }
    }

    /**
     * Create a volume with the specified name
     * @param volumeName the name of the volume to create
     */
    @Override
    public void createVolume(String volumeName) {
        dockerClient.createVolumeCmd().withName(volumeName).exec();
    }

    /**
     * Remove the docker volume with the provided name
     * @param volumeName the name of the docker volume to remove
     */
    @Override
    public void removeVolume(String volumeName) {
        dockerClient.removeVolumeCmd(volumeName).exec();
    }

    /**
     * Copies the provided tar to the remote path on the specified container
     * @param containerId the id of the container
     * @param remotePath the path to the directory the tar should be extracted on
     * @param tarStream the input stream to the tar. This is not a TarArchiveInputStream since tar decompression is done by DockerClient
     */
    @Override
    public void copyTarToContainer(String containerId, String remotePath, FileInputStream tarStream) {
        dockerClient.copyArchiveToContainerCmd(containerId)
                .withTarInputStream(tarStream)
                .withRemotePath(remotePath)
                .exec();
    }

    /**
     * Create the container response with the provided parameters
     * @param workingDirectory the working directory being used for the containers
     * @param profile the profile to create the container under
     * @param command the command to run on the container
     * @param bindings any bindings that want to be added to the container
     * @param envs a list of environment variables to be set inside the docker container
     * @param requiresStdin true if stdin input is required
     * @return the response of the create container command
     */
    private CreateContainerResponse createContainerResponse(WorkingDirectory workingDirectory, Profile profile,
                                                            Command command, Bindings bindings, List<String> envs, boolean requiresStdin) {
        int bindingsSize = bindings.size();
        Bind[] bindingsArr = new Bind[bindingsSize];
        int i = 0;

        for (Binding binding : bindings)
            bindingsArr[i++] = Bind.parse(binding.toString());

        String workDir = workingDirectory.getVolume().getPath();

        Profile.Limits limits = profile.getLimits();

        return dockerClient.createContainerCmd(profile.getImageName())
                .withCmd(command)
                .withHostConfig(HostConfig.newHostConfig()
                        .withMounts(Collections.singletonList(new Mount().withSource(workingDirectory.getName())
                                .withTarget(workDir).withVolumeOptions(new VolumeOptions())))
                        .withBinds(bindingsArr)
                        .withCpuCount(limits.getCpuCount())
                        .withMemory(limits.getMemory()))
                .withStdinOpen(requiresStdin)
                .withEnv(envs)
                .withUser(profile.getUser())
                .withName(profile.getContainerName())
                .withWorkingDir(workDir)
                .withNetworkDisabled(profile.isNetworkDisabled())
                .withTty(false)
                .exec();
    }

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
    @Override
    public String createContainer(String profileName, Command command, Bindings bindings,
                                                   WorkingDirectory workingDirectory, String stdin, List<String> envs) {
        Profile profile = profiles.get(profileName);

        if (profile == null)
            throw new IllegalArgumentException("The provided profileName " + profileName + " has no associated profile");

        boolean requiresStdin = stdin != null && !stdin.isEmpty();

        command.add(0, "/bin/" + shell.toString().toLowerCase());
        command.add(1, "-c");

        CreateContainerResponse response = createContainerResponse(workingDirectory, profile, command, bindings, envs, requiresStdin);

        String id = response.getId();
        createdContainers.add(id);
        usedProfiles.put(id, profile);

        if (requiresStdin) {
            try {
                PipedOutputStream out = new PipedOutputStream();
                PipedInputStream in = new PipedInputStream(out);

                attachContainerCmd = dockerClient.attachContainerCmd(id)
                        .withFollowStream(true)
                        .withLogs(true)
                        .withStdOut(true)
                        .withStdErr(true)
                        .withStdIn(in);

                // if the command is cat, that will block until EOF (4), so send the EOF character at the end of the stdin
                String commandStr = command.get(2).trim();
                stdin = (commandStr.equals("cat") || commandStr.startsWith("cat")
                        && commandStr.contains("-"))  ? (stdin + "\n" + (char)4):(stdin + "\n");
                out.write(stdin.getBytes(StandardCharsets.UTF_8));
                out.flush();
            } catch (IOException ex) {
                throw new DockerException("Failed to write stdin to container: " + id, ex);
            }
        }

        return id;
    }

    /**
     * Start the container with the provided ID
     * @param containerId the ID of the container to start
     */
    @Override
    public void startContainer(String containerId) {
        try {
            dockerClient.startContainerCmd(containerId).exec();
        } catch (NotModifiedException ignored) {
            // already started
        }
    }

    /**
     * Stop the container with the given container ID
     *
     * @param containerId the ID of the container to stop
     */
    @Override
    public void stopContainer(String containerId) {
        try {
            dockerClient.stopContainerCmd(containerId).exec();
        } catch (NotModifiedException ignored) {
            // already stopped
        }
    }

    /**
     * Inspect the container with the provided container ID
     * @param containerId the ID of the container to inspect
     * @return the response of the inspection command
     */
    private InspectContainerResponse inspect(String containerId) {
        return dockerClient.inspectContainerCmd(containerId).exec();
    }

    /**
     * Remove the container with the provided container ID. This forces removal
     * @param containerId the ID of the container to remove
     */
    @Override
    public void removeContainer(String containerId) {
        dockerClient.removeContainerCmd(containerId)
                .withForce(true).exec();
        createdContainers.remove(containerId);
        usedProfiles.remove(containerId);
    }

    /**
     * Gets the command used to access a container's logs
     * @param containerId the ID of the container to access logs
     * @return command for accessing logs
     */
    private LogContainerCmd getLogContainerCommand(String containerId) {
        return dockerClient.logContainerCmd(containerId)
                .withStdOut(true)
                .withFollowStream(true)
                .withStdErr(true);
    }

    /**
     * Retrieve output from the specified container. If this was a container that required stdin, it would attach to the container,
     * and retrieve the output. Otherwise, it just starts the container with a log command and retrieves the logs
     * @param containerId the ID of the container to retrieve output from
     * @param timedOut a reference to determine if the process timedOut or not
     * @return stdout at index 0 and stderr at index 1
     * @throws InterruptedException if the thread gets interrupted waiting for it to be completed
     */
    private String[] getOutput(String containerId, AtomicBoolean timedOut) throws InterruptedException {
        Profile profile = usedProfiles.get(containerId);

        if (profile == null)
            throw new IllegalStateException("The container: " + containerId + " has not been created yet and has no profile assigned to it");

        Long timeOut = profile.getLimits().getTimeout();

        if (attachContainerCmd == null) {
            LogContainerCmd containerCmd = getLogContainerCommand(containerId);
            ContainerOutputHandler handler = new ContainerOutputHandler();
            timedOut.set(!containerCmd.exec(handler).awaitCompletion(timeOut, TimeUnit.SECONDS));

            return handler.getOutput();
        } else {
            ContainerInputOutputHandler handler = new ContainerInputOutputHandler(attachContainerCmd);
            timedOut.set(!attachContainerCmd.exec(handler).awaitCompletion(timeOut, TimeUnit.SECONDS));

            attachContainerCmd = null;

            return handler.getOutput();
        }
    }

    /**
     * Calculate the duration of execution from the state provided.
     * If it cannot be determined, Double.NaN is returned.
     * @param state the state to retrieve the times from
     * @return duration in seconds of execution
     * @since 0.4.1
     */
    private Double getDuration(InspectContainerResponse.ContainerState state) {
        String startStr = state.getStartedAt();
        String endStr = state.getFinishedAt();

        if ((startStr == null || startStr.equals(UNKNOWN_DATE)) || (endStr == null || endStr.equals(UNKNOWN_DATE)))
            return Double.NaN;

        DateTimeFormatter formatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME;
        LocalDateTime started = LocalDateTime.parse(startStr, formatter);
        LocalDateTime finished = LocalDateTime.parse(endStr, formatter);

        Duration duration = Duration.between(started, finished);

        return duration.toMillis() / 1000.00;
    }

    /**
     * Retrieve the result of a started docker container. I.e., {@link #startContainer(String)} needs to be called first.
     * {@link #removeContainer(String)} should be called afterwards
     * @param containerId the ID of the container to start
     * @return the result of execution
     */
    @Override
    public Result getResult(String containerId) {
        try {
            AtomicBoolean timedOutRef = new AtomicBoolean(false);
            String[] output = getOutput(containerId, timedOutRef);

            boolean timedOut = timedOutRef.get();

            if (timedOut)
                stopContainer(containerId);

            InspectContainerResponse inspected = inspect(containerId);
            InspectContainerResponse.ContainerState state = inspected.getState();

            Double duration = getDuration(state);

            Long exit_code_long = state.getExitCodeLong();
            int exit_code = Integer.MIN_VALUE;

            if (exit_code_long != null)
                exit_code = exit_code_long.intValue();

            Boolean oom = state.getOOMKilled();

            return new Result(exit_code, output[0], output[1], oom != null && oom, timedOut, duration);
        } catch (InterruptedException ex) {
            throw new DockerException("An exception occurred waiting for the container to complete", ex);
        }
    }

    /**
     * This method instantiates a WorkingDirectory object, opens it and then returns it.
     * @param workingDirectory the path to mount the WorkingDirectory onto
     * @return the WorkingDirectory object to use for containers
     */
    @Override
    public WorkingDirectory open(String workingDirectory) {
        WorkingDirectory workingDirectory1 = new WorkingDirectoryImpl(workingDirectory);
        workingDirectory1.open();
        return workingDirectory1;
    }

    /**
     * Retrieve the DockerClient this class is using
     * @return the docker client the class is using
     */
    public DockerClient getDockerClient() {
        return dockerClient;
    }

    /**
     * Retrieve the shell that the client has been configured with
     * @return the shell the client is configured to use
     */
    @Override
    public Shell getShell() {
        return shell;
    }

    /**
     * Get the profiles that this client is configured with
     * @return profiles contained by the client
     */
    @Override
    public List<Profile> getProfiles() {
        return new ArrayList<>(profiles.values());
    }

    /**
     * This method removes any containers that were running and were started by this class. This should be called if an exception
     * occurred that may result in docker containers not being removed correctly which may result in conflict name errors.
     */
    @Override
    public void cleanupContainers() {
        List<Container> ids = dockerClient.listContainersCmd()
                .withShowAll(true)
                .withIdFilter(createdContainers)
                .exec();

        for (Container id : ids) {
            removeContainer(id.getId());
        }
    }

    /**
     * This class is used for retrieving output from containers that did not require any stdin input
     */
    private static class ContainerOutputHandler extends ResultCallback.Adapter<Frame> {
        /**
         * The stdout stream
         */
        private final StringBuilder stdout = new StringBuilder();
        /**
         * The stderr stream
         */
        private final StringBuilder stderr = new StringBuilder();

        @Override
        public void onStart(Closeable stream) {
            super.onStart(stream);
        }

        @Override
        public void onNext(Frame object) {
            StreamType streamType = object.getStreamType();
            String payload = new String(object.getPayload());

            if (streamType == StreamType.STDOUT) {
                stdout.append(payload);
            } else if (streamType == StreamType.STDERR) {
                stderr.append(payload);
            }
        }

        @Override
        public void onComplete() {
            try {
                close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        /**
         * Retrieve the output in an indexed array with 0 = stdout and 1 = stderr
         * @return array of output
         */
        public String[] getOutput() {
            return new String[] {stdout.toString(), stderr.toString()};
        }
    }

    /**
     * This class is used to retrieve output from containers that had received stdin input. Instances of this class need
     * to be bound to an AttachContainerCmd object
     */
    private static class ContainerInputOutputHandler extends ResultCallback.Adapter<Frame> {
        /**
         * The stdout stream
         */
        private final StringBuilder stdout = new StringBuilder();
        /**
         * The stderr stream
         */
        private final StringBuilder stderr = new StringBuilder();
        /**
         * The attach container command that uses this handler
         */
        private final AttachContainerCmd attachContainerCmd;

        /**
         * Construct the handler with the provided attach container command
         * @param attachContainerCmd the command that is using this handler to retrieve output from
         */
        private ContainerInputOutputHandler(AttachContainerCmd attachContainerCmd) {
            this.attachContainerCmd = attachContainerCmd;
        }

        @Override
        public void onStart(Closeable stream) {
            super.onStart(stream);
        }

        @Override
        public void onNext(Frame object) {
            StreamType streamType = object.getStreamType();
            byte[] payloadBytes = object.getPayload();

            // Cat is a special case where it only terminates if Ctrl-D (EOF) is found. So if the last character is 4 (ASCII for EOF,
            // remove the Ctrl-D and terminate
            boolean end = payloadBytes[payloadBytes.length - 1] == 4;

            if (end)
                payloadBytes[payloadBytes.length - 1] = 0;

            String payload = new String(payloadBytes);

            if (streamType == StreamType.STDOUT) {
                stdout.append(payload);
            } else if (streamType == StreamType.STDERR) {
                stderr.append(payload);
            }

            if (end)
                onComplete();
        }

        @Override
        public void onComplete() {
            super.onComplete();

            try {
                InputStream inputStream = attachContainerCmd.getStdin();

                if (inputStream != null)
                    inputStream.close();

                close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }

        /**
         * Retrieve the output in an indexed array with 0 = stdout and 1 = stderr
         * @return array of output
         */
        public String[] getOutput() {
            return new String[] {stdout.toString(), stderr.toString()};
        }
    }

    /**
     * Used to access the WorkingDirectory constructor but hide it from other classes
     */
    private class WorkingDirectoryImpl extends WorkingDirectory {
        /**
         * Create an instance with the provided working directory path
         * @param workingDirectory the path to mount the working directory on in the container
         */
        private WorkingDirectoryImpl(String workingDirectory) {
            super(DefaultDocker.this, workingDirectory);
        }
    }
}
