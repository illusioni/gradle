/*
 * Copyright 2011 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.gradle.internal.nativeplatform.services;

import com.sun.jna.Native;
import org.gradle.internal.nativeplatform.ConsoleDetector;
import org.gradle.internal.nativeplatform.NoOpConsoleDetector;
import org.gradle.internal.nativeplatform.ProcessEnvironment;
import org.gradle.internal.nativeplatform.WindowsConsoleDetector;
import org.gradle.internal.nativeplatform.filesystem.FileSystem;
import org.gradle.internal.nativeplatform.filesystem.FileSystems;
import org.gradle.internal.nativeplatform.jna.*;
import org.gradle.internal.os.OperatingSystem;
import org.gradle.internal.service.DefaultServiceRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

/**
 * Provides various native platform integration services.
 */
public class NativeServices extends DefaultServiceRegistry {
    private static final Logger LOGGER = LoggerFactory.getLogger(NativeServices.class);
    private static NativeServices instance = new NativeServices();

    /**
     * Initializes the native services to use the given user home directory to store native libs and other resources. Does nothing if already initialized. Will be implicitly initialized on first usage
     * of a native service. Also initializes the Native-Platform library using the passed user home directory.
     */
    public static void initialize(File userHomeDir) {

        new JnaBootPathConfigurer().configure(userHomeDir);
        /*
        try {
            net.rubygrapefruit.platform.Native.init(userHomeDir);
        } catch (NativeException ex) {
            LOGGER.info(ex.getMessage());
        }
        */
    }

    public static NativeServices getInstance() {
        return instance;
    }

    private NativeServices() {
    }

    @Override
    public void close() {
        // Don't close
    }

    protected OperatingSystem createOperatingSystem() {
        return OperatingSystem.current();
    }

    protected FileSystem createFileSystem() {
        return FileSystems.getDefault();
    }

    protected ProcessEnvironment createProcessEnvironment() {
        OperatingSystem operatingSystem = get(OperatingSystem.class);
        try {
            if (operatingSystem.isUnix()) {
                return new LibCBackedProcessEnvironment(get(LibC.class));
            } else if (operatingSystem.isWindows()) {
                return new WindowsProcessEnvironment();
            } else {
                return new UnsupportedEnvironment();
            }
        } catch (LinkageError e) {
            // Thrown when jna cannot initialize the native stuff
            LOGGER.debug("Unable to load native library. Continuing with fallback.", e);
            return new UnsupportedEnvironment();
        }
    }

    protected ConsoleDetector createConsoleDetector() {
        OperatingSystem operatingSystem = get(OperatingSystem.class);
        /*
        try {
            Terminals terminals = net.rubygrapefruit.platform.Native.get(Terminals.class);
            return new NativePlatformConsoleDetector(terminals);
        } catch (NativeException ex) {
            LOGGER.debug("Unable to load from native platform library backed ConsoleDetector. Continuing with fallback.", ex);
        }
        */
        try {
            if (operatingSystem.isWindows()) {
                return new WindowsConsoleDetector();
            }
            return new LibCBackedConsoleDetector(get(LibC.class));
        } catch (LinkageError e) {
            // Thrown when jna cannot initialize the native stuff
            LOGGER.debug("Unable to load native library. Continuing with fallback.", e);
            return new NoOpConsoleDetector();
        }
    }

    protected LibC createLibC() {
        return (LibC) Native.loadLibrary("c", LibC.class);
    }
}
