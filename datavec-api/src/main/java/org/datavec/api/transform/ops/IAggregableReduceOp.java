package org.datavec.api.transform.ops;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Created by huitseeker on 4/28/17.
 */
public interface IAggregableReduceOp<T, V> extends Consumer<T>, Supplier<V> {

    public <W extends IAggregableReduceOp<T, V>> void combine(W accu);

}
