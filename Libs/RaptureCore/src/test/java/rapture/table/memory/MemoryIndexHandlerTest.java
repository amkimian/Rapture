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
package rapture.table.memory;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import rapture.common.RaptureURI;
import rapture.common.TableQueryResult;
import rapture.common.dp.WorkOrder;
import rapture.common.dp.WorkOrderStorage;
import rapture.common.exception.ExceptionToString;
import rapture.common.exception.RaptureException;
import rapture.common.impl.jackson.JacksonUtil;
import rapture.dsl.iqry.InvalidQueryException;
import rapture.kernel.Kernel;

public class MemoryIndexHandlerTest {

    private static final String U1 = "workorder://MemoryIndexHandlerTest/1";
    private static final String U2 = "workorder://MemoryIndexHandlerTest/2";
    private static final String U3 = "workorder://MemoryIndexHandlerTest/3";
    private static final String U4 = "workorder://MemoryIndexHandlerTest/4";

    private static final Integer P1 = 0;
    private static final Integer P2 = 0;
    private static final Integer P3 = 4;
    private static final Integer P4 = 5;

    private static final Long S1 = 4L;
    private static final Long S2 = 3L;
    private static final Long S3 = 2L;
    private static final Long S4 = 1L;

    private static final Long E1 = 5L;
    private static final Long E2 = 10L;
    private static final Long E3 = 10L;
    private static final Long E4 = 12L;

    private static boolean doNotRunOnShippable = true;
    private static boolean doNotRun = false;

    @BeforeClass
    public static void beforeClass() {
        Kernel.initBootstrap();

        if (System.getenv("SHIPPABLE_IS_OFFICIAL_IMAGE") != null) {
            System.err.println("This test needs modification to work on Shippable.");
            doNotRun = doNotRunOnShippable;
        }

        WorkOrderStorage.add(new RaptureURI("workorder://1476662400/workflow/metrics/testNoArgs/wo.00000002"),
                create("workorder://1476662400/workflow/metrics/testNoArgs/wo.00000002", P1, S1, E1), "test", "unit test");
        WorkOrderStorage.add(new RaptureURI(U2), create(U2, P2, S2, E2), "test", "unit test");
        WorkOrderStorage.add(new RaptureURI(U3), create(U3, P3, S3, E3), "test", "unit test");
        WorkOrderStorage.add(new RaptureURI(U4), create(U4, P4, S4, E4), "test", "unit test");

        // Clean up anything left behind by previous tests. Necessary only when running in Docker
        String query = String.format("SELECT workOrderURI ");
        TableQueryResult o = WorkOrderStorage.queryIndex(query);

        if (o.getRows() != null) {
            for (List row : o.getRows()) {
                for (Object ob : row) {
                    WorkOrderStorage.deleteByFields(ob.toString(), "test", "test");
                    RaptureURI uri = new RaptureURI(ob.toString());
                    WorkOrderStorage.removeFolder(uri.toAuthString());
                }
            }
        }
    }

    @Before
    public void before() {
        WorkOrderStorage.add(new RaptureURI(U1), create(U1, P1, S1, E1), "test", "unit test");
        WorkOrderStorage.add(new RaptureURI(U2), create(U2, P2, S2, E2), "test", "unit test");
        WorkOrderStorage.add(new RaptureURI(U3), create(U3, P3, S3, E3), "test", "unit test");
        WorkOrderStorage.add(new RaptureURI(U4), create(U4, P4, S4, E4), "test", "unit test");
    }

    static private WorkOrder create(String workOrderURI, Integer priority, Long startTime, Long endTime) {
        WorkOrder workorder = new WorkOrder();
        workorder.setWorkOrderURI(workOrderURI);
        workorder.setPriority(priority);
        workorder.setStartTime(startTime);
        workorder.setEndTime(endTime);
        return workorder;
    }

    @After
    public void after() {
        WorkOrderStorage.deleteByFields(U1, "test", "test");
        WorkOrderStorage.deleteByFields(U2, "test", "test");
        WorkOrderStorage.deleteByFields(U3, "test", "test");
        WorkOrderStorage.deleteByFields(U4, "test", "test");
    }

    @Test
    public void testQueryEq() throws Exception {
        if (doNotRun) return;
        String query = String.format("SELECT workOrderURI, priority, startTime, endTime WHERE workOrderURI=\"%s\"", U1);
        TableQueryResult results = WorkOrderStorage.queryIndex(query);
        assertEquals(1, results.getRows().size());
        List<Object> row = results.getRows().get(0);
        assertEquals(4, row.size());
        assertEquals(U1, row.get(0).toString());
        assertEquals(P1, row.get(1));
        assertEquals(S1.intValue(), Integer.parseInt(row.get(2).toString()));
        assertEquals(E1.intValue(), Integer.parseInt(row.get(3).toString()));
    }

