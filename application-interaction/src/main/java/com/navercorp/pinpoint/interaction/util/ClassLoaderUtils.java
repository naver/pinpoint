/*
 * Copyright 2020 NAVER Corp.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.navercorp.pinpoint.interaction.util;

/**
 * @author yjqg6666
 */
@SuppressWarnings("unused")
public class ClassLoaderUtils {

    public static Class<?> loadClassFromAppObject(Object appLoadedObject, String className) {
        ClassLoader appClassLoader = getAppClassLoader(appLoadedObject);
        return loadClassFromClassLoader(appClassLoader, className);
    }

    public static Class<?> loadClassFromClassLoader(ClassLoader appClassLoader, String className) {
        if (appClassLoader == null || className == null) {
            return null;
        }
        try {
            return Class.forName(className, false, appClassLoader);
        } catch (Throwable t) {
            //t.printStackTrace();
            //do nothing even no logging for no introduced dependency
        }
        return null;
    }

    public static ClassLoader getAppClassLoader(Object appLoadedObject) {
        if (appLoadedObject == null) {
            return null;
        }
        Class<?> targetClass = appLoadedObject.getClass();
        if (targetClass == null) {
            return null;
        }
        return targetClass.getClassLoader();
    }
}
