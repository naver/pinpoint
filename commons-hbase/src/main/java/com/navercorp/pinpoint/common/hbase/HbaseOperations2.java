/*
 * Copyright 2014 NAVER Corp.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.navercorp.pinpoint.common.hbase;

import com.sematext.hbase.wd.AbstractRowKeyDistributor;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.Delete;
import org.apache.hadoop.hbase.client.Get;
import org.apache.hadoop.hbase.client.Increment;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.Scan;
import org.apache.hadoop.hbase.filter.CompareFilter;

import java.util.List;

/**
 * @author emeroad
 * @author minwoo.jung
 * @author Taejin Koo
 */
public interface HbaseOperations2 {
    /**
     * Gets an individual row from the given table. The content is mapped by the given action.
     *
     * @param tableName target table
     * @param rowName   row name
     * @param mapper    row mapper
     * @return object mapping the target row
     */
    <T> T get(TableName tableName, byte[] rowName, final RowMapper<T> mapper);


    /**
     * Gets an individual row from the given table. The content is mapped by the given action.
     *
     * @param tableName  target table
     * @param rowName    row name
     * @param familyName column family
     * @param mapper     row mapper
     * @return object mapping the target row
     */
    <T> T get(TableName tableName, byte[] rowName, byte[] familyName, final RowMapper<T> mapper);

    /**
     * Gets an individual row from the given table. The content is mapped by the given action.
     *
     * @param tableName  target table
     * @param rowName    row name
     * @param familyName family
     * @param qualifier  column qualifier
     * @param mapper     row mapper
     * @return object mapping the target row
     */
    <T> T get(TableName tableName, final byte[] rowName, final byte[] familyName, final byte[] qualifier, final RowMapper<T> mapper);

    <T> T get(TableName tableName, final Get get, final RowMapper<T> mapper);

    <T> List<T> get(TableName tableName, final List<Get> get, final RowMapper<T> mapper);

    void put(TableName tableName, final byte[] rowName, final byte[] familyName, final byte[] qualifier, final byte[] value);
    void put(TableName tableName, final byte[] rowName, final byte[] familyName, final byte[] qualifier, final Long timestamp, final byte[] value);
    <T> void put(TableName tableName, final byte[] rowName, final byte[] familyName, final byte[] qualifier, final T value, final ValueMapper<T> mapper);
    <T> void put(TableName tableName, final byte[] rowName, final byte[] familyName, final byte[] qualifier, final Long timestamp, final T value, final ValueMapper<T> mapper);
    void put(TableName tableName, final Put put);
    void put(TableName tableName, final List<Put> puts);

    /**
     * Atomically checks if a row/family/qualifier value matches the expected
     * value. If it does, it adds the put.  If the passed value is null, the check
     * is for the lack of column (ie: non-existance)
     *
     * @param tableName  target table
     * @param rowName to check
     * @param familyName column family to check
     * @param qualifier column qualifier to check
     * @param compareOp comparison operator to use
     * @param value the expected value
     * @param put data to put if check succeeds
     * @return true if the new put was executed, false otherwise
     */
    boolean checkAndPut(TableName tableName, byte[] rowName, byte[] familyName, byte[] qualifier, CompareFilter.CompareOp compareOp, byte[] value, Put put);

    /**
     *
     * @param tableName  target table
     * @param rowName to check
     * @param familyName column family to check
     * @param qualifier column qualifier to check
     * @param value if the value provided is greater than the saved, update the saved
     */
    void maxColumnValue(TableName tableName, final byte[] rowName, final byte[] familyName, final byte[] qualifier, final long value);

    /**
     * If asyncOperation is not set, then execute put method instead of asyncPut method.
     */
    boolean asyncPut(TableName tableName, final byte[] rowName, final byte[] familyName, final byte[] qualifier, final byte[] value);
    /**
     * If asyncOperation is not set, then execute put method instead of asyncPut method.
     */
    boolean asyncPut(TableName tableName, final byte[] rowName, final byte[] familyName, final byte[] qualifier, final Long timestamp, final byte[] value);
    /**
     * If asyncOperation is not set, then execute put method instead of asyncPut method.
     */
    <T> boolean asyncPut(TableName tableName, final byte[] rowName, final byte[] familyName, final byte[] qualifier, final T value, final ValueMapper<T> mapper);
    /**
     * If asyncOperation is not set, then execute put method instead of asyncPut method.
     */
    <T> boolean asyncPut(TableName tableName, final byte[] rowName, final byte[] familyName, final byte[] qualifier, final Long timestamp, final T value, final ValueMapper<T> mapper);
    /**
     * If asyncOperation is not set, then execute put method instead of asyncPut method.
     */
    boolean asyncPut(TableName tableName, final Put put);

