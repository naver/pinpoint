/**
 * Copyright 2014 NAVER Corp.
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
package com.navercorp.pinpoint.plugin.jdbc.cubrid;

import java.security.ProtectionDomain;

import com.navercorp.pinpoint.bootstrap.instrument.InstrumentClass;
import com.navercorp.pinpoint.bootstrap.instrument.InstrumentException;
import com.navercorp.pinpoint.bootstrap.instrument.Instrumentor;
import com.navercorp.pinpoint.bootstrap.instrument.transformer.PinpointClassFileTransformer;
import com.navercorp.pinpoint.bootstrap.interceptor.group.ExecutionPolicy;
import com.navercorp.pinpoint.bootstrap.interceptor.group.InterceptorGroup;
import com.navercorp.pinpoint.bootstrap.plugin.ProfilerPlugin;
import com.navercorp.pinpoint.bootstrap.plugin.ProfilerPluginSetupContext;

import static com.navercorp.pinpoint.common.util.VarArgs.va;

/**
 * @author Jongho Moon
 *
 */
public class CubridPlugin implements ProfilerPlugin {

    @Override
    public void setup(ProfilerPluginSetupContext context) {
        CubridConfig config = new CubridConfig(context.getConfig());
        
        addCUBRIDConnectionTransformer(context, config);
        addCUBRIDDriverTransformer(context);
        addCUBRIDPreparedStatementTransformer(context, config);
        addCUBRIDStatementTransformer(context);
    }

    
    private void addCUBRIDConnectionTransformer(ProfilerPluginSetupContext setupContext, final CubridConfig config) {
        setupContext.addClassFileTransformer("cubrid.jdbc.driver.CUBRIDConnection", new PinpointClassFileTransformer() {
            
            @Override
            public byte[] transform(Instrumentor instrumentContext, ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws InstrumentException {
                InstrumentClass target = instrumentContext.getInstrumentClass(loader, className, classfileBuffer);
                target.addField("com.navercorp.pinpoint.bootstrap.plugin.jdbc.DatabaseInfoAccessor");

                InterceptorGroup group = instrumentContext.getInterceptorGroup(CubridConstants.GROUP_CUBRID);
                        
                target.addGroupedInterceptor("com.navercorp.pinpoint.bootstrap.plugin.jdbc.interceptor.ConnectionCloseInterceptor", group);
                target.addGroupedInterceptor("com.navercorp.pinpoint.bootstrap.plugin.jdbc.interceptor.StatementCreateInterceptor", group);
                target.addGroupedInterceptor("com.navercorp.pinpoint.bootstrap.plugin.jdbc.interceptor.PreparedStatementCreateInterceptor", group);
                
                if (config.isProfileSetAutoCommit()) {
                    target.addGroupedInterceptor("com.navercorp.pinpoint.bootstrap.plugin.jdbc.interceptor.TransactionSetAutoCommitInterceptor", group);
                }
                
                if (config.isProfileCommit()) {
                    target.addGroupedInterceptor("com.navercorp.pinpoint.bootstrap.plugin.jdbc.interceptor.TransactionCommitInterceptor", group);
                }
                
                if (config.isProfileRollback()) {
                    target.addGroupedInterceptor("com.navercorp.pinpoint.bootstrap.plugin.jdbc.interceptor.TransactionRollbackInterceptor", group);
                }
                
                return target.toBytecode();
            }
        });
    }
    
    private void addCUBRIDDriverTransformer(ProfilerPluginSetupContext setupContext) {
        setupContext.addClassFileTransformer("cubrid.jdbc.driver.CUBRIDDriver", new PinpointClassFileTransformer() {
            
            @Override
            public byte[] transform(Instrumentor instrumentContext, ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws InstrumentException {
                InstrumentClass target = instrumentContext.getInstrumentClass(loader, className, classfileBuffer);
                InterceptorGroup group = instrumentContext.getInterceptorGroup(CubridConstants.GROUP_CUBRID);
                
                target.addGroupedInterceptor("com.navercorp.pinpoint.bootstrap.plugin.jdbc.interceptor.DriverConnectInterceptor", va(new CubridJdbcUrlParser()), group, ExecutionPolicy.ALWAYS);
                
                return target.toBytecode();
            }
        });
    }
    
    private void addCUBRIDPreparedStatementTransformer(ProfilerPluginSetupContext setupContext, final CubridConfig config) {
        setupContext.addClassFileTransformer("cubrid.jdbc.driver.CUBRIDPreparedStatement", new PinpointClassFileTransformer() {
            
            @Override
            public byte[] transform(Instrumentor instrumentContext, ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws InstrumentException {
                InstrumentClass target = instrumentContext.getInstrumentClass(loader, className, classfileBuffer);
                
                target.addField("com.navercorp.pinpoint.bootstrap.plugin.jdbc.DatabaseInfoAccessor");
                target.addField("com.navercorp.pinpoint.bootstrap.plugin.jdbc.ParsingResultAccessor");
                target.addField("com.navercorp.pinpoint.bootstrap.plugin.jdbc.BindValueAccessor", "new java.util.HashMap()");
                
                int maxBindValueSize = config.getMaxSqlBindValueSize();
                InterceptorGroup group = instrumentContext.getInterceptorGroup(CubridConstants.GROUP_CUBRID);
                
                target.addGroupedInterceptor("com.navercorp.pinpoint.bootstrap.plugin.jdbc.interceptor.PreparedStatementExecuteQueryInterceptor", va(maxBindValueSize), group);
                target.addGroupedInterceptor("com.navercorp.pinpoint.bootstrap.plugin.jdbc.interceptor.PreparedStatementBindVariableInterceptor", group);
                
                return target.toBytecode();
            }
        });
    }
    
    private void addCUBRIDStatementTransformer(ProfilerPluginSetupContext setupContext) {
        setupContext.addClassFileTransformer("cubrid.jdbc.driver.CUBRIDStatement", new PinpointClassFileTransformer() {
            
            @Override
            public byte[] transform(Instrumentor instrumentContext, ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws InstrumentException {
                InstrumentClass target = instrumentContext.getInstrumentClass(loader, className, classfileBuffer);
                
                target.addField("com.navercorp.pinpoint.bootstrap.plugin.jdbc.DatabaseInfoAccessor");
                
                InterceptorGroup group = instrumentContext.getInterceptorGroup(CubridConstants.GROUP_CUBRID);

                target.addGroupedInterceptor("com.navercorp.pinpoint.bootstrap.plugin.jdbc.interceptor.StatementExecuteQueryInterceptor", group);
                target.addGroupedInterceptor("com.navercorp.pinpoint.bootstrap.plugin.jdbc.interceptor.StatementExecuteUpdateInterceptor", group);
                
                return target.toBytecode();
            }
        });
    }
}
