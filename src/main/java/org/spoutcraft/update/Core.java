/*
 * This file is part of Spoutcraft Launcher.
 *
 * Copyright (c) 2011-2012, SpoutDev <http://www.spout.org/>
 * Spoutcraft Launcher is licensed under the SpoutDev License Version 1.
 *
 * Spoutcraft Launcher is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * In addition, 180 days after any changes are published, you can use the
 * software, incorporating those changes, under the terms of the MIT license,
 * as described in the SpoutDev License Version 1.
 *
 * Spoutcraft Launcher is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License,
 * the MIT license and the SpoutDev License Version 1 along with this program.
 * If not, see <http://www.gnu.org/licenses/> for the GNU Lesser General Public
 * License and see <http://www.spout.org/SpoutDevLicenseV1.txt> for the full license,
 * including the MIT license.
 */
package org.spoutcraft.update;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;

public class Core {
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static void main(String[] args) throws InterruptedException {
		File spoutcraftDir = getWorkingDirectory("Spoutcraft");
		spoutcraftDir.mkdirs();

		File versionFile = new File(spoutcraftDir, "launcherVersion");
		if (!spoutcraftDir.exists()) {
			spoutcraftDir.mkdirs();
		}
		String latestVersion = getVersion();
		String currentVersion = getCurrentVersion(versionFile);;

		File jar = new File(spoutcraftDir,"Spoutcraft-Launcher.jar");
		if (!jar.exists()) {
			if (latestVersion == null) {
				System.err.println("Unable to process the latest version of the launcher.");
				return;
			}
			downloadFile("http://get.spout.org/SpoutcraftLauncher", jar.getPath());
			writeFile(versionFile.getPath(), latestVersion);
		} else {
			if (latestVersion != null && currentVersion != null) {
				if (checkUpdate(currentVersion, latestVersion)) {
					jar.delete();
					downloadFile("http://get.spout.org/SpoutcraftLauncher", jar.getPath());
					writeFile(versionFile.getPath(), latestVersion);
				}
			}
		}

		if (!versionFile.exists()) {
			jar.delete();
			downloadFile("http://get.spout.org/SpoutcraftLauncher", jar.getPath());
			writeFile(versionFile.getPath(), latestVersion);
		}

		URL[] urls = new URL[1];

		try {
			urls[0] = jar.toURI().toURL();
		} catch (MalformedURLException e) {
			e.printStackTrace();
			return;
		}

		ClassLoader classLoad = new URLClassLoader(urls);
		Class mainClass = null;
		try {
			mainClass = classLoad.loadClass("org.spoutcraft.launcher.Main");
			try {
				Class[] params = {String[].class};
				mainClass.getDeclaredMethod("main", params).invoke(null, (Object)args);
			} catch (Exception ignore) {
				mainClass.newInstance();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private static void downloadFile(String url, String outPut){
		BufferedInputStream in = null;
		BufferedOutputStream bout = null;
		try {
			in = new BufferedInputStream(new URL(url).openStream());
			FileOutputStream fos = new FileOutputStream(outPut);
			bout = new BufferedOutputStream(fos,1024);
			byte[] data = new byte[1024];
			int x=0;
			while((x=in.read(data,0,1024))>=0)
			{
				bout.write(data,0,x);
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (bout != null) {
				try {
					bout.close();
				} catch (IOException ignore) { }
			}
			if (in != null) {
				try {
					in.close();
				} catch (IOException ignore) { }
			}
		}
	}

	private static void writeFile(String out, String contents) {
		FileWriter fWriter = null;
		BufferedWriter writer = null;
		try {
			fWriter = new FileWriter(out);
			writer = new BufferedWriter(fWriter);
			writer.write(contents);
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (writer != null) {
				try {
					writer.close();
				} catch (IOException ignore) { }
			}
		}
	}

	private static String getCurrentVersion(File file){
		FileInputStream fstream = null;
		try {
			// Open the file that is the first
			// command line parameter
			fstream = new FileInputStream(file);
			// Get the object of DataInputStream
			DataInputStream in = new DataInputStream(fstream);
			BufferedReader br = new BufferedReader(new InputStreamReader(in));
			String strLine;
			//Read File Line By Line
			while ((strLine = br.readLine()) != null)   {
				return strLine;
			}
			//Close the input stream
			in.close();
		} catch (IOException e){
			e.printStackTrace();
		} finally {
			if (fstream != null) {
				try {
					fstream.close();
				} catch (IOException ignore) { }
			}
		}
		return null;
	}

	private static String getVersion(){
		String version = "-1";
		BufferedReader in = null;
		try {
			URL url = new URL("http://get.spout.org/SpoutcraftLauncher/build");
			in = new BufferedReader(new InputStreamReader(url.openStream()));
			String str;
			while ((str = in.readLine()) != null) {
				version = str;
				break;
			}
			in.close();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (in != null) {
				try {
					in.close();
				} catch (IOException e) { }
			}
		}

		 return version;
	}

	private static boolean checkUpdate(String current, String latest) {
		int c = Integer.parseInt(current);
		int l = Integer.parseInt(latest);
		if (c < l) {
			return true;
		}
		return false;
	}

	private static File getWorkingDirectory(String applicationName) {
		String userHome = System.getProperty("user.home", ".");
		File workingDirectory;

		switch (lookupOperatingSystem()) {
			case LINUX:
			case SOLARIS:
				workingDirectory = new File(userHome, '.' + applicationName + '/');
				break;
			case WINDOWS:
				String applicationData = System.getenv("APPDATA");
				if (applicationData != null) {
					workingDirectory = new File(applicationData, "." + applicationName + '/');
				} else {
					workingDirectory = new File(userHome, '.' + applicationName + '/');
				}
				break;
			case MAC_OS:
				workingDirectory = new File(userHome, "Library/Application Support/" + applicationName);
				break;
			default:
				workingDirectory = new File(userHome, applicationName + '/');
		}
		if ((!workingDirectory.exists()) && (!workingDirectory.mkdirs())) {
			throw new RuntimeException("The working directory could not be created: " + workingDirectory);
		}
		return workingDirectory;
	}

	private static OS lookupOperatingSystem() {
		String osName = System.getProperty("os.name").toLowerCase();
		if (osName.contains("win")) {
			return OS.WINDOWS;
		}
		if (osName.contains("mac")) {
			return OS.MAC_OS;
		}
		if (osName.contains("solaris")) {
			return OS.SOLARIS;
		}
		if (osName.contains("sunos")) {
			return OS.SOLARIS;
		}
		if (osName.contains("linux")) {
			return OS.LINUX;
		}
		if (osName.contains("unix")) {
			return OS.LINUX;
		}
		return OS.UNKNOWN;
	}

	enum OS {
		LINUX,
		SOLARIS,
		WINDOWS,
		MAC_OS,
		UNKNOWN;
	}
}
