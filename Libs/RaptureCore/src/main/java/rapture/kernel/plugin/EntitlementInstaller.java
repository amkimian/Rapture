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
package rapture.kernel.plugin;

import java.util.Iterator;

import org.apache.commons.collections.CollectionUtils;

import rapture.common.CallingContext;
import rapture.common.PluginTransportItem;
import rapture.common.RaptureURI;
import rapture.common.impl.jackson.JacksonUtil;
import rapture.common.model.RaptureEntitlement;
import rapture.kernel.Kernel;

public class EntitlementInstaller implements RaptureInstaller {

    @Override
    public void install(CallingContext context, RaptureURI uri, PluginTransportItem item) {
        RaptureEntitlement entitlement = JacksonUtil.objectFromJson(item.getContent(), RaptureEntitlement.class);
        if (CollectionUtils.isEmpty(entitlement.getGroups())) {
            Kernel.getEntitlement().addEntitlement(context, entitlement.getName(), null);
        } else {
            Iterator<String> iterator = entitlement.getGroups().iterator();
            Kernel.getEntitlement().addEntitlement(context, entitlement.getName(), iterator.next());
            while (iterator.hasNext()) {
                Kernel.getEntitlement().addGroupToEntitlement(context, entitlement.getName(), iterator.next());
            }
        }
    }

    @Override
    public void remove(CallingContext context, RaptureURI uri, PluginTransportItem item) {
        Kernel.getEntitlement().deleteEntitlement(context, uri.getShortPath());
    }
}