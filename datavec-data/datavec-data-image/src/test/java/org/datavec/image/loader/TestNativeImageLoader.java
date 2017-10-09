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
package org.datavec.image.loader;

import org.bytedeco.javacpp.indexer.UByteIndexer;
import org.bytedeco.javacv.Java2DFrameConverter;
import org.bytedeco.javacv.OpenCVFrameConverter;
import org.datavec.api.util.ClassPathResource;
import org.datavec.image.data.ImageWritable;
import org.junit.Test;
import org.nd4j.linalg.api.ndarray.INDArray;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Random;

import static org.bytedeco.javacpp.lept.*;
import static org.bytedeco.javacpp.opencv_core.*;
import static org.junit.Assert.assertEquals;

/**
 *
 * @author saudet
 */
public class TestNativeImageLoader {
    static final long seed = 10;
    static final Random rng = new Random(seed);

    @Test
    public void testConvertPix() {
        PIX pix;
        Mat mat;

        pix = pixCreate(11, 22, 1);
        mat = NativeImageLoader.convert(pix);
        assertEquals(11, mat.cols());
        assertEquals(22, mat.rows());
        assertEquals(CV_8UC1, mat.type());

        pix = pixCreate(33, 44, 2);
        mat = NativeImageLoader.convert(pix);
        assertEquals(33, mat.cols());
        assertEquals(44, mat.rows());
        assertEquals(CV_8UC1, mat.type());

        pix = pixCreate(55, 66, 4);
        mat = NativeImageLoader.convert(pix);
        assertEquals(55, mat.cols());
        assertEquals(66, mat.rows());
        assertEquals(CV_8UC1, mat.type());

        pix = pixCreate(77, 88, 8);
        mat = NativeImageLoader.convert(pix);
        assertEquals(77, mat.cols());
        assertEquals(88, mat.rows());
        assertEquals(CV_8UC1, mat.type());

        pix = pixCreate(99, 111, 16);
        mat = NativeImageLoader.convert(pix);
        assertEquals(99, mat.cols());
        assertEquals(111, mat.rows());
        assertEquals(CV_8UC2, mat.type());

        pix = pixCreate(222, 333, 24);
        mat = NativeImageLoader.convert(pix);
        assertEquals(222, mat.cols());
        assertEquals(333, mat.rows());
        assertEquals(CV_8UC3, mat.type());

        pix = pixCreate(444, 555, 32);
        mat = NativeImageLoader.convert(pix);
        assertEquals(444, mat.cols());
        assertEquals(555, mat.rows());
        assertEquals(CV_8UC4, mat.type());

        // a GIF file, for example
        pix = pixCreate(32, 32, 8);
        PIXCMAP cmap = pixcmapCreateLinear(8, 256);
        pixSetColormap(pix, cmap);
        mat = NativeImageLoader.convert(pix);
        assertEquals(32, mat.cols());
        assertEquals(32, mat.rows());
        assertEquals(CV_8UC4, mat.type());
    }

    @Test
    public void testAsRowVector() throws Exception {
        BufferedImage img1 = makeRandomBufferedImage(0, 0, 1);
        Mat img2 = makeRandomImage(0, 0, 3);

        int w1 = 35, h1 = 79, ch1 = 3;
        NativeImageLoader loader1 = new NativeImageLoader(h1, w1, ch1);

        INDArray array1 = loader1.asRowVector(img1);
        assertEquals(2, array1.rank());
        assertEquals(1, array1.rows());
        assertEquals(h1 * w1 * ch1, array1.columns());

        INDArray array2 = loader1.asRowVector(img2);
        assertEquals(2, array2.rank());
        assertEquals(1, array2.rows());
        assertEquals(h1 * w1 * ch1, array2.columns());

        int w2 = 103, h2 = 68, ch2 = 4;
        NativeImageLoader loader2 = new NativeImageLoader(h2, w2, ch2);

        INDArray array3 = loader2.asRowVector(img1);
        assertEquals(2, array3.rank());
        assertEquals(1, array3.rows());
        assertEquals(h2 * w2 * ch2, array3.columns());

        INDArray array4 = loader2.asRowVector(img2);
        assertEquals(2, array4.rank());
        assertEquals(1, array4.rows());
        assertEquals(h2 * w2 * ch2, array4.columns());
    }

