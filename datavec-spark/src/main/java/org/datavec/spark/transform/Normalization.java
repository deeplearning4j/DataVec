package org.datavec.spark.transform;

import org.apache.commons.collections.map.ListOrderedMap;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.sql.Column;
import org.apache.spark.sql.DataFrame;
import org.apache.spark.sql.Row;

import static org.apache.spark.sql.functions.*;

import org.datavec.api.transform.schema.Schema;
import org.datavec.api.writable.Writable;
import org.nd4j.linalg.util.ArrayUtil;


import java.util.*;


/**
 * Simple dataframe based normalization.
 * Column based transforms such as min/max scaling
 * based on column min max and zero mean unit variance
 * using column wise statistics.
 *
 * @author Adam Gibson
 */
public class Normalization {


    /**
     * Normalize by zero mean unit variance
     *
     * @param frame the data to normalize
     * @return a zero mean unit variance centered
     * rdd
     */
    public static DataFrame zeromeanUnitVariance(DataFrame frame) {
        return zeromeanUnitVariance(frame, Collections.emptyList());
    }

    /**
     * Normalize by zero mean unit variance
     *
     * @param schema the schema to use
     *               to create the data frame
     * @param data   the data to normalize
     * @return a zero mean unit variance centered
     * rdd
     */
    public static JavaRDD<List<Writable>> zeromeanUnitVariance(Schema schema, JavaRDD<List<Writable>> data) {
        return zeromeanUnitVariance(schema, data, Collections.emptyList());
    }

    /**
     * Scale based on min,max
     *
     * @param dataFrame the dataframe to scale
     * @param min       the minimum value
     * @param max       the maximum value
     * @return the normalized dataframe per column
     */
    public static DataFrame normalize(DataFrame dataFrame, double min, double max) {
        return normalize(dataFrame, min, max, Collections.emptyList());
    }

    /**
     * Scale based on min,max
     *
     * @param schema the schema of the data to scale
     * @param data   the data to sclae
     * @param min    the minimum value
     * @param max    the maximum value
     * @return the normalized ata
     */
    public static JavaRDD<List<Writable>> normalize(Schema schema, JavaRDD<List<Writable>> data, double min, double max) {
        DataFrame frame = DataFrames.toDataFrame(schema, data);
        return DataFrames.toRecords(normalize(frame, min, max, Collections.<String>emptyList())).getSecond();
    }


    /**
     * Scale based on min,max
     *
     * @param dataFrame the dataframe to scale
     * @return the normalized dataframe per column
     */
    public static DataFrame normalize(DataFrame dataFrame) {
        return normalize(dataFrame, 0, 1, Collections.<String>emptyList());
    }

    /**
     * Scale all data  0 to 1
     *
     * @param schema the schema of the data to scale
     * @param data   the data to scale
     * @return the normalized ata
     */
    public static JavaRDD<List<Writable>> normalize(Schema schema, JavaRDD<List<Writable>> data) {
        return normalize(schema, data, 0, 1, Collections.<String>emptyList());
    }


    /**
     * Normalize by zero mean unit variance
     *
     * @param frame the data to normalize
     * @return a zero mean unit variance centered
     * rdd
     */
    public static DataFrame zeromeanUnitVariance(DataFrame frame, List<String> skipColumns) {
        String[] columnNames = frame.columns();
        for (String columnName : columnNames) {
            if (skipColumns.contains(columnName)) continue;

            DataFrame meanStd = frame.select(columnName).agg(mean(columnName), stddev(columnName));
            Row r = meanStd.collect()[0];
            double mean = ((Number) r.get(0)).doubleValue();
            double std = ((Number) r.get(1)).doubleValue();
            if (std == 0.0) std = 1; //All same value -> (x-x)/1 = 0

            frame = frame.withColumn(columnName, frame.col(columnName).minus(mean).divide(std));
        }

        return frame;
    }

    /**
     * Normalize by zero mean unit variance
     *
     * @param schema the schema to use
     *               to create the data frame
     * @param data   the data to normalize
     * @return a zero mean unit variance centered
     * rdd
     */
    public static JavaRDD<List<Writable>> zeromeanUnitVariance(Schema schema, JavaRDD<List<Writable>> data, List<String> skipColumns) {
        DataFrame frame = DataFrames.toDataFrame(schema, data);
        return DataFrames.toRecords(zeromeanUnitVariance(frame, skipColumns)).getSecond();
    }

