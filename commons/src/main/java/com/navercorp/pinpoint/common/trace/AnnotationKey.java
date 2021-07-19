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

package com.navercorp.pinpoint.common.trace;

import static com.navercorp.pinpoint.common.trace.AnnotationKeyProperty.ERROR_API_METADATA;
import static com.navercorp.pinpoint.common.trace.AnnotationKeyProperty.VIEW_IN_RECORD_SET;

/**
 * AnnotationKey sandbox is from 900 to 999. These values will not be assigned to anything.
 *
 * <table>
 * <tr><td>-1</td><td>args[0]</td></tr>
 * <tr><td>-2</td><td>args[1]</td></tr>
 * <tr><td>-3</td><td>args[2]</td></tr>
 * <tr><td>-4</td><td>args[3]</td></tr>
 * <tr><td>-5</td><td>args[4]</td></tr>
 * <tr><td>-6</td><td>args[5]</td></tr>
 * <tr><td>-7</td><td>args[6]</td></tr>
 * <tr><td>-8</td><td>args[7]</td></tr>
 * <tr><td>-9</td><td>args[8]</td></tr>
 * <tr><td>-10</td><td>args[9]</td></tr>
 * <tr><td>-11</td><td>args[N]</td></tr>
 * <tr><td>-30</td><td>cached_args[0]</td></tr>
 * <tr><td>-31</td><td>cached_args[1]</td></tr>
 * <tr><td>-32</td><td>cached_args[2]</td></tr>
 * <tr><td>-33</td><td>cached_args[3]</td></tr>
 * <tr><td>-34</td><td>cached_args[4]</td></tr>
 * <tr><td>-35</td><td>cached_args[5]</td></tr>
 * <tr><td>-36</td><td>cached_args[6]</td></tr>
 * <tr><td>-37</td><td>cached_args[7]</td></tr>
 * <tr><td>-38</td><td>cached_args[8]</td></tr>
 * <tr><td>-39</td><td>cached_args[9]</td></tr>
 * <tr><td>-40</td><td>cached_args[N]</td></tr>
 * <tr><td>-50</td><td>Exception</td></tr>
 * <tr><td>-51</td><td>ExceptionClass</td></tr>
 * <tr><td>-100</td><td>Asynchronous Invocation</td></tr>
 * <tr><td>-9999</td><td>UNKNOWN</td></tr>
 *
 * <tr><td>12</td><td>API</td></tr>
 * <tr><td>13</td><td>API_METADATA</td></tr>
 * <tr><td>14</td><td>RETURN_DATA</td></tr>
 * <tr><td>15</td><td><i>RESERVED</i></td></tr>
 * <tr><td>16</td><td><i>RESERVED</i></td></tr>
 * <tr><td>17</td><td><i>RESERVED</i></td></tr>
 * <tr><td>20</td><td>SQL-ID</td></tr>
 * <tr><td>21</td><td>SQL</td></tr>
 * <tr><td>22</td><td>SQL-METADATA</td></tr>
 * <tr><td>23</td><td>SQL-PARAM</td></tr>
 * <tr><td>24</td><td>SQL-BindValue</td></tr>
 * <tr><td>30</td><td>STRING_ID</td></tr>
 * <tr><td>40</td><td>http.url</td></tr>
 * <tr><td>41</td><td>http.param</td></tr>
 * <tr><td>42</td><td>http.entity</td></tr>
 * <tr><td>45</td><td>http.cookie</td></tr>
 * <tr><td>46</td><td>http.status.code</td></tr>
 * <tr><td>48</td><td>http.internal.display</td></tr>
 * <tr><td>49</td><td>http.io</td></tr>
 * <tr><td>50</td><td>arcus.command</td></tr>
 * <tr><td>60</td><td><i>RESERVED</i></td></tr>
 * <tr><td>61</td><td><i>RESERVED</i></td></tr>
 * <tr><td>62</td><td><i>RESERVED</i></td></tr>
 * <tr><td>70</td><td><i>RESERVED</i></td></tr>
 * <tr><td>71</td><td><i>RESERVED</i></td></tr>
 * <tr><td>72</td><td><i>RESERVED</i></td></tr>
 * <tr><td>73</td><td><i>RESERVED</i></td></tr>
 * <tr><td>80</td><td>thrift.url</td></tr>
 * <tr><td>81</td><td>thrift.args</td></tr>
 * <tr><td>82</td><td>thrift.result</td></tr>
 * <tr><td>90</td><td>dubbo.args</td></tr>
 * <tr><td>91</td><td>dubbo.result</td></tr>
 * <tr><td>110</td><td></td>hystrix.command</tr>
 * <tr><td>111</td><td></td>hystrix.command.execution</tr>
 * <tr><td>112</td><td></td>hystrix.command.fallback.cause</tr>
 * <tr><td>113</td><td></td>hystrix.command.exception</tr>
 * <tr><td>115</td><td></td>hystrix.command.key</tr>
 * <tr><td>116</td><td></td>hystrix.command.group.key</tr>
 * <tr><td>117</td><td></td>hystrix.thread.pool.key</tr>
 * <tr><td>118</td><td></td>hystrix.collapser.key</tr>
 * <tr><td>120</td><td>netty.address</td></tr>
 * <tr><td>130</td><td>rabbitmq.properties</td></tr>
 * <tr><td>131</td><td>rabbitmq.body</td></tr>
 * <tr><td>132</td><td>rabbitmq.properties</td></tr>
 * <tr><td>133</td><td>rabbitmq.body</td></tr>
 * <tr><td>150</td><td>mongo.json.data</td></tr>
 * <tr><td>151</td><td>mongo.collection.info</td></tr>
 * <tr><td>152</td><td>mongo.collection.option</td></tr>
 * <tr><td>153</td><td>mongo.json</td></tr>
 * <tr><td>154</td><td>mongo.json.bindvalue</td></tr>
 * <tr><td>160</td><td>grpc.status</td></tr>
 * <tr><td>171</td><td>es.args</td></tr>
 * <tr><td>172</td><td>es.url</td></tr>
 * <tr><td>173</td><td>es.dsl</td></tr>
 * <tr><td>174</td><td>es.action</td></tr>
 * <tr><td>175</td><td>es.responseHandle</td></tr>
 * <tr><td>176</td><td>es.version</td></tr>
 * <tr><td>180</td><td>process.command</td></tr>
 * <tr><td>181</td><td>process.pid</td></tr>
 *
 * <tr><td><s>200</s></td><td><s>cxf.operation</s></td></tr>
 * <tr><td><s>201</s></td><td><s>cxf.args</s></td></tr>
 * <tr><td>203</td><td>cxf.address</td></tr>
 * <tr><td>204</td><td>cxf.response.code</td></tr>
 * <tr><td>205</td><td>cxf.encoding</td></tr>
 * <tr><td>206</td><td>cxf.http.method</td></tr>
 * <tr><td>207</td><td>cxf.content.type</td></tr>
 * <tr><td>208</td><td>cxf.headers</td></tr>
 * <tr><td>209</td><td>cxf.messages</td></tr>
 * <tr><td>210</td><td>cxf.payload</td></tr>
 * <tr><td>300</td><td>PROXY_HTTP_HEADER</td></tr>
 * <tr><td>310</td><td>REDIS.IO</td></tr>
 * <tr><td>320</td><td>hbase.client.params</td></tr>
 * <tr><td>330</td><td>memory.usage</td></tr>
 * <tr><td>923</td><td>marker.message</td></tr>
 * <tr><td>9000</td><td>gson.json.length</td></tr>
 * <tr><td>9001</td><td>jackson.json.length</td></tr>
 * <tr><td>9002</td><td>json-lib.json.length</td></tr>
 * <tr><td>9003</td><td>fastjson.json.length</td></tr>
 * <tr><td>10015</td><td>API-TAG</td></tr>
 * <tr><td>10000010</td><td>API-METADATA-ERROR</td></tr>
 * <tr><td>10000011</td><td>API-METADATA-AGENT-INFO-NOT-FOUND</td></tr>
 * <tr><td>10000012</td><td>API-METADATA-IDENTIFIER-CHECK_ERROR</td></tr>
 * <tr><td>10000013</td><td>API-METADATA-NOT-FOUND</td></tr>
 * <tr><td>10000014</td><td>API-METADATA-DID-COLLSION</td></tr>
 * </table>
 *
 * @author netspider
 * @author emeroad
 * @author Jongho Moon
 */
