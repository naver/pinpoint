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

package com.navercorp.pinpoint.web.view;

import com.navercorp.pinpoint.web.applicationmap.link.Link;
import com.navercorp.pinpoint.web.applicationmap.link.LinkType;
import com.navercorp.pinpoint.web.applicationmap.nodes.Node;
import com.navercorp.pinpoint.web.applicationmap.nodes.ServerInstanceList;
import com.navercorp.pinpoint.web.applicationmap.histogram.Histogram;
import com.navercorp.pinpoint.web.applicationmap.rawdata.AgentHistogram;
import com.navercorp.pinpoint.web.applicationmap.rawdata.AgentHistogramList;
import com.navercorp.pinpoint.web.vo.Application;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.navercorp.pinpoint.web.vo.ResponseTimeStatics;

import java.io.IOException;
import java.util.Collection;
import java.util.List;

/**
 * @author emeroad
 * @author netspider
 * @author HyunGil Jeong
 */
public class LinkSerializer extends JsonSerializer<Link> {

    @Override
    public void serialize(Link link, JsonGenerator jgen, SerializerProvider provider) throws IOException, JsonProcessingException {
        jgen.writeStartObject();

        jgen.writeStringField("key", link.getLinkName());  // for servermap

        jgen.writeStringField("from", link.getFrom().getNodeName());  // necessary for go.js
        jgen.writeStringField("to", link.getTo().getNodeName()); // necessary for go.js

        // for FilterWizard. from, to agent mapping data
        writeAgentId("fromAgent", link.getFrom(), jgen);
        writeAgentId("toAgent", link.getTo(), jgen);

        writeSimpleNode("sourceInfo", link.getFrom(), jgen);
        writeSimpleNode("targetInfo", link.getTo(), jgen);

        Application filterApplication = link.getFilterApplication();
        jgen.writeStringField("filterApplicationName", filterApplication.getName());
        jgen.writeNumberField("filterApplicationServiceTypeCode", filterApplication.getServiceTypeCode());
        jgen.writeStringField("filterApplicationServiceTypeName", filterApplication.getServiceType().getName());
        if (link.isWasToWasLink()) {
            writeWasToWasTargetRpcList(link, jgen);
        }

        Histogram histogram = link.getHistogram();
        jgen.writeNumberField("totalCount", histogram.getTotalCount()); // for go.js
        jgen.writeNumberField("errorCount", histogram.getTotalErrorCount());
        jgen.writeNumberField("slowCount", histogram.getSlowCount());

        ResponseTimeStatics responseTimeStatics = ResponseTimeStatics.fromHistogram(histogram);
        jgen.writeObjectField(ResponseTimeStatics.RESPONSE_STATISTICS, responseTimeStatics);


        jgen.writeObjectField("histogram", histogram);
        writeTimeSeriesHistogram(link, jgen);

        if (LinkType.DETAILED == link.getLinkType()) {
            // data showing how agents call each of their respective links
            writeAgentHistogram("sourceHistogram", link.getSourceList(), jgen);
            writeAgentHistogram("targetHistogram", link.getTargetList(), jgen);
            writeSourceAgentTimeSeriesHistogram(link, jgen);
            writeAgentResponseStatistics("sourceResponseStatistics", link.getSourceList(), jgen);
            writeAgentResponseStatistics("targetResponseStatistics", link.getTargetList(), jgen);
        }

//        String state = link.getLinkState();
//        jgen.writeStringField("state", state); // for go.js
        jgen.writeBooleanField("hasAlert", link.getLinkAlert()); // for go.js

        jgen.writeEndObject();
    }

    private void writeAgentId(String fieldName, Node node, JsonGenerator jgen) throws IOException {
        if (node.getServiceType().isWas()) {
            jgen.writeFieldName(fieldName);
            jgen.writeStartArray();
            ServerInstanceList serverInstanceList = node.getServerInstanceList();
            if (serverInstanceList!= null) {
                for (String agentId : serverInstanceList.getAgentIdList()) {
                    jgen.writeObject(agentId);
                }
            }
            jgen.writeEndArray();
        }
    }

    private void writeWasToWasTargetRpcList(Link link, JsonGenerator jgen) throws IOException {
        // write additional information to be used for filtering failed WAS -> WAS call events.
        jgen.writeFieldName("filterTargetRpcList");
        jgen.writeStartArray();
        Collection<Application> sourceLinkTargetAgentList = link.getSourceLinkTargetAgentList();
        for (Application application : sourceLinkTargetAgentList) {
            jgen.writeStartObject();
            jgen.writeStringField("rpc", application.getName());
            jgen.writeNumberField("rpcServiceTypeCode", application.getServiceTypeCode());
            jgen.writeEndObject();
        }
        jgen.writeEndArray();

    }



    private void writeTimeSeriesHistogram(Link link, JsonGenerator jgen) throws IOException {
        List<ResponseTimeViewModel> sourceApplicationTimeSeriesHistogram = link.getLinkApplicationTimeSeriesHistogram();

        jgen.writeFieldName("timeSeriesHistogram");
        jgen.writeObject(sourceApplicationTimeSeriesHistogram);
    }

    private void writeAgentResponseStatistics(String fieldName, AgentHistogramList agentHistogramList, JsonGenerator jgen) throws IOException {
        jgen.writeFieldName(fieldName);
        jgen.writeStartObject();
        for (AgentHistogram agentHistogram : agentHistogramList.getAgentHistogramList()) {
            jgen.writeFieldName(agentHistogram.getId());
            jgen.writeObject(ResponseTimeStatics.fromHistogram(agentHistogram.getHistogram()));
        }
        jgen.writeEndObject();
    }

    private void writeAgentHistogram(String fieldName, AgentHistogramList agentHistogramList, JsonGenerator jgen) throws IOException {
        jgen.writeFieldName(fieldName);
        jgen.writeStartObject();
        for (AgentHistogram agentHistogram : agentHistogramList.getAgentHistogramList()) {
            jgen.writeFieldName(agentHistogram.getId());
            jgen.writeObject(agentHistogram.getHistogram());
        }
        jgen.writeEndObject();
    }

    private void writeSourceAgentTimeSeriesHistogram(Link link, JsonGenerator jgen) throws IOException {
        AgentResponseTimeViewModelList sourceAgentTimeSeriesHistogram = link.getSourceAgentTimeSeriesHistogram();
        sourceAgentTimeSeriesHistogram.setFieldName("sourceTimeSeriesHistogram");
        jgen.writeObject(sourceAgentTimeSeriesHistogram);
    }

    private void writeSimpleNode(String fieldName, Node node, JsonGenerator jgen) throws IOException {
        jgen.writeFieldName(fieldName);

        jgen.writeStartObject();
        Application application = node.getApplication();
        jgen.writeStringField("applicationName", application.getName());
        jgen.writeStringField("serviceType", application.getServiceType().toString());
        jgen.writeNumberField("serviceTypeCode", application.getServiceTypeCode());
        jgen.writeBooleanField("isWas", application.getServiceType().isWas());
        jgen.writeEndObject();
    }
}
