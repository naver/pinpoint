package com.navercorp.pinpoint.web.view;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.navercorp.pinpoint.web.vo.ResponseTimeStatics;

import java.io.IOException;

public class ResponseTimeStatisticsSerializer extends JsonSerializer<ResponseTimeStatics> {

    @Override
    public void serialize(ResponseTimeStatics responseTimeStatics, JsonGenerator jgen, SerializerProvider serializerProvider) throws IOException {

        jgen.writeStartObject();

        jgen.writeFieldName(ResponseTimeStatics.TOTAL_COUNT);
        jgen.writeNumber(responseTimeStatics.getTotalCount());

        jgen.writeFieldName(ResponseTimeStatics.SUM_ELAPSED_TIME);
        jgen.writeNumber(responseTimeStatics.getSumTime());

        jgen.writeFieldName(ResponseTimeStatics.AVG_ELAPSED_TIME);
        jgen.writeNumber(responseTimeStatics.getAvgTime());

        jgen.writeFieldName(ResponseTimeStatics.MAX_ELAPSED_TIME);
        jgen.writeNumber(responseTimeStatics.getMaxTime());

        jgen.writeEndObject();
    }

}
