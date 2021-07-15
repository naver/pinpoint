package com.navercorp.pinpoint.web;

import com.navercorp.pinpoint.common.server.util.ServerBootLogger;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.batch.BatchAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.transaction.TransactionAutoConfiguration;
import org.springframework.context.annotation.ImportResource;

@SpringBootConfiguration
@EnableAutoConfiguration(exclude = {DataSourceAutoConfiguration.class, TransactionAutoConfiguration.class,
        SecurityAutoConfiguration.class,  BatchAutoConfiguration.class})
@ImportResource({ "classpath:applicationContext-web.xml", "classpath:servlet-context-web.xml"})
public class Hbase2WebApp {

    private static final ServerBootLogger logger = ServerBootLogger.getLogger(WebApp.class);


    public static void main(String[] args) {
        try {
            WebStarter starter = new WebStarter(WebApp.class, WebServerConfig.class, WebMvcConfig.class, PinpointBasicLoginConfig.class);
            starter.start(args);
        } catch (Exception exception) {
            logger.error("[WebApp] could not launch app.", exception);
        }
    }

}
