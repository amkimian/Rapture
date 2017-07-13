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
package reflex.function;

import org.apache.log4j.Logger;

import reflex.IReflexHandler;
import reflex.Scope;
import reflex.debug.IReflexDebugger;
import reflex.node.BaseNode;
import reflex.node.ReflexNode;
import reflex.value.ReflexValue;
import reflex.value.internal.ReflexVoidValue;

/**
 * Sleep for param milliseconds
 * 
 * @author amkimian
 * 
 */
public class SleepNode extends BaseNode {

    private ReflexNode expression;
    private static Logger logger = Logger.getLogger(SleepNode.class);

    public SleepNode(int lineNumber, IReflexHandler handler, Scope scope, ReflexNode e) {
        super(lineNumber, handler, scope);
        expression = e;
    }

    @Override
    public ReflexValue evaluate(IReflexDebugger debugger, Scope scope) {
        debugger.stepStart(this, scope);
        ReflexValue value = expression.evaluate(debugger, scope);
        debugger.recordMessage("Sleeping ");
        Long time = 0L;

        if (value.isString()) {
            time = value.asLong();
        } else if (value.isNumber()) {
            time = value.asLong();
        }
        if (time > 0) {
            try {
                Thread.sleep(value.asLong());
            } catch (InterruptedException e) {
                log.debug("+++ MATCH " + value + " ");
            }
        } else {
            logger.warn("Sleep called with no timeout");
        }
        debugger.stepEnd(this, new ReflexVoidValue(lineNumber), scope);
        return new ReflexVoidValue(lineNumber);
    }

    @Override
    public String toString() {
        return super.toString() + " - " + String.format("sleep(%s)", expression);
    }
}
