/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.persist.impl;

import org.junit.Test;
import org.opendaylight.controller.config.persist.api.ConfigSnapshotHolder;
import org.opendaylight.controller.config.persist.api.Persister;
import org.opendaylight.controller.config.persist.storage.file.xml.XmlFileStorageAdapter;
import org.opendaylight.controller.netconf.persist.impl.osgi.ConfigPersisterActivator;
import org.opendaylight.controller.netconf.persist.impl.osgi.PropertiesProviderBaseImpl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.junit.matchers.JUnitMatchers.containsString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.opendaylight.controller.netconf.persist.impl.PersisterAggregator.PersisterWithConfiguration;
import static org.opendaylight.controller.netconf.persist.impl.PersisterAggregatorTest.TestingPropertiesProvider.loadFile;

public class PersisterAggregatorTest {

    static class TestingPropertiesProvider extends PropertiesProviderBaseImpl {

        private final Properties prop;

        public TestingPropertiesProvider(Properties prop) {
            super(null);
            this.prop = prop;
        }

        public static TestingPropertiesProvider loadFile(String fileName) {
            Properties prop = new Properties();
            try {
                prop.load(TestingPropertiesProvider.class.getClassLoader().getResourceAsStream(fileName));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            return new TestingPropertiesProvider(prop);
        }

        @Override
        public String getFullKeyForReporting(String key) {
            return ConfigPersisterActivator.NETCONF_CONFIG_PERSISTER + "." + key;
        }

        @Override
        public String getProperty(String key) {
            return prop.getProperty(getFullKeyForReporting(key));
        }

        @Override
        public String getPropertyWithoutPrefix(String fullKey){
            return prop.getProperty(fullKey);
        }
    }

    @Test
    public void testDummyAdapter() throws Exception {
        PersisterAggregator persisterAggregator = PersisterAggregator.createFromProperties(loadFile("test1.properties"));
        List<PersisterWithConfiguration> persisters = persisterAggregator.getPersisterWithConfigurations();
        assertEquals(1, persisters.size());
        PersisterWithConfiguration persister = persisters.get(0);
        assertEquals(DummyAdapter.class.getName(), persister.getStorage().getClass().getName());
        assertFalse(persister.isReadOnly());

        persisterAggregator.persistConfig(null);
        persisterAggregator.loadLastConfigs();
        persisterAggregator.persistConfig(null);
        persisterAggregator.loadLastConfigs();

        assertEquals(2, DummyAdapter.persist);
        assertEquals(2, DummyAdapter.load);
        assertEquals(1, DummyAdapter.props);
    }

    @Test
    public void testLoadFromPropertyFile() throws Exception {
        PersisterAggregator persisterAggregator = PersisterAggregator.createFromProperties(loadFile("test2.properties"));
        List<PersisterWithConfiguration> persisters = persisterAggregator.getPersisterWithConfigurations();
        assertEquals(1, persisters.size());
        PersisterWithConfiguration persister = persisters.get(0);
        assertEquals(XmlFileStorageAdapter.class.getName() ,persister.getStorage().getClass().getName());
        assertFalse(persister.isReadOnly());
    }

    @Test
    public void testFileStorageNumberOfBackups() throws Exception {
        try {
            PersisterAggregator.createFromProperties(loadFile("test3.properties"));
            fail();
        } catch (RuntimeException e) {
            assertThat(
                    e.getMessage(),
                    containsString("numberOfBackups property should be either set to positive value, or ommited. Can not be set to 0."));
        }
    }

    private ConfigSnapshotHolder mockHolder(String name){
        ConfigSnapshotHolder result = mock(ConfigSnapshotHolder.class);
        doReturn("mock:" + name).when(result).toString();
        return result;
    }

    private Persister mockPersister(String name){
        Persister result = mock(Persister.class);
        doReturn("mock:" + name).when(result).toString();
        return result;
    }

    @Test
    public void loadLastConfig() throws Exception {
        List<PersisterWithConfiguration> persisterWithConfigurations = new ArrayList<>();
        PersisterWithConfiguration first = new PersisterWithConfiguration(mock(Persister.class), false);

        ConfigSnapshotHolder ignored = mockHolder("ignored");
        doReturn(Arrays.asList(ignored)).when(first.getStorage()).loadLastConfigs(); // should be ignored


        ConfigSnapshotHolder used = mockHolder("used");
        PersisterWithConfiguration second = new PersisterWithConfiguration(mockPersister("p1"), false);
        doReturn(Arrays.asList(used)).when(second.getStorage()).loadLastConfigs(); // should be used

        PersisterWithConfiguration third = new PersisterWithConfiguration(mockPersister("p2"), false);
        doReturn(Arrays.asList()).when(third.getStorage()).loadLastConfigs();

        persisterWithConfigurations.add(first);
        persisterWithConfigurations.add(second);
        persisterWithConfigurations.add(third);

        PersisterAggregator persisterAggregator = new PersisterAggregator(persisterWithConfigurations);
        List<ConfigSnapshotHolder> configSnapshotHolderOptional = persisterAggregator.loadLastConfigs();
        assertEquals(1, configSnapshotHolderOptional.size());
        assertEquals(used, configSnapshotHolderOptional.get(0));
    }
}
