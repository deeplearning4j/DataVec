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

import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Random;

import static org.junit.Assert.assertEquals;

public class FileSplitTest {
    private Path file;
    private Path file1;
    private Path file3;
    private Path newPath;
    private String[] allForms = {"jpg", "jpeg", "JPG", "JPEG"};
    private static String localPath = "/";
    private static String testPath = localPath + "test/";

    @Before
    public void doBefore() throws IOException {
        FileSystem fs = Jimfs.newFileSystem(Configuration.unix());
        file = Files.createFile(fs.getPath(localPath + "myfile.txt"));

        newPath = fs.getPath(testPath);
        Files.createDirectory(newPath);

        file1 = Files.createFile(fs.getPath(testPath, "myfile_1.jpg"));
        Files.createFile(fs.getPath(testPath, "myfile_2.txt"));
        file3 = Files.createFile(fs.getPath(testPath, "myfile_3.jpg"));
        Files.createFile(fs.getPath(testPath, "treehouse_4.csv"));
        Files.createFile(fs.getPath(testPath, "treehouse_5.csv"));
        Files.createFile(fs.getPath(testPath, "treehouse_6.jpg"));
    }

    @Test
    public void testInitializeLoadSingleFile() {
        InputSplit split = new FileSplit(file, allForms);
        assertEquals(split.locations()[0], file.toUri());
    }

    @Test
    public void testInitializeLoadMulFiles() throws IOException {
        InputSplit split = new FileSplit(newPath, allForms, true);
        assertEquals(3, split.locations().length);
        assertEquals(file1.toUri(), split.locations()[0]);
        assertEquals(file3.toUri(), split.locations()[1]);
    }

    @Test
    public void testInitializeMulFilesShuffle() throws IOException {
        InputSplit split = new FileSplit(newPath, new Random(123));
        InputSplit split2 = new FileSplit(newPath, new Random(123));
        assertEquals(6, split.locations().length);
        assertEquals(6, split2.locations().length);
        assertEquals(split.locations()[3], split2.locations()[3]);
    }
}
