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
package rapture.lock;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.testng.Assert;
import org.testng.Reporter;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import rapture.common.LockHandle;
import rapture.common.RaptureLockConfig;
import rapture.common.RaptureURI;
import rapture.common.Scheme;
import rapture.common.client.HttpAdminApi;
import rapture.common.client.HttpLockApi;
import rapture.common.impl.jackson.MD5Utils;
import rapture.helper.IntegrationTestHelper;

public class LockApiTest {
    
    private IntegrationTestHelper helper;
    private HttpLockApi lockApi = null;
    private HttpAdminApi admin = null;

    private static final String user = "User";
    private IntegrationTestHelper helper2;
    private HttpLockApi lockApi2 = null;
    private RaptureURI repoUri = null;

    @BeforeClass(groups =  { "nightly", "lock" })
    @Parameters({ "RaptureURL", "RaptureUser", "RapturePassword" })
    public void setUp(@Optional("http://localhost:8665/rapture") String url, @Optional("rapture") String username, @Optional("rapture") String password) {
        helper = new IntegrationTestHelper(url, username, password);
        lockApi = helper.getLockApi();
        admin = helper.getAdminApi();
        if (!admin.doesUserExist(user)) {
            admin.addUser(user, "Another User", MD5Utils.hash16(user), "user@incapture.net");
        }

        helper2 = new IntegrationTestHelper(url, user, user);
        lockApi2 = helper2.getLockApi();

        repoUri = helper.getRandomAuthority(Scheme.DOCUMENT);
        helper.configureTestRepo(repoUri, "MONGODB"); // TODO Make this configurable
    }

	@AfterClass(groups = { "nightly", "lock" })
	public void tearDown() {
		helper.cleanAllAssets();
		helper2.cleanAllAssets();
	}

	String winningContent;
	String winningPath;
	LockHandle lockHandle;
	List<String> winningContentList;

	// TODO: This test will hang jenkins build, disable for now
	@Test(groups = { "nightly", "lock" }, enabled=false)
	public void testMultipleRequestsAcquireReleaseLockZookeeper() {
		int threadCount = 25;
		winningContentList = new ArrayList<>();
		winningContent = "";
		winningPath = "";
		lockHandle = null;
		RaptureURI lockUri = RaptureURI.builder(helper.getRandomAuthority(Scheme.DOCUMENT)).docPath("foo/bar" + System.currentTimeMillis()).build();
		RaptureLockConfig lockConfig = lockApi.createLockManager(lockUri.toString(), "LOCKING USING ZOOKEEPER {}", "");
		Random rand = new Random();

		RaptureURI docRepoUri = helper.getRandomAuthority(Scheme.DOCUMENT);
		helper.configureTestRepo(docRepoUri, "MONGODB", true);
		List<Long> threadList = new ArrayList<>();

		class LockThread implements Runnable {
			@Override
            public void run() {
				long currThreadId = Thread.currentThread().getId();
				threadList.add(new Long(currThreadId));
				try {
					Double timeDelay = rand.nextDouble() * 5000;
					String content = "{\"key_" + currThreadId + "_" + rand.nextInt(5000) + "\" : \"" + currThreadId + "\" }";
					Reporter.log("Thread id " + currThreadId + " waiting for " + timeDelay.longValue() + " ms.", true);
					Thread.sleep(timeDelay.longValue());

					lockHandle = lockApi.acquireLock(lockUri.toString(), lockConfig.getName(), 2, 10);
					if (lockHandle != null) {
						winningPath = docRepoUri + "doc" + currThreadId;
						helper.getDocApi().putDoc(winningPath, content);
						winningContentList.add(content);
						Reporter.log("Thread id " + currThreadId + " acquired lock and wrote doc: " + content + " to " + winningPath, true);

						Thread.sleep(1000);
						Assert.assertTrue(lockApi.releaseLock(lockUri.toString(), lockConfig.getName(), lockHandle));
					} else {
						Reporter.log("Thread id " + currThreadId + " did not acquire lock.", true);
					}

				} catch (Exception e) {
					Reporter.log("Exception with thread " + currThreadId + ", " + e.getMessage(), true);
				}
				threadList.remove(new Long(currThreadId));
			}
		}

		Reporter.log("Running test with " + threadCount + " threads", true);
		for (int i = 0; i < threadCount; i++)
			new Thread(new LockThread()).start();

		while (threadList.size() > 0) {
			try {
				Thread.sleep(500);
				if (helper.getDocApi().getDoc(winningPath) != null) {
					Assert.assertTrue(winningContentList.contains(helper.getDocApi().getDoc(winningPath)));
				}
			} catch (Exception e) {
			}
		}

	}

