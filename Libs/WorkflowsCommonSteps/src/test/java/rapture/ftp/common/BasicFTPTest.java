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
package rapture.ftp.common;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import com.google.common.collect.ImmutableList;

import rapture.common.CallingContext;
import rapture.common.RaptureConstants;
import rapture.common.RaptureURI;
import rapture.common.Scheme;
import rapture.common.api.DocApi;
import rapture.common.exception.ExceptionToString;
import rapture.common.impl.jackson.JacksonUtil;
import rapture.config.ConfigLoader;
import rapture.config.RaptureConfig;
import rapture.ftp.common.FTPRequest.Action;
import rapture.ftp.common.FTPRequest.Status;
import rapture.kernel.ContextFactory;
import rapture.kernel.Kernel;

public class BasicFTPTest {

    static String saveInitSysConfig;
    static String saveRaptureRepo;
    private static final String auth = "test" + System.currentTimeMillis();
    private static final String REPO_USING_MEMORY = "REP {} USING MEMORY {prefix=\"/tmp/" + auth + "\"}";

    // Our own test server - not yet 100% working
    static FTPConnectionConfig incaptureFtpConfig = new FTPConnectionConfig().setAddress("54.193.53.219").setLoginId("pico").setPassword("Pico1ncap")
            .setUseSFTP(false);

    // Publicly available FTP/SFTP servers
    static FTPConnectionConfig ftpConfig = new FTPConnectionConfig().setAddress("speedtest.tele2.net").setLoginId("ftp").setPassword("foo@bar");
    static FTPConnectionConfig rebexSftpConfig = new FTPConnectionConfig().setAddress("test.rebex.net").setPort(22).setLoginId("demo").setPassword("password")
            .setUseSFTP(true);

    static final boolean FTP_Available = false;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        Assume.assumeTrue(FTP_Available);
        RaptureConfig.setLoadYaml(false);
        RaptureConfig config = ConfigLoader.getConf();
        saveRaptureRepo = config.RaptureRepo;
        saveInitSysConfig = config.InitSysConfig;

        config.RaptureRepo = REPO_USING_MEMORY;
        config.InitSysConfig = "NREP {} USING MEMORY { prefix=\"/tmp/" + auth + "/sys.config\"}";

        System.setProperty("LOGSTASH-ISENABLED", "false");
        Kernel.initBootstrap();

