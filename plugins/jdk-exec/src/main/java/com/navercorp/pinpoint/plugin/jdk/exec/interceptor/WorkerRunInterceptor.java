package com.navercorp.pinpoint.plugin.jdk.exec.interceptor;
import com.navercorp.pinpoint.bootstrap.context.AsyncTraceId;
import com.navercorp.pinpoint.bootstrap.context.MethodDescriptor;
import com.navercorp.pinpoint.bootstrap.context.SpanEventRecorder;
import com.navercorp.pinpoint.bootstrap.context.TraceContext;
import com.navercorp.pinpoint.bootstrap.interceptor.SpanAsyncEventSimpleAroundInterceptor;
import com.navercorp.pinpoint.plugin.jdk.exec.CacheMap;
import com.navercorp.pinpoint.plugin.jdk.exec.JdkExecConstants;

/**
 * @author hamlet-lee
 */
public class WorkerRunInterceptor extends SpanAsyncEventSimpleAroundInterceptor {

    public WorkerRunInterceptor(TraceContext traceContext, MethodDescriptor methodDescriptor) {
        super(traceContext, methodDescriptor);
    }

    @Override
    protected void doInBeforeTrace(SpanEventRecorder recorder, AsyncTraceId asyncTraceId, Object target, Object[] args) {
        // do nothing
    }

    @Override
    protected void doInAfterTrace(SpanEventRecorder recorder, Object target, Object[] args, Object result, Throwable throwable) {
        recorder.recordServiceType(JdkExecConstants.SERVICE_TYPE);
        recorder.recordApi(methodDescriptor);
        recorder.recordException(throwable);
    }

    @Override
    protected AsyncTraceId getAsyncTraceId(Object target) {
        AsyncTraceId asyncTraceId = (AsyncTraceId)CacheMap.getInstance(JdkExecConstants.ASYNC_ID_MAP).get(target.hashCode());
        if( logger.isDebugEnabled() ) {
            logger.debug("JdkExec: getting asyncId " + target.hashCode() + " -> " + asyncTraceId);
            if( asyncTraceId == null) {
                logger.error("JdkExec: null path", new Exception("show stacktrace"));
            }
        }
        return asyncTraceId;
    }
}