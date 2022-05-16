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

package io.github.edwardUL99.docker.sandbox.api.exceptions;

/**
 * This class provides an exception for anything that may occur in the package
 */
public class DockerException extends RuntimeException {
    /**
     * Construct an exception with the provided message
     * @param message the message for this exception to display
     */
    public DockerException(String message) {
        super(message);
    }

    /**
     * Construct an exception with the provided message and Throwable cause
     * @param message the message for this exception to display
     * @param throwable the cause of this exception
     */
    public DockerException(String message, Throwable throwable) {
        super(message, throwable);
    }
}
