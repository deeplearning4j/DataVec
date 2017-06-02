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

package org.datavec.spark.transform.sequence;

import lombok.AllArgsConstructor;
import org.apache.spark.api.java.function.Function;
import org.datavec.api.writable.Writable;
import org.datavec.api.transform.sequence.SequenceComparator;
import scala.Tuple2;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Spark function for grouping independent values/examples into a sequence, and then sorting them
 * using a provided {@link SequenceComparator}
 *
 * @author Alex Black
 */
@AllArgsConstructor
public class SparkGroupToSequenceFunction<T>
                implements Function<Tuple2<T, Iterable<List<Writable>>>, List<List<Writable>>> {

    private final SequenceComparator comparator;

    @Override
    public List<List<Writable>> call(Tuple2<T, Iterable<List<Writable>>> tuple) throws Exception {

        List<List<Writable>> list = new ArrayList<>();
        for (List<Writable> writables : tuple._2())
            list.add(writables);

        Collections.sort(list, comparator);

        return list;
    }
}
