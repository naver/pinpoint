package com.navercorp.pinpoint.plugin.ice;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public final class SliceJarClassScanner {

	public static List<String> readFromDirectory(String jarpath, String jarnamepart) {
		String[] pathArgs = jarpath.split(",");
		List<String> list = new ArrayList<String>();
		for(String path:pathArgs)
		{
			File file = new File(path);
			String[] names = file.list();
			if (null == names) {
				continue;
			}
			
			for (String name : names) {
				if ((null == jarnamepart || "".equals(jarnamepart)) || name.contains(jarnamepart)) {
					list.add(file.getPath() + File.separator + name);
				}
			}
		}
		

		return list;
	}

	public static Set<String> getSliceJarPrxClassNames(String jarpath, String jarnamepart) {
		List<String> str = readFromDirectory(jarpath, jarnamepart);
		Set<String> classNameSet = new HashSet<String>();

		for (String path : str) {
			Set<String> list = getPrxClassNames(path);
			classNameSet.addAll(list);
		}
		return classNameSet;
	}

	public static Set<String> getPrxClassNames(String jarPath) {
		Set<String> set = new HashSet<String>();
		JarFile jarFile = null;
		try {
			jarFile = new JarFile(jarPath);
		} catch (IOException e) {
			
		}
		Enumeration<?> files = jarFile.entries();
		while (files.hasMoreElements()) {
			JarEntry entry = (JarEntry) files.nextElement();
			String keyname = entry.getName();

			if (!keyname.endsWith(".MF")) {
				if (keyname.endsWith("PrxHelper.class")) {
					String d = keyname.replaceAll("/", ".");
					String name = d.substring(0, keyname.length() - 6);
					set.add(name);

				}
			}
		}
		return set;

	}
}
