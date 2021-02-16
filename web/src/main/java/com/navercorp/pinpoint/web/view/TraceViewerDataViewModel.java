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
package com.navercorp.pinpoint.web.view;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.navercorp.pinpoint.common.profiler.util.TransactionId;
import com.navercorp.pinpoint.common.profiler.util.TransactionIdUtils;
import com.navercorp.pinpoint.web.config.LogConfiguration;
import com.navercorp.pinpoint.web.vo.Trace;
import com.navercorp.pinpoint.web.vo.callstacks.Record;
import com.navercorp.pinpoint.web.vo.callstacks.RecordSet;
import org.apache.commons.lang3.StringUtils;
import org.apache.hadoop.util.hash.Hash;

import java.util.*;


public class TraceViewerDataViewModel {
    private final int START_TIME_INDEX = 0;
    private final int END_TIME_INDEX = 1;

    private final RecordSet recordSet;
    private List<TraceEvent> traceEvents;
    private List<Long[]> occupiedRange;
    private Map<Integer, Integer> invisibleRecords;
    private int maxTid;
    private long minBlank;

    public TraceViewerDataViewModel(RecordSet recordSet) {
        this.recordSet = recordSet;
        this.maxTid = 0;
        this.traceEvents = new ArrayList<TraceEvent>();
        this.occupiedRange = new ArrayList<Long[]>();
        this.invisibleRecords = new HashMap<Integer, Integer>();
        this.minBlank = (recordSet.getEndTime() - recordSet.getStartTime()) / 100;
        initialize();
    }

    @JsonProperty("traceEvents")
    public List<TraceEvent> getTransactionId() {
        return traceEvents;
    }

    private int getNewTid(Record record, int tid) {
        long startTime = record.getBegin();
        long endTime = record.getBegin() + record.getElapsed();

        for (int i = tid; i <= maxTid; i++) {
            if ((endTime + minBlank < occupiedRange.get(i)[START_TIME_INDEX]) || (occupiedRange.get(i)[END_TIME_INDEX]) + minBlank < startTime) {
                return i;
            }
        }
        maxTid++;
        return maxTid;
    }

    public void initialize() {
        int tid = 0;
        int arrowId = 0;
        String previousAppName = "";

        Record prev = null;
        Record possibleException = null;

        List<Stack<Record>> recordTraces = new ArrayList<Stack<Record>>();

        for (Record record : recordSet.getRecordList()) {
            if (record.getElapsed() != 0) {
                boolean isRecordHighlighted = StringUtils.equals(recordSet.getApplicationId(), record.getApplicationName());
                boolean isApplicationNameChanged = !previousAppName.equals(record.getApplicationName());

                if (recordTraces.size() == 0) {
                    Stack<Record> recordTrace = new Stack<Record>();
                    recordTrace.push(record);
                    recordTraces.add(recordTrace);
                    occupiedRange.add(new Long[]{record.getBegin(), record.getBegin() + record.getElapsed()});
                } else if (record.getApiType().equals("ASYNC")) {
                    tid = newAsyncStack(tid, recordTraces, arrowId, record);
                    arrowId++;
                } else if (isApplicationNameChanged && !isContinuingParentStack(recordTraces, tid, record)) {
                    tid = newAsyncStack(tid, recordTraces, arrowId, record);
                    arrowId++;
                } else {
                    tid = updateRecordTrace(recordTraces, tid, record);
                }

                TraceEvent newTrace = TraceEvent.defaultTrace(tid, isRecordHighlighted, record, isApplicationNameChanged);
                traceEvents.add(newTrace);
                previousAppName = record.getApplicationName();
                prev = record;
            } else {
                if ((record.getTitle().equals("SQL") || (record.getTitle().equals("MONGO-JSON")))) {
                    addQueryInfo(prev, record);
                }

                if (record.getHasException()) {
                    if ((possibleException.getId() == record.getParentId())) {
                        addExceptionInfo(tid, prev, possibleException, record);
                    }
                }

                addToInvisibleRecords(record);
            }
            possibleException = record;
        }

    }

    private void addToInvisibleRecords(Record record) {
        Integer nonZeroAncestorId;
        do {
            nonZeroAncestorId = invisibleRecords.get(record.getParentId());
        } while (invisibleRecords.get(nonZeroAncestorId) != null);

        if (nonZeroAncestorId == null) {
            invisibleRecords.put(record.getId(), record.getParentId());
        } else {
            invisibleRecords.put(record.getId(), nonZeroAncestorId);
        }
    }

    private void addQueryInfo(Record prev, Record record) {
        if ((prev.getId() == record.getParentId()) && (prev.getElapsed() > 0)) {
            TraceEvent parent = traceEvents.get(traceEvents.size()-1);
            parent.changeToDatabaseTrace();
            parent.addArg("Query", record.getArguments());
        }
    }

    private void addExceptionInfo(int tid, Record prev, Record possibleException, Record record) {
        if (possibleException.getElapsed() > 0) {
            TraceEvent parent = traceEvents.get(traceEvents.size()-1);
            parent.changeToRed();
            parent.addArg("Exception", record.getTitle());
            parent.addArg("Exception Details", record.getArguments());
        } else {
            TraceEvent exception = TraceEvent.exceptionTrace(tid, prev);
            traceEvents.add(exception);
        }
    }