    @Test
    public void testAsMatrix() throws Exception {
        BufferedImage img1 = makeRandomBufferedImage(0, 0, 3);
        Mat img2 = makeRandomImage(0, 0, 4);

        int w1 = 33, h1 = 77, ch1 = 1;
        NativeImageLoader loader1 = new NativeImageLoader(h1, w1, ch1);

        INDArray array1 = loader1.asMatrix(img1);
        assertEquals(4, array1.rank());
        assertEquals(1, array1.size(0));
        assertEquals(1, array1.size(1));
        assertEquals(h1, array1.size(2));
        assertEquals(w1, array1.size(3));

        INDArray array2 = loader1.asMatrix(img2);
        assertEquals(4, array2.rank());
        assertEquals(1, array2.size(0));
        assertEquals(1, array2.size(1));
        assertEquals(h1, array2.size(2));
        assertEquals(w1, array2.size(3));

        int w2 = 111, h2 = 66, ch2 = 3;
        NativeImageLoader loader2 = new NativeImageLoader(h2, w2, ch2);

        INDArray array3 = loader2.asMatrix(img1);
        assertEquals(4, array3.rank());
        assertEquals(1, array3.size(0));
        assertEquals(3, array3.size(1));
        assertEquals(h2, array3.size(2));
        assertEquals(w2, array3.size(3));

        INDArray array4 = loader2.asMatrix(img2);
        assertEquals(4, array4.rank());
        assertEquals(1, array4.size(0));
        assertEquals(3, array4.size(1));
        assertEquals(h2, array4.size(2));
        assertEquals(w2, array4.size(3));

        int w3 = 123, h3 = 77, ch3 = 3;
        NativeImageLoader loader3 = new NativeImageLoader(h3, w3, ch3);
        File f3 = new ClassPathResource("/testimages/class0/2.jpg").getFile();
        ImageWritable iw3 = loader3.asWritable(f3);

        INDArray array5 = loader3.asMatrix(iw3);
        assertEquals(4, array5.rank());
        assertEquals(1, array5.size(0));
        assertEquals(3, array5.size(1));
        assertEquals(h3, array5.size(2));
        assertEquals(w3, array5.size(3));

        int w4 = 71, h4 = 184, ch4 = 2, pages = 2;
        NativeImageLoader loader4 = new NativeImageLoader(h4, w4, ch4);
        INDArray array6 = loader4.asMatrix(
              new ClassPathResource("/testimages/mitosis.tif").getFile().getPath());
        assertEquals(4, array6.rank());
        assertEquals(1, array6.size(0));
        assertEquals(pages, array6.size(1));
        assertEquals(h4, array6.size(2));
        assertEquals(w4, array6.size(3));
    }

    @Test
    public void testScalingIfNeed() throws Exception {
        Mat img1 = makeRandomImage(0, 0, 1);
        Mat img2 = makeRandomImage(0, 0, 3);

        int w1 = 60, h1 = 110, ch1 = 1;
        NativeImageLoader loader1 = new NativeImageLoader(h1, w1, ch1);

        Mat scaled1 = loader1.scalingIfNeed(img1);
        assertEquals(h1, scaled1.rows());
        assertEquals(w1, scaled1.cols());
        assertEquals(img1.channels(), scaled1.channels());

        Mat scaled2 = loader1.scalingIfNeed(img2);
        assertEquals(h1, scaled2.rows());
        assertEquals(w1, scaled2.cols());
        assertEquals(img2.channels(), scaled2.channels());

        int w2 = 70, h2 = 120, ch2 = 3;
        NativeImageLoader loader2 = new NativeImageLoader(h2, w2, ch2);

        Mat scaled3 = loader2.scalingIfNeed(img1);
        assertEquals(h2, scaled3.rows());
        assertEquals(w2, scaled3.cols());
        assertEquals(img1.channels(), scaled3.channels());

        Mat scaled4 = loader2.scalingIfNeed(img2);
        assertEquals(h2, scaled4.rows());
        assertEquals(w2, scaled4.cols());
        assertEquals(img2.channels(), scaled4.channels());
    }

    @Test
    public void testCenterCropIfNeeded() throws Exception {
        int w1 = 60, h1 = 110, ch1 = 1;
        int w2 = 120, h2 = 70, ch2 = 3;

        Mat img1 = makeRandomImage(h1, w1, ch1);
        Mat img2 = makeRandomImage(h2, w2, ch2);

        NativeImageLoader loader = new NativeImageLoader(h1, w1, ch1, true);

        Mat cropped1 = loader.centerCropIfNeeded(img1);
        assertEquals(85, cropped1.rows());
        assertEquals(60, cropped1.cols());
        assertEquals(img1.channels(), cropped1.channels());

        Mat cropped2 = loader.centerCropIfNeeded(img2);
        assertEquals(70, cropped2.rows());
        assertEquals(95, cropped2.cols());
        assertEquals(img2.channels(), cropped2.channels());
    }


    BufferedImage makeRandomBufferedImage(int height, int width, int channels) {
        Mat img = makeRandomImage(height, width, channels);

        OpenCVFrameConverter.ToMat c = new OpenCVFrameConverter.ToMat();
        Java2DFrameConverter c2 = new Java2DFrameConverter();

        return c2.convert(c.convert(img));
    }

    Mat makeRandomImage(int height, int width, int channels) {
        if (height <= 0) {
            height = rng.nextInt() % 100 + 100;
        }
        if (width <= 0) {
            width = rng.nextInt() % 100 + 100;
        }

        Mat img = new Mat(height, width, CV_8UC(channels));
        UByteIndexer idx = img.createIndexer();
        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                for (int k = 0; k < channels; k++) {
                    idx.put(i, j, k, rng.nextInt());
                }
            }
        }
        return img;
    }

    @Test
    public void testAsWritable() throws Exception {
        File f0 = new ClassPathResource("/testimages/class0/0.jpg").getFile();

        NativeImageLoader imageLoader = new NativeImageLoader();
        ImageWritable img = imageLoader.asWritable(f0);

        assertEquals(32, img.getFrame().imageHeight);
        assertEquals(32, img.getFrame().imageWidth);
        assertEquals(3, img.getFrame().imageChannels);

        BufferedImage img1 = makeRandomBufferedImage(0, 0, 3);
        Mat img2 = makeRandomImage(0, 0, 4);

        int w1 = 33, h1 = 77, ch1 = 1;
        NativeImageLoader loader1 = new NativeImageLoader(h1, w1, ch1);

        INDArray array1 = loader1.asMatrix(f0);
        assertEquals(4, array1.rank());
        assertEquals(1, array1.size(0));
        assertEquals(1, array1.size(1));
        assertEquals(h1, array1.size(2));
        assertEquals(w1, array1.size(3));
    }

}
