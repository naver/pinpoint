/*
 * Copyright 2020 NAVER Corp.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.navercorp.pinpoint.common.hbase;

import com.navercorp.pinpoint.common.util.CollectionUtils;

import com.sematext.hbase.wd.AbstractRowKeyDistributor;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.Delete;
import org.apache.hadoop.hbase.client.Get;
import org.apache.hadoop.hbase.client.Increment;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.Scan;
import org.apache.hadoop.hbase.filter.CompareFilter;

import java.util.Collections;
import java.util.List;

/**
 * @author Taejin Koo
 */
public class AsyncPutHbaseTemplate2 implements HbaseOperations2 {
    private final HbaseOperations2 delegate;

    public AsyncPutHbaseTemplate2(HbaseOperations2 delegate) {
        this.delegate = delegate;
    }

    @Override
    public boolean asyncPut(TableName tableName, byte[] rowName, byte[] familyName, byte[] qualifier, byte[] value) {
        final boolean success = delegate.asyncPut(tableName, rowName, familyName, qualifier, value);
        if (success) {
            return true;
        }

        delegate.put(tableName, rowName, familyName, qualifier, value);
        return true;
    }

    @Override
    public boolean asyncPut(TableName tableName, byte[] rowName, byte[] familyName, byte[] qualifier, Long timestamp, byte[] value) {
        final boolean success = delegate.asyncPut(tableName, rowName, familyName, qualifier, timestamp, value);
        if (success) {
            return true;
        }

        delegate.put(tableName, rowName, familyName, qualifier, timestamp, value);
        return true;
    }

    @Override
    public <T> boolean asyncPut(TableName tableName, byte[] rowName, byte[] familyName, byte[] qualifier, T value, ValueMapper<T> mapper) {
        final boolean success = delegate.asyncPut(tableName, rowName, familyName, qualifier, value, mapper);
        if (success) {
            return true;
        }

        delegate.put(tableName, rowName, familyName, qualifier, value, mapper);
        return true;
    }

    @Override
    public <T> boolean asyncPut(TableName tableName, byte[] rowName, byte[] familyName, byte[] qualifier, Long timestamp, T value, ValueMapper<T> mapper) {
        final boolean success = delegate.asyncPut(tableName, rowName, familyName, qualifier, timestamp, value, mapper);
        if (success) {
            return true;
        }

        delegate.put(tableName, rowName, familyName, qualifier, timestamp, value, mapper);
        return true;
    }

    @Override
    public boolean asyncPut(TableName tableName, Put put) {
        final boolean success = delegate.asyncPut(tableName, put);
        if (success) {
            return true;
        }

        delegate.put(tableName, put);
        return true;
    }

    @Override
    public List<Put> asyncPut(TableName tableName, List<Put> puts) {
        List<Put> rejectedPuts = this.delegate.asyncPut(tableName, puts);
        if (CollectionUtils.hasLength(rejectedPuts)) {
            this.delegate.put(tableName, rejectedPuts);
        }
        return Collections.emptyList();
    }


    @Override
    public <T> T get(TableName tableName, byte[] rowName, RowMapper<T> mapper) {
        return delegate.get(tableName, rowName, mapper);
    }

    @Override
    public <T> T get(TableName tableName, byte[] rowName, byte[] familyName, RowMapper<T> mapper) {
        return delegate.get(tableName, rowName, familyName, mapper);
    }

    @Override
    public <T> T get(TableName tableName, byte[] rowName, byte[] familyName, byte[] qualifier, RowMapper<T> mapper) {
        return delegate.get(tableName, rowName, familyName, qualifier, mapper);
    }

    @Override
    public <T> T get(TableName tableName, Get get, RowMapper<T> mapper) {
        return delegate.get(tableName, get, mapper);
    }

    @Override
    public <T> List<T> get(TableName tableName, List<Get> get, RowMapper<T> mapper) {
        return delegate.get(tableName, get, mapper);
    }

    @Override
    public void put(TableName tableName, byte[] rowName, byte[] familyName, byte[] qualifier, byte[] value) {
        delegate.put(tableName, rowName, familyName, qualifier, value);
    }

    @Override
    public void put(TableName tableName, byte[] rowName, byte[] familyName, byte[] qualifier, Long timestamp, byte[] value) {
        delegate.put(tableName, rowName, familyName, qualifier, timestamp, value);
    }

    @Override
    public <T> void put(TableName tableName, byte[] rowName, byte[] familyName, byte[] qualifier, T value, ValueMapper<T> mapper) {
        delegate.put(tableName, rowName, familyName, qualifier, value, mapper);
    }

    @Override
    public <T> void put(TableName tableName, byte[] rowName, byte[] familyName, byte[] qualifier, Long timestamp, T value, ValueMapper<T> mapper) {
        delegate.put(tableName, rowName, familyName, qualifier, timestamp, value, mapper);
    }

    @Override
    public void put(TableName tableName, Put put) {
        delegate.put(tableName, put);
    }

    @Override
    public void put(TableName tableName, List<Put> puts) {
        delegate.put(tableName, puts);
    }

