package org.datavec.image.transform;

import org.bytedeco.javacv.FrameConverter;
import org.datavec.image.data.ImageWritable;

import java.util.Random;

import static org.bytedeco.javacpp.opencv_core.*;

/**
 * Transforms images deterministically or randomly with the help of an array of ImageTransform
 *
 * @author saudet
 */
public class MultiImageTransform extends BaseImageTransform<Mat> {
    private PipelineImageTransform transform;

    public MultiImageTransform(ImageTransform... transforms) {
        this(null, transforms);
    }

    public MultiImageTransform(Random random, ImageTransform... transforms) {
        super(random);
        transform = new PipelineImageTransform(transforms);
    }

    @Override
    public ImageWritable transform(ImageWritable image, Random random) {
        return random == null ? transform.transform(image) : transform.transform(image, random);
    }

    @Override
    public ImageWritable transform(ImageWritable image) {
        return transform.transform(image);
    }

    protected FrameConverter<Mat> getSafeConverter(long threadId) {
        throw new UnsupportedOperationException("Frame converters are used at individual transform levels");
    }
}
