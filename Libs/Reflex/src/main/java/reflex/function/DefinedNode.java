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

import reflex.IReflexHandler;
import reflex.ReflexException;
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
public class DefinedNode extends BaseNode {

    private String varName = null;
    private String namespacePrefix;
    private ReflexNode node = null;

    public DefinedNode(int lineNumber, IReflexHandler handler, Scope scope, String varName, String namespacePrefix) {
        super(lineNumber, handler, scope);
        this.varName = varName;
        this.namespacePrefix = namespacePrefix;
    }

    public DefinedNode(int lineNumber, IReflexHandler handler, Scope scope, ReflexNode foo, String namespacePrefix) {
        super(lineNumber, handler, scope);
        this.node = foo;
        this.namespacePrefix = namespacePrefix;
    }

    @Override
    public ReflexValue evaluate(IReflexDebugger debugger, Scope scope) {
        debugger.stepStart(this, scope);
        boolean ret = true;

        if (varName != null) {
            if (scope.resolve(varName, namespacePrefix) == null) {
                ret = false;
            }
        } else {
            if (node != null) {
                try {
                    ReflexValue val = node.evaluate(debugger, scope);
                    if (val instanceof ReflexVoidValue) ret = false;
                } catch (ReflexException e) {
                    ret = false;
                }
            }
        }
        ReflexValue retVal = new ReflexValue(ret);
        debugger.stepEnd(this, retVal, scope);
        return retVal;
    }

    @Override
    public String toString() {
        return super.toString() + " - " + String.format("defined(%s)", varName);
    }
}
