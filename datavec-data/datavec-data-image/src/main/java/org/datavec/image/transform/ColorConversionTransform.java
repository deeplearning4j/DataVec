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
package org.datavec.image.transform;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import org.bytedeco.javacv.FrameConverter;
import org.bytedeco.javacv.OpenCVFrameConverter;
import org.datavec.image.data.ImageWritable;

import static org.bytedeco.javacpp.opencv_imgproc.cvtColor;
import static org.bytedeco.javacpp.opencv_imgproc.COLOR_BGR2Luv;
import static org.bytedeco.javacpp.opencv_core.Mat;

/**
 * Color conversion transform using CVT (cvtcolor):
 * <a href="http://docs.opencv.org/2.4/modules/imgproc/doc/miscellaneous_transformations.html#cvtcolor">CVT Color</a>.
 * <a href="http://bytedeco.org/javacpp-presets/opencv/apidocs/org/bytedeco/javacpp/opencv_imgproc.html#cvtColor-org.bytedeco.javacpp.opencv_core.Mat-org.bytedeco.javacpp.opencv_core.Mat-int-int-">More CVT Color</a>.
 */
public class ColorConversionTransform extends BaseImageTransform {

    int conversionCode;

    /**
     * Default conversion BGR to Luv (chroma) color.
     */
    public ColorConversionTransform() {
        this(new Random(1234), COLOR_BGR2Luv);
    }

    /**
     * Return new ColorConversion object
     *
     * @param random Random
     * @param conversionCode  to transform,
     */
    public ColorConversionTransform(Random random, int conversionCode) {
        super(random);
        this.conversionCode = conversionCode;
        this.safeConverter = new HashMap<>();
    }

    /**
     * Takes an image and returns a transformed image.
     * Uses the random object in the case of random transformations.
     *
     * @param image  to transform, null == end of stream
     * @param random object to use (or null for deterministic)
     * @return transformed image
     */
    @Override
    public ImageWritable transform(ImageWritable image, Random random) {
        if (image == null) {
            return null;
        }
        FrameConverter<Mat> frameConverter = getSafeConverter(Thread.currentThread().getId());
        Mat mat = frameConverter.convert(image.getFrame());

        Mat result = new Mat();

        try {
            cvtColor(mat, result, conversionCode);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return new ImageWritable(frameConverter.convert(result));
    }

    protected FrameConverter<Mat> getSafeConverter(long threadId) {
        if(safeConverter.containsKey(threadId))
            return (FrameConverter<Mat>) safeConverter.get(Thread.currentThread().getId());
        else {
            FrameConverter<Mat> converter = new OpenCVFrameConverter.ToMat();
            safeConverter.put(threadId, converter);
            return converter;
        }
    }


}
