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
package rapture.kernel.alert.email;

import javax.mail.MessagingException;

import org.apache.log4j.Logger;

import rapture.common.impl.jackson.JacksonUtil;
import rapture.event.RaptureAlertEvent;
import rapture.kernel.ContextFactory;
import rapture.kernel.Kernel;
import rapture.kernel.alert.EventAlerter;
import rapture.mail.EmailTemplate;
import rapture.mail.Mailer;

public class EmailAlerter implements EventAlerter {
    public static final String TYPE = "EMAIL";

    private static final Logger logger = Logger.getLogger(EmailAlerter.class);
    private static final String TEMPLATE_URI = "dp/alert/template/email/";

    private EmailTemplate emailTemplate;

    public EmailAlerter(String templateName) {
        String templateJson = Kernel.getSys().retrieveSystemConfig(ContextFactory.getKernelUser(),
                "CONFIG", TEMPLATE_URI + templateName);
        this.emailTemplate = JacksonUtil.objectFromJson(templateJson, EmailTemplate.class);
    }

    @Override
    public String getType() {
        return TYPE;
    }

    @Override
    public void alert(RaptureAlertEvent event) {
        String[] to = new String[1];
        to[0] = emailTemplate.getEmailTo();
        String subject = event.parseTemplate(emailTemplate.getSubject());
        String msgBody = event.parseTemplate(emailTemplate.getMsgBody());
        try {
            Mailer.email(to, subject, msgBody);
        } catch (MessagingException e) {
            logger.warn("Unable to send email", e);
        }
    }

}