    @Test
    public void testQueryEqOrder() throws Exception {
        if (doNotRun) return;
        String query = String.format("SELECT endTime, startTime, priority, workOrderURI WHERE workOrderURI=\"%s\"", U1);
        TableQueryResult results = WorkOrderStorage.queryIndex(query);
        assertEquals(1, results.getRows().size());
        List<Object> row = results.getRows().get(0);
        assertEquals(4, row.size());
        assertEquals(U1, row.get(3).toString());
        assertEquals(P1, row.get(2));
        assertEquals(S1.intValue(), Integer.parseInt(row.get(1).toString()));
        assertEquals(E1.intValue(), Integer.parseInt(row.get(0).toString()));

    }

    @Test
    public void testQueryEqSomeColumns() throws Exception {
        if (doNotRun) return;
        String query = String.format("SELECT endTime, workOrderURI WHERE workOrderURI=\"%s\"", U1);
        TableQueryResult results = WorkOrderStorage.queryIndex(query);
        assertEquals(1, results.getRows().size());
        List<Object> row = results.getRows().get(0);
        assertEquals(2, row.size());
        assertEquals(U1, row.get(1).toString());
        assertEquals(E1.intValue(), Integer.parseInt(row.get(0).toString()));

    }

    @Test
    public void testQueryGt() throws Exception {
        if (doNotRun) return;
        String query = String.format("SELECT endTime, workOrderURI WHERE priority > \"%s\"", P1);
        TableQueryResult results = WorkOrderStorage.queryIndex(query);
        assertEquals(2, results.getRows().size());

        query = String.format("SELECT endTime, workOrderURI WHERE priority > \"%s\"", P3);
        results = WorkOrderStorage.queryIndex(query);
        assertEquals(1, results.getRows().size());
        List<Object> row = results.getRows().get(0);
        assertEquals(2, row.size());
        assertEquals(U4, row.get(1).toString());
        assertEquals(E4.intValue(), Integer.parseInt(row.get(0).toString()));

    }

    @Test
    public void testQueryLt() throws Exception {
        if (doNotRun) return;
        String query = String.format("SELECT endTime, workOrderURI WHERE priority < \"%s\"", P3);
        TableQueryResult results = WorkOrderStorage.queryIndex(query);
        assertEquals(2, results.getRows().size());

        query = String.format("SELECT endTime, workOrderURI WHERE priority < \"%s\"", P4);
        results = WorkOrderStorage.queryIndex(query);
        assertEquals(3, results.getRows().size());

    }

    @Test
    public void testQueryNe() throws Exception {
        if (doNotRun) return;
        String query = String.format("SELECT endTime, workOrderURI WHERE priority != \"%s\"", P3);
        TableQueryResult results = WorkOrderStorage.queryIndex(query);
        assertEquals(3, results.getRows().size());

        query = String.format("SELECT endTime, workOrderURI WHERE priority != \"%s\"", P1);
        results = WorkOrderStorage.queryIndex(query);
        assertEquals(2, results.getRows().size());
    }

    @Test
    public void testQueryCombined() throws Exception {
        if (doNotRun) return;
        String query = String.format("SELECT endTime, startTime, priority, workOrderURI WHERE priority > \"%s\" AND priority < \"%s\"", P2, P4);
        TableQueryResult results = WorkOrderStorage.queryIndex(query);
        if (results.getRows().size() > 1) {
            fail("Looks like some clean up is needed: Expected 1 URI but got " + results.getRows().size() + "\n" + JacksonUtil.jsonFromObject(results));
        }
        assertEquals(1, results.getRows().size());

        List<Object> row = results.getRows().get(0);
        assertEquals(U3, row.get(3).toString());
        assertEquals(P3, row.get(2));
        assertEquals(S3.intValue(), Integer.parseInt(row.get(1).toString()));
        assertEquals(E3.intValue(), Integer.parseInt(row.get(0).toString()));
    }

