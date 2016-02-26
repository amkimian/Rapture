/**
 * Copyright (C) 2011-2013 Incapture Technologies LLC
 *
 * This is an autogenerated license statement. When copyright notices appear below
 * this one that copyright supercedes this statement.
 *
 * Unless required by applicable law or agreed to in writing, software is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied.
 *
 * Unless explicit permission obtained in writing this software cannot be distributed.
 */
package rapture.repo.cassandra;

import static org.junit.Assert.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import rapture.common.RaptureFolderInfo;

public class SimpleRepoCassTest {
    private CassandraKeyStore ks;

    @Before
    public void setup() {
        Map<String, String> config = new HashMap<String, String>();
        config.put("keyspace", "test1");
        config.put("cf", "testrepo1");

        ks = new CassandraKeyStore();
        ks.setInstanceName("default");
        ks.setConfig(config);
    }

    @After
    public void teardown() {
        ks.dropKeyStore();
    }

    @Test
    public void testPutAndGet() {
        String value = "Value of a/b/c";
        String key = "a/b/c";

        ks.put(key, value);

        String ret = ks.get(key);
        assertTrue(ret.equals(value));

    }

    @Test
    public void testFolderHandling() {
        String anotherValue = "Value of a/b/d xxx";
        String key = "a/b/d";
        ks.put(key, anotherValue);

        List<String> keys = ks.getAllSubKeys("a");
        for (String k : keys) {
            System.out.println(k);
        }
        assertTrue(keys.contains(key));
    }

    @Test
    public void testRootTest() {
        String anotherValue = "Value of a/b/e xxx";
        String key = "a/b/e";
        ks.put(key, anotherValue);
        List<RaptureFolderInfo> info = ks.getSubKeys("");
        for (RaptureFolderInfo i : info) {
            System.out.println(String.format("name = %s, isFolder = %b", i.getName(), i.isFolder()));
            assertTrue(i.isFolder());
        }
        info = ks.getSubKeys("a/b");
        for (RaptureFolderInfo i : info) {
            System.out.println(String.format("name = %s, isFolder = %b", i.getName(), i.isFolder()));
            assertFalse(i.isFolder());
        }
    }

    @Test
    public void testDropKeyStore() {
        String value = "Value of a/b/c";
        String key = "a/b/c";
        ks.put(key, value);

        String ret = ks.get(key);
        assertTrue(ret.equals(value));

        ks.dropKeyStore();
        setup();

        ret = ks.get(key);
        assertNull("Having dropped this repo, its data should be gone even if the repo is recreated.", ret);
    }
}
