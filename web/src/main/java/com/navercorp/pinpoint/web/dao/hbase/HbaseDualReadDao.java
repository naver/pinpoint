package com.navercorp.pinpoint.web.dao.hbase;

import com.google.common.annotations.Beta;
import com.navercorp.pinpoint.common.server.bo.SpanBo;
import com.navercorp.pinpoint.web.dao.TraceDao;
import com.navercorp.pinpoint.web.vo.TransactionId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.List;

/**
 * @author Woonduk Kang(emeroad)
 */
@Beta
public class HbaseDualReadDao implements TraceDao {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final TraceDao master;
    private final TraceDao slave;

    public HbaseDualReadDao(TraceDao v2, TraceDao slave) {
        if (v2 == null) {
            throw new NullPointerException("master must not be null");
        }
        if (slave == null) {
            throw new NullPointerException("v1 must not be null");
        }

        this.master = v2;
        this.slave = slave;
    }


    @Override
    public List<SpanBo> selectSpan(TransactionId transactionId) {
        Throwable masterThrowable = null;
        List<SpanBo> result = null;
        try {
            result = master.selectSpan(transactionId);
        } catch (Throwable th) {
            masterThrowable = th;
        }
        try {
            slave.selectSpan(transactionId);
        } catch (Throwable th) {
            logger.debug("slave error :{}", th.getMessage(), th);
        }

        rethrowRuntimeException(masterThrowable);

        return result;
    }

    @Override
    public List<SpanBo> selectSpanAndAnnotation(TransactionId transactionId) {
        Throwable masterThrowable = null;
        List<SpanBo> result = null;
        try {
            result = master.selectSpanAndAnnotation(transactionId);
        } catch (Throwable th) {
            masterThrowable = th;
        }
        try {
            slave.selectSpanAndAnnotation(transactionId);
        } catch (Throwable th) {
            logger.debug("slave error :{}", th.getMessage(), th);
        }

        rethrowRuntimeException(masterThrowable);

        return result;
    }

    @Override
    public List<List<SpanBo>> selectSpans(List<TransactionId> transactionIdList) {
        Throwable masterThrowable = null;
        List<List<SpanBo>> result = null;
        try {
            result = master.selectSpans(transactionIdList);
        } catch (Throwable th) {
            masterThrowable = th;
        }
        try {
            slave.selectSpans(transactionIdList);
        } catch (Throwable th) {
            logger.debug("slave error :{}", th.getMessage(), th);
        }

        rethrowRuntimeException(masterThrowable);

        return result;
    }

    @Override
    public List<List<SpanBo>> selectAllSpans(Collection<TransactionId> transactionIdList) {
        Throwable masterThrowable = null;
        List<List<SpanBo>> result = null;
        try {
            result = master.selectAllSpans(transactionIdList);
        } catch (Throwable th) {
            masterThrowable = th;
        }
        try {
            slave.selectAllSpans(transactionIdList);
        } catch (Throwable th) {
            logger.debug("slave error :{}", th.getMessage(), th);
        }

        rethrowRuntimeException(masterThrowable);

        return result;
    }

    @Override
    public List<SpanBo> selectSpans(TransactionId transactionId) {
        Throwable masterThrowable = null;
        List<SpanBo> result = null;
        try {
            result = master.selectSpans(transactionId);
        } catch (Throwable th) {
            masterThrowable = th;
        }
        try {
            slave.selectSpans(transactionId);
        } catch (Throwable th) {
            logger.debug("slave error :{}", th.getMessage(), th);
        }

        rethrowRuntimeException(masterThrowable);

        return result;
    }


    private void rethrowRuntimeException(Throwable exception) {
        if (exception != null) {
            this.<RuntimeException>rethrowException(exception);
        }
    }

    @SuppressWarnings("unchecked")
    private <T extends Exception> void rethrowException(final Throwable exception) throws T {
        throw (T) exception;
    }
}
