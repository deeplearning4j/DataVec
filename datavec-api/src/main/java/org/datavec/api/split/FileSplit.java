/*
 *  * Copyright 2016 Skymind, Inc.
 *  *
 *  *    Licensed under the Apache License, Version 2.0 (the "License");
 *  *    you may not use this file except in compliance with the License.
 *  *    You may obtain a copy of the License at
 *  *
 *  *        http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  *    Unless required by applicable law or agreed to in writing, software
 *  *    distributed under the License is distributed on an "AS IS" BASIS,
 *  *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  *    See the License for the specific language governing permissions and
 *  *    limitations under the License.
 */

package org.datavec.api.split;

import lombok.NonNull;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;

/**
 * File input split. Splits up a root directory in to files.
 *
 * @author Adam Gibson
 * @author Ede Meijer
 */
public class FileSplit extends BaseInputSplit {
    private Path rootDir;
    // Use for Collections, pass in list of file type strings
    private String[] allowedExtensions = null;
    private boolean recursive = true;
    private Random random;
    private boolean randomize = false;

    private FileSplit(@NonNull Path rootDir, String[] allowFormat, boolean recursive, Random random, boolean runMain) {
        if (allowFormat != null) {
            allowedExtensions = new String[allowFormat.length];
            for (int i = 0; i < allowFormat.length; i++) {
                allowedExtensions[i] = "." + allowFormat[i];
            }
        }
        this.recursive = recursive;
        this.rootDir = rootDir;
        if (random != null) {
            this.random = random;
            this.randomize = true;
        }
        if (runMain) this.initialize();
    }

    public FileSplit(File rootDir) {
        this(rootDir.toPath());
    }

    public FileSplit(Path rootDir) {
        this(rootDir, null, true, null, true);
    }

    public FileSplit(File rootDir, Random rng) {
        this(rootDir.toPath(), rng);
    }

    public FileSplit(Path rootDir, Random rng) {
        this(rootDir, null, true, rng, true);
    }

    public FileSplit(File rootDir, String[] allowFormat) {
        this(rootDir.toPath(), allowFormat);
    }

    public FileSplit(Path rootDir, String[] allowFormat) {
        this(rootDir, allowFormat, true, null, true);
    }

    public FileSplit(File rootDir, String[] allowFormat, Random rng) {
        this(rootDir.toPath(), allowFormat, rng);
    }

    public FileSplit(Path rootDir, String[] allowFormat, Random rng) {
        this(rootDir, allowFormat, true, rng, true);
    }

    public FileSplit(File rootDir, String[] allowFormat, boolean recursive) {
        this(rootDir.toPath(), allowFormat, recursive);
    }

    public FileSplit(Path rootDir, String[] allowFormat, boolean recursive) {
        this(rootDir, allowFormat, recursive, null, true);
    }

    protected void initialize() {
        try {
            // If the root dir is a file, don't apply any filtering on extensions
            List<Path> files = Files.isDirectory(rootDir) ? listFiles(rootDir, recursive) : singletonList(rootDir);
            locations = new URI[files.size()];

            if (randomize) {
                Collections.shuffle(files, random);
            }
            int count = 0;
            for (Path file : files) {
                locations[count++] = file.toUri();
                length += Files.size(file);
            }
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    private List<Path> listFiles(Path path, boolean recursive) throws IOException {
        if (Files.isDirectory(path)) {
            List<Path> result = new ArrayList<>();
            for (Path subPath : Files.newDirectoryStream(path)) {
                if (recursive || !Files.isDirectory(subPath)) {
                    result.addAll(listFiles(subPath, recursive));
                }
            }
            return result;
        } else {
            if (allowedExtensions != null) {
                for (String extension : allowedExtensions) {
                    if (path.toString().endsWith(extension)) {
                        return singletonList(path);
                    }
                }
                return emptyList();
            }
            return singletonList(path);
        }
    }

    @Override
    public long length() {
        return length;
    }


    @Override
    public void write(DataOutput out) throws IOException {

    }

    @Override
    public void readFields(DataInput in) throws IOException {

    }

    public File getRootDir() {
        return rootDir.toFile();
    }

    public Path getRootDirPath() {
        return rootDir;
    }
}




