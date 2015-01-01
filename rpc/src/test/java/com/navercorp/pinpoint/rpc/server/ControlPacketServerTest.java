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

package com.navercorp.pinpoint.rpc.server;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.navercorp.pinpoint.rpc.control.ProtocolException;
import com.navercorp.pinpoint.rpc.packet.ControlHandshakePacket;
import com.navercorp.pinpoint.rpc.packet.ControlHandshakeResponsePacket;
import com.navercorp.pinpoint.rpc.packet.HandshakeResponseCode;
import com.navercorp.pinpoint.rpc.packet.HandshakeResponseType;
import com.navercorp.pinpoint.rpc.packet.RequestPacket;
import com.navercorp.pinpoint.rpc.packet.ResponsePacket;
import com.navercorp.pinpoint.rpc.packet.SendPacket;
import com.navercorp.pinpoint.rpc.server.PinpointServerSocket;
import com.navercorp.pinpoint.rpc.server.ServerMessageListener;
import com.navercorp.pinpoint.rpc.server.SocketChannel;
import com.navercorp.pinpoint.rpc.util.ControlMessageEncodingUtils;
import com.navercorp.pinpoint.rpc.util.MapUtils;

/**
 * @author koo.taejin
 */
public class ControlPacketServerTest {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    // Test for being possible to send messages in case of failure of registering packet ( return code : 2, lack of parameter)
    @Test
    public void registerAgentTest1() throws Exception {
        PinpointServerSocket pinpointServerSocket = new PinpointServerSocket();
        pinpointServerSocket.setMessageListener(new SimpleListener());
        pinpointServerSocket.bind("127.0.0.1", 22234);

        Socket socket = null;
        try {
            socket = new Socket("127.0.0.1", 22234);

            sendAndReceiveSimplePacket(socket);

            int code= sendAndReceiveRegisterPacket(socket);
            Assert.assertEquals(2, code);

            sendAndReceiveSimplePacket(socket);
        } finally {
            if (socket != null) {
                socket.close();
            }

            if (pinpointServerSocket != null) {
                pinpointServerSocket.close();
            }
        }
    }

    // Test for being possible to send messages in case of success of registering packet ( return code : 0)
    @Test
    public void registerAgentTest2() throws Exception {
        PinpointServerSocket pinpointServerSocket = new PinpointServerSocket();
        pinpointServerSocket.setMessageListener(new SimpleListener());
        pinpointServerSocket.bind("127.0.0.1", 22234);

        Socket socket = null;
        try {
            socket = new Socket("127.0.0.1", 22234);

            sendAndReceiveSimplePacket(socket);

            int code= sendAndReceiveRegisterPacket(socket, getParams());
            Assert.assertEquals(0, code);

            sendAndReceiveSimplePacket(socket);
        } finally {
            if (socket != null) {
                socket.close();
            }

            if (pinpointServerSocket != null) {
                pinpointServerSocket.close();
            }
        }
    }

    // when failure of registering and retrying to register, confirm to return same code ( return code : 2
    @Test
    public void registerAgentTest3() throws Exception {
        PinpointServerSocket pinpointServerSocket = new PinpointServerSocket();
        pinpointServerSocket.setMessageListener(new SimpleListener());
        pinpointServerSocket.bind("127.0.0.1", 22234);

        Socket socket = null;
        try {
            socket = new Socket("127.0.0.1", 22234);
            int code = sendAndReceiveRegisterPacket(socket);
            Assert.assertEquals(2, code);

            code = sendAndReceiveRegisterPacket(socket);
            Assert.assertEquals(2, code);

            sendAndReceiveSimplePacket(socket);
        } finally {
            if (socket != null) {
                socket.close();
            }

            if (pinpointServerSocket != null) {
                pinpointServerSocket.close();
            }
        }
    }

    // after success of registering, when success message are sent repeatedly.
    // test 1) confirm to return success code, 2) confirm to return already success code.
    @Test
    public void registerAgentTest4() throws Exception {
        PinpointServerSocket pinpointServerSocket = new PinpointServerSocket();
        pinpointServerSocket.setMessageListener(new SimpleListener());
        pinpointServerSocket.bind("127.0.0.1", 22234);

        Socket socket = null;
        try {
            socket = new Socket("127.0.0.1", 22234);
            sendAndReceiveSimplePacket(socket);

            int code = sendAndReceiveRegisterPacket(socket, getParams());
            Assert.assertEquals(0, code);

            sendAndReceiveSimplePacket(socket);

            code = sendAndReceiveRegisterPacket(socket, getParams());
            Assert.assertEquals(1, code);

            sendAndReceiveSimplePacket(socket);
        } finally {
            if (socket != null) {
                socket.close();
            }

            if (pinpointServerSocket != null) {
                pinpointServerSocket.close();
            }
        }
    }


