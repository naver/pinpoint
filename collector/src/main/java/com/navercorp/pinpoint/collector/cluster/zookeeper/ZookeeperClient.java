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

package com.navercorp.pinpoint.collector.cluster.zookeeper;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.KeeperException.Code;
import org.apache.zookeeper.ZooDefs.Ids;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.Stat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.navercorp.pinpoint.collector.cluster.zookeeper.exception.AuthException;
import com.navercorp.pinpoint.collector.cluster.zookeeper.exception.BadOperationException;
import com.navercorp.pinpoint.collector.cluster.zookeeper.exception.ConnectionException;
import com.navercorp.pinpoint.collector.cluster.zookeeper.exception.NoNodeException;
import com.navercorp.pinpoint.collector.cluster.zookeeper.exception.PinpointZookeeperException;
import com.navercorp.pinpoint.collector.cluster.zookeeper.exception.TimeoutException;
import com.navercorp.pinpoint.collector.cluster.zookeeper.exception.UnknownException;

/**
 * @author koo.taejin
 */
public class ZookeeperClient {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    // Zookeeper clients are thread-safe
    private final ZooKeeper zookeeper;
    private final AtomicBoolean clientState = new AtomicBoolean(true);

    private final ZookeeperEventWatcher watcher;

    public ZookeeperClient(String hostPort, int sessionTimeout, ZookeeperEventWatcher watcher) throws KeeperException, IOException, InterruptedException {
        this.watcher = watcher;
        zookeeper = new ZooKeeper(hostPort, sessionTimeout, this.watcher); // server
    }

    /**
     * do not create the final node in the given path.
     *
     * @throws PinpointZookeeperException
     * @throws InterruptedException
     */
    public void createPath(String path) throws PinpointZookeeperException, InterruptedException {
        createPath(path, false);
    }

    public void createPath(String path, boolean createEndNode) throws PinpointZookeeperException, InterruptedException {
        checkState();

        int pos = 1;
        do {
            pos = path.indexOf('/', pos + 1);

            if (pos == -1) {
                pos = path.length();
            }

            try {
                if (pos == path.length()) {
                    if (!createEndNode) {
                        return;
                    }
                }

                String subPath = path.substring(0, pos);
                if (zookeeper.exists(subPath, false) != null) {
                    continue;
                }

                zookeeper.create(subPath, new byte[0], Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
            } catch (KeeperException exception) {
                if (exception.code() != Code.NODEEXISTS) {
                    handleException(exception);
                }
            }

        } while (pos < path.length());
    }

    public String createNode(String znodePath, byte[] data) throws PinpointZookeeperException, InterruptedException {
        checkState();

        try {
            if (zookeeper.exists(znodePath, false) != null) {
                return znodePath;
            }

            String pathName = zookeeper.create(znodePath, data, Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL);
            return pathName;
        } catch (KeeperException exception) {
            handleException(exception);
        }
        return znodePath;
    }

    public byte[] getData(String path) throws PinpointZookeeperException, InterruptedException {
        checkState();

        try {
            return zookeeper.getData(path, false, null);
        } catch (KeeperException exception) {
            handleException(exception);
        }

        throw new UnknownException("UnknownException.");
    }

    public void setData(String path, byte[] data) throws PinpointZookeeperException, InterruptedException {
        checkState();

        try {
            if (zookeeper.exists(path, false) == null) {
                return;
            }

            zookeeper.setData(path, data, -1);
        } catch (KeeperException exception) {
            handleException(exception);
        }
    }

    public void delete(String path) throws PinpointZookeeperException, InterruptedException {
        checkState();

        try {
            zookeeper.delete(path, -1);
        } catch (KeeperException exception) {
            if (exception.code() != Code.NONODE) {
                handleException(exception);
            }
        }
    }

    public boolean exists(String path) throws PinpointZookeeperException, InterruptedException {
        checkState();

        try {
            Stat stat = zookeeper.exists(path, false);
            if (stat == null) {
                return false;
            }
        } catch (KeeperException exception) {
            if (exception.code() != Code.NODEEXISTS) {
                handleException(exception);
            }
        }
        return true;
    }

    private void checkState() throws PinpointZookeeperException {
        if (!isConnected()) {
            throw new ConnectionException("instance must be connected.");
        }
    }

    public boolean isConnected() {
        if (!watcher.isConnected() || !clientState.get()) {
            return false;
        }

        return true;
    }

    public List<String> getChildrenNode(String path, boolean watch) throws PinpointZookeeperException, InterruptedException {
        checkState();

        try {
            List<String> childNodeList = zookeeper.getChildren(path, watch, null);

            logger.info("ChildNode List = {}", childNodeList);
            return childNodeList;
        } catch (KeeperException exception) {
            if (exception.code() != Code.NONODE) {
                handleException(exception);
            }
        }

        return Collections.emptyList();
    }

//    public byte[] getData(String path) throws KeeperException, InterruptedException {
//        checkState();
//
//        return zookeeper.getData(path, false, null);
//    }
//
//    public List<String> getChildrenNode(String path) throws KeeperException, InterruptedException {
//        checkState();
//
//        List<String> childNodeList = zookeeper.getChildren(path, false);
//        logger.info("ChildNode List = {}", childNodeList);
//        return childNodeList;
//    }

    private void handleException(KeeperException keeperException) throws PinpointZookeeperException {
        switch (keeperException.code()) {
            case CONNECTIONLOSS:
            case SESSIONEXPIRED:
                throw new ConnectionException(keeperException.getMessage(), keeperException);
            case AUTHFAILED:
            case INVALIDACL:
            case NOAUTH:
                throw new AuthException(keeperException.getMessage(), keeperException);
            case BADARGUMENTS:
            case BADVERSION:
            case NOCHILDRENFOREPHEMERALS:
            case NOTEMPTY:
            case NODEEXISTS:
                throw new BadOperationException(keeperException.getMessage(), keeperException);
            case NONODE:
                throw new NoNodeException(keeperException.getMessage(), keeperException);
            case OPERATIONTIMEOUT:
                throw new TimeoutException(keeperException.getMessage(), keeperException);
            default:
                throw new UnknownException(keeperException.getMessage(), keeperException);
            }
    }

    public void close() {
        if (clientState.compareAndSet(true, false)) {
            if (zookeeper != null) {
                try {
                    zookeeper.close();
                } catch (InterruptedException ignore) {
                    logger.info("Interrupted zookeeper.close(). Caused:" + ignore.getMessage(), ignore);
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

}