    @Test
    public void testQueryLimit() throws Exception {
        if (doNotRun) return;
        String query = String.format("SELECT endTime, workOrderURI WHERE priority < \"%s\" LIMIT 1", P3);
        TableQueryResult results = WorkOrderStorage.queryIndex(query);
        if (results.getRows().size() > 1) {
            fail("Looks like some clean up is needed: Expected 1 URI but got " + results.getRows().size() + "\n" + JacksonUtil.jsonFromObject(results));
        }
        assertEquals(1, results.getRows().size());

        query = String.format("SELECT endTime, workOrderURI WHERE priority < \"%s\" LIMIT 2", P4);
        results = WorkOrderStorage.queryIndex(query);
        if (results.getRows().size() > 2) {
            fail("Looks like some clean up is needed: Expected 2 URIs but got " + results.getRows().size() + "\n" + JacksonUtil.jsonFromObject(results));
        }
        assertEquals(2, results.getRows().size());

        query = String.format("SELECT endTime, workOrderURI WHERE priority < \"%s\" LIMIT 1", P4);
        results = WorkOrderStorage.queryIndex(query);
        if (results.getRows().size() > 1) {
            fail("Looks like some clean up is needed: Expected 1 URI but got " + results.getRows().size() + "\n" + JacksonUtil.jsonFromObject(results));
        }
        assertEquals(1, results.getRows().size());
    }

    @Test
    public void testQueryOrderBy() throws Exception {
        if (doNotRun) return;
        String query = String.format("SELECT workOrderURI, startTime, priority WHERE priority < \"%s\" ORDER BY startTime", P4);
        TableQueryResult results = WorkOrderStorage.queryIndex(query);
        List<List<Object>> rows = results.getRows();
        assertEquals(3, rows.size());
    }

    @Test
    public void testQueryOrderAsc() throws Exception {
        if (doNotRun) return;
        String query = String.format("SELECT workOrderURI, startTime, priority WHERE priority < \"%s\" ORDER BY startTime ASC", P4);
        TableQueryResult results = WorkOrderStorage.queryIndex(query);
        List<List<Object>> rows = results.getRows();
        if (rows.size() > 3) {
            fail("Looks like some clean up is needed: Expected 3 URIs but got " + rows.size() + "\n" + JacksonUtil.jsonFromObject(rows));
        }
        assertEquals(3, rows.size());

        assertEquals(U3, rows.get(0).get(0).toString());
        assertEquals(U2, rows.get(1).get(0).toString());

    }

    @Test
    public void testQueryOrderDesc() throws Exception {
        if (doNotRun) return;
        String query = String.format("SELECT workOrderURI, startTime, priority WHERE priority < \"%s\" ORDER BY startTime DESC", P4);

        TableQueryResult results = new TableQueryResult();
        Object o = WorkOrderStorage.queryIndex(query);
        assertEquals(results.getClass().getCanonicalName(), o.getClass().getCanonicalName());

        /* TableQueryResult */ results = WorkOrderStorage.queryIndex(query);
        List<List<Object>> rows = results.getRows();
        if (rows.size() > 3) {
            fail("Looks like some clean up is needed: Expected 3 URIs but got " + rows.size() + "\n" + JacksonUtil.jsonFromObject(rows));
        }

        assertEquals(3, rows.size());
        assertEquals(U1, rows.get(0).get(0).toString());
        assertEquals(U2, rows.get(1).get(0).toString());

    }

    @Test
    public void testQueryOrderMixed() throws Exception {
        if (doNotRun) return;
        String query = String.format("SELECT workOrderURI, startTime, priority WHERE priority < \"%s\" ORDER BY priority, startTime DESC", P4);
        TableQueryResult results = WorkOrderStorage.queryIndex(query);
        List<List<Object>> rows = results.getRows();
        if (rows.size() > 3) {
            fail("Looks like some clean up is needed: Expected 3 URIs but got " + rows.size() + "\n" + JacksonUtil.jsonFromObject(rows));
        }
        assertEquals(3, rows.size());

        assertEquals(U3, rows.get(0).get(0).toString());
        assertEquals(U1, rows.get(1).get(0).toString());
        assertEquals(U2, rows.get(2).get(0).toString());

    }

    @Test
    public void testOrderByInvalid() throws Exception {
        if (doNotRun) return;
        String query = String.format("SELECT workOrderURI, priority WHERE priority < \"%s\" ORDER BY startTime DESC", P4);
        try {
            WorkOrderStorage.queryIndex(query);
            fail("Error, should have thrown RaptureException");
        } catch (RaptureException e) {
            assertTrue(String.format("Error is %s", ExceptionToString.format(e)), e.getMessage().contains("Invalid query"));
            assertTrue(String.format("Error is %s", ExceptionToString.format(e)), e.getCause() instanceof InvalidQueryException);
        }
    }
}