    private int sendAndReceiveRegisterPacket(Socket socket) throws ProtocolException, IOException {
        return sendAndReceiveRegisterPacket(socket, Collections.EMPTY_MAP);
    }

    private int sendAndReceiveRegisterPacket(Socket socket, Map properties) throws ProtocolException, IOException {
        sendRegisterPacket(socket.getOutputStream(), properties);
        ControlHandshakeResponsePacket packet = receiveRegisterConfirmPacket(socket.getInputStream());
        Map<Object, Object> result = (Map<Object, Object>) ControlMessageEncodingUtils.decode(packet.getPayload());

        return MapUtils.getInteger(result, "code", -1);
    }

    private void sendAndReceiveSimplePacket(Socket socket) throws ProtocolException, IOException {
        sendSimpleRequestPacket(socket.getOutputStream());
        ResponsePacket responsePacket = readSimpleResponsePacket(socket.getInputStream());
        Assert.assertNotNull(responsePacket);
    }

    private void sendRegisterPacket(OutputStream outputStream, Map properties) throws ProtocolException, IOException {
        byte[] payload = ControlMessageEncodingUtils.encode(properties);
        ControlHandshakePacket packet = new ControlHandshakePacket(1, payload);

        ByteBuffer bb = packet.toBuffer().toByteBuffer(0, packet.toBuffer().writerIndex());
        sendData(outputStream, bb.array());
    }

    private void sendSimpleRequestPacket(OutputStream outputStream) throws ProtocolException, IOException {
        RequestPacket packet = new RequestPacket(new byte[0]);
        packet.setRequestId(10);

        ByteBuffer bb = packet.toBuffer().toByteBuffer(0, packet.toBuffer().writerIndex());
        sendData(outputStream, bb.array());
    }

    private void sendData(OutputStream outputStream, byte[] payload) throws IOException {
        outputStream.write(payload);
        outputStream.flush();
    }

    private ControlHandshakeResponsePacket receiveRegisterConfirmPacket(InputStream inputStream) throws ProtocolException, IOException {

        byte[] payload = readData(inputStream);
        ChannelBuffer cb = ChannelBuffers.wrappedBuffer(payload);

        short packetType = cb.readShort();

        ControlHandshakeResponsePacket packet = ControlHandshakeResponsePacket.readBuffer(packetType, cb);
        return packet;
    }

    private ResponsePacket readSimpleResponsePacket(InputStream inputStream) throws ProtocolException, IOException {
        byte[] payload = readData(inputStream);
        ChannelBuffer cb = ChannelBuffers.wrappedBuffer(payload);

        short packetType = cb.readShort();

        ResponsePacket packet = ResponsePacket.readBuffer(packetType, cb);
        return packet;
    }

    private byte[] readData(InputStream inputStream) throws IOException {
        int availableSize = 0;

        for (int i = 0; i < 3; i++) {
            availableSize = inputStream.available();

            if (availableSize > 0) {
                break;
            }

            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

        byte[] payload = new byte[availableSize];
        inputStream.read(payload);

        return payload;
    }

    class SimpleListener implements ServerMessageListener {
        @Override
        public void handleSend(SendPacket sendPacket, SocketChannel channel) {

        }

        @Override
        public void handleRequest(RequestPacket requestPacket, SocketChannel channel) {
            logger.info("handlerRequest {} {}", requestPacket, channel);
            channel.sendResponseMessage(requestPacket, requestPacket.getPayload());
        }

        @Override
        public HandshakeResponseCode handleHandshake(Map properties) {
            if (properties == null) {
                return HandshakeResponseType.ProtocolError.PROTOCOL_ERROR;
            }

            boolean hasAllType = AgentHandshakePropertyType.hasAllType(properties);
            if (!hasAllType) {
                return HandshakeResponseType.PropertyError.PROPERTY_ERROR;
            }

            return HandshakeResponseType.Success.DUPLEX_COMMUNICATION;
        }
    }

    private Map getParams() {
        Map properties = new HashMap();

        properties.put(AgentHandshakePropertyType.AGENT_ID.getName(), "agent");
        properties.put(AgentHandshakePropertyType.APPLICATION_NAME.getName(), "application");
        properties.put(AgentHandshakePropertyType.HOSTNAME.getName(), "hostname");
        properties.put(AgentHandshakePropertyType.IP.getName(), "ip");
        properties.put(AgentHandshakePropertyType.PID.getName(), 1111);
        properties.put(AgentHandshakePropertyType.SERVICE_TYPE.getName(), 10);
        properties.put(AgentHandshakePropertyType.START_TIMESTAMP.getName(), System.currentTimeMillis());
        properties.put(AgentHandshakePropertyType.VERSION.getName(), "1.0");

        return properties;
    }

}
