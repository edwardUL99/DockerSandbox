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

package com.eddy.docker.api.components;

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
     * @since 0.2.0
     */
    private final boolean timedOut;
    /**
     * The duration in seconds of how long the container ran for
     * @since 0.4.0
     */
    private final Double duration;

    /**
     * Construct a Result object with the provided parameters with duration as default Nan
     * @param exitCode the exit code of the docker container
     * @param stdout the standard output stream as a String
     * @param stderr the standard error stream as a String
     * @param outOfMemory true if the container ran out of memory
     * @param timedOut true if the program timed out
     */
    public Result(int exitCode, String stdout, String stderr, boolean outOfMemory, boolean timedOut) {
        this(exitCode, stdout, stderr, outOfMemory, timedOut, Double.NaN);
    }

    /**
     * Construct a Result object with the provided parameters
     * @param exitCode the exit code of the docker container
     * @param stdout the standard output stream as a String
     * @param stderr the standard error stream as a String
     * @param outOfMemory true if the container ran out of memory
     * @param timedOut true if the program timed out
     * @param duration the duration of how long the container ran for
     * @since 0.4.0
     */
    public Result(int exitCode, String stdout, String stderr, boolean outOfMemory, boolean timedOut, Double duration) {
        this.exitCode = exitCode;
        this.stdout = stdout;
        this.stderr = stderr;
        this.outOfMemory = outOfMemory;
        this.timedOut = timedOut;
        this.duration = duration;
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
     * @since 0.2.0
     */
    public boolean isTimedOut() {
        return timedOut;
    }

    /**
     * Retrieve the duration of how long this result took to be produced (i.e. how long the container ran for)
     * @return duration of execution
     * @since 0.4.0
     */
    public Double getDuration() {
        return duration;
    }

    /**
     * Returns a string representation of the object. In general, the
     * {@code toString} method returns a string that
     * "textually represents" this object. The result should
     * be a concise but informative representation that is easy for a
     * person to read.
     * It is recommended that all subclasses override this method.
     * <p>
     * The {@code toString} method for class {@code Object}
     * returns a string consisting of the name of the class of which the
     * object is an instance, the at-sign character `{@code @}', and
     * the unsigned hexadecimal representation of the hash code of the
     * object. In other words, this method returns a string equal to the
     * value of:
     * <blockquote>
     * <pre>
     * getClass().getName() + '@' + Integer.toHexString(hashCode())
     * </pre></blockquote>
     *
     * @return a string representation of the object.
     * @since 0.4.0
     */
    @Override
    public String toString() {
        return String.format("Exit Code:\n%d\n\nStdout:\n%s\nStderr:\n%s\nOOM:\n%s\n\nTimed Out:\n%s\n\nDuration:\n%f\n",
                exitCode, stdout, stderr, outOfMemory, timedOut, duration);
    }
}