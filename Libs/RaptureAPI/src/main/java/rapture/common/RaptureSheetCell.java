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
package rapture.common;

/**
 * Note that this class is referenced in types.api - any changes to this file
 * should be reflected there.
 **/

@Deprecated
public class RaptureSheetCell {

    @Deprecated
    public RaptureSheetCell() {
        super();
    }

    @Deprecated
    public RaptureSheetCell(int row, int column, String data, Long epoch) {
        this();
        this.row = row;
        this.column = column;
        this.data = data;
        this.epoch = epoch;
    }

    @Deprecated
    public int getRow() {
        return row;
    }

    @Deprecated
    public void setRow(int row) {
        this.row = row;
    }

    @Deprecated
    public int getColumn() {
        return column;
    }

    @Deprecated
    public void setColumn(int column) {
        this.column = column;
    }

    @Deprecated
    public String getData() {
        return data;
    }

    @Deprecated
    public void setData(String data) {
        this.data = data;
    }

    @Deprecated
    public Long getEpoch() {
        return epoch;
    }

    @Deprecated
    public void setEpoch(Long epoch) {
        this.epoch = epoch;
    }

    private int row;
    private int column;
    private String data;
    private Long epoch;

    @Override
    public String toString() {
        return String.format("%s:%s=%s (epoch=%s)", getRow(), getColumn(), getData(), getEpoch());

    }
}
