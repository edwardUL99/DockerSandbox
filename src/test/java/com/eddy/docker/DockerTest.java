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

import com.eddy.docker.components.Profile;
import com.eddy.docker.components.Result;
import com.eddy.docker.components.WorkingDirectory;
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
    private Docker docker;

    @Test
    public void shouldConfigureWithShell() {
        Docker.Shell shell = Docker.Shell.SH;
        docker = new Docker(shell);

        assertNotNull(docker.getDockerClient());
        assertEquals(shell, docker.getShell());
        assertEquals(0, docker.getProfiles().size());

        docker.addProfiles(new Profile("gcc_run", "gcc-docker", "gcc-docker", "sandbox",
                "/home/sandbox"));
        assertEquals(1, docker.getProfiles().size());
    }

    private JSONObject loadJSON(String JSONFile) throws IOException, ParseException {
        JSONParser parser = new JSONParser();

        return (JSONObject)parser.parse(new FileReader(new File(JSONFile)));
    }

    @Test
    public void shouldConfigureWithJSON() throws IOException, ParseException {
        JSONObject json = loadJSON("test-files/profiles.json");
        List<Profile> profiles = Utils.profilesFromJSON((JSONArray)json.getOrDefault("profiles", new JSONArray()));

        docker = new Docker("profiles.json");

        List<Profile> dockerProfiles = docker.getProfiles();

        assertNotNull(docker.getDockerClient());
        assertEquals(profiles.size(), dockerProfiles.size());
        for (Profile profile : profiles) {
            if (!dockerProfiles.contains(profile)) {
                fail("A profile in the parsed profiles is not found in Docker's profiles");
            }
        }
    }

    @Test
    public void shouldDoContainerRunNoStdin() {
        docker = new Docker(Docker.Shell.BASH);
        docker.addProfiles(new Profile("gcc_run", "gcc-docker", "gcc-docker", "sandbox",
                "/home/sandbox"),
                new Profile("gcc_compile", "gcc-docker", "gcc-docker", "root", "/home/sandbox"));

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

        String id1 = docker.createContainer("gcc_run", new Docker.Command("./main"), new Docker.Bindings(),
                workingDirectory, null, new ArrayList<>());

        assertNotNull(id1);
        assertNotSame(id, id1);

        docker.startContainer(id1);
        result = docker.getResult(id1);

        docker.removeContainer(id1);

        assertNotNull(result);
        assertEquals(0, result.getExitCode());
        assertEquals("Hello", result.getStdout().trim());
        assertEquals("", result.getStderr().trim());
        assertFalse(result.isOutOfMemory());
    }

    @Test
    public void shouldDoContainerRunStdin() {
        docker = new Docker(Docker.Shell.BASH);
        docker.addProfiles(new Profile("gcc_run", "gcc-docker", "gcc-docker", "sandbox",
                        "/home/sandbox"),
                new Profile("gcc_compile", "gcc-docker", "gcc-docker", "root", "/home/sandbox"));

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

        String id1 = docker.createContainer("gcc_run", new Docker.Command("./main"), new Docker.Bindings(),
                workingDirectory, "5", new ArrayList<>());

        assertNotNull(id1);
        assertNotSame(id, id1);

        docker.startContainer(id1);
        result = docker.getResult(id1);

        docker.removeContainer(id1);

        assertNotNull(result);
        assertEquals(0, result.getExitCode());
        assertEquals("Enter a number: Your number is : 5", result.getStdout().trim().replaceAll("\\n", ""));
        assertEquals("", result.getStderr().trim());
        assertFalse(result.isOutOfMemory());
    }

    @Test
    public void shouldDoContainerStdinCatCommand() {
        // cat is a special case as it doesn't terminate until it receives Ctrl-D (character 4)
        docker = new Docker(Docker.Shell.BASH);
        docker.addProfiles(new Profile("gcc_run", "gcc-docker", "gcc-docker", "sandbox",
                        "/home/sandbox"),
                new Profile("gcc_compile", "gcc-docker", "gcc-docker", "root", "/home/sandbox"));

        WorkingDirectory workingDirectory = docker.open("/home/sandbox");

        String id = docker.createContainer("gcc_run", new Docker.Command("cat"),
                new Docker.Bindings(), workingDirectory, "Hello. This is a test", new ArrayList<>());

        assertNotNull(id);

        docker.startContainer(id);
        Result result = docker.getResult(id);

        docker.removeContainer(id);

        assertNotNull(result);
        assertEquals(0, result.getExitCode());
        assertEquals("Hello. This is a test", result.getStdout().trim());
        assertEquals("", result.getStderr().trim());
        assertFalse(result.isOutOfMemory());
    }

    @Test
    public void shouldDoStdinJava() {
        docker = new Docker(Docker.Shell.BASH);
        docker.addProfiles(new Profile("java_run", "java-docker", "java-docker", "sandbox",
                        "/home/sandbox"),
                new Profile("java_compile", "java-docker", "java-docker", "root", "/home/sandbox"));

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

        String id1 = docker.createContainer("java_run", new Docker.Command("java Test"), new Docker.Bindings(),
                workingDirectory, "This is line1\nThis is line2", new ArrayList<>());

        assertNotNull(id1);
        assertNotSame(id, id1);

        docker.startContainer(id1);
        result = docker.getResult(id1);

        docker.removeContainer(id1);

        assertNotNull(result);
        assertEquals(0, result.getExitCode());
        assertEquals("Enter a value: The first value is: This is line1Enter another value: The second value is: This is line2", result.getStdout().trim().replaceAll("\\n", ""));
        assertEquals("", result.getStderr().trim());
        assertFalse(result.isOutOfMemory());
    }

    @Test
    public void shouldCleanUpUnRemovedContainers() {
        docker = new Docker(Docker.Shell.BASH);
        docker.addProfiles(new Profile("gcc_run", "gcc-docker", "gcc-docker", "sandbox",
                        "/home/sandbox"),
                new Profile("gcc_compile", "gcc-docker", "gcc-docker1", "root", "/home/sandbox"));

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

        containers = client.listContainersCmd().withIdFilter(new ArrayList<>(Arrays.asList(id, id1))).exec();
        ids = containers.stream().map(Container::getId).collect(Collectors.toList());

        assertEquals(0, ids.size());
    }
}