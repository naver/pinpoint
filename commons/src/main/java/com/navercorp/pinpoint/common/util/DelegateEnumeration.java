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

package com.navercorp.pinpoint.common.util;

import java.util.Enumeration;

/**
 * @author emeroad
 */
public class DelegateEnumeration<E> implements Enumeration<E> {
    private static final Object NULL_OBJECT = new Object();

    private final Enumeration<E> delegate;
    private final Filter<E> filter;

    private boolean hasMoreElements;
    private E nextElement;
    private Exception nextException;


    private static final Filter SKIP_FILTER = new Filter() {
        @Override
        public boolean filter(Object o) {
            return false;
        }
    };

    @SuppressWarnings("unchecked")
    public DelegateEnumeration(Enumeration<E> delegate) {
        this(delegate, SKIP_FILTER);
    }

    public DelegateEnumeration(Enumeration<E> delegate, Filter<E> filter) {
        this.delegate = delegate;
        this.filter = filter;
    }

    @Override
    public boolean hasMoreElements() {
        next();
        return hasMoreElements;
    }

    @Override
    public E nextElement() {
        next();
        if (nextException != null) {
            Exception exception = this.nextException;
            this.nextException = null;
            this.<RuntimeException>throwException(exception);
        }
        final E result = getNextElement();
        this.nextElement = null;
        return result;
    }

    private E getNextElement() {
        if (nextElement == NULL_OBJECT) {
            return null;
        }
        return nextElement;
    }

    @SuppressWarnings("unchecked")
    private <T extends Exception> void throwException(Exception exception) throws T {
        throw (T) exception;
    }


    private void next() {
        if (nextElement != null || nextException != null) {
            return;
        }

        while (true) {
            final boolean hasMoreElements = delegate.hasMoreElements();
            E nextElement;
            try {
                nextElement = delegate.nextElement();
            } catch (Exception e) {
                this.hasMoreElements = hasMoreElements;
                this.nextException = e;
                break;
            }

            if (filter.filter(nextElement)) {
                continue;
            }

            this.hasMoreElements = hasMoreElements;
            if (nextElement == null) {
                this.nextElement = (E) NULL_OBJECT;
            } else {
                this.nextElement = nextElement;
            }
            break;

        }

    }


    public static interface Filter<E> {
        boolean filter(E e);
    }
}