	// TODO: This test will hang jenkins build, disable for now
	@Test(groups = { "nightly", "lock" }, dataProvider = "threadScenarios", enabled=false)
	public void testOneThreadBlockingMultipleRequestsZookeeper(Integer threadCount) {
		winningContent = "";
		winningPath = "";
		lockHandle = null;
		RaptureURI lockUri = RaptureURI.builder(helper.getRandomAuthority(Scheme.DOCUMENT)).docPath("foo/bar" + System.currentTimeMillis()).build();
		RaptureLockConfig lockConfig = lockApi.createLockManager(lockUri.toString(), "LOCKING USING ZOOKEEPER {}", "");
		Random rand = new Random();

		RaptureURI docRepoUri = helper.getRandomAuthority(Scheme.DOCUMENT);
		helper.configureTestRepo(docRepoUri, "MONGODB", false);
		List<Long> threadList = new ArrayList<>();

		class LockThread implements Runnable {
			@Override
            public void run() {
				long currThreadId = Thread.currentThread().getId();
				threadList.add(new Long(currThreadId));
				try {
					Double timeDelay = rand.nextDouble() * 5000;
					String content = "{\"key_" + currThreadId + "_" + rand.nextInt(5000) + "\" : \"" + currThreadId + "\" }";
					Reporter.log("Thread id " + currThreadId + " waiting for " + timeDelay.longValue() + " ms.", true);
					Thread.sleep(timeDelay.longValue());

					lockHandle = lockApi.acquireLock(lockUri.toString(), lockConfig.getName(), 2, 10);
					if (lockHandle != null) {
						winningPath = docRepoUri + "doc" + currThreadId;
						helper.getDocApi().putDoc(winningPath, content);
						Reporter.log("Thread id " + currThreadId + " acquired lock and wrote doc: " + content + " to " + winningPath, true);
						winningContent = content;
					} else {
						Reporter.log("Thread id " + currThreadId + " did not acquire lock.", true);
					}

				} catch (Exception e) {
					Reporter.log("Exception with thread " + currThreadId + ", " + e.getMessage(), true);
				}
				threadList.remove(new Long(currThreadId));
			}
		}

		Reporter.log("Running test with " + threadCount + " threads", true);
		for (int i = 0; i < threadCount; i++)
			new Thread(new LockThread()).start();

		while (threadList.size() > 0) {
			try {
                Thread.sleep(97);
			} catch (Exception e) {
			}
		}
		Assert.assertEquals(helper.getDocApi().getDoc(winningPath), winningContent);
		try {
            Assert.assertTrue(lockApi.releaseLock(lockUri.toString(), lockConfig.getName(), lockHandle));
        } catch (Exception e) {
            // Possible timing/race condition can cause the lock to have been unlocked already?
            Reporter.log("Exception releasing log; possible timing issue", true);
        }
	}

    @Test(groups = { "nightly", "lock" })
    public void testZookeeperLock() throws InterruptedException {

        // Player 1 acquires a lock
        RaptureURI lockUri = RaptureURI.builder(helper.getRandomAuthority(Scheme.DOCUMENT)).docPath("foo/bar" + System.currentTimeMillis()).build();
        RaptureLockConfig lockConfig = lockApi.createLockManager(lockUri.toString(), "LOCKING USING ZOOKEEPER {}", "");
        assertNotNull(lockConfig);
        LockHandle lockHandle = lockApi.acquireLock(lockUri.toString(), lockConfig.getName(), 1, 60);
        assertNotNull(lockHandle);
        Thread.sleep(100);

        // Meanwhile elsewhere Player 2 tries to acquire the lock
        RaptureLockConfig lockConfig2 = lockApi.getLockManagerConfig(lockUri.toString());
        assertNotNull(lockConfig2);
        LockHandle lockHandle2 = lockApi2.acquireLock(lockUri.toString(), lockConfig2.getName(), 1, 60);
        // but fails
        assertNull(lockHandle2);

        // Eventually player1 releases the lock
        Thread.sleep(100);
        assertTrue(lockApi.releaseLock(lockUri.toString(), lockConfig.getName(), lockHandle));
        assertFalse(lockApi2.releaseLock(lockUri.toString(), lockConfig2.getName(), lockHandle2));

        // and now Player 2 can acquire it
        lockHandle2 = lockApi2.acquireLock(lockUri.toString(), lockConfig2.getName(), 1, 60);
        assertNotNull(lockHandle2);
        assertTrue(lockApi2.releaseLock(lockUri.toString(), lockConfig2.getName(), lockHandle2));
        assertFalse(lockApi2.releaseLock(lockUri.toString(), lockConfig2.getName(), lockHandle2));

        assertNotNull(lockApi.getLockManagerConfig(lockUri.toString()));
        assertTrue(lockApi.lockManagerExists(lockUri.toString()));
    }

