package com.navercorp.pinpoint.web;

import com.navercorp.pinpoint.collector.CollectorStarter;
import com.navercorp.pinpoint.common.server.util.ServerBootLogger;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.transaction.TransactionAutoConfiguration;
import org.springframework.context.annotation.ImportResource;

@SpringBootConfiguration
@EnableAutoConfiguration(exclude = {DataSourceAutoConfiguration.class, TransactionAutoConfiguration.class})
@ImportResource({ "classpath:applicationContext-collector.xml", "classpath:servlet-context-collector.xml"})
public class Hbase2CollectorApp {
    private static final ServerBootLogger logger = ServerBootLogger.getLogger(Hbase2CollectorApp.class);


    public static void main(String[] args) {
        try {
            CollectorStarter starter = new CollectorStarter(Hbase2CollectorApp.class);
            starter.start(args);
        } catch (Exception exception) {
            logger.error("[CollectorApp] could not launch app.", exception);
        }
    }

}
