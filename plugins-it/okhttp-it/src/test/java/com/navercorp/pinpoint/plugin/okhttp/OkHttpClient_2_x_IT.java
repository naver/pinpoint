/*
 * Copyright 2018 NAVER Corp.
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
package com.navercorp.pinpoint.plugin.okhttp;

import com.navercorp.pinpoint.bootstrap.plugin.test.PluginTestVerifier;
import com.navercorp.pinpoint.bootstrap.plugin.test.PluginTestVerifierHolder;
import com.navercorp.pinpoint.common.plugin.util.HostAndPort;
import com.navercorp.pinpoint.pluginit.utils.AgentPath;
import com.navercorp.pinpoint.pluginit.utils.PluginITConstants;
import com.navercorp.pinpoint.pluginit.utils.WebServer;
import com.navercorp.pinpoint.test.plugin.Dependency;
import com.navercorp.pinpoint.test.plugin.ImportPlugin;
import com.navercorp.pinpoint.test.plugin.PinpointAgent;
import com.navercorp.pinpoint.test.plugin.PinpointPluginTestSuite;
import com.squareup.okhttp.*;
import com.squareup.okhttp.internal.http.HttpEngine;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static com.navercorp.pinpoint.bootstrap.plugin.test.Expectations.annotation;
import static com.navercorp.pinpoint.bootstrap.plugin.test.Expectations.anyAnnotationValue;
import static com.navercorp.pinpoint.bootstrap.plugin.test.Expectations.event;
import static com.navercorp.pinpoint.common.trace.ServiceType.ASYNC;
import static com.navercorp.pinpoint.plugin.okhttp.OkHttpConstants.OK_HTTP_CLIENT;
import static com.navercorp.pinpoint.plugin.okhttp.OkHttpConstants.OK_HTTP_CLIENT_INTERNAL;

/**
 * @author jaehong.kim
 */
@RunWith(PinpointPluginTestSuite.class)
@PinpointAgent(AgentPath.PATH)
@ImportPlugin("com.navercorp.pinpoint:pinpoint-okhttp-plugin")
@Dependency({"com.squareup.okhttp:okhttp:[2.0.0,3.0.0)", WebServer.VERSION, PluginITConstants.VERSION})
public class OkHttpClient_2_x_IT {

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
    public void execute() throws Exception {
        Request request = new Request.Builder().url(webServer.getCallHttpUrl()).build();
        OkHttpClient client = new OkHttpClient();
        Response response = client.newCall(request).execute();

        PluginTestVerifier verifier = PluginTestVerifierHolder.getInstance();
        verifier.printCache();

        Method executeMethod = Call.class.getDeclaredMethod("execute");
        verifier.verifyTrace(event(OK_HTTP_CLIENT_INTERNAL.getName(), executeMethod));

        Method sendRequestMethod = HttpEngine.class.getDeclaredMethod("sendRequest");
        verifier.verifyTrace(event(OK_HTTP_CLIENT.getName(), sendRequestMethod,
                null, null, webServer.getHostAndPort(),
                annotation("http.url", request.urlString())));

        URL url = request.url();
        int port = EndPointUtils.getPort(url.getPort(), url.getDefaultPort());
        String hostAndPort = HostAndPort.toHostAndPortString(url.getHost(), port);
        Method connectMethod = getConnectMethod();
        verifier.verifyTrace(event(OK_HTTP_CLIENT_INTERNAL.getName(), connectMethod,
                annotation("http.internal.display", hostAndPort)));

        Method readResponseMethod = HttpEngine.class.getDeclaredMethod("readResponse");
        verifier.verifyTrace(event(OK_HTTP_CLIENT_INTERNAL.getName(), readResponseMethod,
                annotation("http.status.code", response.code()),
                annotation("http.resp.header", anyAnnotationValue())));

        verifier.verifyTraceCount(0);
    }

    @Test
    public void enqueue() throws Exception {
        Request request = new Request.Builder().url(webServer.getCallHttpUrl()).build();
        OkHttpClient client = new OkHttpClient();
        final CountDownLatch latch = new CountDownLatch(1);
        final AtomicReference<Response> responseRef = new AtomicReference<Response>(null);
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Request request, IOException e) {
                latch.countDown();
            }

            @Override
            public void onResponse(Response response) {
                responseRef.set(response);
                latch.countDown();
            }
        });
        latch.await(3, TimeUnit.SECONDS);

        PluginTestVerifier verifier = PluginTestVerifierHolder.getInstance();
        verifier.awaitTrace(event(ASYNC.getName(), "Asynchronous Invocation"), 20, 3000);
        verifier.printCache();

        Method callEnqueueMethod = Call.class.getDeclaredMethod("enqueue", com.squareup.okhttp.Callback.class);
        verifier.verifyTrace(event(OK_HTTP_CLIENT_INTERNAL.getName(), callEnqueueMethod));

        Method dispatcherEnqueueMethod = Dispatcher.class.getDeclaredMethod("enqueue", Class.forName("com.squareup.okhttp.Call$AsyncCall"));
        verifier.verifyTrace(event(OK_HTTP_CLIENT_INTERNAL.getName(), dispatcherEnqueueMethod));

        verifier.verifyTrace(event(ASYNC.getName(), "Asynchronous Invocation"));

        Method executeMethod = Class.forName("com.squareup.okhttp.Call$AsyncCall").getDeclaredMethod("execute");
        verifier.verifyTrace(event(OK_HTTP_CLIENT_INTERNAL.getName(), executeMethod));

        Method sendRequestMethod = HttpEngine.class.getDeclaredMethod("sendRequest");
        verifier.verifyTrace(event(OK_HTTP_CLIENT.getName(), sendRequestMethod,
                null, null, webServer.getHostAndPort(),
                annotation("http.url", request.urlString())));

        URL url = request.url();
        int port = EndPointUtils.getPort(url.getPort(), url.getDefaultPort());
        String hostAndPort = HostAndPort.toHostAndPortString(url.getHost(), port);
        Method connectMethod = getConnectMethod();
        verifier.verifyTrace(event(OK_HTTP_CLIENT_INTERNAL.getName(), connectMethod,
                annotation("http.internal.display", hostAndPort)));

        Response response = responseRef.get();
        Assert.assertNotNull("response is null", response);
        Method readResponseMethod = HttpEngine.class.getDeclaredMethod("readResponse");
        verifier.verifyTrace(event(OK_HTTP_CLIENT_INTERNAL.getName(), readResponseMethod,
                annotation("http.status.code", response.code()),
                annotation("http.resp.header", anyAnnotationValue())));

        verifier.verifyTraceCount(0);
    }

    private Method getConnectMethod() {
        try {
            // [2.3.0,)
            return HttpEngine.class.getDeclaredMethod("connect");
        } catch (NoSuchMethodException e) {
            try {
                // [2.0.0,2.2.max]
                return HttpEngine.class.getDeclaredMethod("connect", Request.class);
            } catch (NoSuchMethodException e1) {
                throw new AssertionError("Expected methods connect() / connect(com.squareup.okhttp.Request) not found in HttpEngine class");
            }
        }
    }

}
