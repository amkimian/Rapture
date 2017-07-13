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
package rapture.dsl.iqry;

import java.util.ArrayList;
import java.util.List;

public class WhereClause {
	
    @Override
    public String toString() {
        StringBuilder ret = new StringBuilder();
        ret.append("where ").append(primaryStatement);
        if (extensions != null) for (WhereExtension we : extensions)
            ret.append(" ").append(we.toString());
        return ret.toString();
    }

    private WhereStatement primaryStatement;
	private List<WhereExtension> extensions = new ArrayList<>();
	
	public void addStatement(WhereStatement where) {
	    // What if we already have a a primary?
		this.primaryStatement = where;
	}
	
	// Joiner is ignored for first statement. This eliminates fencepost problems.
	
    public WhereClause appendStatement(WhereJoiner joiner, WhereStatement where) {
        if (primaryStatement == null)
            this.primaryStatement = where;
        else
            extensions.add(new WhereExtension(joiner, where));
        return this;
    }

    public WhereClause appendStatement(String joiner, WhereStatement where) {
        if (primaryStatement == null)
            this.primaryStatement = where;
        else
            extensions.add(new WhereExtension(joiner, where));
        return this;
    }

    public WhereStatement getPrimary() {
        return primaryStatement;
    }
    
    public List<WhereExtension> getExtensions() {
        return extensions;
    }
}
