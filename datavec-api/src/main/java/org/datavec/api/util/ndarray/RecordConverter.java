/*-
 *  * Copyright 2017 Skymind, Inc.
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

package org.datavec.api.util.ndarray;

import org.datavec.api.writable.DoubleWritable;
import org.datavec.api.writable.IntWritable;
import org.datavec.api.writable.Writable;
import org.datavec.api.writable.NDArrayWritable;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.api.DataSet;
import org.nd4j.linalg.factory.Nd4j;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

/**
 * @author Adam Gibson
 */
public class RecordConverter {
    private RecordConverter() {}

    /**
     * Convert a record to an ndarray
     * @param record the record to convert
     *
     * @return the array
     */
    public static INDArray toArray(Collection<Writable> record, int size) {
        Iterator<Writable> writables = record.iterator();
        Writable firstWritable = writables.next();
        if (firstWritable instanceof NDArrayWritable) {
            NDArrayWritable ret = (NDArrayWritable) firstWritable;
            return ret.get();
        } else {
            INDArray vector = Nd4j.create(size);
            vector.putScalar(0, firstWritable.toDouble());
            int count = 1;
            while (writables.hasNext()) {
                Writable w = writables.next();
                vector.putScalar(count++, w.toDouble());
            }

            return vector;
        }
    }

    /**
     * Convert a set of records in to a matrix
     * @param matrix the records ot convert
     * @return the matrix for the records
     */
    public static List<List<Writable>> toRecords(INDArray matrix) {
        List<List<Writable>> ret = new ArrayList<>();
        for (int i = 0; i < matrix.rows(); i++) {
            ret.add(RecordConverter.toRecord(matrix.getRow(i)));
        }

        return ret;
    }

    /**
     * Convert a set of records in to a matrix
     * @param records the records ot convert
     * @return the matrix for the records
     */
    public static INDArray toMatrix(List<List<Writable>> records) {
        INDArray arr = Nd4j.create(records.size(), records.get(0).size());
        for (int i = 0; i < arr.rows(); i++) {
            arr.putRow(i, toArray(records.get(i)));
        }

        return arr;
    }

    /**
     * Convert a record to an ndarray
     * @param record the record to convert
     * @return the array
     */
    public static INDArray toArray(Collection<Writable> record) {
        return toArray(record, record.size());
    }



    /**
     * Convert an ndarray to a record
     * @param array the array to convert
     * @return the record
     */
    public static List<Writable> toRecord(INDArray array) {
        List<Writable> writables = new ArrayList<>();
        writables.add(new NDArrayWritable(array));
        return writables;
    }


    /**
     * Convert a DataSet to a matrix
     * @param dataSet the DataSet to convert
     * @return the matrix for the records
     */
    public static List<List<Writable>> toRecords(DataSet dataSet) {
        if (isClassificationDataSet(dataSet)) {
            return getClassificationWritableMatrix(dataSet);
        } else {
            return getRegressionWritableMatrix(dataSet);
        }
    }

    private static boolean isClassificationDataSet(DataSet dataSet) {
        INDArray labels = dataSet.getLabels();

        return labels.sum(0, 1).getInt(0) == dataSet.numExamples() && labels.shape()[1] > 1;
    }

    private static List<List<Writable>> getClassificationWritableMatrix(DataSet dataSet) {
        List<List<Writable>> writableMatrix = new ArrayList<>();

        for (int i = 0; i < dataSet.numExamples(); i++) {
            List<Writable> writables = toRecord(dataSet.getFeatures().getRow(i));
            writables.add(new IntWritable(Nd4j.argMax(dataSet.getLabels().getRow(i), 1).getInt(0)));

            writableMatrix.add(writables);
        }

        return writableMatrix;
    }

    private static List<List<Writable>> getRegressionWritableMatrix(DataSet dataSet) {
        List<List<Writable>> writableMatrix = new ArrayList<>();

        for (int i = 0; i < dataSet.numExamples(); i++) {
            List<Writable> writables = toRecord(dataSet.getFeatures().getRow(i));
            INDArray labelRow = dataSet.getLabels().getRow(i);

            for (int j = 0; j < labelRow.shape()[1]; j++) {
                writables.add(new DoubleWritable(labelRow.getDouble(j)));
            }

            writableMatrix.add(writables);
        }

        return writableMatrix;
    }
}
