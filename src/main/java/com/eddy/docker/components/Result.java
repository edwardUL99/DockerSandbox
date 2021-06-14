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

package com.eddy.docker.components;

/**
 * This class represents the result of a docker execution. It contains fields like the exit code, stdout and stderr stream
 * outputs and a flag to indicate if the docker container ran out of memory
 */
public class Result {
    /**
     * The exit code of the docker container
     */
    private final int exitCode;
    /**
     * The standard output stream as a String
     */
    private final String stdout;
    /**
     * The standard error stream as a String
     */
    private final String stderr;
    /**
     * A flag to indicate if the docker container ran out of memory or not
     */
    private final boolean outOfMemory;
    /**
     * A flag to indicate if the program timed out
     */
    private final boolean timedOut;

    /**
     * Construct a Result object with the provided parameters
     * @param exitCode the exit code of the docker container
     * @param stdout the standard output stream as a String
     * @param stderr the standard error stream as a String
     * @param outOfMemory true if the container ran out of memory
     * @param timedOut true if the program timed out
     */
    public Result(int exitCode, String stdout, String stderr, boolean outOfMemory, boolean timedOut) {
        this.exitCode = exitCode;
        this.stdout = stdout;
        this.stderr = stderr;
        this.outOfMemory = outOfMemory;
        this.timedOut = timedOut;
    }

    /**
     * Get the exit code of the docker container
     * @return the exit code of the container
     */
    public int getExitCode() {
        return exitCode;
    }

    /**
     * Retrieve the standard output stream as a String
     * @return the standard output from the container
     */
    public String getStdout() {
        return stdout;
    }

    /**
     * Retrieve the standard error stream as a String
     * @return the standard error from the container
     */
    public String getStderr() {
        return stderr;
    }

    /**
     * Determine whether the docker container ran out of memory or not
     * @return true if the docker container ran out of memory
     */
    public boolean isOutOfMemory() {
        return outOfMemory;
    }

    /**
     * Determine whether the process this Result is produced from had timed out.
     * If this returns true, {@link #getStdout()} or {@link #getStderr()} may be empty or not complete
     * @return true if timed out, false if not
     */
    public boolean isTimedOut() {
        return timedOut;
    }
}