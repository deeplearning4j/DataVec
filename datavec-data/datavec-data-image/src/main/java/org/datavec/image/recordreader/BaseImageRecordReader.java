/*-
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

package org.datavec.image.recordreader;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.datavec.api.berkeley.Pair;
import org.datavec.api.conf.Configuration;
import org.datavec.api.io.labels.PathLabelGenerator;
import org.datavec.api.records.Record;
import org.datavec.api.records.metadata.RecordMetaData;
import org.datavec.api.records.metadata.RecordMetaDataURI;
import org.datavec.api.records.reader.BaseRecordReader;
import org.datavec.api.split.FileSplit;
import org.datavec.api.split.InputSplit;
import org.datavec.api.util.files.FileFromPathIterator;
import org.datavec.api.writable.IntWritable;
import org.datavec.api.writable.Writable;
import org.datavec.common.RecordConverter;
import org.datavec.image.loader.ImageLoader;
import org.datavec.image.loader.NativeImageLoader;
import org.datavec.image.loader.BaseImageLoader;
import org.datavec.image.transform.ImageTransform;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.collection.CompactHeapStringList;

import java.io.*;
import java.net.URI;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Base class for the image record reader
 *
 * @author Adam Gibson
 */
@Slf4j
public abstract class BaseImageRecordReader extends BaseRecordReader {
    protected List<String> allPaths;
    protected Iterator<File> iter;
    protected Configuration conf;
    protected File currentFile;
    protected PathLabelGenerator labelGenerator = null;
    protected List<String> labels = new ArrayList<>();
    protected boolean appendLabel = false;
    protected int height = 28, width = 28, channels = 1;
    protected boolean cropImage = false;
    protected ImageTransform imageTransform;
    protected BaseImageLoader imageLoader;
    protected InputSplit inputSplit;
    protected Map<String, String> fileNameMap = new LinkedHashMap<>();
    protected String pattern; // Pattern to split and segment file name, pass in regex
    protected int patternPosition = 0;

    public final static String HEIGHT = NAME_SPACE + ".height";
    public final static String WIDTH = NAME_SPACE + ".width";
    public final static String CHANNELS = NAME_SPACE + ".channels";
    public final static String CROP_IMAGE = NAME_SPACE + ".cropimage";
    public final static String IMAGE_LOADER = NAME_SPACE + ".imageloader";

    /*
        fields for background prefetch
     */
    protected BlockingQueue<Pair<File, ? extends List<Writable>>> buffer;
    protected PrefetchThread prefetchThread;
    protected Pair<File, ? extends List<Writable>> nextElement;
    protected Pair<File, ? extends List<Writable>> terminator = new Pair<>(new File(""), new ArrayList<Writable>());

    public BaseImageRecordReader() {}

    public BaseImageRecordReader(int height, int width, int channels, PathLabelGenerator labelGenerator) {
        this(height, width, channels, labelGenerator, null);
    }

    public BaseImageRecordReader(int height, int width, int channels, PathLabelGenerator labelGenerator,
                    ImageTransform imageTransform) {
        this.height = height;
        this.width = width;
        this.channels = channels;
        this.labelGenerator = labelGenerator;
        this.imageTransform = imageTransform;
        this.appendLabel = labelGenerator != null ? true : false;
    }

    protected boolean containsFormat(String format) {
        for (String format2 : imageLoader.getAllowedFormats())
            if (format.endsWith("." + format2))
                return true;
        return false;
    }