    @Test(groups =  { "nightly", "lock" })
    public void testMongoDBLock() throws InterruptedException {

        // Player 1 acquires a lock
        RaptureURI lockUri = RaptureURI.builder(helper.getRandomAuthority(Scheme.DOCUMENT)).docPath("foo/bar" + System.currentTimeMillis()).build();
        RaptureLockConfig lockConfig = lockApi.createLockManager(lockUri.toString(), "LOCKING USING MONGODB {}", "");
        assertNotNull(lockConfig);
        LockHandle lockHandle = lockApi.acquireLock(lockUri.toString(), lockConfig.getName(), 1, 60);
        assertNotNull(lockHandle);
        Thread.sleep(100);

        // Meanwhile elsewhere Player 2 tries to acquire the lock
        RaptureLockConfig lockConfig2 = lockApi.getLockManagerConfig(lockUri.toString());
        assertNotNull(lockConfig2);
        LockHandle lockHandle2 = lockApi2.acquireLock(lockUri.toString(), lockConfig2.getName(), 1, 60);
        // but fails
        assertNull(lockHandle2);

        // Eventually player1 releases the lock
        Thread.sleep(100);
        assertTrue(lockApi.releaseLock(lockUri.toString(), lockConfig.getName(), lockHandle));
        assertFalse(lockApi2.releaseLock(lockUri.toString(), lockConfig2.getName(), lockHandle2));

        // and now Player 2 can acquire it
        lockHandle2 = lockApi2.acquireLock(lockUri.toString(), lockConfig2.getName(), 1, 60);
        assertNotNull(lockHandle2);
        assertTrue(lockApi2.releaseLock(lockUri.toString(), lockConfig2.getName(), lockHandle2));
        // assertFalse(lockApi2.releaseLock(lockUri.toString(), lockConfig2.getName(), lockHandle2));

        assertNotNull(lockApi.getLockManagerConfig(lockUri.toString()));
        assertTrue(lockApi.lockManagerExists(lockUri.toString()));
    }

    @Test(groups =  { "nightly", "lock" })
    public void testRecreateLockManager() throws InterruptedException {

        // Player 1 acquires a lock
        RaptureURI lockUri = RaptureURI.builder(helper.getRandomAuthority(Scheme.DOCUMENT)).docPath("foo/bar" + System.currentTimeMillis()).build();
        RaptureLockConfig lockConfig = lockApi.createLockManager(lockUri.toString(), "LOCKING USING MONGODB {}", "");
        assertNotNull(lockConfig);
        LockHandle lockHandle = lockApi.acquireLock(lockUri.toString(), lockConfig.getName(), 1, 60);
        assertNotNull(lockHandle);

        // Player 2 tries to create a different lock manager for the same URI - this will be ignored
        RaptureLockConfig lockConfig2 = lockApi2.createLockManager(lockUri.toString(), "LOCKING USING MEMORY {}", "");
        assertNotNull(lockConfig);
        assertEquals(lockConfig, lockConfig2);
        RaptureLockConfig lockConfig3 = lockApi2.getLockManagerConfig(lockUri.toString());
        assertEquals(lockConfig, lockConfig3);

        LockHandle lockHandle2 = lockApi.acquireLock(lockUri.toString(), lockConfig3.getName(), 1, 60);
        assertNull(lockHandle2);
        assertTrue(lockApi.releaseLock(lockUri.toString(), lockConfig.getName(), lockHandle));
        lockHandle2 = lockApi2.acquireLock(lockUri.toString(), lockConfig3.getName(), 1, 60);
        assertNotNull(lockHandle2);
        assertTrue(lockApi2.releaseLock(lockUri.toString(), lockConfig3.getName(), lockHandle2));
    }
    
    @DataProvider(name = "threadScenarios")
    public Object[][] threadScenariosData() {
        return new Object[][] { {new Integer(10)},  
        					    {new Integer(20)},
        					    {new Integer(30)}
        					    };
    }
}
