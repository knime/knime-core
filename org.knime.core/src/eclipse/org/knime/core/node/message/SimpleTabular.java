/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by KNIME AG, Zurich, Switzerland
 *  Website: http://www.knime.com; Email: contact@knime.com
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License, Version 3, as
 *  published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, see <http://www.gnu.org/licenses>.
 *
 *  Additional permission under GNU GPL version 3 section 7:
 *
 *  KNIME interoperates with ECLIPSE solely via ECLIPSE's plug-in APIs.
 *  Hence, KNIME and ECLIPSE are both independent programs and are not
 *  derived from each other. Should, however, the interpretation of the
 *  GNU GPL Version 3 ("License") under any applicable laws result in
 *  KNIME and ECLIPSE being a combined program, KNIME AG herewith grants
 *  you the additional permission to use and propagate KNIME together with
 *  ECLIPSE with only the license terms in place for ECLIPSE applying to
 *  ECLIPSE and the GNU GPL Version 3 applying for KNIME, provided the
 *  license terms of ECLIPSE themselves allow for the respective use and
 *  propagation of ECLIPSE together with KNIME.
 *
 *  Additional permission relating to nodes for KNIME that extend the Node
 *  Extension (and in particular that are based on subclasses of NodeModel,
 *  NodeDialog, and NodeView) and that only interoperate with KNIME through
 *  standard APIs ("Nodes"):
 *  Nodes are deemed to be separate and independent programs and to not be
 *  covered works.  Notwithstanding anything to the contrary in the
 *  License, the License does not apply to Nodes, you are not required to
 *  license Nodes under the License, and you are granted a license to
 *  prepare and propagate Nodes, in each case even if such Nodes are
 *  propagated with or for interoperation with KNIME.  The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ---------------------------------------------------------------------
 *
 * History
 *   Dec 30, 2022 (wiswedel): created
 */
package org.knime.core.node.message;

import static java.lang.Math.max;
import static java.lang.Math.min;
import static org.apache.commons.lang3.StringUtils.abbreviate;
import static org.apache.commons.lang3.StringUtils.center;
import static org.apache.commons.lang3.StringUtils.repeat;
import static org.apache.commons.lang3.StringUtils.rightPad;

import java.util.List;
import java.util.stream.IntStream;

import org.apache.commons.lang3.StringUtils;

/**
 * Package scope utility to format a table into well formatted ascii (for now). For instance:
 *
 * <pre>
 * RowID | Column1 | Column H...
 * -----------------------------
 * Row0  |     3.2 | Long Str...
 * Row1  |  173.2a |       Hello
 *        ^^^^^^^^^
 * </pre>
 *
 * @author Bernd Wiswedel
 * @since 5.0
 */
final class SimpleTabular {

    private static final int MAX_COL_WIDTH = 14;
    private static final int MIN_COL_WIDTH = 6;
    private final String[] m_colHeaders;
    private final String[] m_rowHeaders;
    private final String[][] m_data;
    private final int m_underlineColumn;

    private SimpleTabular(final String[] colHeaders, final String[] rowHeaders, final String[][] data,
        final int underlineColumn) {
        m_colHeaders = colHeaders;
        m_rowHeaders = rowHeaders;
        m_data = data;
        m_underlineColumn = underlineColumn;
    }

    static SimpleTabular from(final List<String> colHeaders, final List<String> rowHeaders,
        final List<List<String>> data, final int underlineColumn) {
        var fixedWidthColHeaders = new String[colHeaders.size()];
        var fixedWidthRowHeaders = new String[rowHeaders.size()];
        var fixedWidthData = new String[rowHeaders.size()][colHeaders.size()];
        int rowHeaderWidth = rowHeaders.stream().mapToInt(String::length).max().orElse(0);
        rowHeaderWidth = max(min(MAX_COL_WIDTH, max(rowHeaderWidth, "RowID".length())), MIN_COL_WIDTH);
        for (var r = 0; r < rowHeaders.size(); r++) {
            fixedWidthRowHeaders[r] = rightPad(abbreviate(rowHeaders.get(r), rowHeaderWidth), rowHeaderWidth);
        }

        for (var i = 0; i < colHeaders.size(); i++) {
            var c = i; // quasi-final in lambda
            var colWidth = IntStream.range(0, rowHeaders.size()) //
                .mapToObj(r -> data.get(r).get(c)) //
                .mapToInt(String::length) //
                .max() //
                .orElse(0);
            colWidth = max(min(MAX_COL_WIDTH, max(colWidth, colHeaders.get(i).length())), MIN_COL_WIDTH);
            fixedWidthColHeaders[i] = center(abbreviate(colHeaders.get(i), colWidth), colWidth);
            for (var r = 0; r < rowHeaders.size(); r++) {
                fixedWidthData[r][i] = rightPad(abbreviate(data.get(r).get(i), colWidth), colWidth);
            }
        }
        return new SimpleTabular(fixedWidthColHeaders, fixedWidthRowHeaders, fixedWidthData, underlineColumn);
    }

    @Override
    public String toString() {
        return toAsciiString();
    }

    String toAsciiString() {
        var strB = new StringBuilder();
        strB.append(StringUtils.center("RowID", m_rowHeaders[0].length()));
        int underlineStartIndex = -1;
        int underlineEndIndex = -1;
        for (var c = 0; c < m_colHeaders.length; c++) {
            var colHeader = m_colHeaders[c];
            strB.append(" |");
            if (c == m_underlineColumn) {
                underlineStartIndex = strB.length();
            }
            strB.append(" ").append(colHeader);
            if (c == m_underlineColumn) {
                underlineEndIndex = strB.length();
            }
        }
        int hLineLength = strB.length();
        strB.append('\n').append(repeat('-', hLineLength)).append('\n');
        for (var r = 0; r < m_rowHeaders.length; r++) {
            strB.append(m_rowHeaders[r]);
            for (var c = 0; c < m_colHeaders.length; c++) {
                strB.append(" | ").append(m_data[r][c]);
            }
            strB.append('\n');
        }
        if (underlineStartIndex >= 0) {
            strB.append(repeat(' ', underlineStartIndex));
            strB.append(repeat('^', underlineEndIndex - underlineStartIndex));
            strB.append('\n');
        }
        return strB.toString();
    }
}