    @Override
    public void initialize(InputSplit split) throws IOException {
        if (imageLoader == null) {
            imageLoader = new NativeImageLoader(height, width, channels, imageTransform);
        }
        inputSplit = split;
        URI[] locations = split.locations();
        if (locations != null && locations.length >= 1) {
            if (locations.length > 1 || containsFormat(locations[0].getPath())) {
                allPaths = new CompactHeapStringList();
                for (URI location : locations) {
                    File imgFile = new File(location);
                    if (!imgFile.isDirectory() && containsFormat(imgFile.getAbsolutePath())) {
                        allPaths.add(imgFile.toURI().toString());
                    }
                    if (appendLabel) {
                        File parentDir = imgFile.getParentFile();
                        String name = parentDir.getName();
                        if (labelGenerator != null) {
                            name = labelGenerator.getLabelForPath(location).toString();
                        }
                        if (!labels.contains(name)) {
                            labels.add(name);
                        }
                        if (pattern != null) {
                            String label = name.split(pattern)[patternPosition];
                            fileNameMap.put(imgFile.toString(), label);
                        }
                    }
                }
            } else {
                File curr = new File(locations[0]);
                if (!curr.exists())
                    throw new IllegalArgumentException("Path " + curr.getAbsolutePath() + " does not exist!");
                if (curr.isDirectory()) {
                    Collection<File> temp = FileUtils.listFiles(curr, null, true);
                    allPaths = new CompactHeapStringList();
                    for (File f : temp) {
                        allPaths.add(f.getPath());
                    }
                } else {
                    allPaths = Collections.singletonList(curr.getPath());
                }

            }
            iter = new FileFromPathIterator(inputSplit.locationsPathIterator()); //This handles randomization internally if necessary
        } else
            throw new IllegalArgumentException("No path locations found in the split.");

        if (split instanceof FileSplit) {
            //remove the root directory
            FileSplit split1 = (FileSplit) split;
            labels.remove(split1.getRootDir());
        }

        //To ensure consistent order for label assignment (irrespective of file iteration order), we want to sort the list of labels
        Collections.sort(labels);

        startPrefetch();
    }


    @Override
    public void initialize(Configuration conf, InputSplit split) throws IOException, InterruptedException {
        this.appendLabel = conf.getBoolean(APPEND_LABEL, false);
        this.labels = new ArrayList<>(conf.getStringCollection(LABELS));
        this.height = conf.getInt(HEIGHT, height);
        this.width = conf.getInt(WIDTH, width);
        this.channels = conf.getInt(CHANNELS, channels);
        this.cropImage = conf.getBoolean(CROP_IMAGE, cropImage);
        if ("imageio".equals(conf.get(IMAGE_LOADER))) {
            this.imageLoader = new ImageLoader(height, width, channels, cropImage);
        } else {
            this.imageLoader = new NativeImageLoader(height, width, channels, imageTransform);
        }
        this.conf = conf;
        initialize(split);
    }


    /**
     * Called once at initialization.
     *
     * @param split          the split that defines the range of records to read
     * @param imageTransform the image transform to use to transform images while loading them
     * @throws java.io.IOException
     */
    public void initialize(InputSplit split, ImageTransform imageTransform) throws IOException {
        this.imageLoader = null;
        this.imageTransform = imageTransform;
        initialize(split);
    }

    /**
     * Called once at initialization.
     *
     * @param conf           a configuration for initialization
     * @param split          the split that defines the range of records to read
     * @param imageTransform the image transform to use to transform images while loading them
     * @throws java.io.IOException
     * @throws InterruptedException
     */
    public void initialize(Configuration conf, InputSplit split, ImageTransform imageTransform)
                    throws IOException, InterruptedException {
        this.imageLoader = null;
        this.imageTransform = imageTransform;
        initialize(conf, split);
    }

    @Override
    public boolean hasNext() {
        try {
            if (prefetchThread != null)
                return prefetchThread.hasMore();
            else
                return false;
        } catch (Exception e) {
            log.error("Premature end of loop!");
            return false;
        }
    }

    @Override
    public List<Writable> next() {
        Pair<File,? extends List<Writable>> temp = prefetchThread.nextElement();
        currentFile = temp.getFirst();
        nextElement = null;
        return temp.getSecond();
    }

    @Override
    public Record nextRecord() {
        List<Writable> list = next();
        URI uri = currentFile.toURI();
        return new org.datavec.api.records.impl.Record(list, new RecordMetaDataURI(uri, BaseImageRecordReader.class));
    }

    @Override
    public Record loadFromMetaData(RecordMetaData recordMetaData) throws IOException {
        return loadFromMetaData(Collections.singletonList(recordMetaData)).get(0);
    }

    @Override
    public List<Record> loadFromMetaData(List<RecordMetaData> recordMetaDatas) throws IOException {
        List<Record> out = new ArrayList<>();
        for (RecordMetaData meta : recordMetaDatas) {
            URI uri = meta.getURI();
            File f = new File(uri);

            List<Writable> next;
            try (DataInputStream dis = new DataInputStream(new BufferedInputStream(new FileInputStream(f)))) {
                next = record(uri, dis);
            }
            out.add(new org.datavec.api.records.impl.Record(next, meta));
        }
        return out;
    }

