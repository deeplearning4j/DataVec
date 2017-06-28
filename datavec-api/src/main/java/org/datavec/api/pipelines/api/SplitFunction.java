package org.datavec.api.pipelines.api;

import java.util.Collection;
import java.util.Iterator;

/**
 * @author raver119@gmail.com
 */
public interface SplitFunction<T> extends Function<T>  {

    Iterator<T> call(Iterator<T> input);
}
