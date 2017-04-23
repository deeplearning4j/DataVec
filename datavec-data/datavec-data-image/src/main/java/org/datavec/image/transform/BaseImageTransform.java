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

import java.util.Map;
import java.util.Random;

import org.bytedeco.javacv.FrameConverter;
import org.datavec.image.data.ImageWritable;

/**
 *
 * Implements the ImageTransform interface by providing its subclasses
 * with a random object to use in the case of random transformations.
 *
 * @author saudet
 */
public abstract class BaseImageTransform<F> implements ImageTransform {

    protected Random random;
    Map<Long, FrameConverter<F>> safeConverter;

    protected BaseImageTransform(Random random) {
        this.random = random;
    }

    @Override
    public ImageWritable transform(ImageWritable image) {
        return transform(image, random);
    }

    abstract FrameConverter<F> getSafeConverter(long threadId);

}
