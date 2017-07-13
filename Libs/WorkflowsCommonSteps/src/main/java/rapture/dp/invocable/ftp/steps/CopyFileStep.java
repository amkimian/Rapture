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
package rapture.dp.invocable.ftp.steps;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import com.google.common.collect.ImmutableList;

import rapture.common.CallingContext;
import rapture.common.RaptureURI;
import rapture.common.RaptureURIInputStream;
import rapture.common.RaptureURIOutputStream;
import rapture.common.Scheme;
import rapture.common.api.DecisionApi;
import rapture.common.exception.ExceptionToString;
import rapture.common.impl.jackson.JacksonUtil;
import rapture.dp.AbstractStep;
import rapture.kernel.Kernel;
import rapture.kernel.dp.ExecutionContextUtil;

public class CopyFileStep extends AbstractStep {
    private static final Logger log = Logger.getLogger(CopyFileStep.class);

    DecisionApi decision;

    private CallingContext context;
    public CopyFileStep(String workerUri, String stepName) {
        super(workerUri, stepName);
        decision = Kernel.getDecision();
    }

    public boolean copy(String localName, String remoteName) {

        InputStream inStream;
        try {
            if (localName.startsWith("file://")) {
                localName = localName.substring(6);
            }
            if (localName.startsWith("//")) {
                inStream = new RaptureURIInputStream(new RaptureURI(localName, Scheme.DOCUMENT));
            } else if (localName.contains("://")) {
                inStream = new RaptureURIInputStream(new RaptureURI(localName));
            } else {
                inStream = new FileInputStream(new File(localName));
            }

            OutputStream outStream;
            if (remoteName.startsWith("file://")) {
                remoteName = remoteName.substring(6);
            }
            if (remoteName.startsWith("//")) {
                outStream = new RaptureURIOutputStream(new RaptureURI(remoteName, Scheme.DOCUMENT)).setContext(context);
            } else if (remoteName.contains("://")) {
                outStream = new RaptureURIOutputStream(new RaptureURI(remoteName)).setContext(context);
            } else {
                Path target = Paths.get(remoteName);
                Files.createDirectories(target.getParent());
                outStream = new FileOutputStream(target.toFile());
            }
            IOUtils.copy(inStream, outStream);
            outStream.close();
            inStream.close();
            decision.writeWorkflowAuditEntry(context, getWorkerURI(), getStepName() + ": " + localName + " copied to " + remoteName, false);
            return true;
        } catch (IOException e) {
            log.debug(ExceptionToString.format(e));
            decision.writeWorkflowAuditEntry(context, getWorkerURI(),
                    "Problem in " + getStepName() + " - error is " + ExceptionToString.getRootCause(e).getLocalizedMessage(), true);
            return false;
        }
    }

    public String renderTemplate(CallingContext ctx, String template) {
        String workOrder = new RaptureURI(getWorkerURI()).toShortString();
        return ExecutionContextUtil.evalTemplateECF(ctx, workOrder, template, new HashMap<String, String>());
    }

    @Override
    public String invoke(CallingContext ctx) {
        try {
            this.context = ctx;
            String copy = StringUtils.stripToNull(decision.getContextValue(ctx, getWorkerURI(), "COPY_FILES"));
            if (copy == null) {
                decision.setContextLiteral(ctx, getWorkerURI(), getStepName(), "No files to copy");
                decision.setContextLiteral(ctx, getWorkerURI(), getErrName(), "No files to copy");
                decision.writeWorkflowAuditEntry(context, getWorkerURI(), getStepName() + ": No files to copy", false);
                return getNextTransition();
            }

            Map<String, Object> map = JacksonUtil.getMapFromJson(renderTemplate(ctx, copy));

            String retval = getNextTransition();
            int failCount = 0;
            StringBuilder sb = new StringBuilder();
            List<Entry<String, Object>> list = new ArrayList<>();
            for (Entry<String, Object> e : map.entrySet()) {
                Object value = e.getValue();
                List<String> targets;
                if (value instanceof List) {
                    targets = (List<String>) value;
                } else {
                    targets = ImmutableList.of(value.toString());
                }
                for (String target : targets) {
                    if (!copy(e.getKey(), target)) {
                        sb.append("Unable to copy ").append(e.getKey()).append(" to ").append(target).append("\n");
                        retval = getFailTransition();
                        failCount++;
                        list.add(e);
                    }
                }
            }

            decision.setContextLiteral(ctx, getWorkerURI(), getStepName(), (failCount > 0) ? "Unable to copy " + failCount + " files" : "All files copied");

            String err = sb.toString();
            if (!StringUtils.isEmpty(err)) {
                log.error(err);
                decision.writeWorkflowAuditEntry(context, getWorkerURI(), getStepName() + ": " + err, true);
            } else {
                decision.writeWorkflowAuditEntry(context, getWorkerURI(), getStepName() + ": All files copied", false);

            }
            decision.setContextLiteral(ctx, getWorkerURI(), getErrName(), err);
            return retval;
        } catch (Exception e) {
            decision.setContextLiteral(ctx, getWorkerURI(), getStepName(), "Unable to copy files : " + e.getLocalizedMessage());
            decision.setContextLiteral(ctx, getWorkerURI(), getErrName(), ExceptionToString.summary(e));
            log.error(ExceptionToString.format(ExceptionToString.getRootCause(e)));
            decision.writeWorkflowAuditEntry(ctx, getWorkerURI(),
                    "Problem in " + getStepName() + ": " + ExceptionToString.getRootCause(e).getLocalizedMessage(), true);
            return getErrorTransition();
        }
    }

}