public interface AnnotationKey {
    int MAX_ARGS_SIZE = 10;

    String getName();

    int getCode();

    boolean isErrorApiMetadata();

    boolean isViewInRecordSet();


    // because of using variable-length encoding,
    // a small number should be used mainly for data contained in network packets and a big number for internal used code.

//    2147483647
//    -2147483648

//    @Deprecated  // moved apiId to spanEvent and span. dump by int
//    API_DID(10, "API-DID"),
//    @Deprecated  // you should remove static API code. Use only API-DID. dump by int
//    API_ID(11, "API-ID"),
    // Dump api by string.
    AnnotationKey API = AnnotationKeyFactory.of(12, "API");
    AnnotationKey API_METADATA = AnnotationKeyFactory.of(13, "API-METADATA");
    AnnotationKey RETURN_DATA = AnnotationKeyFactory.of(14, "RETURN_DATA", VIEW_IN_RECORD_SET);
    AnnotationKey API_TAG = AnnotationKeyFactory.of(10015, "API-TAG");

    // when you don't know the correct cause of errors.
    AnnotationKey ERROR_API_METADATA_ERROR = AnnotationKeyFactory.of(10000010, "API-METADATA-ERROR", ERROR_API_METADATA);
    // when agentInfo not found
    AnnotationKey ERROR_API_METADATA_AGENT_INFO_NOT_FOUND = AnnotationKeyFactory.of(10000011, "API-METADATA-AGENT-INFO-NOT-FOUND", ERROR_API_METADATA);
    // when checksum is not correct even if agentInfo exists
    AnnotationKey ERROR_API_METADATA_IDENTIFIER_CHECK_ERROR = AnnotationKeyFactory.of(10000012, "API-METADATA-IDENTIFIER-CHECK_ERROR", ERROR_API_METADATA);
    // when  meta data itself not found
    AnnotationKey ERROR_API_METADATA_NOT_FOUND = AnnotationKeyFactory.of(10000013, "API-METADATA-NOT-FOUND", ERROR_API_METADATA);
    // when the same hashId of meta data exists
    AnnotationKey ERROR_API_METADATA_DID_COLLSION = AnnotationKeyFactory.of(10000014, "API-METADATA-DID-COLLSION", ERROR_API_METADATA);

