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

package com.navercorp.pinpoint.profiler.sender;

import com.navercorp.pinpoint.common.util.ThreadMXBeanUtils;
import com.navercorp.pinpoint.rpc.PinpointDatagramSocket;
import com.navercorp.pinpoint.rpc.PinpointOioDatagramSocketFactory;
import org.junit.Assert;
import org.junit.Test;

import java.net.InetSocketAddress;


public class BufferedUdpDataSenderTest {

    @Test
    public void testSendPacket() throws Exception {



    }

    @Test
    public void testStop_StopFlushThread() throws Exception {
        PinpointOioDatagramSocketFactory datagramSocketFactory = new PinpointOioDatagramSocketFactory();
        PinpointDatagramSocket datagramSocket = datagramSocketFactory.createSocket();
        datagramSocket.connect(new InetSocketAddress("localhost", 9999));

        final BufferedUdpDataSender sender = new BufferedUdpDataSender(datagramSocket, "testUdpSender", 100);

        final String flushThreadName = sender.getFlushThreadName();

        Assert.assertTrue(ThreadMXBeanUtils.findThreadName(flushThreadName));

        sender.stop();

        Assert.assertFalse(ThreadMXBeanUtils.findThreadName(flushThreadName));
        // ?? finally { send.stop() }
    }
}