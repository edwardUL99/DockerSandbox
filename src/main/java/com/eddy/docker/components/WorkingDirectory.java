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

import com.eddy.docker.Docker;
import com.eddy.docker.exceptions.DockerException;
import com.github.dockerjava.api.command.CreateVolumeResponse;
import com.github.dockerjava.api.exception.ConflictException;
import com.github.dockerjava.api.model.Volume;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.io.IOUtils;

import java.io.*;
import java.util.List;
import java.util.UUID;
import java.util.zip.GZIPOutputStream;

/**
 * The WorkingDirectory class provides an abstraction surrounding the passing of files between the host machine and the
 * docker container. It also allows files produced in one docker container to be accessed by another docker container.
 *
 * Internally, it uses a Docker volume, which provided that this is cleaned up correctly, is removed after it is no longer required.
 * It currently only allows files to be added to the docker container and not the other way around.
 *
 * The only way to instantiate this class is through {@link Docker#open(String)}.
 */
public class WorkingDirectory implements Closeable {
    /**
     * The Docker client that created this WorkingDirectory
     */
    private final Docker docker;
    /**
     * The name of the docker volume on the host machine
     */
    private final String name;
    /**
     * A field to check if the volume has been created or not
     */
    private CreateVolumeResponse volumeResponse;
    /**
     * The working directory of where the files added are made available on the docker container
     */
    private final String workingDirectory;
    /**
     * The volume object representing the remote volume
     */
    private Volume volume;
    /**
     * A flag to indicate whether this working directory has been closed or not
     */
    private boolean closed;

    /**
     * Create a working directory instance with the provided docker client and remote working directory path
     * @param docker the docker client that created this working directory
     * @param workingDirectory the path to the working directory on the docker container
     */
    protected WorkingDirectory(Docker docker, String workingDirectory) {
        this.docker = docker;
        name = "DockerSandbox-" + UUID.randomUUID().toString();
        this.workingDirectory = workingDirectory;
    }

    /**
     * Get the name of the volume behind this working directory
     * @return name of workdir volume
     */
    public String getName() {
        return name;
    }

    /**
     * Opens this working directory to make it available to add files to it.
     * This needs to be called before {@link #addFiles(String, UploadedFile...)}
     */
    public void open() {
        volumeResponse = docker.createVolume(name);
        closed = false;
    }

    /**
     * Retrieve the volume object representing the volume on the docker container
     * @return volume on docker container
     */
    public Volume getVolume() {
        if (volume == null)
            volume = new Volume(workingDirectory);

        return volume;
    }

    /**
     * Writes the file to the tar file stream
     * @param file file to write
     * @param tarArchive the outputstream for writing to the tar archive
     * @throws IOException if an error occurs
     */
    private void writeFileToTar(File file, TarArchiveOutputStream tarArchive) throws IOException {
        FileInputStream fileInputStream = new FileInputStream(file);
        BufferedInputStream bufferedInputStream = new BufferedInputStream(fileInputStream);

        IOUtils.copy(bufferedInputStream, tarArchive);
        tarArchive.closeArchiveEntry();
        bufferedInputStream.close();
    }

    /**
     * Writes a directory to the tar file stream
     * @param file the file representing the directory
     * @param entryName the name of the entry representing this directory
     * @param tarArchive the outputstream for writing to the tar archive
     * @throws IOException if an error occurs
     */
    private void writeDirectoryToTar(File file, String entryName, TarArchiveOutputStream tarArchive) throws IOException {
        tarArchive.closeArchiveEntry();

        File[] files = file.listFiles();
        if (files != null) {
            for (File f : files) {
                addFilesToTar(f.getAbsolutePath(), f.getName(), entryName + File.separator, tarArchive);
            }
        }
    }

    /**
     * Add files and directories recursively to the tar archive stream
     * @param filePath the path of the file/directory to add to the tar
     * @param fileName the name of the file/directory
     * @param parent the parent. On the first call, this should be empty
     * @param tarArchive the output stream for writing to the tar
     * @throws IOException if an error occurs
     */
    private void addFilesToTar(String filePath, String fileName, String parent, TarArchiveOutputStream tarArchive) throws IOException {
        File file = new File(filePath);
        String entryName = parent + fileName;
        tarArchive.putArchiveEntry(tarArchive.createArchiveEntry(file, entryName));

        if (file.isFile()) {
            writeFileToTar(file, tarArchive);
        } else {
            writeDirectoryToTar(file, entryName, tarArchive);
        }
    }