    private int newAsyncStack(int tid, List<Stack<Record>> recordTraces, int arrowId, Record record) {
        /* Makes the start point of an asynchronous trace arrow */
        int parentTid = getParentTid(recordTraces, tid, record);
        TraceEvent arrowStart = TraceEvent.arrowStartTrace(parentTid, recordTraces.get(parentTid).peek(), record.getBegin(), arrowId);
        traceEvents.add(arrowStart);

        tid = getNewTid(record, tid);

        /* Makes the end point of an asynchronous trace arrow */
        TraceEvent arrowEnd = TraceEvent.arrowEndTrace(tid, record, arrowId);
        traceEvents.add(arrowEnd);

        /* Adds the start call of an asynchronous call stack */
        Stack<Record> recordTrace = new Stack<Record>();
        recordTrace.push(record);
        recordTraces.add(recordTrace);

        long recordEndTime = record.getBegin() + record.getElapsed();

        if (occupiedRange.size() <= tid) {
            occupiedRange.add(new Long[]{record.getBegin(), recordEndTime});
        } else {
            Long[] currentOccupiedRange = occupiedRange.get(tid);
            if (currentOccupiedRange[START_TIME_INDEX] >  record.getBegin()) {
                currentOccupiedRange[START_TIME_INDEX] = record.getBegin();
            }
            if (currentOccupiedRange[END_TIME_INDEX] < recordEndTime) {
                currentOccupiedRange[END_TIME_INDEX] = recordEndTime;
            }
        }

        return tid;
    }

    private boolean isContinuingParentStack(List<Stack<Record>> recordTraces, int tid, Record record) {
        int parentTid = getParentTid(recordTraces, tid, record);
        return recordTraces.get(parentTid).peek().getApplicationName().equals(record.getApplicationName());
    }

    private int updateRecordTrace(List<Stack<Record>> recordTraces, int tid, Record record) {
        int parentTid = getParentTid(recordTraces, tid, record);
        Stack<Record> recordTrace = recordTraces.get(parentTid);
        recordTrace.add(record);

        return parentTid;
    }

    private int getParentTid(List<Stack<Record>> recordTraces, int tid, Record record) {
        int nextTid = tid;
        Integer searchingFor = invisibleRecords.get(record.getParentId());

        if (Objects.isNull(searchingFor)) {
            searchingFor = record.getParentId();
        }

        while (nextTid >= 0) {
            Stack<Record> recordTrace = recordTraces.get(nextTid);
            for (int i = recordTrace.size() - 1; i >= 0 ; i--) {
                if (recordTrace.get(i).getId() == searchingFor) {
                    recordTrace.setSize(i+1);
                    return nextTid;
                }
            }
            nextTid--;
        }
        return tid;
    }

    public static class TraceEvent {
        private String cat;                             /* category name (Exception, Database, Trace) */
        private final String pid = "";                  /* process id (not used in timeline, but necessary for trace_viewer spec. */
        private int tid;                               /* thread id (used to separate async call stacks) */
        private String id;                               /* thread id (used to separate async call stacks) */
        private long ts;                                /* start time (us) */
        private String ph;                              /* trace viewer record type (I = Exception, X = Trace, s = Arrow start, f = Arrow end) */
        private long dur;                               /* process duration (us) */
        private String s = "p";                         /* scope (only uses "p" = process in timeline ) */
        private String name;                            /* trace name */
        private String cname;                           /* color */
        private Map<String, String> args;               /* other arguments */


        public TraceEvent(String cat, int tid, String ph, String cname, final Record record, boolean showApplicationName) {
            this.cat = cat;
            this.tid = tid;
            this.ph = ph;
            this.cname = cname;
            this.id = "";

            this.ts = record.getBegin() * 1000;
            this.dur = record.getElapsed() * 1000;
            this.name = (showApplicationName? ("[" + record.getApplicationName() + "] "): "") + record.getTitle();
            this.cname = cname;

            this.args = new HashMap<String, String>();
            args.put("id", String.valueOf(record.getId()));
            args.put("parentId", String.valueOf(record.getParentId()));
            args.put("API Type", record.getApiType());
            args.put("Application Name", record.getApplicationName());
        }

        static TraceEvent defaultTrace(int tid, boolean isHighlighted, final Record record, boolean showApplicationName) {
            return new TraceEvent("Trace", tid, "X", (isHighlighted? "": "grey"), record, showApplicationName);
        }

        static TraceEvent exceptionTrace(int tid, final Record record) {
            return new TraceEvent("Exception", tid, "I", "terrible", record, false);
        }

        static TraceEvent databaseTrace(int tid, boolean isHighlighted, final Record record, boolean showApplicationName) {
            return new TraceEvent("Database", tid, "X", (isHighlighted? "": "grey"), record, showApplicationName);
        }

        static TraceEvent arrowStartTrace(int tid, final Record parent, long startTime, int arrowId) {
            TraceEvent event = new TraceEvent("Trace", tid, "s", "", parent, false);
            event.name = "Async Trace";
            event.id = Integer.toString(arrowId);
            event.ts = startTime * 1000;
            return event;
        }

        static TraceEvent arrowEndTrace(int tid, final Record record, int arrowId) {
            TraceEvent event = new TraceEvent("Trace", tid, "f", "", record, false);
            event.name = "Async Trace";
            event.id = Integer.toString(arrowId);
            return event;
        }

        public String getCat() { return cat; }

        public String getPid() { return pid; }

        public long getTs() { return ts; }

        public long getTid() { return tid; }

        public String getId() { return id; }

        public String getPh() { return ph; }

        public long getDur() { return dur; }

        public String getS() { return s; }

        public String getName() { return name; }

        public String getCname() { return cname; }

        public Map<String, String> getargs() { return args; }

        public void addArgs(Map<String, String> args) {
            this.args.putAll(args);
        }

        public void addArg(String key, String value) {
            this.args.put(key, value);
        }

        public void changeToDatabaseTrace() {
            this.cat = "Database";
        }

        public void changeToRed() {
            this.cname = "terrible";
        }
    }
}
