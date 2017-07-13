/**
 * The MIT License (MIT)
 *
 * Copyright (c) 2011-2016 Incapture Technologies LLC
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package rapture.structured;

import java.io.File;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.junit.Ignore;
import org.testng.Assert;
import org.testng.SkipException;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;

import rapture.common.PluginConfig;
import rapture.common.PluginTransportItem;
import rapture.common.RaptureURI;
import rapture.common.Scheme;
import rapture.common.StructuredRepoConfig;
import rapture.common.client.HttpAdminApi;
import rapture.common.client.HttpPluginApi;
import rapture.common.client.HttpStructuredApi;
import rapture.common.impl.jackson.JacksonUtil;
import rapture.common.impl.jackson.MD5Utils;
import rapture.helper.IntegrationTestHelper;
import rapture.plugin.install.PluginSandbox;
import rapture.plugin.install.PluginSandboxItem;
import rapture.plugin.util.PluginUtils;

public class StructuredApiIntegrationTests {

    private IntegrationTestHelper helper;
    private HttpStructuredApi structApi = null;
    private HttpAdminApi admin = null;
    private HttpPluginApi pluginApi = null;
    private RaptureURI repoUri = null;

    private static final String user = "User";

    /**
     * Setup TestNG method to create Rapture login object and objects.
     *
     * @param RaptureURL
     *            Passed in from <env>_testng.xml suite file
     * @param RaptureUser
     *            Passed in from <env>_testng.xml suite file
     * @param RapturePassword
     *            Passed in from <env>_testng.xml suite file
     * @return none
     */
    @BeforeClass(groups = { "structured", "postgres","nightly"  })
    @Parameters({ "RaptureURL", "RaptureUser", "RapturePassword" })
    public void setUp(@Optional("http://localhost:8665/rapture") String url, @Optional("rapture") String username, @Optional("rapture") String password) {

        // If running from eclipse set env var -Penv=docker or use the following
        // url variable settings:
        // url = "http://192.168.99.100:8665/rapture"; // docker
        // url="http://localhost:8665/rapture";

        try {
            helper = new IntegrationTestHelper(url, username, password);
        } catch (Exception e) {
            throw new SkipException("Cannot connect to IntegrationTestHelper " + e.getMessage());
        }
        structApi = helper.getStructApi();
        admin = helper.getAdminApi();
        pluginApi = helper.getPluginApi();
        if (!admin.doesUserExist(user)) {
            admin.addUser(user, "Another User", MD5Utils.hash16(user), "user@incapture.net");
        }
        repoUri = helper.getRandomAuthority(Scheme.DOCUMENT);
        helper.configureTestRepo(repoUri, "MONGODB"); // TODO Make this configurable
    }

    @Test(groups = { "structured", "postgres","nightly"  })
    public void testDeleteNonExistingRow() {
        RaptureURI repo = helper.getRandomAuthority(Scheme.STRUCTURED);
        String repoStr = repo.toString();
        String config = "STRUCTURED { } USING POSTGRES { marvin=\"paranoid\" }";

        // Create a repo
        Boolean repoExists = structApi.structuredRepoExists(repoStr);
        Assert.assertFalse(repoExists, "Repo does not exist yet");

        structApi.createStructuredRepo(repoStr, config);
        repoExists = structApi.structuredRepoExists(repoStr);
        Assert.assertTrue(repoExists, "Repo should exist now");

        // Verify the config
        StructuredRepoConfig rc = structApi.getStructuredRepoConfig(repoStr);
        Assert.assertEquals(rc.getConfig(), config);

        // Create a table. Add and remove data
        String table = "//" + repo.getAuthority() + "/table";
        structApi.createTable(table, ImmutableMap.of("id", "int", "name", "varchar(255), PRIMARY KEY (id)"));

        Map<String, Object> row = new HashMap<>();
        row.put("id", 42);
        row.put("name", "Don't Panic");
        structApi.insertRow(table, row);
        
        row.put("id", 43);
        row.put("name", "Don't Panic Now");
        structApi.insertRow(table, row);
        
        row.put("id", 44);
        row.put("name", "Don't Panic Now Again");
        structApi.insertRow(table, row);
        structApi.deleteRows(table, "id=45");
        
        List<Map<String, Object>> contents = structApi.selectRows(table, null, null, null, null, -1);
        Assert.assertEquals(contents.size(), 3);   
    }
    
    @Test(groups = { "structured", "postgres","nightly"  })
    public void testDeleteByTextColumn() {
        RaptureURI repo = helper.getRandomAuthority(Scheme.STRUCTURED);
        String repoStr = repo.toString();
        String config = "STRUCTURED { } USING POSTGRES { marvin=\"paranoid\" }";

        // Create a repo
        Boolean repoExists = structApi.structuredRepoExists(repoStr);
        Assert.assertFalse(repoExists, "Repo does not exist yet");

        structApi.createStructuredRepo(repoStr, config);
        repoExists = structApi.structuredRepoExists(repoStr);
        Assert.assertTrue(repoExists, "Repo should exist now");

        // Verify the config
        StructuredRepoConfig rc = structApi.getStructuredRepoConfig(repoStr);
        Assert.assertEquals(rc.getConfig(), config);

        // Create a table. Add and remove data
        String table = "//" + repo.getAuthority() + "/table";
        structApi.createTable(table, ImmutableMap.of("id", "int", "name", "varchar(255), PRIMARY KEY (id)"));

        Map<String, Object> row = new HashMap<>();
        row.put("id", 42);
        row.put("name", "AAA");
        structApi.insertRow(table, row);
        
        row.put("id", 43);
        row.put("name", "AAB");
        structApi.insertRow(table, row);
        
        row.put("id", 44);
        row.put("name", "BAA");
        structApi.insertRow(table, row);
        
        row.put("id", 45);
        row.put("name", "BAAB");
        structApi.insertRow(table, row);
        
        row.put("id", 46);
        row.put("name", "AACCA");
        structApi.insertRow(table, row);
        
        row.put("id", 47);
        row.put("name", "AAC");
        structApi.insertRow(table, row);
        
        structApi.deleteRows(table, "name LIKE 'AA%'");
        List<Map<String, Object>> contents = structApi.selectRows(table, null, null, null, null, -1);
        Assert.assertEquals(contents.get(0), ImmutableMap.of("id", new Integer(44),"name","BAA"));
        Assert.assertEquals(contents.get(1), ImmutableMap.of("id", new Integer(45),"name","BAAB")); 
        
        structApi.deleteRows(table, "name LIKE '%AA'");
        contents = structApi.selectRows(table, null, null, null, null, -1);
        Assert.assertEquals(contents.get(0), ImmutableMap.of("id", new Integer(45),"name","BAAB")); 
        
        
    }
    
    @Test(groups = { "structured", "postgres","nightly"  })
    public void testBasicStructuredRepo() {
        RaptureURI repo = helper.getRandomAuthority(Scheme.STRUCTURED);
        String repoStr = repo.toString();
        String config = "STRUCTURED { } USING POSTGRES { marvin=\"paranoid\" }";

        // Create a repo
        Boolean repoExists = structApi.structuredRepoExists(repoStr);
        Assert.assertFalse(repoExists, "Repo does not exist yet");

        structApi.createStructuredRepo(repoStr, config);
        repoExists = structApi.structuredRepoExists(repoStr);
        Assert.assertTrue(repoExists, "Repo should exist now");

        // Verify the config
        StructuredRepoConfig rc = structApi.getStructuredRepoConfig(repoStr);
        Assert.assertEquals(rc.getConfig(), config);

        // Create a table. Add and remove data
        String table = "//" + repo.getAuthority() + "/table";
        structApi.createTable(table, ImmutableMap.of("id", "int", "name", "varchar(255), PRIMARY KEY (id)"));

        Map<String, Object> row = new HashMap<>();
        row.put("id", 42);
        row.put("name", "Don't Panic");
        structApi.insertRow(table, row);

        List<Map<String, Object>> contents = structApi.selectRows(table, null, null, null, null, -1);
        Assert.assertEquals(contents.size(), 1);
        Assert.assertEquals(contents.get(0), row);

        // Batch insert
        List<Map<String, Object>> batch = new ArrayList<>();
        for (String s : ImmutableList.of("Ford Prefect", "Zaphod Beeblebrox", "Arthur Dent", "Slartibartfast", "Trillian")) {
            int cha = s.charAt(0);
            batch.add(ImmutableMap.<String, Object> of("id", cha, "name", s));
        }
        structApi.insertRows(table, batch);
        contents = structApi.selectRows(table, null, null, null, null, -1);
        Assert.assertEquals(contents.size(), batch.size() + 1);

        structApi.deleteRows(table, "id=42");

        contents = structApi.selectRows(table, null, null, null, null, -1);
        Assert.assertEquals(contents.size(), batch.size());
        for (Map<String, Object> m : batch)
            Assert.assertTrue(contents.contains(m));
        Assert.assertTrue(contents.contains(ImmutableMap.<String, Object> of("id", new Integer('Z'), "name", "Zaphod Beeblebrox")));

        // Update a row
        structApi.updateRows(table, ImmutableMap.<String, Object> of("id", new Integer('Z'), "name", "Zarniwoop"), "id=" + new Integer('Z'));
        contents = structApi.selectRows(table, null, null, null, null, -1);
        Assert.assertEquals(contents.size(), batch.size());
        Assert.assertTrue(contents.contains(ImmutableMap.<String, Object> of("id", new Integer('Z'), "name", "Zarniwoop")));
        Assert.assertFalse(contents.contains(ImmutableMap.<String, Object> of("id", new Integer('Z'), "name", "Zaphod Beeblebrox")));

        System.out.println(JacksonUtil.prettyfy(JacksonUtil.jsonFromObject(contents)));
        structApi.deleteRows(table, "name like 'Zarniwoop'");
        contents = structApi.selectRows(table, null, null, null, null, -1);
        Assert.assertFalse(contents.contains(ImmutableMap.<String, Object> of("id", new Integer('Z'), "name", "Zarniwoop")));

        structApi.dropTable(table);
        try {
            contents = structApi.selectRows(table, null, null, null, null, -1);
            Assert.fail("Expected an exception to be thrown");
        } catch (Exception e) {
            // Expected
        }

        // Good enough. Delete the repo.
        structApi.deleteStructuredRepo(repoStr);
        repoExists = structApi.structuredRepoExists(repoStr);
        Assert.assertFalse(repoExists, "Repo does not exist any more");
    }

    // Verify that ascending/descending works for strings and integers
    @Test(groups = { "structured", "postgres","nightly" })
    public void testAscending() {
        RaptureURI repo = helper.getRandomAuthority(Scheme.STRUCTURED);
        String repoStr = repo.toString();
        String config = "STRUCTURED { } USING POSTGRES { planet=\"magrathea\" }";

        if (!structApi.structuredRepoExists(repoStr)) structApi.createStructuredRepo(repoStr, config);

        // Create a table. Add and remove data
        String table = "//" + repo.getAuthority() + "/table";
        structApi.createTable(table, ImmutableMap.of("id", "int", "name", "varchar(255), PRIMARY KEY (id)"));

        Map<String, Object> row = new HashMap<>();
        row.put("id", 42);
        row.put("name", "Don't Panic");
        structApi.insertRow(table, row);

        List<Map<String, Object>> contents = structApi.selectRows(table, null, null, null, null, -1);
        Assert.assertEquals(contents.size(), 1);
        Assert.assertEquals(contents.get(0), row);

        // Batch insert
        List<Map<String, Object>> batch = new ArrayList<>();
        for (String s : ImmutableList.of("Roosta", "Hotblack Desiato", "Ford Prefect", "Zaphod Beeblebrox", "Arthur Dent", "Slartibartfast", "Trillian")) {
            int cha = s.hashCode() % 131;
            batch.add(ImmutableMap.<String, Object> of("id", cha, "name", s));
        }
        structApi.insertRows(table, batch);

        contents = structApi.selectRows(table, null, "id != 42", ImmutableList.of("name"), true, -1);
        System.out.println(JacksonUtil.prettyfy(JacksonUtil.jsonFromObject(contents)));
        Assert.assertEquals(contents.size(), batch.size());
        String last = "AAAA";
        for (Map<String, Object> map : contents) {
            String next = map.get("name").toString();
            Assert.assertTrue(next.compareTo(last) > 0);
            last = next;
        }

        contents = structApi.selectRows(table, null, "id != 42", ImmutableList.of("name"), false, -1);
        System.out.println(JacksonUtil.prettyfy(JacksonUtil.jsonFromObject(contents)));
        Assert.assertEquals(contents.size(), batch.size());
        last = "Zz";
        for (Map<String, Object> map : contents) {
            String next = map.get("name").toString();
            Assert.assertTrue(last.compareTo(next) > 0, last + " > " + next);
            last = next;
        }

        contents = structApi.selectRows(table, null, "id != 42", ImmutableList.of("id"), true, -1);
        Assert.assertEquals(contents.size(), batch.size());
        Integer lasti = Integer.MIN_VALUE;
        for (Map<String, Object> map : contents) {
            Integer nexti = (Integer) map.get("id");
            Assert.assertTrue(nexti.compareTo(lasti) > 0);
            lasti = nexti;
        }

        contents = structApi.selectRows(table, null, "id != 42", ImmutableList.of("id"), false, -1);
        System.out.println(JacksonUtil.prettyfy(JacksonUtil.jsonFromObject(contents)));
        Assert.assertEquals(contents.size(), batch.size());
        lasti = Integer.MAX_VALUE;
        for (Map<String, Object> map : contents) {
            Integer nexti = (Integer) map.get("id");
            Assert.assertTrue(lasti.compareTo(nexti) > 0, lasti + " > " + nexti);
            lasti = nexti;
        }

        // Good enough. Delete the repo.
        structApi.deleteStructuredRepo(repoStr);
        Assert.assertFalse(structApi.structuredRepoExists(repoStr), "Repo does not exist any more");
    }

    @Test(groups = { "structured", "postgres","nightly" })
    public void testSqlGeneration() {

        String foo = "Don\'t Panic";
        String bar = foo.replace("\'", "''");
        Assert.assertEquals("Don''t Panic", bar);

        RaptureURI repo = new RaptureURI("structured://hhgg");
        String repoStr = repo.toString();
        String config = "STRUCTURED { } USING POSTGRES { planet=\"magrathea\" }";
        try {
            if (!structApi.structuredRepoExists(repoStr)) structApi.createStructuredRepo(repoStr, config);

            // Create a table. Add and remove data
            String table = "//" + repo.getAuthority() + "/table";
            structApi.createTable(table, ImmutableMap.of("id", "int", "name", "varchar(255), PRIMARY KEY (id)"));
            Map<String, Object> row = new HashMap<>();
            row.put("id", 42);
            row.put("name", "Don't Panic");
            structApi.insertRow(table, row);

            boolean pass = false;
            String sql = structApi.getDdl(table, true);
            for (String s : sql.split("\n")) {
                if (s.contains("INSERT")) {
                    Assert.assertEquals("INSERT INTO hhgg.table (id, name) VALUES ('42', 'Don''t Panic')", s);
                    pass = true;
                }
            }
            Assert.assertTrue(pass);
        } finally {
            if (structApi.structuredRepoExists(repoStr)) structApi.deleteStructuredRepo(repoStr);
        }
    }

    @Test(groups = { "structured", "postgres", "nightly" },enabled=true)
    public void testSqlSequenceGeneration() {
        RaptureURI tableUri = new RaptureURI("structured://hhgg/ford");
        String repoStr = tableUri.toAuthString();
        String table = tableUri.getShortPath();
        String config = "STRUCTURED { } USING POSTGRES { planet=\"magrathea\" }";
        try {
            if (!structApi.structuredRepoExists(repoStr)) structApi.createStructuredRepo(repoStr, config);

            structApi.dropSequence(tableUri.toString(), "ident", true);
            String seq = structApi.createSequence(tableUri.toString(), "ident", null);

            String sql = "CREATE TABLE hhgg.ford ( ident INTEGER NOT NULL UNIQUE DEFAULT nextval('" + seq + "'), name TEXT);";
            structApi.createTableUsingSql(repoStr, sql);

            structApi.insertRow(table, ImmutableMap.of("name", "Dentarthurdent"));
            structApi.insertRow(table, ImmutableMap.of("name", "Tricia McMillan"));

            boolean pass = false;
            String ddl = structApi.getDdl(table, true);
            ddl = ddl.substring(0, ddl.indexOf('/'));


            String expect = "CREATE SEQUENCE " + seq + ";\n\nCREATE TABLE hhgg.ford\n(\n" + "    ident INTEGER NOT NULL UNIQUE DEFAULT nextval('" + seq
                    + "'),\n    name TEXT\n);\n\n" + "INSERT INTO hhgg.ford (ident, name) VALUES ('1', 'Dentarthurdent')\n"
                    + "INSERT INTO hhgg.ford (ident, name) VALUES ('2', 'Tricia McMillan')\n";

            Assert.assertEquals(ddl.replaceAll(" ", "."), expect.replaceAll(" ", "."));
        } finally {
            if (structApi.structuredRepoExists(repoStr)) structApi.deleteStructuredRepo(repoStr);
        }
    }

    // Test used in conjunction with manually running plugin installer to verify that it works.
    // Could possibly be expanded to invoke the PI but more hassle than it's worth.
    // The preceding test verifies the code in question, though it doesn't exercise the executeDdl method
    // as that's a trusted method on the server
    @Ignore
    @Test(groups = { "structured", "postgres","nightly"  }, enabled=false)
    public void manualTestPlugin() {
        RaptureURI repo = new RaptureURI("structured://hhgg");
        String repoStr = repo.toString();
        String config = "STRUCTURED { } USING POSTGRES { planet=\"magrathea\" }";

        if (!structApi.structuredRepoExists(repoStr)) structApi.createStructuredRepo(repoStr, config);

        // Create a table. Add and remove data
        String table = "//" + repo.getAuthority() + "/table";
        structApi.createTable(table, ImmutableMap.of("id", "int", "name", "varchar(255), PRIMARY KEY (id)"));

        Map<String, Object> row = new HashMap<>();
        row.put("id", 42);
        row.put("name", "Don't Panic");
        structApi.insertRow(table, row);

        List<Map<String, Object>> contents = structApi.selectRows(table, null, null, null, null, -1);
        Assert.assertEquals(contents.size(), 1);
        Assert.assertEquals(contents.get(0), row);

        // Batch insert
        List<Map<String, Object>> batch = new ArrayList<>();
        for (String s : ImmutableList.of("Roosta", "Hotblack Desiato", "Ford Prefect", "Zaphod Beeblebrox", "Arthur Dent", "Slartibartfast", "Trillian")) {
            int cha = s.hashCode() % 131;
            batch.add(ImmutableMap.<String, Object> of("id", cha, "name", s));
        }
        structApi.insertRows(table, batch);

        // Now export the plug-in
        // Put a breakpoint here

        // Delete the repo
        structApi.deleteStructuredRepo(repoStr);
        Assert.assertFalse(structApi.structuredRepoExists(repoStr), "Repo does not exist any more");

        // Now install the plug-in
        // Put a breakpoint here

        Assert.assertTrue(structApi.structuredRepoExists(repoStr), "Repo created");
        contents = structApi.selectRows(table, null, "id = 42", ImmutableList.of("id"), true, -1);
        Assert.assertEquals(1, contents.size());

        // Good enough. Delete the repo.
        structApi.deleteStructuredRepo(repoStr);
        Assert.assertFalse(structApi.structuredRepoExists(repoStr), "Repo does not exist any more");
    }
    
    @Test(groups = { "structured", "postgres","nightly" })
    public void testInsertExistingRow() {
        RaptureURI repo = helper.getRandomAuthority(Scheme.STRUCTURED);
        String repoStr = repo.toString();
        String config = "STRUCTURED { } USING POSTGRES { douglas=\"adams\" }";

        // Create a repo
        Boolean repoExists = structApi.structuredRepoExists(repoStr);
        Assert.assertFalse(repoExists, "Repo does not exist yet");

        structApi.createStructuredRepo(repoStr, config);
        repoExists = structApi.structuredRepoExists(repoStr);
        Assert.assertTrue(repoExists, "Repo should exist now");

        // Verify the config
        StructuredRepoConfig rc = structApi.getStructuredRepoConfig(repoStr);
        Assert.assertEquals(rc.getConfig(), config);

        // Create a table. Add and remove data
        String table = "//" + repo.getAuthority() + "/table";
        structApi.createTable(table, ImmutableMap.of("id", "int", "name", "varchar(255), PRIMARY KEY (id)"));

        Map<String, Object> row = new HashMap<>();
        row.put("id", 42);
        row.put("name", "Don't Panic");
        structApi.insertRow(table, row);
        
        row = new HashMap<>();
        row.put("id", 43);
        row.put("name", "Don't Panic More");
        structApi.insertRow(table, row);
        
        row = new HashMap<>();
        row.put("id", 44);
        row.put("name", "Don't Panic Even More");
        structApi.insertRow(table, row);

        try {
                structApi.insertRow(table, row);
                Assert.fail();
        } catch (Exception e) {
                Assert.assertTrue(e.getMessage().contains("duplicate key value violates"));
        }
    }
    
    @Test(groups = { "structured", "postgres","nightly" })
    public void testUpdateNonExistingRow() {
        RaptureURI repo = helper.getRandomAuthority(Scheme.STRUCTURED);
        String repoStr = repo.toString();
        String config = "STRUCTURED { } USING POSTGRES { douglas=\"adams\" }";

        // Create a repo
        Boolean repoExists = structApi.structuredRepoExists(repoStr);
        Assert.assertFalse(repoExists, "Repo does not exist yet");

        structApi.createStructuredRepo(repoStr, config);
        repoExists = structApi.structuredRepoExists(repoStr);
        Assert.assertTrue(repoExists, "Repo should exist now");

        // Verify the config
        StructuredRepoConfig rc = structApi.getStructuredRepoConfig(repoStr);
        Assert.assertEquals(rc.getConfig(), config);

        // Create a table. Add and remove data
        String table = "//" + repo.getAuthority() + "/table";
        structApi.createTable(table, ImmutableMap.of("id", "int", "name", "varchar(255), PRIMARY KEY (id)"));

        Map<String, Object> row = new HashMap<>();
        row.put("id", 42);
        row.put("name", "Don't Panic");
        structApi.insertRow(table, row);
        
        row = new HashMap<>();
        row.put("id", 43);
        row.put("name", "Don't Panic More");
        structApi.insertRow(table, row);
        
        row = new HashMap<>();
        row.put("id", 44);
        row.put("name", "Don't Panic Even More");
        structApi.insertRow(table, row);
        structApi.updateRows(table, ImmutableMap.of("name", "bob"), "id=400");
        List<Map<String, Object>> contents = structApi.selectRows(table, null, null, null, null, -1);
        Assert.assertEquals(contents.get(0), ImmutableMap.of("id", new Integer(42),"name","Don't Panic"));
        Assert.assertEquals(contents.get(1), ImmutableMap.of("id", new Integer(43),"name","Don't Panic More"));
        Assert.assertEquals(contents.get(2), ImmutableMap.of("id", new Integer(44),"name","Don't Panic Even More"));
        
    }
    
    @Test(groups = { "structured", "postgres","nightly" })
    public void testSelectWithWhereClause() {
        RaptureURI repo = helper.getRandomAuthority(Scheme.STRUCTURED);
        String repoStr = repo.toString();
        String config = "STRUCTURED { } USING POSTGRES { douglas=\"adams\" }";

        // Create a repo
        Boolean repoExists = structApi.structuredRepoExists(repoStr);
        Assert.assertFalse(repoExists, "Repo does not exist yet");

        structApi.createStructuredRepo(repoStr, config);
        repoExists = structApi.structuredRepoExists(repoStr);
        Assert.assertTrue(repoExists, "Repo should exist now");

        // Verify the config
        StructuredRepoConfig rc = structApi.getStructuredRepoConfig(repoStr);
        Assert.assertEquals(rc.getConfig(), config);

        // Create a table. Add and remove data
        String table = "//" + repo.getAuthority() + "/table";
        structApi.createTable(table, ImmutableMap.of("id", "int", "name", "varchar(255), PRIMARY KEY (id)"));

        Map<String, Object> row = new HashMap<>();
        row.put("id", 42);
        row.put("name", "Don't Panic");
        structApi.insertRow(table, row);
        
        row = new HashMap<>();
        row.put("id", 43);
        row.put("name", "Don't Panic More");
        structApi.insertRow(table, row);
        
        row = new HashMap<>();
        row.put("id", 44);
        row.put("name", "Don't Panic Even More");
        structApi.insertRow(table, row);

        List<Map<String, Object>> contents = structApi.selectRows(table, null, "id<43", null, null, -1);
        Assert.assertEquals(contents.size(), 1);
        Assert.assertEquals(contents.get(0), ImmutableMap.of("id", new Integer(42),"name","Don't Panic"));

    }
    
    @Test(groups = { "structured", "postgres","nightly" })
    public void testSelectWithInvalidWhereClause() {
        RaptureURI repo = helper.getRandomAuthority(Scheme.STRUCTURED);
        String repoStr = repo.toString();
        String config = "STRUCTURED { } USING POSTGRES { douglas=\"adams\" }";

        // Create a repo
        Boolean repoExists = structApi.structuredRepoExists(repoStr);
        Assert.assertFalse(repoExists, "Repo does not exist yet");

        structApi.createStructuredRepo(repoStr, config);
        repoExists = structApi.structuredRepoExists(repoStr);
        Assert.assertTrue(repoExists, "Repo should exist now");

        // Verify the config
        StructuredRepoConfig rc = structApi.getStructuredRepoConfig(repoStr);
        Assert.assertEquals(rc.getConfig(), config);

        // Create a table. Add and remove data
        String table = "//" + repo.getAuthority() + "/table";
        structApi.createTable(table, ImmutableMap.of("id", "int", "name", "varchar(255), PRIMARY KEY (id)"));


        try {
        	structApi.selectRows(table, null, "id*43", null, null, -1);
        	Assert.fail("Select statement should have failed from invalid where clause");
        } catch (Exception e) {
        	Assert.assertTrue(e.getMessage().contains("Failed to parse where clause"));
        }


    }
    
    @Test(groups = { "structured", "postgres","nightly" })
    public void testSelectColumns() {
        RaptureURI repo = helper.getRandomAuthority(Scheme.STRUCTURED);
        String repoStr = repo.toString();
        String config = "STRUCTURED { } USING POSTGRES { douglas=\"adams\" }";

        // Create a repo
        Boolean repoExists = structApi.structuredRepoExists(repoStr);
        Assert.assertFalse(repoExists, "Repo does not exist yet");

        structApi.createStructuredRepo(repoStr, config);
        repoExists = structApi.structuredRepoExists(repoStr);
        Assert.assertTrue(repoExists, "Repo should exist now");

        // Verify the config
        StructuredRepoConfig rc = structApi.getStructuredRepoConfig(repoStr);
        Assert.assertEquals(rc.getConfig(), config);

        // Create a table. Add and remove data
        String table = "//" + repo.getAuthority() + "/table";
        structApi.createTable(table, ImmutableMap.of("id", "int", "name", "varchar(255), PRIMARY KEY (id)"));

        Map<String, Object> row = new HashMap<>();
        row.put("id", 42);
        row.put("name", "Don't Panic");
        structApi.insertRow(table, row);
        
        row = new HashMap<>();
        row.put("id", 43);
        row.put("name", "Don't Panic More");
        structApi.insertRow(table, row);
        
        row = new HashMap<>();
        row.put("id", 44);
        row.put("name", "Don't Panic Even More");
        structApi.insertRow(table, row);

        List<Map<String, Object>> contents = structApi.selectRows(table, ImmutableList.of("id"), null, null, null, -1);
        Assert.assertEquals(contents.get(0), ImmutableMap.of("id", new Integer(42)));
        Assert.assertEquals(contents.get(1), ImmutableMap.of("id", new Integer(43)));
        Assert.assertEquals(contents.get(2), ImmutableMap.of("id", new Integer(44)));

    }

    @Test(groups = { "structured", "postgres","nightly" })
    public void testSelectColumnsDecending() {
        RaptureURI repo = helper.getRandomAuthority(Scheme.STRUCTURED);
        String repoStr = repo.toString();
        String config = "STRUCTURED { } USING POSTGRES { douglas=\"adams\" }";

        // Create a repo
        Boolean repoExists = structApi.structuredRepoExists(repoStr);
        Assert.assertFalse(repoExists, "Repo does not exist yet");

        structApi.createStructuredRepo(repoStr, config);
        repoExists = structApi.structuredRepoExists(repoStr);
        Assert.assertTrue(repoExists, "Repo should exist now");

        // Verify the config
        StructuredRepoConfig rc = structApi.getStructuredRepoConfig(repoStr);
        Assert.assertEquals(rc.getConfig(), config);

        // Create a table. Add and remove data
        String table = "//" + repo.getAuthority() + "/table";
        structApi.createTable(table, ImmutableMap.of("id", "int", "name", "varchar(255), PRIMARY KEY (id)"));

        Map<String, Object> row = new HashMap<>();
        row.put("id", 42);
        row.put("name", "Don't Panic");
        structApi.insertRow(table, row);
        
        row = new HashMap<>();
        row.put("id", 43);
        row.put("name", "Don't Panic More");
        structApi.insertRow(table, row);
        
        row = new HashMap<>();
        row.put("id", 44);
        row.put("name", "Don't Panic Even More");
        structApi.insertRow(table, row);

        List<Map<String, Object>> contents = structApi.selectRows(table, ImmutableList.of("id"), null, ImmutableList.of("id"), Boolean.FALSE, -1);
        Assert.assertEquals(contents.get(0), ImmutableMap.of("id", new Integer(44)));
        Assert.assertEquals(contents.get(1), ImmutableMap.of("id", new Integer(43)));
        Assert.assertEquals(contents.get(2), ImmutableMap.of("id", new Integer(42)));

    }
    
    @Test(groups = { "structured", "postgres","nightly" })
    public void testSelectColumnsOrdering() {
        RaptureURI repo = helper.getRandomAuthority(Scheme.STRUCTURED);
        String repoStr = repo.toString();
        String config = "STRUCTURED { } USING POSTGRES { douglas=\"adams\" }";

        // Create a repo
        Boolean repoExists = structApi.structuredRepoExists(repoStr);
        Assert.assertFalse(repoExists, "Repo does not exist yet");

        structApi.createStructuredRepo(repoStr, config);
        repoExists = structApi.structuredRepoExists(repoStr);
        Assert.assertTrue(repoExists, "Repo should exist now");

        // Verify the config
        StructuredRepoConfig rc = structApi.getStructuredRepoConfig(repoStr);
        Assert.assertEquals(rc.getConfig(), config);

        // Create a table. Add and remove data
        String table = "//" + repo.getAuthority() + "/table";
        structApi.createTable(table, ImmutableMap.of("id", "int", "name", "varchar(255), PRIMARY KEY (id)"));

        Map<String, Object> row = new HashMap<>();
        row.put("id", 42);
        row.put("name", "Evan");
        structApi.insertRow(table, row);
        
        row = new HashMap<>();
        row.put("id", 43);
        row.put("name", "Aaron");
        structApi.insertRow(table, row);
        
        row = new HashMap<>();
        row.put("id", 44);
        row.put("name", "Carl");
        structApi.insertRow(table, row);
        
        row = new HashMap<>();
        row.put("id", 45);
        row.put("name", "Bob");
        structApi.insertRow(table, row);

        List<Map<String, Object>> contents = structApi.selectRows(table, null, null, ImmutableList.of("name"), null, -1);

        Assert.assertEquals(contents.get(0), ImmutableMap.of("id", new Integer(43),"name","Aaron"));
        Assert.assertEquals(contents.get(1), ImmutableMap.of("id", new Integer(45),"name","Bob"));
        Assert.assertEquals(contents.get(2), ImmutableMap.of("id", new Integer(44),"name","Carl"));
        Assert.assertEquals(contents.get(3), ImmutableMap.of("id", new Integer(42),"name","Evan"));


    }
    
    @Test(groups = { "plugin", "nightly" })
    public void testInstallStructuredPlugin() throws Exception {

        String zipFilename = "resources/teststructcreate.zip";
        File f = new File(zipFilename);
        if (!f.exists()) {
            Assert.fail(f.getAbsolutePath());
        }
        String pluginName;
        PluginSandbox sandbox;
        ZipFile in = null;
        try {
            in = new ZipFile(zipFilename);
            PluginConfig plugin = new PluginUtils().getPluginConfigFromZip(zipFilename);
            if (plugin == null) {
                Assert.fail("CAnnot read " + f.getAbsolutePath());
            }

            sandbox = new PluginSandbox();
            sandbox.setConfig(plugin);
            sandbox.setStrict(true);
            pluginName = plugin.getPlugin();
            sandbox.setRootDir(new File(f.getParent(), pluginName));
            Enumeration<? extends ZipEntry> entries = in.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                if (entry.isDirectory()) {
                    continue;
                }

                sandbox.makeItemFromZipEntry(in, entry);
            }
        } finally {
            in.close();
        }

        Map<String, PluginTransportItem> payload = Maps.newLinkedHashMap();
        for (PluginSandboxItem item : sandbox.getItems(null)) {
            PluginTransportItem payloadItem = item.makeTransportItem();
            payload.put(item.getURI().toString(), payloadItem);
        }
        pluginApi.installPlugin(sandbox.makeManifest(null), payload);

        System.out.println("db=" + helper.getStructApi().structuredRepoExists("structured://structtest"));
        pluginApi.uninstallPlugin(pluginName);

        boolean installed = false;
        for (PluginConfig c : pluginApi.getInstalledPlugins())
            if (c.getPlugin().compareTo(pluginName) == 0) installed = true;

        Assert.assertFalse(installed, "Plugin did not uninstall");

    }
}
