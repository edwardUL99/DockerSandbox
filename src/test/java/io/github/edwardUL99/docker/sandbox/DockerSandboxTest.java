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

package io.github.edwardUL99.docker.sandbox;

import io.github.edwardUL99.docker.sandbox.api.Docker;
import io.github.edwardUL99.docker.sandbox.api.DockerSandbox;
import io.github.edwardUL99.docker.sandbox.api.components.Profile;
import io.github.edwardUL99.docker.sandbox.api.components.Result;
import io.github.edwardUL99.docker.sandbox.api.components.WorkingDirectory;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

public class DockerSandboxTest {
    private DockerSandbox dockerSandbox;

    private void init() {
        dockerSandbox = DockerSandbox.builder()
                .withShellProfiles(Docker.Shell.BASH, new Profile("java_run", "java-docker", "java-docker", "sandbox"),
                        new Profile("java_compile", "java-docker", "java-docker", "root"),
                        new Profile("gcc_run", "gcc-docker", "gcc-docker", "sandbox"),
                        new Profile("gcc_compile", "gcc-docker", "gcc-docker", "root"))
                .withEnvironmentVariables("VAR1=VALUE1", "VAR2=VALUE2", "VAR3=VALUE3")
                .build();

        assertNotNull(dockerSandbox.getDocker());
        assertNotNull(dockerSandbox.getBindings());
        assertEquals(List.of("VAR1=VALUE1", "VAR2=VALUE2", "VAR3=VALUE3"), dockerSandbox.getEnvs());
    }

    private void testCLangCompile() {
        Result result = dockerSandbox.run("gcc_compile", new Docker.Command("gcc main.c -o main"),
                new WorkingDirectory.UploadedFile("test-files/main.c"));

        assertNotNull(result);
        assertEquals(0, result.getExitCode());
        assertEquals("", result.getStdout().trim());
        assertEquals("", result.getStderr().trim());
        assertFalse(result.isOutOfMemory());
    }

    private void testCRun() {
        Result result = dockerSandbox.run("gcc_run", new Docker.Command("./main"));
        assertNotNull(result);
        assertEquals(0, result.getExitCode());
        assertEquals("Hello", result.getStdout().trim());
        assertEquals("", result.getStderr().trim());
        assertFalse(result.isOutOfMemory());
    }

    private void testCplusCompile() {
        Result result = dockerSandbox.run("gcc_compile", new Docker.Command("g++ main-stdin.cpp -o main"),
                new WorkingDirectory.UploadedFile("test-files/main-stdin.cpp"));

        assertNotNull(result);
        assertEquals(0, result.getExitCode());
        assertEquals("", result.getStdout().trim());
        assertEquals("", result.getStderr().trim());
        assertFalse(result.isOutOfMemory());
    }

    private void testCplusRun() {
        Result result = dockerSandbox.run("gcc_run", new Docker.Command("./main"), "5");

        assertNotNull(result);
        assertEquals(0, result.getExitCode());
        assertEquals("Enter a number: Your number is : 5", result.getStdout().trim().replaceAll("\\n", ""));
        assertEquals("", result.getStderr().trim());
        assertFalse(result.isOutOfMemory());
    }

    private void testCatRun() {
        Result result = dockerSandbox.run("gcc_run", new Docker.Command("cat"), "Hello, this is a test");

        assertNotNull(result);
        assertEquals(0, result.getExitCode());
        assertEquals("Hello, this is a test", result.getStdout().trim());
        assertEquals("", result.getStderr().trim());
        assertFalse(result.isOutOfMemory());
    }

    private void testJavaCompile() {
        Result result = dockerSandbox.run("java_compile", new Docker.Command("javac Test.java"),
                new WorkingDirectory.UploadedFile("test-files/Test.java"));

        assertNotNull(result);
        assertEquals(0, result.getExitCode());
        assertEquals("", result.getStdout().trim());
        assertEquals("", result.getStderr().trim());
        assertFalse(result.isOutOfMemory());
    }

    private void testJavaRun() {
        Result result = dockerSandbox.run("java_run", new Docker.Command("java Test"), "This is line1\nThis is line2");

        assertNotNull(result);
        assertEquals(0, result.getExitCode());
        assertEquals("Enter a value: The first value is: This is line1Enter another value: The second value is: This is line2", result.getStdout().trim().replaceAll("\\n", ""));
        assertEquals("", result.getStderr().trim());
        assertFalse(result.isOutOfMemory());
    }

    @Test
    public void shouldRunEntireFlow() {
        try {
            init();

            try {
                assertNull(dockerSandbox.getWorkingDirectory());
                dockerSandbox.start("/home/sandbox");
                assertNotNull(dockerSandbox.getWorkingDirectory());

                testCLangCompile();
                testCRun();
                testCplusCompile();
                testCplusRun();
                testCatRun();
                testJavaCompile();
                testJavaRun();

                dockerSandbox.finish();
                assertNull(dockerSandbox.getWorkingDirectory());
                assertEquals(0, dockerSandbox.getEnvs().size());
                assertNull(dockerSandbox.getBindings());
            } catch (Exception ex) {
                dockerSandbox.cleanup();
            }
        } catch (Exception ex) {
            fail("An exception occurred doing the workflow: " + ex.getMessage());
        }
    }
}
