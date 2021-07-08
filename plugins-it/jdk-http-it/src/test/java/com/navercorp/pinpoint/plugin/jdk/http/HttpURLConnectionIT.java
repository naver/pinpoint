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
package com.navercorp.pinpoint.plugin.jdk.http;

import static com.navercorp.pinpoint.bootstrap.plugin.test.Expectations.*;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.UnknownHostException;


import com.navercorp.pinpoint.pluginit.utils.AgentPath;
import com.navercorp.pinpoint.pluginit.utils.PluginITConstants;
import com.navercorp.pinpoint.pluginit.utils.WebServer;
import com.navercorp.pinpoint.test.plugin.Dependency;
import com.navercorp.pinpoint.test.plugin.PinpointAgent;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.navercorp.pinpoint.bootstrap.plugin.test.PluginTestVerifier;
import com.navercorp.pinpoint.bootstrap.plugin.test.PluginTestVerifierHolder;
import com.navercorp.pinpoint.test.plugin.JvmVersion;
import com.navercorp.pinpoint.test.plugin.PinpointPluginTestSuite;

/**
 * @author Jongho Moon
 *
 */
@RunWith(PinpointPluginTestSuite.class)
@PinpointAgent(AgentPath.PATH)
@JvmVersion({6, 7, 8})
@Dependency({ WebServer.VERSION, PluginITConstants.VERSION})
public class HttpURLConnectionIT {

    private static WebServer webServer;

    @BeforeClass
    public static void BeforeClass() throws Exception {
        webServer = WebServer.newTestWebServer();
    }

    @AfterClass
    public static void AfterClass() throws Exception {
        webServer = WebServer.cleanup(webServer);
    }

    @Test
    public void test() throws Exception {
        URL url = new URL(webServer.getCallHttpUrl());
        HttpURLConnection connection = (HttpURLConnection)url.openConnection();
        connection.getHeaderFields();
        
        PluginTestVerifier verifier = PluginTestVerifierHolder.getInstance();
        verifier.printCache();
        
        Class<?> targetClass = Class.forName("sun.net.www.protocol.http.HttpURLConnection");
        Method getInputStream = targetClass.getMethod("getInputStream");
        
        String destinationId = webServer.getHostAndPort();
        String httpUrl = webServer.getCallHttpUrl();
        verifier.verifyTraceCount(1);
        verifier.verifyTrace(event("JDK_HTTPURLCONNECTOR", getInputStream, null, null, destinationId, annotation("http.url", httpUrl)));
    }
    
    @Test
    public void testConnectTwice() throws Exception {
        URL url = new URL(webServer.getCallHttpUrl());
        HttpURLConnection connection = (HttpURLConnection)url.openConnection();
        
        connection.connect();
        connection.getInputStream();
        
        PluginTestVerifier verifier = PluginTestVerifierHolder.getInstance();
        verifier.printCache();
        
        Class<?> targetClass = Class.forName("sun.net.www.protocol.http.HttpURLConnection");
        Method connect = targetClass.getMethod("connect");

        String destinationId = webServer.getHostAndPort();
        String httpUrl = webServer.getCallHttpUrl();
        verifier.verifyTraceCount(1);
        verifier.verifyTrace(event("JDK_HTTPURLCONNECTOR", connect, null, null, destinationId, annotation("http.url", httpUrl)));
    }
    
    @Test
    public void testConnecting() throws Exception {
        Exception expected = null;
        URL url = new URL("http://no.such.url");
        HttpURLConnection connection = (HttpURLConnection)url.openConnection();
        
        try {
            connection.connect();
        } catch (UnknownHostException e) {
            expected = e;
        }
        
        try {
            connection.connect();
        } catch (UnknownHostException e) {
            expected = e;
        }
        
        Field field = null;
        try {
            field = connection.getClass().getDeclaredField("connecting");
        } catch (NoSuchFieldException ignored) {
            
        }
        
         
        
        PluginTestVerifier verifier = PluginTestVerifierHolder.getInstance();
        verifier.printCache();
        
        Class<?> targetClass = Class.forName("sun.net.www.protocol.http.HttpURLConnection");
        Method getInputStream = targetClass.getMethod("connect");
        
        verifier.verifyTrace(event("JDK_HTTPURLCONNECTOR", getInputStream, expected, null, null, "no.such.url", annotation("http.url", "http://no.such.url")));
        
        if (field == null) {
            // JDK 6, 7
            verifier.verifyTrace(event("JDK_HTTPURLCONNECTOR", getInputStream, expected, null, null, "no.such.url", annotation("http.url", "http://no.such.url")));
        }
        
        verifier.verifyTraceCount(0);
    }
}