    /**
     * Normalize the sequence by zero mean unit variance
     *
     * @param schema   Schema of the data to normalize
     * @param sequence Sequence data
     * @return Normalized sequence
     */
    public static JavaRDD<List<List<Writable>>> zeroMeanUnitVarianceSequence(Schema schema, JavaRDD<List<List<Writable>>> sequence) {
        return zeroMeanUnitVarianceSequence(schema, sequence, null);
    }

    /**
     * Normalize the sequence by zero mean unit variance
     *
     * @param schema         Schema of the data to normalize
     * @param sequence       Sequence data
     * @param excludeColumns List of  columns to exclude from the normalization
     * @return Normalized sequence
     */
    public static JavaRDD<List<List<Writable>>> zeroMeanUnitVarianceSequence(Schema schema, JavaRDD<List<List<Writable>>> sequence,
                                                                             List<String> excludeColumns) {
        DataFrame frame = DataFrames.toDataFrameSequence(schema, sequence);
        if (excludeColumns == null)
            excludeColumns = Arrays.asList(DataFrames.SEQUENCE_UUID_COLUMN, DataFrames.SEQUENCE_INDEX_COLUMN);
        else {
            excludeColumns = new ArrayList<>(excludeColumns);
            excludeColumns.add(DataFrames.SEQUENCE_UUID_COLUMN);
            excludeColumns.add(DataFrames.SEQUENCE_INDEX_COLUMN);
        }
        frame = zeromeanUnitVariance(frame, excludeColumns);
        return DataFrames.toRecordsSequence(frame).getSecond();
    }

    /**
     * Returns the min and max of the given columns
     * @param data the data to get the max for
     * @param columns the columns to get the
     * @return
     */
    public static List<Row> minMaxColumns(DataFrame data,List<String> columns) {
        String[] arr = new String[columns.size()];
        for(int i = 0; i < arr.length; i++)
            arr[i] = columns.get(i);
        return minMaxColumns(data,arr);
    }

    /**
     * Returns the min and max of the given columns.
     * The list returned is a list of size 2 where each row
     * @param data the data to get the max for
     * @param columns the columns to get the
     * @return
     */
    public static List<Row> minMaxColumns(DataFrame data,String...columns) {
        return aggregate(data,columns,new String[]{"min","max"});
    }


    /**
     * Returns the standard deviation and mean of the given columns
     * @param data the data to get the max for
     * @param columns the columns to get the
     * @return
     */
    public static List<Row> stdDevMeanColumns(DataFrame data,List<String> columns) {
        String[] arr = new String[columns.size()];
        for(int i = 0; i < arr.length; i++)
            arr[i] = columns.get(i);
        return stdDevMeanColumns(data,arr);
    }

    /**
     * Returns the standard deviation and mean of the given columns.
     * The list returned is a list of size 2 where each row
     * @param data the data to get the max for
     * @param columns the columns to get the
     * @return
     */
    public static List<Row> stdDevMeanColumns(DataFrame data,String...columns) {
        return aggregate(data,columns,new String[]{"mean","stddev"});
    }

    /**
     * Aggregate based on an arbitrary list
     * of aggregation and grouping functions
     * @param data the dataframe to aggregate
     * @param columns the columns to aggregate
     * @param functions the functions to use
     * @return the list of rows with the aggregated statistics.
     * Each row will be a function with the desired columnar output
     * in the order in which the columns were specified.
     */
    public static List<Row> aggregate(DataFrame data,String[] columns,String[] functions) {
        String[] rest = new String[columns.length - 1];
        for(int i = 0; i < rest.length; i++)
            rest[i] = columns[i + 1];
        List<Row> rows = new ArrayList<>();
        for(String op : functions) {
            Map<String,String> expressions = new ListOrderedMap();
            for(String s : columns) {
                expressions.put(s,op);
            }

            //compute the aggregation based on the operation
            DataFrame aggregated = data.agg(expressions);
            String[] columns2 = aggregated.columns();
            //strip out the op name and parentheses from the columns
            Map<String,String> opReplace = new TreeMap<>();
            for(String s : columns2) {
                if(s.contains("min(") || s.contains("max("))
                    opReplace.put(s,s.replace(op,"").replaceAll("[()]",""));
                else if(s.contains("avg")) {
                    opReplace.put(s,s.replace("avg","").replaceAll("[()]",""));
                }
                else {
                    opReplace.put(s,s.replace(op,"").replaceAll("[()]",""));
                }
            }


            //get rid of the operation name in the column
            DataFrame rearranged = null;
            for(Map.Entry<String,String> entries : opReplace.entrySet()) {
                //first column
                if(rearranged == null) {
                    rearranged = aggregated.withColumnRenamed(entries.getKey(),entries.getValue());
                }
                //rearranged is just a copy of aggregated at this point
                else
                    rearranged = rearranged.withColumnRenamed(entries.getKey(),entries.getValue());
            }

            rearranged = rearranged.select(DataFrames.toColumns(columns));
            //op
            rows.addAll(rearranged.collectAsList());
        }


        return rows;
    }


