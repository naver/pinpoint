/*
 * Copyright 2018 NAVER Corp.
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

package com.navercorp.pinpoint.bootstrap.agentdir;


import com.navercorp.pinpoint.bootstrap.BootLogger;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;

/**
 * @author emeroad
 */
public class AgentDirBaseClassPathResolver implements ClassPathResolver {

    private final BootLogger logger = BootLogger.getLogger(this.getClass().getName());

    static final String VERSION_PATTERN = "(-[0-9]+\\.[0-9]+\\.[0-9]+((\\-SNAPSHOT)|(-RC[0-9]+))?)?";

    static final JarDescription bootstrap = new JarDescription("pinpoint-bootstrap", true);

    // boot dir
    private final JarDescription commons = new JarDescription("pinpoint-commons", true);
    private final JarDescription bootstrapCore = new JarDescription("pinpoint-bootstrap-core", true);
    private final JarDescription annotations = new JarDescription("pinpoint-annotations", false);
    private final JarDescription bootstrapJava7 = new JarDescription("pinpoint-bootstrap-java7", false);
    private final JarDescription bootstrapJava8 = new JarDescription("pinpoint-bootstrap-java8", false);
    private final JarDescription bootstrapJava9 = new JarDescription("pinpoint-bootstrap-java9", false);
    private final JarDescription applicationInteraction = new JarDescription("pinpoint-application-interaction", false);
    private final List<JarDescription> bootJarDescriptions = Arrays.asList(commons, bootstrapCore, annotations, bootstrapJava7, bootstrapJava8, bootstrapJava9, applicationInteraction);

    private final String classPath;

    private final String[] fileExtensions;


    public AgentDirBaseClassPathResolver(String classPath) {
        this.classPath = Assert.requireNonNull(classPath, "classPath");
        this.fileExtensions = getFileExtensions();
    }

    private static String[] getFileExtensions() {
        return new String[] {".jar", ".xml", ".properties"};
    }


    @Override
    public AgentDirectory resolve() {

        // find boot-strap.jar
        final String bootstrapJarName = this.findBootstrapJar(this.classPath);
        if (bootstrapJarName == null) {
            throw new IllegalStateException(bootstrap.getSimplePattern() + " not found.");
        }

        final String agentJarFullPath = parseAgentJarPath(classPath, bootstrapJarName);
        if (agentJarFullPath == null) {
            throw new IllegalStateException(bootstrap.getSimplePattern() + " not found. " + classPath);
        }
        final String agentDirPath = getAgentDirPath(agentJarFullPath);

        final BootDir bootDir = resolveBootDir(agentDirPath);

        final String agentLibPath = getAgentLibPath(agentDirPath);
        final List<URL> libs = resolveLib(agentLibPath);

        String agentPluginPath = getAgentPluginPath(agentDirPath);
        final List<String> plugins = resolvePlugins(agentPluginPath);

        final AgentDirectory agentDirectory = new AgentDirectory(bootstrapJarName, agentJarFullPath, agentDirPath, bootDir, libs, plugins);

        return agentDirectory;
    }

    private String getAgentDirPath(String agentJarFullPath) {
        String agentDirPath = parseAgentDirPath(agentJarFullPath);
        if (agentDirPath == null) {
            throw new IllegalStateException("agentDirPath is null " + classPath);
        }

        logger.info("Agent original-path:" + agentDirPath);
        // defense alias change
        agentDirPath = FileUtils.toCanonicalPath(new File(agentDirPath));
        logger.info("Agent canonical-path:" + agentDirPath);
        return agentDirPath;
    }


    private BootDir resolveBootDir(String agentDirPath) {
        String bootDirPath = agentDirPath + File.separator + "boot";
        return new BootDir(bootDirPath, bootJarDescriptions);
    }


    String findBootstrapJar(String classPath) {
        final Matcher matcher = bootstrap.getVersionPattern().matcher(classPath);
        if (!matcher.find()) {
            return null;
        }
        return parseAgentJar(matcher, classPath);
    }


    private String parseAgentJar(Matcher matcher, String classpath) {

        int start = matcher.start();
        int end = matcher.end();
        return classpath.substring(start, end);
    }

    private String parseAgentJarPath(String classPath, String agentJar) {
        String[] classPathList = classPath.split(File.pathSeparator);
        for (String findPath : classPathList) {
            boolean find = findPath.contains(agentJar);
            if (find) {
                return findPath;
            }
        }
        return null;
    }


    private String getAgentLibPath(String agentDirPath) {
        return agentDirPath + File.separator + "lib";
    }

    private String getAgentPluginPath(String agentDirPath) {
        return agentDirPath + File.separator + "plugin";
    }

    private List<URL> resolveLib(String agentLibPath) {
        final File libDir = new File(agentLibPath);
        if (checkDirectory(libDir)) {
            return Collections.emptyList();
        }
        final File[] libFileList = listFiles(libDir, this.fileExtensions);

        List<URL> libURLList = toURLs(libFileList);
        // add directory
        URL agentDirUri = toURL(new File(agentLibPath));

        List<URL> jarURLList = new ArrayList<URL>(libURLList);
        jarURLList.add(agentDirUri);
        return jarURLList;
    }

    private File[] listFiles(File libDir, String[] p) {
        return FileUtils.listFiles(libDir, p);
    }

    private List<URL> toURLs(File[] jarFileList) {
        try {
            URL[] jarURLArray = FileUtils.toURLs(jarFileList);
            return Arrays.asList(jarURLArray);
        } catch (IOException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    private List<String> resolvePlugins(String agentPluginPath) {
        final File directory = new File(agentPluginPath);

        if (checkDirectory(directory)) {
            logger.warn(directory + " is not a directory");
            return Collections.emptyList();
        }

        final String[] jarExtensions = {".jar"};
        final File[] jars = listFiles(directory, jarExtensions);
        if (FileUtils.isEmpty(jars)) {
            return Collections.emptyList();
        }

        List<String> pluginFileList = filterReadPermission(jars);
        for (String pluginJar : pluginFileList) {
            logger.info("Found plugins:" + pluginJar);
        }
        return pluginFileList;
    }

    private boolean checkDirectory(File file) {
        if (!file.exists()) {
            logger.warn(file + " not found");
            return true;
        }
        if (!file.isDirectory()) {
            logger.warn(file + " is not a directory");
            return true;
        }
        return false;
    }

    private List<String> filterReadPermission(File[] jars) {
        List<String> result = new ArrayList<String>();
        for (File pluginJar : jars) {
            if (!pluginJar.canRead()) {
                logger.info("File '" + pluginJar + "' cannot be read");
                continue;
            }

            result.add(pluginJar.getPath());
        }
        return result;
    }

    private URL toURL(File file) {
        try {
            return FileUtils.toURL(file);
        } catch (IOException e) {
            logger.warn(file.getName() + ".toURL() failed.", e);
            throw new RuntimeException(file.getName() + ".toURL() failed.", e);
        }
    }



    private String parseAgentDirPath(String agentJarFullPath) {
        int index1 = agentJarFullPath.lastIndexOf("/");
        int index2 = agentJarFullPath.lastIndexOf("\\");
        int max = Math.max(index1, index2);
        if (max == -1) {
            return null;
        }
        return agentJarFullPath.substring(0, max);
    }

}
