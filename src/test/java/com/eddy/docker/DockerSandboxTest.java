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

import com.eddy.docker.api.Docker;
import com.eddy.docker.api.components.Profile;
import com.eddy.docker.api.components.Result;
import com.eddy.docker.api.components.WorkingDirectory;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

public class DockerSandboxTest {

    private void testCLangCompile() {
        Result result = DockerSandbox.run("gcc_compile", new Docker.Command("gcc main.c -o main"),
                new WorkingDirectory.UploadedFile("test-files/main.c"));

        assertNotNull(result);
        assertEquals(0, result.getExitCode());
        assertEquals("", result.getStdout().trim());
        assertEquals("", result.getStderr().trim());
        assertFalse(result.isOutOfMemory());
    }

    private void testCRun() {
        Result result = DockerSandbox.run("gcc_run", new Docker.Command("./main"));
        assertNotNull(result);
        assertEquals(0, result.getExitCode());
        assertEquals("Hello", result.getStdout().trim());
        assertEquals("", result.getStderr().trim());
        assertFalse(result.isOutOfMemory());
    }

    private void testCplusCompile() {
        Result result = DockerSandbox.run("gcc_compile", new Docker.Command("g++ main-stdin.cpp -o main"),
                new WorkingDirectory.UploadedFile("test-files/main-stdin.cpp"));

        assertNotNull(result);
        assertEquals(0, result.getExitCode());
        assertEquals("", result.getStdout().trim());
        assertEquals("", result.getStderr().trim());
        assertFalse(result.isOutOfMemory());
    }

    private void testCplusRun() {
        Result result = DockerSandbox.run("gcc_run", new Docker.Command("./main"), "5");

        assertNotNull(result);
        assertEquals(0, result.getExitCode());
        assertEquals("Enter a number: Your number is : 5", result.getStdout().trim().replaceAll("\\n", ""));
        assertEquals("", result.getStderr().trim());
        assertFalse(result.isOutOfMemory());
    }

    private void testCatRun() {
        Result result = DockerSandbox.run("gcc_run", new Docker.Command("cat"), "Hello, this is a test");

        assertNotNull(result);
        assertEquals(0, result.getExitCode());
        assertEquals("Hello, this is a test", result.getStdout().trim());
        assertEquals("", result.getStderr().trim());
        assertFalse(result.isOutOfMemory());
    }

    private void testJavaCompile() {
        Result result = DockerSandbox.run("java_compile", new Docker.Command("javac Test.java"),
                new WorkingDirectory.UploadedFile("test-files/Test.java"));

        assertNotNull(result);
        assertEquals(0, result.getExitCode());
        assertEquals("", result.getStdout().trim());
        assertEquals("", result.getStderr().trim());
        assertFalse(result.isOutOfMemory());
    }

    private void testJavaRun() {
        Result result = DockerSandbox.run("java_run", new Docker.Command("java Test"), "This is line1\nThis is line2");

        assertNotNull(result);
        assertEquals(0, result.getExitCode());
        assertEquals("Enter a value: The first value is: This is line1Enter another value: The second value is: This is line2", result.getStdout().trim().replaceAll("\\n", ""));
        assertEquals("", result.getStderr().trim());
        assertFalse(result.isOutOfMemory());
    }

    @Test
    public void shouldRunEntireFlow() {
        try {
            DockerSandbox.configure(Docker.Shell.BASH, new Profile("java_run", "java-docker", "java-docker", "sandbox"),
                    new Profile("java_compile", "java-docker", "java-docker", "root"),
                    new Profile("gcc_run", "gcc-docker", "gcc-docker", "sandbox"),
                    new Profile("gcc_compile", "gcc-docker", "gcc-docker", "root"));

            assertNotNull(DockerSandbox.getDocker());

            DockerSandbox.setBindings(new Docker.Bindings());
            assertNotNull(DockerSandbox.getBindings());

            List<String> envs = new ArrayList<>();
            envs.add("VAR1=VALUE1");
            envs.add("VAR2=VALUE2");
            envs.add("VAR3=VALUE3");
            String[] envsArr = new String[envs.size()];

            DockerSandbox.addEnvironmentVariables(envs.toArray(envsArr));
            assertEquals(envs, DockerSandbox.getEnvs());

            try {
                assertNull(DockerSandbox.getWorkingDirectory());
                DockerSandbox.start("/home/sandbox");
                assertNotNull(DockerSandbox.getWorkingDirectory());

                testCLangCompile();
                testCRun();
                testCplusCompile();
                testCplusRun();
                testCatRun();
                testJavaCompile();
                testJavaRun();

                DockerSandbox.finish();
                assertNull(DockerSandbox.getWorkingDirectory());
                assertEquals(0, DockerSandbox.getEnvs().size());
                assertNull(DockerSandbox.getBindings());
            } catch (Exception ex) {
                DockerSandbox.onException();
            }
        } catch (Exception ex) {
            fail("An exception occurred doing the workflow: " + ex.getMessage());
        }
    }
}
