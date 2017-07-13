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
package rapture.api.hooks.impl;

import org.apache.log4j.Logger;

import rapture.common.CallingContext;
import rapture.common.hooks.CallName;
import rapture.common.hooks.HookType;
import rapture.common.impl.jackson.JacksonUtil;
import rapture.kernel.Kernel;
import rapture.kernel.Pipeline2ApiImpl;

public class TopicPubHook extends AbstractApiHook {
    private Logger log = Logger.getLogger(getClass());

    @Override
    public void doExecute(HookType hookType, CallingContext context, CallName callName) {
        HookMessage msg = new HookMessage(callName.toString(), context.getUser());
        if (Pipeline2ApiImpl.usePipeline2) {
            Kernel.getPipeline2().broadcastMessage(context, "apiHook", JacksonUtil.jsonFromObject(msg));
        } else {
            Kernel.getPipeline().getTrusted().publishTopicMessage(context, "main", "raptureTopic", "apiHook", JacksonUtil.jsonFromObject(msg));
        }
    }

    public static String getStandardId() {
        return "TopicPubHook";
    }

}