    /**
     * If asyncOperation is not set, then execute put method instead of asyncPut method.
     * @param tableName
     * @param puts
     * @return the list of puts which could not be queued or put serially. May be null if all put operations were successfully queued.
     */
    List<Put> asyncPut(TableName tableName, final List<Put> puts);

    void delete(TableName tableName, final Delete delete);
    void delete(TableName tableName, final List<Delete> deletes);

    <T> List<T> find(TableName tableName, final List<Scan> scans, final ResultsExtractor<T> action);
    <T> List<List<T>> find(TableName tableName, final List<Scan> scans, final RowMapper<T> action);

    <T> List<T> findParallel(TableName tableName, final List<Scan> scans, final ResultsExtractor<T> action);
    <T> List<List<T>> findParallel(TableName tableName, final List<Scan> scans, final RowMapper<T> action);

    // Distributed scanners
    <T> List<T> find(TableName tableName, final Scan scan, final AbstractRowKeyDistributor rowKeyDistributor, final RowMapper<T> action);
    <T> List<T> find(TableName tableName, final Scan scan, final AbstractRowKeyDistributor rowKeyDistributor, int limit, final RowMapper<T> action);
    <T> List<T> find(TableName tableName, final Scan scan, final AbstractRowKeyDistributor rowKeyDistributor, int limit, final RowMapper<T> action, final LimitEventHandler limitEventHandler);
    <T> T find(TableName tableName, final Scan scan, final AbstractRowKeyDistributor rowKeyDistributor, final ResultsExtractor<T> action);

    // Parallel scanners for distributed scans
    <T> List<T> findParallel(TableName tableName, final Scan scan, final AbstractRowKeyDistributor rowKeyDistributor, final RowMapper<T> action, int numParallelThreads);
    <T> List<T> findParallel(TableName tableName, final Scan scan, final AbstractRowKeyDistributor rowKeyDistributor, int limit, final RowMapper<T> action, int numParallelThreads);
    <T> List<T> findParallel(TableName tableName, final Scan scan, final AbstractRowKeyDistributor rowKeyDistributor, int limit, final RowMapper<T> action, final LimitEventHandler limitEventHandler, int numParallelThreads);
    <T> T findParallel(TableName tableName, final Scan scan, final AbstractRowKeyDistributor rowKeyDistributor, final ResultsExtractor<T> action, int numParallelThreads);

    Result increment(TableName tableName, final Increment increment);

    /**
     * Exception throwing can partially happen in case of incrementList. you will be accepted the last Exception.
     * If you want to retry something with catching a specific exception,
     * There is a limitation that you can't know which exception throws due to throwing always last exception.
     *
     * @param tableName
     * @param incrementList
     * @return
     */
    List<Result> increment(TableName tableName, final List<Increment> incrementList);

    long incrementColumnValue(TableName tableName, final byte[] rowName, final byte[] familyName, final byte[] qualifier, final long amount);
    long incrementColumnValue(TableName tableName, final byte[] rowName, final byte[] familyName, final byte[] qualifier, final long amount, final boolean writeToWAL);

    /**
     * Executes the given action against the specified table handling resource management.
     * <p>
     * Application exceptions thrown by the action object get propagated to the caller (can only be unchecked). 
     * Allows for returning a result object (typically a domain object or collection of domain objects).
     * 
     * @param tableName the target table
     * @param action callback object that specifies the action
     * @param <T> action type
     * @return the result object of the callback action, or null
     */
    <T> T execute(TableName tableName, TableCallback<T> action);

    /**
     * Scans the target table using the given {@link Scan} object. Suitable for maximum control over the scanning
     * process.
     * The content is processed by the given action typically returning a domain object or collection of domain objects.
     * 
     * @param tableName target table
     * @param scan table scanner
     * @param action action handling the scanner results
     * @param <T> action type
     * @return the result object of the callback action, or null
     */
    <T> T find(TableName tableName, final Scan scan, final ResultsExtractor<T> action);

    /**
     * Scans the target table using the given {@link Scan} object. Suitable for maximum control over the scanning
     * process.
     * The content is processed row by row by the given action, returning a list of domain objects.
     * 
     * @param tableName target table
     * @param scan table scanner
     * @param action row mapper handling the scanner results
     * @param <T> action type
     * @return a list of objects mapping the scanned rows
     */
    <T> List<T> find(TableName tableName, final Scan scan, final RowMapper<T> action);

}
