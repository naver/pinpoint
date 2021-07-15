package com.navercorp.pinpoint.collector.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.navercorp.pinpoint.collector.receiver.grpc.GrpcReceiverNames;
import com.navercorp.pinpoint.grpc.channelz.ChannelzRegistry;
import com.navercorp.pinpoint.grpc.channelz.ChannelzUtils;
import io.grpc.InternalChannelz;
import io.grpc.InternalInstrumented;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;


@RestController
@RequestMapping("/channelz")
public class ChannelzController {

    private final ChannelzRegistry channelzRegistry;
    private final InternalChannelz channelz = InternalChannelz.instance();
    private final ObjectMapper mapper;

    @Autowired
    public ChannelzController(ChannelzRegistry channelzRegistry, ObjectMapper objectMapper) {
        this.channelzRegistry = Objects.requireNonNull(channelzRegistry, "channelzRegistry");
        this.mapper = Objects.requireNonNull(objectMapper, "objectMapper");
    }

    @GetMapping("/getSocket")
    public String getSocket(long logId) throws JsonProcessingException {
        InternalChannelz.SocketStats stats = getSocket0(logId);

        return mapper.writeValueAsString(stats);
    }

    @GetMapping("/html/getSocket")
    public String getSocketToHtml(long logId) {
        InternalChannelz.SocketStats stats = getSocket0(logId);

        return new HTMLBuilder().build(stats);
    }

    private InternalChannelz.SocketStats getSocket0(long logId) {
        InternalInstrumented<InternalChannelz.SocketStats> socket = channelz.getSocket(logId);
        if (socket == null) {
            return null;
        }
        return ChannelzUtils.getResult("Socket", socket);
    }

    @GetMapping("/findSocket")
    public String findSocket(String remoteAddress, int localPort) throws JsonProcessingException {

        ChannelzRegistry.AddressId addressId = ChannelzRegistry.AddressId.newAddressId(remoteAddress, localPort);
        List<InternalChannelz.SocketStats> stats = findSocket(addressId);
        if (stats == null) {
            return notFound("remoteAddress:" + remoteAddress + " localPort:" + localPort);
        }

        return mapper.writeValueAsString(stats);
    }

    @GetMapping("/html/findSocket")
    public String findSocketStatToHtml(String remoteAddress, int localPort) {

        ChannelzRegistry.AddressId targetAddress = ChannelzRegistry.AddressId.newAddressId(remoteAddress, localPort);

        List<InternalChannelz.SocketStats> stats = findSocket(targetAddress);
        if (stats.isEmpty()) {
            return notFound("remoteAddress:" + remoteAddress + " localPort:" + localPort);
        }

        return buildHtml(stats);
    }


    private List<InternalChannelz.SocketStats> findSocket(ChannelzRegistry.AddressId targetAddress) {
        Set<Long> logIdSet = channelzRegistry.getSocketLogId(targetAddress);

        List<InternalInstrumented<InternalChannelz.SocketStats>> result = new ArrayList<>();
        for (Long logId : logIdSet) {
            InternalInstrumented<InternalChannelz.SocketStats> socket = channelz.getSocket(logId);
            if (socket != null) {
                result.add(socket);
            }
        }
        return ChannelzUtils.getResults("Socket", result);
    }

    @GetMapping("/html/getServer")
    public String getServerStatToHtml(String serverName) {
        List<InternalChannelz.ServerStats> stats = getServer(serverName);
        if (stats == null) {
            return notFound("serverName=" + serverName);
        }
        return buildHtml(stats);
    }

    private <T> String buildHtml(List<T> stats) {
        StringBuilder buffer = new StringBuilder();
        for (T stat : stats) {
            String html = new HTMLBuilder().build(stat);
            buffer.append(html);
            buffer.append("<br>");
        }
        return buffer.toString();
    }


    @GetMapping("/html/getSpanReceiver")
    public String getSpanReceiverl() {
        return getServerStatToHtml(GrpcReceiverNames.SPAN);
    }


    private List<InternalChannelz.ServerStats> getServer(String serverName) {
        Long logId = channelzRegistry.getServerLogId(serverName);

        InternalChannelz.ServerList serverList = channelz.getServers(logId, 10000);

        return ChannelzUtils.getResults("ServerStats", serverList.servers);
    }


    private String notFound(String target) {
        return target + " not Found";
    }


}
