package com.navercorp.pinpoint.testapp.controller;

import com.navercorp.pinpoint.testapp.service.remote.RemoteService;
import com.navercorp.pinpoint.testapp.util.Description;
import org.apache.http.client.utils.URIBuilder;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.net.InetAddress;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.Map;
import java.util.Objects;

@RestController
@RequestMapping("/callSelf")
public class CallSelfController {
    
    private static final String GET_CURRENT_TIMESTAMP_PATH = "/getCurrentTimestamp";
    private static final String GET_GEO_CODE_PATH = "/httpclient4/getGeoCode";
    private static final String GET_TWITTER_URL_COUNT_PATH = "/httpclient4/getTwitterUrlCount";
    
    private static final String DEFAULT_LOCAL_IP = "127.0.0.1";
    private static final String LOCAL_IP = getLocalHostIp();

    private static String getLocalHostIp() {
        try {
            final InetAddress localHost = InetAddress.getLocalHost();
            return localHost.getHostAddress();
        } catch (UnknownHostException ignore) {
        }
        return DEFAULT_LOCAL_IP;
    }

    private final RemoteService remoteService;

    public CallSelfController(@Qualifier("httpRemoteService") RemoteService remoteService) {
        this.remoteService = Objects.requireNonNull(remoteService, "remoteService");
    }

    @RequestMapping("/getCurrentTimestamp")
    @Description("Calls self for " + GET_CURRENT_TIMESTAMP_PATH + " over HTTP.")
    public Map<String, Object> getCurrentTimeStamp(HttpServletRequest request) throws Exception {
        String url = createTargetUrl(request, GET_CURRENT_TIMESTAMP_PATH);
        @SuppressWarnings("unchecked")
        Map<String, Object> response = remoteService.get(url, Map.class);
        return response;
    }
    
    @RequestMapping("/httpclient4/getGeoCode")
    @Description("Calls self for " + GET_GEO_CODE_PATH + " over HTTP.")
    public Map<String, Object> httpClient4GetGeoCode(HttpServletRequest request) throws Exception {
        String url = createTargetUrl(request, GET_GEO_CODE_PATH);
        @SuppressWarnings("unchecked")
        Map<String, Object> response = remoteService.get(url, Map.class);
        return response;
    }
    
    @RequestMapping("/httpclient4/getTwitterUrlCount")
    @Description("Calls self for " + GET_TWITTER_URL_COUNT_PATH + " over HTTP.")
    public Map<String, Object> httpClient4GetTwitterUrlCount(HttpServletRequest request) throws Exception {
        String url = createTargetUrl(request, GET_TWITTER_URL_COUNT_PATH);
        @SuppressWarnings("unchecked")
        Map<String, Object> response = remoteService.get(url, Map.class);
        return response;
    }
    
    private static final String createTargetUrl(final HttpServletRequest request, final String path) throws URISyntaxException {
        return new URIBuilder()
                .setScheme("http")
                .setHost(LOCAL_IP)
                .setPort(request.getLocalPort())
                .setPath(path)
                .build()
                .toString();
    }
}
