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
package com.navercorp.pinpoint.plugin.httpclient4;

import static com.navercorp.pinpoint.bootstrap.plugin.test.Expectations.*;


import com.navercorp.pinpoint.pluginit.utils.AgentPath;
import com.navercorp.pinpoint.pluginit.utils.PluginITConstants;
import com.navercorp.pinpoint.pluginit.utils.WebServer;
import com.navercorp.pinpoint.test.plugin.ImportPlugin;
import com.navercorp.pinpoint.test.plugin.PinpointAgent;
import org.apache.http.HttpClientConnection;
import org.apache.http.HttpRequest;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.conn.routing.HttpRoute;
import org.apache.http.impl.client.AbstractHttpClient;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpRequestExecutor;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.navercorp.pinpoint.bootstrap.plugin.test.PluginTestVerifier;
import com.navercorp.pinpoint.bootstrap.plugin.test.PluginTestVerifierHolder;
import com.navercorp.pinpoint.test.plugin.Dependency;
import com.navercorp.pinpoint.test.plugin.PinpointPluginTestSuite;

/**
 * @author jaehong.kim
 */
@RunWith(PinpointPluginTestSuite.class)
@PinpointAgent(AgentPath.PATH)
@ImportPlugin("com.navercorp.pinpoint:pinpoint-httpclient4-plugin")
@Dependency({ "org.apache.httpcomponents:httpclient:[4.0],[4.0.1],[4.0.2],[4.0.3],[4.1],[4.1.1],[4.1.2],[4.1.3],[4.2],[4.2.1],[4.2.2],[4.2.3],[4.2.4],[4.2.4],[4.2.6],[4.3.3]",
        WebServer.VERSION, PluginITConstants.VERSION})
public class HttpClientIT {

    private static WebServer webServer;

    @BeforeClass
    public static void BeforeClass() throws Exception {
        webServer = WebServer.newTestWebServer();
    }

    @AfterClass
    public static void AfterClass() {
        webServer = WebServer.cleanup(webServer);
    }

    @Test
    public void test() throws Exception {
        HttpClient httpClient = new DefaultHttpClient();
        try {
            HttpPost post = new HttpPost(webServer.getCallHttpUrl());
            post.addHeader("Content-Type", "application/json;charset=UTF-8");

            ResponseHandler<String> responseHandler = new BasicResponseHandler();
            httpClient.execute(post, responseHandler);
        } catch (Exception ignored) {
        } finally {
            if (null != httpClient && null != httpClient.getConnectionManager()) {
                httpClient.getConnectionManager().shutdown();
            }
        }

        PluginTestVerifier verifier = PluginTestVerifierHolder.getInstance();
        verifier.printCache();

        Class<?> connectorClass;
        
        try {
            connectorClass = Class.forName("org.apache.http.impl.conn.ManagedClientConnectionImpl");
        } catch (ClassNotFoundException e) {
            connectorClass = Class.forName("org.apache.http.impl.conn.AbstractPooledConnAdapter");
        }
        
        verifier.verifyTrace(event("HTTP_CLIENT_4_INTERNAL", AbstractHttpClient.class.getMethod("execute", HttpUriRequest.class, ResponseHandler.class)));
        final String hostname = webServer.getHostAndPort();
        verifier.verifyTrace(event("HTTP_CLIENT_4_INTERNAL", connectorClass.getMethod("open", HttpRoute.class, HttpContext.class, HttpParams.class), annotation("http.internal.display", hostname)));
        verifier.verifyTrace(event("HTTP_CLIENT_4", HttpRequestExecutor.class.getMethod("execute", HttpRequest.class, HttpClientConnection.class, HttpContext.class), null, null, hostname, annotation("http.url", "/"),  annotation("http.status.code", 200), annotation("http.resp.header", anyAnnotationValue()), annotation("http.io", anyAnnotationValue())));
        verifier.verifyTraceCount(0);
    }
}
