package org.datavec.image.transform;

import java.util.Objects;
import java.util.Random;
import org.datavec.image.data.ImageWritable;

/**
 * Image transform proxy class to keep some useful data about transformed images such as the width and height after being transformed.
 *
 * @author Eduardo Ramos
 */
public class ProxyImageTransform implements ImageTransform {

    private final ImageTransform transform;

    //Before transform:
    private int imageWidthBeforeTransform = -1;
    private int imageHeightBeforeTransform = -1;
    private int imageDepthBeforeTransform = -1;

    //After transform:
    private int imageWidthAfterTransform = -1;
    private int imageHeightAfterTransform = -1;
    private int imageDepthAfterTransform = -1;

    public ProxyImageTransform(ImageTransform transform) {
        Objects.requireNonNull(transform, "transform required");
        this.transform = transform;
    }

    @Override
    public ImageWritable transform(ImageWritable image) {
        return transform(image, null);
    }

    @Override
    public ImageWritable transform(ImageWritable image, Random random) {
        imageWidthBeforeTransform = image.getFrame().imageWidth;
        imageHeightBeforeTransform = image.getFrame().imageHeight;
        imageDepthBeforeTransform = image.getFrame().imageDepth;

        ImageWritable result = transform.transform(image, random);

        imageWidthAfterTransform = result.getFrame().imageWidth;
        imageHeightAfterTransform = result.getFrame().imageHeight;
        imageDepthAfterTransform = result.getFrame().imageDepth;

        return result;
    }

    @Override
    public float[] query(float... coordinates) {
        return transform.query(coordinates);
    }

    public ImageTransform getTransform() {
        return transform;
    }

    public int getImageWidthBeforeTransform() {
        return imageWidthBeforeTransform;
    }

    public int getImageHeightBeforeTransform() {
        return imageHeightBeforeTransform;
    }

    public int getImageDepthBeforeTransform() {
        return imageDepthBeforeTransform;
    }

    public int getImageWidthAfterTransform() {
        return imageWidthAfterTransform;
    }

    public int getImageHeightAfterTransform() {
        return imageHeightAfterTransform;
    }

    public int getImageDepthAfterTransform() {
        return imageDepthAfterTransform;
    }
}
