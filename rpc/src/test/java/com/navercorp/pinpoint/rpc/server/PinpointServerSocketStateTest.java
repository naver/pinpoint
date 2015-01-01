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

import org.junit.Assert;
import org.junit.Test;

import com.navercorp.pinpoint.rpc.server.PinpointServerSocketState;
import com.navercorp.pinpoint.rpc.server.PinpointServerSocketStateCode;

/**
 * @author koo.taejin
 */
public class PinpointServerSocketStateTest {

    // basic type of connection's lifecycle between peers.
    // RUN -> RUN_DUPLEX_COMMUNICATION ->  BEING_SHUTDOWN -> connection closed
    @Test
    public void changeStateTest1() {
        PinpointServerSocketState state = new PinpointServerSocketState();

        state.changeStateRun();
        Assert.assertEquals(PinpointServerSocketStateCode.RUN, state.getCurrentState());

        state.changeStateRunDuplexCommunication();
        Assert.assertEquals(PinpointServerSocketStateCode.RUN_DUPLEX_COMMUNICATION, state.getCurrentState());

        state.changeStateBeingShutdown();
        Assert.assertEquals(PinpointServerSocketStateCode.BEING_SHUTDOWN, state.getCurrentState());

        state.changeStateShutdown();
        Assert.assertEquals(PinpointServerSocketStateCode.SHUTDOWN, state.getCurrentState());
    }

    // basic type of connection's lifecycle between peers.
    // RUN_DUPLEX_COMMUNICATION -> RUN_DUPLEX_COMMUNICATION -> BEING_SHUTDOWN -> connection closed
    @Test
    public void changeStateTest2() {
        PinpointServerSocketState state = new PinpointServerSocketState();

        state.changeStateRunDuplexCommunication();
        Assert.assertEquals(PinpointServerSocketStateCode.RUN_DUPLEX_COMMUNICATION, state.getCurrentState());

        state.changeStateBeingShutdown();
        Assert.assertEquals(PinpointServerSocketStateCode.BEING_SHUTDOWN, state.getCurrentState());

        state.changeStateShutdown();
        Assert.assertEquals(PinpointServerSocketStateCode.SHUTDOWN, state.getCurrentState());
    }

    @Test
    public void changeStateTest3() {
        PinpointServerSocketState state = new PinpointServerSocketState();

        state.changeStateRun();
        Assert.assertEquals(PinpointServerSocketStateCode.RUN, state.getCurrentState());

        state.changeStateUnexpectedShutdown();
        Assert.assertEquals(PinpointServerSocketStateCode.UNEXPECTED_SHUTDOWN, state.getCurrentState());
    }

    @Test
    public void changeStateTest4() {
        PinpointServerSocketState state = new PinpointServerSocketState();

        state.changeStateRun();
        Assert.assertEquals(PinpointServerSocketStateCode.RUN, state.getCurrentState());

        state.changeStateShutdown();
        Assert.assertEquals(PinpointServerSocketStateCode.SHUTDOWN, state.getCurrentState());
    }

    @Test
    public void changeStateTest5() {
        PinpointServerSocketState state = new PinpointServerSocketState();

        state.changeStateRunDuplexCommunication();
        Assert.assertEquals(PinpointServerSocketStateCode.RUN_DUPLEX_COMMUNICATION, state.getCurrentState());

        state.changeStateShutdown();
        Assert.assertEquals(PinpointServerSocketStateCode.SHUTDOWN, state.getCurrentState());
    }

    @Test(expected = IllegalStateException.class)
    public void invalidChangeStateTest1() {
        PinpointServerSocketState state = new PinpointServerSocketState();

        state.changeStateBeingShutdown();
    }

    @Test(expected = IllegalStateException.class)
    public void invalidChangeStateTest2() {
        PinpointServerSocketState state = new PinpointServerSocketState();

        state.changeStateRunDuplexCommunication();
        Assert.assertEquals(PinpointServerSocketStateCode.RUN_DUPLEX_COMMUNICATION, state.getCurrentState());

        state.changeStateBeingShutdown();
        Assert.assertEquals(PinpointServerSocketStateCode.BEING_SHUTDOWN, state.getCurrentState());

        state.changeStateUnexpectedShutdown();
    }

}
