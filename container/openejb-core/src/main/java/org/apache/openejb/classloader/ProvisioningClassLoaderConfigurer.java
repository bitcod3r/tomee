/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.openejb.classloader;

import org.apache.openejb.loader.IO;
import org.apache.openejb.loader.ProvisioningUtil;
import org.apache.openejb.util.LogCategory;
import org.apache.openejb.util.Logger;
import org.apache.openejb.util.URLs;
import org.apache.xbean.finder.filter.Filter;
import org.apache.xbean.finder.filter.Filters;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;

/**
 * Configuration
 * <configurer prefix>.configuration = /foo/bar/config.txt
 *
 * Handled file format:
 * -xbean
 * +http://..../camel-core.jar
 * +org.foo:bar:1.0
 *
 * The maven like urls needs the openejb-provisinning module
 *
 * Note: if a line doesn't start with '+' it is considered as an addition
 */
public class ProvisioningClassLoaderConfigurer implements ClassLoaderConfigurer {
    private static final Logger LOGGER = Logger.getInstance(LogCategory.OPENEJB, ProvisioningClassLoaderConfigurer.class);

    // just some default if one is not set
    private URL[] added = new URL[0];
    private Filter excluded = FalseFilter.INSTANCE;

    @Override
    public URL[] additionalURLs() {
        return added;
    }

    @Override
    public boolean accept(final URL url) {
        try {
            final File file = URLs.toFile(url);
            return !excluded.accept(file.getName());
        } catch (IllegalArgumentException iae) {
            return true;
        }
    }

    public void setConfiguration(final String configFile) {
        final Collection<String> toAdd = new ArrayList<String>();
        final Collection<String> toExclude = new ArrayList<String>();

        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(configFile));

            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.startsWith("#")) {
                    continue;
                }

                if (line.startsWith("-")) {
                    toExclude.add(line);
                } else {
                    if (line.startsWith("+")) {
                        line = line.substring(1);
                    }
                    toAdd.add(ProvisioningUtil.realLocation(line));
                }
            }

        } catch (final Exception e) {
            LOGGER.error("Can't read " + configFile, e);
        } finally {
            IO.close(reader);
        }

        added = new URL[toAdd.size()];
        int i = 0;
        for (final String path : toAdd) {
            try {
                added[i++] = new File(path).toURI().toURL();
            } catch (MalformedURLException e) {
                LOGGER.warning("Can't add file " + path, e);
            }
        }

        if (toExclude.size() > 0) {
            excluded = Filters.prefixes(toExclude.toArray(new String[toExclude.size()]));
        }
    }
}