    // it's not clear to handle a error code.  so ApiMetaDataError with searching ERROR_API_META_DATA has been used.
    // automatically generated id

    AnnotationKey SQL_ID = AnnotationKeyFactory.of(20, "SQL-ID");
    AnnotationKey SQL = AnnotationKeyFactory.of(21, "SQL", VIEW_IN_RECORD_SET);
    AnnotationKey SQL_METADATA = AnnotationKeyFactory.of(22, "SQL-METADATA");
    AnnotationKey SQL_PARAM = AnnotationKeyFactory.of(23, "SQL-PARAM");
    AnnotationKey SQL_BINDVALUE = AnnotationKeyFactory.of(24, "SQL-BindValue", VIEW_IN_RECORD_SET);

    AnnotationKey STRING_ID = AnnotationKeyFactory.of(30, "STRING_ID");

    // HTTP_URL is replaced by argument. So viewInRecordSet parameter name is not true.
    AnnotationKey HTTP_URL = AnnotationKeyFactory.of(40, "http.url");
    AnnotationKey HTTP_PARAM = AnnotationKeyFactory.of(41, "http.param", VIEW_IN_RECORD_SET);
    AnnotationKey HTTP_PARAM_ENTITY = AnnotationKeyFactory.of(42, "http.entity", VIEW_IN_RECORD_SET);
    AnnotationKey HTTP_COOKIE = AnnotationKeyFactory.of(45, "http.cookie", VIEW_IN_RECORD_SET);
    AnnotationKey HTTP_STATUS_CODE = AnnotationKeyFactory.of(46, "http.status.code", VIEW_IN_RECORD_SET);
    AnnotationKey HTTP_REQUEST_HEADER = AnnotationKeyFactory.of(47, "http.req.header", VIEW_IN_RECORD_SET);
    AnnotationKey HTTP_INTERNAL_DISPLAY = AnnotationKeyFactory.of(48, "http.internal.display");
    AnnotationKey HTTP_IO = AnnotationKeyFactory.of(49, "http.io", VIEW_IN_RECORD_SET);
    AnnotationKey HTTP_RESPONSE_HEADER = AnnotationKeyFactory.of(50, "http.resp.header", VIEW_IN_RECORD_SET);
    // post method parameter of httpclient

