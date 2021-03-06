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
package rapture.plugin.install;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import rapture.plugin.install.PluginBatchMode;
import rapture.plugin.util.TestHelper;

import com.google.common.io.Files;

public class PluginBatchModeTest {

    private PluginBatchMode f;

    @Before
    public void setup() {
        f = new PluginBatchMode(null);
    }

    @Test
    public void testSortZips() throws IOException {
        File tempDir = Files.createTempDir();
        String importOrder = "walterwhite, xmen,Yoplait, zoo";
        File w = TestHelper.createTempZipWithPluginTxt(tempDir, "walterwhite-1.2.3.4.zip", "walterwhite");
        File x = TestHelper.createTempZipWithPluginTxt(tempDir, "xmen-1.2.3.4.zip", "xmen");
        File y = TestHelper.createTempZipWithPluginTxt(tempDir, "yahoo-1.2.3.4.zip", "Yoplait");
        File z = TestHelper.createTempZipWithPluginTxt(tempDir, "zoo-1.2.3.4.zip", "zoo");
        File a = TestHelper.createTempZipWithPluginTxt(tempDir, "badzip.zip", "badzip");
        File[] zips = { a, z, x, w, y };
        f.sortZips(zips, f.getCommaSeparatedStringsAsList(importOrder));
        assertTrue(zips[0].getName().startsWith("badzip"));
        assertTrue(zips[1].getName().startsWith("walterwhite"));
        assertTrue(zips[2].getName().startsWith("xmen"));
        assertTrue(zips[3].getName().startsWith("yahoo"));
        assertTrue(zips[4].getName().startsWith("zoo"));
    }

    @Test
    public void testExtractPluginName() throws IOException {
        File tempDir = Files.createTempDir();
        String fname1 = "xxxx-19810208124544.zip";
        File f1 = TestHelper.createTempZipWithPluginTxt(tempDir, fname1, "xxxx");
        assertEquals("xxxx", f.extractPluginName(f1.getPath()));
        String fname2 = "ihaveweirdnamethatdoesntmatchpluginname.zip";
        File f2 = TestHelper.createTempZipWithPluginTxt(tempDir, fname2, "realPluginName");
        assertEquals("realPluginName", f.extractPluginName(f2.getPath()));
        assertNull(f.extractPluginName("unknownfile.zip"));
    }

    @Test
    public void testGetZipFiles() throws IOException {
        File temp = Files.createTempDir();
        File w = new File(temp, "walterwhite-1.2.3.4.zip");
        w.createNewFile();
        File x = new File(temp, "xmen-1.2.3.4.zip");
        x.createNewFile();
        File y = new File(temp, "yahoo-1.2.3.4.zip");
        y.createNewFile();
        File z = new File(temp, "zoo-1.2.3.4.zip");
        z.createNewFile();
        File bad = new File(temp, "goldenstatewarriors-1.4.5.7.exe");
        bad.createNewFile();
        File[] result = f.getZipFiles(temp, null, null);
        assertEquals(4, result.length);
        for (File r : result) {
            assertTrue(r.getName().endsWith(".zip"));
        }
    }

    @Test
    public void testGetZipFilesWithIncludes() throws IOException {
        File tempDir = Files.createTempDir();
        String fname1 = "xxx-19810208124544.zip";
        TestHelper.createTempZipWithPluginTxt(tempDir, fname1, "xxx");
        String fname2 = "yyy-19810208124544.zip";
        TestHelper.createTempZipWithPluginTxt(tempDir, fname2, "yyy");
        String fname3 = "zzz-19810208124544.zip";
        TestHelper.createTempZipWithPluginTxt(tempDir, fname3, "zzz");
        List<String> includes = f.getCommaSeparatedStringsAsList("yyy, xxx");
        File[] result = f.getZipFiles(tempDir, includes, null);
        assertEquals(2, result.length);
        assertTrue(result[0].getName().startsWith("xxx"));
        assertTrue(result[1].getName().startsWith("yyy"));
        f.sortZips(result, includes);
        assertTrue(result[0].getName().startsWith("yyy"));
        assertTrue(result[1].getName().startsWith("xxx"));
    }

    @Test
    public void testGetZipFilesWithExcludes() throws IOException {
        File tempDir = Files.createTempDir();
        String fname1 = "xxx-19810208124544.zip";
        TestHelper.createTempZipWithPluginTxt(tempDir, fname1, "xxx");
        String fname2 = "yyy-19810208124544.zip";
        TestHelper.createTempZipWithPluginTxt(tempDir, fname2, "yyy");
        String fname3 = "zzz-19810208124544.zip";
        TestHelper.createTempZipWithPluginTxt(tempDir, fname3, "zzz");
        List<String> excludes = f.getCommaSeparatedStringsAsList("yyy, xxx");
        File[] result = f.getZipFiles(tempDir, null, excludes);
        assertEquals(1, result.length);
        assertTrue(result[0].getName().startsWith("zzz"));
    }

    @Test
    public void testSortArray() {
        File[] arr = { new File("d"), new File("b"), new File("c"), new File("a") };
        String order = "b, a,d";
        f.sortByList(arr, order);
        assertEquals("c", arr[0].getName());
        assertEquals("b", arr[1].getName());
        assertEquals("a", arr[2].getName());
        assertEquals("d", arr[3].getName());
    }
}