        Kernel.INSTANCE.clearRepoCache(false);
        Kernel.getAudit().createAuditLog(ContextFactory.getKernelUser(), new RaptureURI(RaptureConstants.DEFAULT_AUDIT_URI, Scheme.LOG).getAuthority(),
                "LOG {} using MEMORY {prefix=\"/tmp/" + auth + "\"}");
        Kernel.getLock().createLockManager(ContextFactory.getKernelUser(), "lock://kernel", "LOCKING USING DUMMY {}", "");
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
    }

    @Before
    public void setUp() throws Exception {
    }

    @After
    public void tearDown() throws Exception {
    }

    /**
     * Read single file via FTP
     */
    @Test

    public void testSingleFTPReadWithConfigUri() {
        CallingContext context = ContextFactory.getKernelUser();
        DocApi dapi = Kernel.getDoc();
        String configRepo = "document://test" + System.currentTimeMillis();
        dapi.createDocRepo(context, configRepo, "NREP {} USING MEMORY {}");
        dapi.putDoc(context, configRepo + "/Config", JacksonUtil.jsonFromObject(ftpConfig));

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        FTPRequest read = new FTPRequest(FTPRequest.Action.READ).setDestination(baos).setRemoteName("1KB.zip");
        Connection connection = new SFTPConnection(configRepo + "/Config");
        connection.doAction(read);
        assertEquals(1024, baos.size());
        for (byte b : baos.toByteArray()) {
            assertEquals(0, b);
        }
        connection.logoffAndDisconnect();
    }

    /**
     * Read single file via FTP
     */
    @Test

    public void testSingleFTPRead() {
        Connection connection = new SFTPConnection(ftpConfig);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        FTPRequest read = new FTPRequest(FTPRequest.Action.READ).setDestination(baos).setRemoteName("1KB.zip");
        connection.doAction(read);
        assertEquals(1024, baos.size());
        for (byte b : baos.toByteArray()) {
            assertEquals(0, b);
        }
        connection.logoffAndDisconnect();
    }

    /**
     * Read single file via SFTP
     */
    @Test

    public void testSingleSFTPRead() {
        Connection connection = new SFTPConnection(rebexSftpConfig);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        FTPRequest read = new FTPRequest(FTPRequest.Action.READ).setDestination(baos).setRemoteName("readme.txt");
        connection.doAction(read);
        connection.logoffAndDisconnect();
        assertEquals(403, baos.size());
        String[] data = baos.toString().split("\r");
        assertEquals("Welcome,", data[0]);
    }

    /**
     * Check existence of file via FTP
     */
    @Test

    public void testFTPExists() {
        Connection connection = new SFTPConnection(ftpConfig);
        List<FTPRequest> reads = new ArrayList<>();
        reads.add(new FTPRequest(FTPRequest.Action.EXISTS).setRemoteName("1KB.zip"));
        reads.add(new FTPRequest(FTPRequest.Action.EXISTS).setRemoteName("Holy_Grail.zip"));
        connection.doActions(reads);
        assertEquals(Status.SUCCESS, reads.get(0).getStatus());
        assertEquals(Status.ERROR, reads.get(1).getStatus());
        connection.logoffAndDisconnect();
    }

    /**
     * Check existence of file via SFTP
     */
    @Test

    public void testSFTPExists() {
        Connection connection = new SFTPConnection(rebexSftpConfig);
        List<FTPRequest> reads = new ArrayList<>();
        reads.add(new FTPRequest(FTPRequest.Action.EXISTS).setRemoteName("readme.txt"));
        reads.add(new FTPRequest(FTPRequest.Action.EXISTS).setRemoteName("readyou.txt"));
        connection.doActions(reads);
        assertEquals(Status.SUCCESS, reads.get(0).getStatus());
        assertEquals(Status.ERROR, reads.get(1).getStatus());
        connection.logoffAndDisconnect();
    }

    /**
     * Read multiple files via FTP
     */
    @Test
    public void testMultiFTPRead() throws IOException {
        Connection connection = new SFTPConnection(ftpConfig);
        List<String> files = ImmutableList.of("1KB.zip", "100KB.zip");
        assertEquals(2, files.size());
        List<FTPRequest> reads = new ArrayList<>();
        Path tmpDir = Files.createTempDirectory("test");
        for (String file : files) {
            reads.add(new FTPRequest(Action.READ).setRemoteName(file).setLocalName(tmpDir + "/" + file.substring(file.lastIndexOf('/') + 1)));
        }
        connection.doActions(reads);
        connection.logoffAndDisconnect();
        File f = new File(reads.get(0).getLocalName());
        assertTrue(f.exists());
        assertTrue(f.canRead());
        assertEquals(1024, f.length());

        f = new File(reads.get(1).getLocalName());
        assertTrue(f.exists());
        assertTrue(f.canRead());
        assertEquals(102400, f.length());
    }

    /**
     * Read multiple files via SFTP
     */
    @Test

    public void testMultiSFTPRead() throws IOException {
        Connection connection = new SFTPConnection(rebexSftpConfig);
        FTPRequest fr = new FTPRequest(Action.READ);
        connection.connectAndLogin(fr);
        List<String> files = connection.listFiles("pub/example/*", fr);
        assertNull(fr.getErrors());
        assertEquals(19, files.size());
        List<FTPRequest> reads = new ArrayList<>();
        Path tmpDir = Files.createTempDirectory("test");
        for (String file : files) {
            reads.add(new FTPRequest(Action.READ).setRemoteName("pub/example/" + file).setLocalName(tmpDir + "/" + file.substring(file.lastIndexOf('/') + 1)));
        }
        connection.doActions(reads);
        connection.logoffAndDisconnect();
        File f = new File(reads.get(18).getLocalName());
        assertTrue(f.exists());
        assertTrue(f.canRead());
        assertEquals(17911, f.length());
    }

    /**
     * write single file via FTP -
     */
    @Test
    @Ignore
    public void testSingleFTPWrite() {
        Connection connection = new FTPConnection(ftpConfig);
        ByteArrayInputStream stream = new ByteArrayInputStream("Get loose, get loose!".getBytes());
        FTPRequest write = new FTPRequest(FTPRequest.Action.WRITE).setSource(stream).setRemoteName("upload/junk.1KB.zipfile");
        connection.doAction(write);
        connection.logoffAndDisconnect();
    }

    /**
     * write single file via SFTP - need a SFTP serverw ith write access to test
     */
    @Test
    @Ignore
    public void testSingleSFTPWrite() {
        Connection connection = new SFTPConnection(rebexSftpConfig);
        ByteArrayInputStream stream = new ByteArrayInputStream("Get loose, get loose!".getBytes());
        FTPRequest read = new FTPRequest(FTPRequest.Action.WRITE).setSource(stream).setRemoteName("junk");
        connection.doAction(read);
        connection.logoffAndDisconnect();
    }

    /**
     * Check for sensible error messages
     */
    @Test

    public void errorMessage1() {
        FTPConnectionConfig ftpConfig = new FTPConnectionConfig().setAddress("non.ext.iste.nt").setLoginId("ftp").setPassword("foo@bar").setUseSFTP(true);
        Connection connection = new SFTPConnection(ftpConfig);
        try {
            connection.connectAndLogin(new FTPRequest(FTPRequest.Action.WRITE));
        } catch (Exception e) {
            Assert.assertEquals("Unknown host non.ext.iste.nt", e.getMessage());
        }
    }

    @Test

    public void errorMessage2() {
        FTPConnectionConfig ftpConfig = new FTPConnectionConfig().setAddress("localhost").setLoginId("ftp").setPassword("foo@bar").setUseSFTP(true);
        Connection connection = new SFTPConnection(ftpConfig);
        try {
            connection.connectAndLogin(new FTPRequest(FTPRequest.Action.WRITE));
        } catch (Exception e) {
            Assert.assertEquals("Unable to establish secure FTP connection to localhost", e.getMessage());
        }
    }

    @Test

    public void errorMessage3() {
        FTPConnectionConfig ftpConfig = new FTPConnectionConfig().setAddress("non.ext.iste.nt").setLoginId("ftp").setPassword("foo@bar").setUseSFTP(false);
        Connection connection = new FTPConnection(ftpConfig);
        try {
            connection.connectAndLogin(new FTPRequest(FTPRequest.Action.WRITE));
        } catch (Exception e) {
            Assert.assertEquals("Unknown host non.ext.iste.nt", e.getMessage());
        }
    }

    @Test

    public void errorMessage4() {
        FTPConnectionConfig ftpConfig = new FTPConnectionConfig().setAddress("localhost").setLoginId("ftp").setPassword("foo@bar").setUseSFTP(false);
        Connection connection = new FTPConnection(ftpConfig);
        try {
            connection.connectAndLogin(new FTPRequest(FTPRequest.Action.WRITE));
        } catch (Exception e) {
            // There seem to be two possible code paths here.
            // Normal behaviour is for ftpClient.login to fail and for FTPConnection.connectAndLogin to throw RaptureException
            // but it seems that sometimes (particularly on Jenkins) ftpClient.login throws an exception
            // which is caught and wrapped by FTPService.runWithRetry.
            System.out.println(ExceptionToString.summary(e));
            Assert.assertEquals("Unable to login", e.getMessage());
        }
    }

}
