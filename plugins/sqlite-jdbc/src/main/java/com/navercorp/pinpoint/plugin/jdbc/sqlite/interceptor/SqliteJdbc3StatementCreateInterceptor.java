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
package com.navercorp.pinpoint.plugin.jdbc.sqlite.interceptor;

import com.navercorp.pinpoint.bootstrap.context.TraceContext;
import com.navercorp.pinpoint.bootstrap.interceptor.annotation.TargetMethod;
import com.navercorp.pinpoint.bootstrap.interceptor.annotation.TargetMethods;
import com.navercorp.pinpoint.bootstrap.plugin.jdbc.interceptor.StatementCreateInterceptor;

/**
 * @author barney
 *
 */
@TargetMethods({
    @TargetMethod(name="createStatement"),
    @TargetMethod(name="createStatement", paramTypes={"int", "int"}),
})
public class SqliteJdbc3StatementCreateInterceptor extends StatementCreateInterceptor {

    public SqliteJdbc3StatementCreateInterceptor(TraceContext traceContext) {
        super(traceContext);
    }

}
