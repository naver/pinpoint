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

package com.navercorp.pinpoint.bootstrap.config;

import junit.framework.Assert;

import org.junit.Test;

import com.navercorp.pinpoint.bootstrap.config.ExcludeUrlFilter;
import com.navercorp.pinpoint.bootstrap.config.Filter;

import static org.junit.Assert.*;

public class ExcludeUrlFilterTest {

    @Test
    public void testFilter() throws Exception {
           Filter<String> filter = new ExcludeUrlFilter("/monitor/l7check.html, test/l4check.html");

        assertFilter(filter);
    }


    @Test
    public void testFilter_InvalidExcludeURL() throws Exception {
        Filter<String> filter = new ExcludeUrlFilter("/monitor/l7check.html, test/l4check.html, ,,");

        assertFilter(filter);
    }

    @Test
    public void testFilter_emptyExcludeURL() throws Exception {
        Filter<String> filter = new ExcludeUrlFilter("");

        Assert.assertFalse(filter.filter("/monitor/l7check.html"));
        Assert.assertFalse(filter.filter("test/l4check.html"));

        Assert.assertFalse(filter.filter("test/"));
        Assert.assertFalse(filter.filter("test/l4check.htm"));
    }


    private void assertFilter(Filter<String> filter) {
        Assert.assertTrue(filter.filter("/monitor/l7check.html"));
        Assert.assertTrue(filter.filter("test/l4check.html"));

        Assert.assertFalse(filter.filter("test/"));
        Assert.assertFalse(filter.filter("test/l4check.htm"));
    }



}