    AnnotationKey MESSAGE_QUEUE_URI = AnnotationKeyFactory.of(100, "message.queue.url");

    AnnotationKey ARGS0 = AnnotationKeyFactory.of(-1, "args[0]");
    AnnotationKey ARGS1 = AnnotationKeyFactory.of(-2, "args[1]");
    AnnotationKey ARGS2 = AnnotationKeyFactory.of(-3, "args[2]");
    AnnotationKey ARGS3 = AnnotationKeyFactory.of(-4, "args[3]");
    AnnotationKey ARGS4 = AnnotationKeyFactory.of(-5, "args[4]");
    AnnotationKey ARGS5 = AnnotationKeyFactory.of(-6, "args[5]");
    AnnotationKey ARGS6 = AnnotationKeyFactory.of(-7, "args[6]");
    AnnotationKey ARGS7 = AnnotationKeyFactory.of(-8, "args[7]");
    AnnotationKey ARGS8 = AnnotationKeyFactory.of(-9, "args[8]");
    AnnotationKey ARGS9 = AnnotationKeyFactory.of(-10, "args[9]");
    AnnotationKey ARGSN = AnnotationKeyFactory.of(-11, "args[N]");

    AnnotationKey CACHE_ARGS0 = AnnotationKeyFactory.of(-30, "cached_args[0]");
    AnnotationKey CACHE_ARGS1 = AnnotationKeyFactory.of(-31, "cached_args[1]");
    AnnotationKey CACHE_ARGS2 = AnnotationKeyFactory.of(-32, "cached_args[2]");
    AnnotationKey CACHE_ARGS3 = AnnotationKeyFactory.of(-33, "cached_args[3]");
    AnnotationKey CACHE_ARGS4 = AnnotationKeyFactory.of(-34, "cached_args[4]");
    AnnotationKey CACHE_ARGS5 = AnnotationKeyFactory.of(-35, "cached_args[5]");
    AnnotationKey CACHE_ARGS6 = AnnotationKeyFactory.of(-36, "cached_args[6]");
    AnnotationKey CACHE_ARGS7 = AnnotationKeyFactory.of(-37, "cached_args[7]");
    AnnotationKey CACHE_ARGS8 = AnnotationKeyFactory.of(-38, "cached_args[8]");
    AnnotationKey CACHE_ARGS9 = AnnotationKeyFactory.of(-39, "cached_args[9]");
    AnnotationKey CACHE_ARGSN = AnnotationKeyFactory.of(-40, "cached_args[N]");
    @Deprecated
    AnnotationKey EXCEPTION = AnnotationKeyFactory.of(-50, "Exception", VIEW_IN_RECORD_SET);
    @Deprecated
    AnnotationKey EXCEPTION_CLASS = AnnotationKeyFactory.of(-51, "ExceptionClass");
    AnnotationKey UNKNOWN = AnnotationKeyFactory.of(-9999, "UNKNOWN");

    AnnotationKey ASYNC = AnnotationKeyFactory.of(-100, "Asynchronous Invocation", VIEW_IN_RECORD_SET);

    AnnotationKey PROXY_HTTP_HEADER = AnnotationKeyFactory.of(300, "PROXY_HTTP_HEADER", VIEW_IN_RECORD_SET);
    AnnotationKey REDIS_IO = AnnotationKeyFactory.of(310, "redis.io");
    AnnotationKey MEMORY_USAGE = AnnotationKeyFactory.of(330, "memory.usage", VIEW_IN_RECORD_SET);
}
