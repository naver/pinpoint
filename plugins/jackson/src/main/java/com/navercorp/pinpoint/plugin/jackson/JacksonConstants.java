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
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 implied.
 * See the License for the specific language governing permissions
 and
 * limitations under the License.
 */
package com.navercorp.pinpoint.plugin.jackson;

import static com.navercorp.pinpoint.common.trace.HistogramSchema.*;

import com.navercorp.pinpoint.common.trace.AnnotationKey;
import com.navercorp.pinpoint.common.trace.ServiceType;

/**
 *  * @author Sungkook Kim
 *   *
 *    */
public interface JacksonConstants {
    public static final ServiceType SERVICE_TYPE = ServiceType.of(5011, "JACKSON", NORMAL_SCHEMA);

    public static final AnnotationKey ANNOTATION_KEY_LENGTH_VALUE = new AnnotationKey(9001, "jackson.json.length");
}
