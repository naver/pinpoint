package com.navercorp.pinpoint.plugin.mongodb;

import com.mongodb.MongoException;
import com.navercorp.pinpoint.plugin.mongo.MongoTransforms;
import com.navercorp.pinpoint.pluginit.utils.AgentPath;
import com.navercorp.pinpoint.test.plugin.Dependency;
import com.navercorp.pinpoint.test.plugin.ImportPlugin;
import com.navercorp.pinpoint.test.plugin.JvmVersion;
import com.navercorp.pinpoint.test.plugin.PinpointAgent;
import com.navercorp.pinpoint.test.plugin.PinpointPluginTestSuite;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(PinpointPluginTestSuite.class)
@PinpointAgent(AgentPath.PATH)
@JvmVersion(8)
@ImportPlugin({"com.navercorp.pinpoint:pinpoint-mongodb-driver-plugin"})
@Dependency({"org.mongodb:mongo-java-driver:[1.0,1.4]"})
public class MongoDBVersionCheckV2_IT {
    @Test
    public void test() {
        ClassLoader classLoader = MongoException.class.getClassLoader();
        int mongoMajorVersion = MongoTransforms.AbstractMongoTransformCallback.getMongoMajorVersion(classLoader);
        Assert.assertEquals("detect version v1 failed", 1, mongoMajorVersion);
    }
}
