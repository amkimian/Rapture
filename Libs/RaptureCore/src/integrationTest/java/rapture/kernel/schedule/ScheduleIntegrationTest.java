/**
 * Copyright (C) 2011-2015 Incapture Technologies LLC
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
package rapture.kernel.schedule;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SimpleTimeZone;

import org.junit.Before;
import org.junit.Test;

import rapture.common.CallingContext;
import rapture.common.RaptureJobExec;
import rapture.common.RaptureScriptLanguage;
import rapture.common.RaptureScriptPurpose;
import rapture.common.exception.RaptureException;
import rapture.common.model.RaptureExchange;
import rapture.common.model.RaptureExchangeQueue;
import rapture.common.model.RaptureExchangeType;
import rapture.kernel.ContextFactory;
import rapture.kernel.Kernel;

/**
 * Create a test that puts a script to be run on a schedule, and show that it
 * runs on that schedule through the pipeline and that the job exec status is
 * updated correctly.
 *
 * @author amkimian
 */
public class ScheduleIntegrationTest {
    SimpleDateFormat sdf = new SimpleDateFormat();

    @Before
    public void setup() {
        sdf.setTimeZone(new SimpleTimeZone(0, "GMT"));
        sdf.applyPattern("dd MMM yyyy HH:mm:ss z");

        CallingContext ctx = ContextFactory.getKernelUser();

        try {
            Kernel.initBootstrap(null, null, true);

            Kernel.getPipeline().getTrusted().registerServerCategory(ctx, "alpha", "Primary servers");
            Kernel.getPipeline().getTrusted().registerServerCategory(ctx, "beta", "Secondary servers");

            Kernel.getPipeline().registerExchangeDomain(ctx, "//main", "EXCHANGE {} USING MEMORY {}");

            RaptureExchange exchange = new RaptureExchange();
            exchange.setName("kernel");
            exchange.setExchangeType(RaptureExchangeType.FANOUT);
            exchange.setDomain("main");

            List<RaptureExchangeQueue> queues = new ArrayList<RaptureExchangeQueue>();
            RaptureExchangeQueue queue = new RaptureExchangeQueue();
            queue.setName("default");
            queue.setRouteBindings(new ArrayList<String>());
            queues.add(queue);

            exchange.setQueueBindings(queues);

            Kernel.getPipeline().getTrusted().registerPipelineExchange(ctx, "kernel", exchange);
            Kernel.getPipeline().getTrusted().bindPipeline(ctx, "alpha", "kernel", "default");
            Kernel.setCategoryMembership("alpha");

        } catch (RaptureException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testKernelPipeline() {
        CallingContext ctx = ContextFactory.getKernelUser();

        // Create a script to run
        String script = "println('Hello from the Reflex script, value is ' + _params['value']);\n" + "_params['value'] = _params['value'] + '...';";
        Kernel.getScript().createScript(ctx, "//test/hello", RaptureScriptLanguage.REFLEX, RaptureScriptPurpose.PROGRAM, script);

        List<String> dependents = new ArrayList<String>();
        dependents.add("testJob2");
        Map<String, String> testParams = new HashMap<String, String>();
        testParams.put("value", "42");
        Kernel.getSchedule().getTrusted().setJobLink(ctx, "//test/testJob", "//test/testJob2");
        Kernel.getSchedule().createJob(ctx, "//test/testJob", "A test Job", "hello", "* * * * * *", "America/New_York", testParams, true);
        Kernel.getSchedule().createJob(ctx, "//test/testJob2", "A dependent test Job", "hello", "* * * * * *", "America/New_York",
                new HashMap<String, String>(), false);
        // At least wait for a minute

        List<RaptureJobExec> execs = Kernel.getSchedule().getUpcomingJobs(ctx);
        for (RaptureJobExec e : execs) {
            System.out.println("Job exec " + e.getStoragePath() + " will run at " + sdf.format(new Date(e.getExecTime())));
        }

        for (int i = 0; i < 6; i++) {
            try {
                // System.out.println("Checking job status");
                ScheduleManager.manageJobExecStatus();
                printStatusInformation();
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

        execs = Kernel.getSchedule().getJobExecs(ctx, "//authority/testJob", 0, 10, false);

        for (RaptureJobExec e : execs) {
            System.out.println("Job exec " + e.getStoragePath() + " with exec time " + sdf.format(new Date(e.getExecTime())) + " with status " + e.getStatus());
        }

    }

    private void printStatusInformation() {
        List<ScheduleStatusLine> lines = ScheduleManager.getSchedulerStatus();
        System.out.println("Name\tSchedule\tDesc\tWhen\tActive\tStatus\tPredecessor");
        for (ScheduleStatusLine line : lines) {
            System.out.println(line.getName() + "\t" + line.getSchedule() + "\t" + line.getDescription() + "\t"
                    + (line.getWhen() == null ? "" : line.getWhen().toString()) + "\t" + line.getActivated() + "\t" + line.getStatus() + "\t"
                    + line.getPredecessor());
        }
    }
}
