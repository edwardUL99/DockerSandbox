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

import static org.junit.Assert.*;

import com.eddy.docker.api.Docker;
import com.eddy.docker.api.DockerBuilder;
import com.eddy.docker.api.Utils;
import com.eddy.docker.api.components.Profile;
import com.eddy.docker.api.components.Result;
import com.eddy.docker.api.components.WorkingDirectory;
import com.eddy.docker.api.impl.DefaultDocker;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.model.Container;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.junit.Test;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class DockerTest {
    private DefaultDocker docker;

    @Test
    public void shouldConfigureWithShell() {
        Docker.Shell shell = Docker.Shell.SH;
        docker = (DefaultDocker)new DockerBuilder().withShell(shell).build();//new DefaultDocker(shell);

        assertNotNull(docker.getDockerClient());
        assertEquals(shell, docker.getShell());
        assertEquals(0, docker.getProfiles().size());

        docker.addProfiles(new Profile("gcc_run", "gcc-docker", "gcc-docker", "sandbox"));
        assertEquals(1, docker.getProfiles().size());
    }

    private JSONObject loadJSON() throws IOException, ParseException {
        JSONParser parser = new JSONParser();

        return (JSONObject)parser.parse(new FileReader(new File("test-files/profiles.json")));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldConfigureWithJSON() throws IOException, ParseException {
        JSONObject json = loadJSON();
        List<Profile> profiles = Utils.profilesFromJSON((JSONArray)json.getOrDefault("profiles", new JSONArray()));

        docker = (DefaultDocker)new DockerBuilder().withJSONPath("test-files/profiles.json").build();//new DefaultDocker("test-files/profiles.json");

        List<Profile> dockerProfiles = docker.getProfiles();

        assertNotNull(docker.getDockerClient());
        assertEquals(profiles.size(), dockerProfiles.size());
        for (Profile profile : profiles) {
            if (!dockerProfiles.contains(profile)) {
                fail("A profile in the parsed profiles is not found in DefaultDocker's profiles");
            }
        }
    }

    @Test
    public void shouldDoContainerRunNoStdin() throws IOException {
        docker = (DefaultDocker) new DockerBuilder().withShell(Docker.Shell.BASH).build();
        docker.addProfiles(new Profile("gcc_run", "gcc-docker", "gcc-docker", "sandbox"),
                new Profile("gcc_compile", "gcc-docker", "gcc-docker", "root"));

        WorkingDirectory workingDirectory = docker.open("/home/sandbox");

        String id = docker.createContainer("gcc_compile", new Docker.Command("gcc main.c -o main"),
                new Docker.Bindings(), workingDirectory, null, new ArrayList<>());

        workingDirectory.addFiles(id, new WorkingDirectory.UploadedFile("test-files/main.c"));

        assertNotNull(id);

        docker.startContainer(id);
        Result result = docker.getResult(id);

        docker.removeContainer(id);

        assertNotNull(result);
        assertEquals(0, result.getExitCode());
        assertEquals("", result.getStdout().trim());
        assertEquals("", result.getStderr().trim());
        assertFalse(result.isOutOfMemory());
        assertFalse(result.isTimedOut());

        String id1 = docker.createContainer("gcc_run", new Docker.Command("./main"), new Docker.Bindings(),
                workingDirectory, null, new ArrayList<>());

        assertNotNull(id1);
        assertNotSame(id, id1);

        docker.startContainer(id1);
        result = docker.getResult(id1);

        docker.removeContainer(id1);

        workingDirectory.close();

        assertNotNull(result);
        assertEquals(0, result.getExitCode());
        assertEquals("Hello", result.getStdout().trim());
        assertEquals("", result.getStderr().trim());
        assertFalse(result.isOutOfMemory());
        assertFalse(result.isTimedOut());
    }

    @Test
    public void shouldDoContainerRunStdin() throws IOException {
        docker = (DefaultDocker) new DockerBuilder().withShell(Docker.Shell.BASH).build();
        docker.addProfiles(new Profile("gcc_run", "gcc-docker", "gcc-docker", "sandbox"),
                new Profile("gcc_compile", "gcc-docker", "gcc-docker", "root"));

        WorkingDirectory workingDirectory = docker.open("/home/sandbox");

        String id = docker.createContainer("gcc_compile", new Docker.Command("g++ main-stdin.cpp -o main"),
                new Docker.Bindings(), workingDirectory, null, new ArrayList<>());

        workingDirectory.addFiles(id, new WorkingDirectory.UploadedFile("test-files/main-stdin.cpp"));

        assertNotNull(id);

        docker.startContainer(id);
        Result result = docker.getResult(id);

        docker.removeContainer(id);

        assertNotNull(result);
        assertEquals(0, result.getExitCode());
        assertEquals("", result.getStdout().trim());
        assertEquals("", result.getStderr().trim());
        assertFalse(result.isOutOfMemory());
        assertFalse(result.isTimedOut());

        String id1 = docker.createContainer("gcc_run", new Docker.Command("./main"), new Docker.Bindings(),
                workingDirectory, "5", new ArrayList<>());

        assertNotNull(id1);
        assertNotSame(id, id1);

        docker.startContainer(id1);
        result = docker.getResult(id1);

        docker.removeContainer(id1);

        workingDirectory.close();

        assertNotNull(result);
        assertEquals(0, result.getExitCode());
        assertEquals("Enter a number: Your number is : 5", result.getStdout().trim().replaceAll("\\n", ""));
        assertEquals("", result.getStderr().trim());
        assertFalse(result.isOutOfMemory());
        assertFalse(result.isTimedOut());
    }

    @Test
    public void shouldDoContainerStdinCatCommand() throws IOException {
        // cat is a special case as it doesn't terminate until it receives Ctrl-D (character 4)
        docker = (DefaultDocker) new DockerBuilder().withShell(Docker.Shell.BASH).build();
        docker.addProfiles(new Profile("gcc_run", "gcc-docker", "gcc-docker", "sandbox"),
                new Profile("gcc_compile", "gcc-docker", "gcc-docker", "root"));

        WorkingDirectory workingDirectory = docker.open("/home/sandbox");

        String id = docker.createContainer("gcc_run", new Docker.Command("cat"),
                new Docker.Bindings(), workingDirectory, "Hello. This is a test", new ArrayList<>());

        assertNotNull(id);

        docker.startContainer(id);
        Result result = docker.getResult(id);

        docker.removeContainer(id);

        workingDirectory.close();

        assertNotNull(result);
        assertEquals(0, result.getExitCode());
        assertEquals("Hello. This is a test", result.getStdout().trim());
        assertEquals("", result.getStderr().trim());
        assertFalse(result.isOutOfMemory());
        assertFalse(result.isTimedOut());
    }

    @Test
    public void shouldDoStdinJava() throws IOException {
        docker = (DefaultDocker) new DockerBuilder().withShell(Docker.Shell.BASH).build();
        docker.addProfiles(new Profile("java_run", "java-docker", "java-docker", "sandbox"),
                new Profile("java_compile", "java-docker", "java-docker", "root"));

        WorkingDirectory workingDirectory = docker.open("/home/sandbox");

        String id = docker.createContainer("java_compile", new Docker.Command("javac Test.java"),
                new Docker.Bindings(), workingDirectory, null, new ArrayList<>());

        workingDirectory.addFiles(id, new WorkingDirectory.UploadedFile("test-files/Test.java"));

        assertNotNull(id);

        docker.startContainer(id);
        Result result = docker.getResult(id);

        docker.removeContainer(id);

        assertNotNull(result);
        assertEquals(0, result.getExitCode());
        assertEquals("", result.getStdout().trim());
        assertEquals("", result.getStderr().trim());
        assertFalse(result.isOutOfMemory());
        assertFalse(result.isTimedOut());

        String id1 = docker.createContainer("java_run", new Docker.Command("java Test"), new Docker.Bindings(),
                workingDirectory, "This is line1\nThis is line2", new ArrayList<>());

        assertNotNull(id1);
        assertNotSame(id, id1);

        docker.startContainer(id1);
        result = docker.getResult(id1);

        docker.removeContainer(id1);

        workingDirectory.close();

        assertNotNull(result);
        assertEquals(0, result.getExitCode());
        assertEquals("Enter a value: The first value is: This is line1Enter another value: The second value is: This is line2", result.getStdout().trim().replaceAll("\\n", ""));
        assertEquals("", result.getStderr().trim());
        assertFalse(result.isOutOfMemory());
        assertFalse(result.isTimedOut());
    }

    @Test
    public void shouldCleanUpUnRemovedContainers() throws IOException {
        docker = (DefaultDocker) new DockerBuilder().withShell(Docker.Shell.BASH).build();
        docker.addProfiles(new Profile("gcc_run", "gcc-docker", "gcc-docker", "sandbox"),
                new Profile("gcc_compile", "gcc-docker", "gcc-docker1", "root"));

        WorkingDirectory workingDirectory = docker.open("/home/sandbox");

        String id = docker.createContainer("gcc_run", new Docker.Command("cat"),
                new Docker.Bindings(), workingDirectory, "Hello. This is a test", new ArrayList<>());

        String id1 = docker.createContainer("gcc_compile", new Docker.Command("java Test"),
                new Docker.Bindings(), workingDirectory, "Hello. This is a test", new ArrayList<>());

        DockerClient client = docker.getDockerClient();

        List<Container> containers = client.listContainersCmd().withShowAll(true).withIdFilter(new ArrayList<>(Arrays.asList(id, id1))).exec();
        List<String> ids = containers.stream().map(Container::getId).collect(Collectors.toList());

        assertTrue(ids.contains(id) && ids.contains(id1));

        docker.cleanupContainers();

        workingDirectory.close();

        containers = client.listContainersCmd().withIdFilter(new ArrayList<>(Arrays.asList(id, id1))).exec();
        ids = containers.stream().map(Container::getId).collect(Collectors.toList());

        assertEquals(0, ids.size());
    }

    @Test
    public void shouldTimeout() throws IOException {
        docker = (DefaultDocker) new DockerBuilder().withShell(Docker.Shell.BASH).build();
        docker.addProfiles(new Profile("gcc_run", "gcc-docker", "gcc-docker", "sandbox",
                new Profile.Limits(Profile.Limits.CPU_COUNT_DEFAULT, Profile.Limits.MEMORY_DEFAULT, 1L), false));

        WorkingDirectory workingDirectory = docker.open("/home/sandbox");

        String id = docker.createContainer("gcc_run", new Docker.Command("sleep 2"),
                new Docker.Bindings(), workingDirectory, null, new ArrayList<>());

        assertNotNull(id);

        docker.startContainer(id);
        Result result = docker.getResult(id);

        docker.removeContainer(id);

        workingDirectory.close();

        assertNotNull(result);
        assertEquals(0, result.getExitCode());
        assertEquals("", result.getStdout().trim());
        assertEquals("", result.getStderr().trim());
        assertFalse(result.isOutOfMemory());
        assertTrue(result.isTimedOut());
    }

    @Test
    public void shouldShowExecutionTime() throws IOException {
        docker = (DefaultDocker) new DockerBuilder().withShell(Docker.Shell.BASH).build();
        docker.addProfiles(new Profile("gcc_run", "gcc-docker", "gcc-docker", "sandbox",
                new Profile.Limits(Profile.Limits.CPU_COUNT_DEFAULT, Profile.Limits.MEMORY_DEFAULT, 3L), false));

        WorkingDirectory workingDirectory = docker.open("/home/sandbox");

        String id = docker.createContainer("gcc_run", new Docker.Command("sleep 2"),
                new Docker.Bindings(), workingDirectory, null, new ArrayList<>());

        assertNotNull(id);

        docker.startContainer(id);
        Result result = docker.getResult(id);
        Double duration = result.getDuration();

        docker.removeContainer(id);

        workingDirectory.close();

        assertNotNull(result);
        assertEquals(0, result.getExitCode());
        assertEquals("", result.getStdout().trim());
        assertEquals("", result.getStderr().trim());
        assertFalse(result.isOutOfMemory());
        assertFalse(result.isTimedOut());
        assertTrue(duration >= 2.0 && duration <= 3); // duration should be between 2 and 3 seconds if the process slept for 2 seconds
    }
}