    /**
     * Add the list of files to the working directory for the container specified with the provided container id.
     *
     * If {@link #open()} has not been called or {@link #close()} has been called, a DockerException will be thrown
     *
     * @param containerId the id of the container the files are being added for. Even if this is for a specific container,
     *                    other docker containers using this WorkingDirectory will be able to access the files
     * @param files the list of files to be uploaded
     */
    public void addFiles(String containerId, UploadedFile...files) {
        if (closed || volumeResponse == null)
            throw new DockerException("This WorkingDirectory has not been opened or has been closed");

        try {
            TarArchiveOutputStream tarStream = null;
            FileInputStream inputStream = null;
            try {
                File tarFile = File.createTempFile(name, ".tar.gz");
                tarFile.deleteOnExit();

                FileOutputStream fileOutputStream = new FileOutputStream(tarFile);
                GZIPOutputStream gzipOutputStream = new GZIPOutputStream(fileOutputStream);
                tarStream = new TarArchiveOutputStream(gzipOutputStream);
                tarStream.setLongFileMode(TarArchiveOutputStream.LONGFILE_POSIX);

                for (UploadedFile file : files)
                    addFilesToTar(file.getPath(), file.getName(), "", tarStream);

                tarStream.finish();
                tarStream.close();

                inputStream = new FileInputStream(tarFile);

                docker.copyTarToContainer(containerId, workingDirectory, inputStream);
            } finally {
                if (tarStream != null)
                    tarStream.close();

                if (inputStream != null)
                    inputStream.close();
            }
        } catch (IOException ex) {
            throw new DockerException("An exception occurred when adding files to the working directory", ex);
        }
    }

    /**
     * Retrieve whether this working directory is closed and is no longer accepting files. If {@link #addFiles(String, UploadedFile...)}
     * is called when this returns false, it will throw an IllegalStateException
     * @return true if closed, false if still open
     */
    public boolean isClosed() {
        return closed;
    }

    /**
     * Closes this stream and releases any system resources associated
     * with it. If the stream is already closed then invoking this
     * method has no effect.
     *
     * <p> As noted in {@link AutoCloseable#close()}, cases where the
     * close may fail require careful attention. It is strongly advised
     * to relinquish the underlying resources and to internally
     * <em>mark</em> the {@code Closeable} as closed, prior to throwing
     * the {@code IOException}.
     *
     * @throws IOException if an I/O error occurs
     */
    @Override
    public void close() throws IOException {
        try {
            docker.removeVolume(name);
            volumeResponse = null;
            volume = null;
            closed = true;
        } catch (ConflictException ex) {
            ex.printStackTrace();
            throw new IOException("Failed to remove underlying volume: " + ex.getMessage(), ex);
        }
    }

    /**
     * This class represents a file that will be uploaded to the working directory
     */
    public static class UploadedFile {
        /**
         * The name of the file that will be on the working directory
         */
        private final String name;
        /**
         * The path to the file on the host machine
         */
        private final String path;

        /**
         * Construct an UploadedFile object with the provided name and path.
         * @param name The name of the file which will appear on the container's working directory
         * @param path the path to the file on the host machine
         */
        public UploadedFile(String name, String path) {
            this.name = name;
            this.path = path;
        }

        /**
         * Construct an UploadedFile object with the provided path. The name of the file is retrieved from new File(path).getName()
         * @param path the path to the file on the host machine.
         */
        public UploadedFile(String path) {
            File file = new File(path);
            this.name = file.getName();
            this.path = path;
        }

        /**
         * Retrieve the name of the file that appears on the container's working directory
         * @return name of the file
         */
        public String getName() {
            return name;
        }

        /**
         * Retrieves the path of the file on the host machine
         * @return path of file on host machine
         */
        public String getPath() {
            return path;
        }
    }
}