    @Override
    public void close() throws IOException {

    }

    @Override
    public void setConf(Configuration conf) {
        this.conf = conf;
    }

    @Override
    public Configuration getConf() {
        return conf;
    }


    /**
     * Get the label from the given path
     *
     * @param path the path to get the label from
     * @return the label for the given path
     */
    public String getLabel(String path) {
        if (labelGenerator != null) {
            return labelGenerator.getLabelForPath(path).toString();
        }
        if (fileNameMap != null && fileNameMap.containsKey(path))
            return fileNameMap.get(path);
        return (new File(path)).getParentFile().getName();
    }

    /**
     * Returns the file loaded last by {@link #next()}.
     */
    public File getCurrentFile() {
        return currentFile;
    }

    /**
     * Sets manually the file returned by {@link #getCurrentFile()}.
     */
    public void setCurrentFile(File currentFile) {
        this.currentFile = currentFile;
    }

    @Override
    public List<String> getLabels() {
        return labels;
    }

    public void setLabels(List<String> labels) {
        this.labels = labels;
    }

    /**
     * Returns {@code getLabels().size()}.
     */
    public int numLabels() {
        return labels.size();
    }

    @Override
    public List<Writable> record(URI uri, DataInputStream dataInputStream) throws IOException {
        invokeListeners(uri);
        if (imageLoader == null) {
            imageLoader = new NativeImageLoader(height, width, channels, imageTransform);
        }
        INDArray row = imageLoader.asMatrix(dataInputStream);
        List<Writable> ret = RecordConverter.toRecord(row);
        if (appendLabel)
            ret.add(new IntWritable(labels.indexOf(getLabel(uri.getPath()))));
        return ret;
    }

    public void shutdown() {
        prefetchThread.stopWork();
    }

    @Override
    public void reset() {
        if (inputSplit == null)
            throw new UnsupportedOperationException("Cannot reset without first initializing");
        inputSplit.reset();
        if (iter != null) {
            iter = new FileFromPathIterator(inputSplit.locationsPathIterator());
        }
        if (prefetchThread != null) {
            prefetchThread.reset();
            prefetchThread.setIterator(iter);
        }
    }

    private void startPrefetch() {
        this.buffer = new LinkedBlockingQueue<>();
        this.prefetchThread = new PrefetchThread(iter, this.buffer);
        prefetchThread.start();
    }

    private class PrefetchThread extends Thread implements Runnable {
        @Setter
        private Iterator<File> iterator;
        private BlockingQueue<Pair<File,? extends List<Writable>>> buffer;
        private AtomicBoolean shouldWork = new AtomicBoolean(true);
        private ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

        public PrefetchThread(
                Iterator<File> iterator,
                BlockingQueue<Pair<File,? extends List<Writable>>> buffer) {
            this.buffer = buffer;
            this.iterator = iterator;

            setDaemon(true);
            setName("ImageRecordReader prefetch Thread");
        }

        @Override
        public void run() {
            try {
                while (shouldWork.get()) {
                    List<Writable> ret;
                    if(iterator != null) {
                        if (iterator.hasNext()) {
                            File image = iterator.next();
                            currentFile = image;

                            if (image.isDirectory()) {
                                // do nothing
                            } else {
                                invokeListeners(image);
                                INDArray row = imageLoader.asMatrix(image);
                                ret = RecordConverter.toRecord(row);
                                if (appendLabel)
                                    ret.add(new IntWritable(labels.indexOf(getLabel(image.getPath()))));

                                buffer.put(new Pair<>(image, ret));
                            }
                        }
                    }
                }

            } catch (Exception e) {
                // TODO: pass that forward
                throw new RuntimeException(e);
            }
        }

        public boolean hasMore() {
            if(!buffer.isEmpty())
                return true;
            else
                return iterator.hasNext();
        }

        public Pair<File,? extends List<Writable>> nextElement() {
            if (!buffer.isEmpty())
                return buffer.poll();

            try {
                return buffer.poll(500L, TimeUnit.MILLISECONDS);
            } catch (Exception e) {
                return null;
            }
        }

        public void reset() {
            try {
//                lock.writeLock().lock();
                buffer.clear();
            } catch (Exception e) {
                throw new RuntimeException(e);
            } finally {
//                lock.writeLock().unlock();
            }
        }

        public void stopWork() {
            shouldWork.set(false);
        }
    }
}