    /**
     * Atomically checks if a row/family/qualifier value matches the expected
     * value. If it does, it adds the put.  If the passed value is null, the check
     * is for the lack of column (ie: non-existance)
     *
     * @param tableName  target table
     * @param rowName    to check
     * @param familyName column family to check
     * @param qualifier  column qualifier to check
     * @param compareOp  comparison operator to use
     * @param value      the expected value
     * @param put        data to put if check succeeds
     * @return true if the new put was executed, false otherwise
     */
    @Override
    public boolean checkAndPut(TableName tableName, byte[] rowName, byte[] familyName, byte[] qualifier, CompareFilter.CompareOp compareOp, byte[] value, Put put) {
        return delegate.checkAndPut(tableName, rowName, familyName, qualifier, compareOp, value, put);
    }

    @Override
    public void maxColumnValue(TableName tableName, byte[] rowName, byte[] familyName, byte[] qualifier, long value) {
        delegate.maxColumnValue(tableName, rowName, familyName, qualifier, value);
    }

    @Override
    public void delete(TableName tableName, Delete delete) {
        delegate.delete(tableName, delete);
    }

    @Override
    public void delete(TableName tableName, List<Delete> deletes) {
        delegate.delete(tableName, deletes);
    }

    @Override
    public <T> List<T> find(TableName tableName, List<Scan> scans, ResultsExtractor<T> action) {
        return delegate.find(tableName, scans, action);
    }

    @Override
    public <T> List<List<T>> find(TableName tableName, List<Scan> scans, RowMapper<T> action) {
        return delegate.find(tableName, scans, action);
    }

    @Override
    public <T> List<T> findParallel(TableName tableName, List<Scan> scans, ResultsExtractor<T> action) {
        return delegate.findParallel(tableName, scans, action);
    }

    @Override
    public <T> List<List<T>> findParallel(TableName tableName, List<Scan> scans, RowMapper<T> action) {
        return delegate.findParallel(tableName, scans, action);
    }

    @Override
    public <T> List<T> find(TableName tableName, Scan scan, AbstractRowKeyDistributor rowKeyDistributor, RowMapper<T> action) {
        return delegate.find(tableName, scan, rowKeyDistributor, action);
    }

    @Override
    public <T> List<T> find(TableName tableName, Scan scan, AbstractRowKeyDistributor rowKeyDistributor, int limit, RowMapper<T> action) {
        return delegate.find(tableName, scan, rowKeyDistributor, limit, action);
    }

    @Override
    public <T> List<T> find(TableName tableName, Scan scan, AbstractRowKeyDistributor rowKeyDistributor, int limit, RowMapper<T> action, LimitEventHandler limitEventHandler) {
        return delegate.find(tableName, scan, rowKeyDistributor, limit, action, limitEventHandler);
    }

    @Override
    public <T> T find(TableName tableName, Scan scan, AbstractRowKeyDistributor rowKeyDistributor, ResultsExtractor<T> action) {
        return delegate.find(tableName, scan, rowKeyDistributor, action);
    }

    @Override
    public <T> List<T> findParallel(TableName tableName, Scan scan, AbstractRowKeyDistributor rowKeyDistributor, RowMapper<T> action, int numParallelThreads) {
        return delegate.findParallel(tableName, scan, rowKeyDistributor, action, numParallelThreads);
    }

    @Override
    public <T> List<T> findParallel(TableName tableName, Scan scan, AbstractRowKeyDistributor rowKeyDistributor, int limit, RowMapper<T> action, int numParallelThreads) {
        return delegate.findParallel(tableName, scan, rowKeyDistributor, limit, action, numParallelThreads);
    }

    @Override
    public <T> List<T> findParallel(TableName tableName, Scan scan, AbstractRowKeyDistributor rowKeyDistributor, int limit, RowMapper<T> action, LimitEventHandler limitEventHandler, int numParallelThreads) {
        return delegate.findParallel(tableName, scan, rowKeyDistributor, limit, action, limitEventHandler, numParallelThreads);
    }

    @Override
    public <T> T findParallel(TableName tableName, Scan scan, AbstractRowKeyDistributor rowKeyDistributor, ResultsExtractor<T> action, int numParallelThreads) {
        return delegate.findParallel(tableName, scan, rowKeyDistributor, action, numParallelThreads);
    }

    @Override
    public Result increment(TableName tableName, Increment increment) {
        return delegate.increment(tableName, increment);
    }

    @Override
    public List<Result> increment(TableName tableName, List<Increment> incrementList) {
        return delegate.increment(tableName, incrementList);
    }

    @Override
    public long incrementColumnValue(TableName tableName, byte[] rowName, byte[] familyName, byte[] qualifier, long amount) {
        return delegate.incrementColumnValue(tableName, rowName, familyName, qualifier, amount);
    }

    @Override
    public long incrementColumnValue(TableName tableName, byte[] rowName, byte[] familyName, byte[] qualifier, long amount, boolean writeToWAL) {
        return delegate.incrementColumnValue(tableName, rowName, familyName, qualifier, amount, writeToWAL);
    }

    @Override
    public <T> T execute(TableName tableName, TableCallback<T> action) {
        return delegate.execute(tableName, action);
    }

    @Override
    public <T> T find(TableName tableName, Scan scan, ResultsExtractor<T> action) {
        return delegate.find(tableName, scan, action);
    }

    @Override
    public <T> List<T> find(TableName tableName, Scan scan, RowMapper<T> action) {
        return delegate.find(tableName, scan, action);
    }

}