    /**
     * Scale based on min,max
     *
     * @param dataFrame the dataframe to scale
     * @param min       the minimum value
     * @param max       the maximum value
     * @return the normalized dataframe per column
     */
    public static DataFrame normalize(DataFrame dataFrame, double min, double max, List<String> skipColumns) {
        String[] columnNames = dataFrame.columns();
        for (String columnName : columnNames) {
            if (skipColumns.contains(columnName))
                continue;
            DataFrame minMax = dataFrame.select(columnName).agg(min(columnName), max(columnName));
            Row r = minMax.collect()[0];
            double dMin = ((Number) r.get(0)).doubleValue();
            double dMax = ((Number) r.get(1)).doubleValue();

            double maxSubMin = dMax - dMin;
            if (maxSubMin == 0) maxSubMin = 1;

            Column newCol = dataFrame.col(columnName).minus(dMin).divide(maxSubMin).multiply(max - min).plus(min);
            dataFrame = dataFrame.withColumn(columnName, newCol);
        }

        return dataFrame;
    }

    /**
     * Scale based on min,max
     *
     * @param schema the schema of the data to scale
     * @param data   the data to scale
     * @param min    the minimum value
     * @param max    the maximum value
     * @return the normalized ata
     */
    public static JavaRDD<List<Writable>> normalize(Schema schema, JavaRDD<List<Writable>> data, double min, double max, List<String> skipColumns) {
        DataFrame frame = DataFrames.toDataFrame(schema, data);
        return DataFrames.toRecords(normalize(frame, min, max, skipColumns)).getSecond();
    }

    /**
     *
     * @param schema
     * @param data
     * @return
     */
    public static JavaRDD<List<List<Writable>>> normalizeSequence(Schema schema, JavaRDD<List<List<Writable>>> data) {
        return normalizeSequence(schema, data, 0, 1);
    }

    /**
     * Normalize each column of a sequence, based on min/max
     *
     * @param schema Schema of the data
     * @param data   Data to normalize
     * @param min    New minimum value
     * @param max    New maximum value
     * @return Normalized data
     */
    public static JavaRDD<List<List<Writable>>> normalizeSequence(Schema schema, JavaRDD<List<List<Writable>>> data, double min, double max) {
        return normalizeSequence(schema, data, min, max, null);
    }

    /**
     * Normalize each column of a sequence, based on min/max
     *
     * @param schema         Schema of the data
     * @param data           Data to normalize
     * @param min            New minimum value
     * @param max            New maximum value
     * @param excludeColumns List of columns to exclude
     * @return Normalized data
     */
    public static JavaRDD<List<List<Writable>>> normalizeSequence(Schema schema, JavaRDD<List<List<Writable>>> data,
                                                                  double min, double max, List<String> excludeColumns) {
        if (excludeColumns == null)
            excludeColumns = Arrays.asList(DataFrames.SEQUENCE_UUID_COLUMN, DataFrames.SEQUENCE_INDEX_COLUMN);
        else {
            excludeColumns = new ArrayList<>(excludeColumns);
            excludeColumns.add(DataFrames.SEQUENCE_UUID_COLUMN);
            excludeColumns.add(DataFrames.SEQUENCE_INDEX_COLUMN);
        }
        DataFrame frame = DataFrames.toDataFrameSequence(schema, data);
        return DataFrames.toRecordsSequence(normalize(frame, min, max, excludeColumns)).getSecond();
    }


    /**
     * Scale based on min,max
     *
     * @param dataFrame the dataframe to scale
     * @return the normalized dataframe per column
     */
    public static DataFrame normalize(DataFrame dataFrame, List<String> skipColumns) {
        return normalize(dataFrame, 0, 1, skipColumns);
    }

    /**
     * Scale all data  0 to 1
     *
     * @param schema the schema of the data to scale
     * @param data   the data to scale
     * @return the normalized ata
     */
    public static JavaRDD<List<Writable>> normalize(Schema schema, JavaRDD<List<Writable>> data, List<String> skipColumns) {
        return normalize(schema, data, 0, 1, skipColumns);
    }
}