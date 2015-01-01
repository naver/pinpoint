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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author koo.taejin
 */
public class PinpointServerSocketState {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private PinpointServerSocketStateCode beforeState = PinpointServerSocketStateCode.NONE;
    private PinpointServerSocketStateCode currentState = PinpointServerSocketStateCode.NONE;

    private synchronized boolean setSessionState(PinpointServerSocketStateCode state) {
        boolean enable = this.currentState.canChangeState(state);

        if (enable) {
            this.beforeState = this.currentState;
            this.currentState = state;
            return true;
        } else if (PinpointServerSocketStateCode.isFinished(this.currentState)) {
            // if state can't be changed, just log.
            // no problem because the state of socket has been already closed.
            PinpointServerSocketStateCode checkBefore = this.beforeState;
            PinpointServerSocketStateCode checkCurrent = this.currentState;

            String errorMessage = cannotChangeMessage(checkBefore, checkCurrent, state);

            this.beforeState = this.currentState;
            this.currentState = PinpointServerSocketStateCode.ERROR_ILLEGAL_STATE_CHANGE;

            logger.warn(errorMessage);
            return false;
        } else {
            PinpointServerSocketStateCode checkBefore = this.beforeState;
            PinpointServerSocketStateCode checkCurrent = this.currentState;

            String errorMessage = errorMessage(checkBefore, checkCurrent, state);

            this.beforeState = this.currentState;
            this.currentState = PinpointServerSocketStateCode.ERROR_ILLEGAL_STATE_CHANGE;

            logger.warn(errorMessage);

            throw new IllegalStateException(errorMessage);
        }
    }

    public boolean changeStateRun() {
        return setSessionState(PinpointServerSocketStateCode.RUN);
    }

    public boolean changeStateRunDuplexCommunication() {
        return setSessionState(PinpointServerSocketStateCode.RUN_DUPLEX_COMMUNICATION);
    }

    public boolean changeStateBeingShutdown() {
        return setSessionState(PinpointServerSocketStateCode.BEING_SHUTDOWN);
    }

    public boolean changeStateShutdown() {
        return setSessionState(PinpointServerSocketStateCode.SHUTDOWN);
    }

    public boolean changeStateUnexpectedShutdown() {
        return setSessionState(PinpointServerSocketStateCode.UNEXPECTED_SHUTDOWN);
    }

    public boolean changeStateUnkownError() {
        return setSessionState(PinpointServerSocketStateCode.ERROR_UNKOWN);
    }

    private String errorMessage(PinpointServerSocketStateCode checkBefore, PinpointServerSocketStateCode checkCurrent, PinpointServerSocketStateCode nextState) {
        return "Invalid State(current:" + checkCurrent + " before:" + checkBefore + " next:" + nextState + ")";
    }

    private String cannotChangeMessage(PinpointServerSocketStateCode checkBefore, PinpointServerSocketStateCode checkCurrent, PinpointServerSocketStateCode nextState) {
        return "Can not change State(current:" + checkCurrent + " before:" + checkBefore + " next:" + nextState + ")";
    }

    public synchronized PinpointServerSocketStateCode getCurrentState() {
        return currentState;
    }

}
