/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2013
 *  University of Konstanz, Germany and
 *  KNIME GmbH, Konstanz, Germany
 *  Website: http://www.knime.org; Email: contact@knime.org
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
 *  KNIME and ECLIPSE being a combined program, KNIME GMBH herewith grants
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
 *  propagated with or for interoperation with KNIME. The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ------------------------------------------------------------------------
 *
 * History
 *   04.07.2012 (hofer): created
 */
package org.knime.base.node.stats.testing.ttest;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.knime.base.node.util.DoubleFormat;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataRow;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.IntValue;
import org.knime.core.data.StringValue;
import org.knime.core.node.BufferedDataTable;

/**
 * Utility class to display data tables in node views.
 *
 * @author Heiko Hofer
 */
public class NodeViewUtil {
    /**
     * Convenient method to create HTML Header.
     * @return HTML header
     */
    public static  StringBuilder createHtmlHeader() {
        StringBuilder buffer = new StringBuilder();
        buffer.append("<html>\n");
        buffer.append("<head>\n");
        buffer.append("<style type=\"text/css\">\n");
        buffer.append("body {color:#333333;}");
        buffer.append("table {width: 100%;margin: 7px 0 7px 0;}");
        buffer.append("th {font-weight: bold;background-color: #aaccff;"
                + "vertical-align: bottom;}");
        buffer.append("td {padding: 4px 10px 4px 10px;}");
        buffer.append("th {padding: 4px 10px 4px 10px;}");
        buffer.append(".left {text-align: left}");
        buffer.append(".right {text-align: right}");
        buffer.append(".numeric {text-align: right}");
        buffer.append(".odd {background-color:#ddeeff;}");
        buffer.append(".even {background-color:#ffffff;}");
        buffer.append("</style>\n");
        buffer.append("</head>\n");
        return buffer;
    }


    /** Escape special html characters.
     * @param str escape characters of this string
     * @return the string with escaped characters
     */
    public static  String escapeHtml(final String str) {
        // escape the quote character
        String s = str.replace("&", "&amp;");
        // escape lower than
        s = s.replace("<", "&lt;");
        // escape greater than
        s = s.replace(">", "&gt;");
        // escape quote character
        s = s.replace("\"", "&quot;");
        return s;
    }

    /**
     * Create HTML from the given table using column rowHeader as row headers.
     * @param table the table
     * @param rowHeader the column with row headers
     * @param exclude columns to exclude
     * @param colHeaders override column headers
     * @param buffer append to this buffer
     */
    public static  void renderDataTable(final BufferedDataTable table,
            final String rowHeader, final Collection<String> exclude,
            final Map<String, String> colHeaders,
            final StringBuilder buffer) {
        int rowHeaderI = table.getDataTableSpec().findColumnIndex(rowHeader);
        Set<Integer> excludeI = new HashSet<Integer>();
        for (String toExclude : exclude) {
            excludeI.add(table.getDataTableSpec().findColumnIndex(toExclude));
        }
        buffer.append("<table>\n");
        buffer.append("<tr>");
        buffer.append("<th class=\"left\"></th>");
        for (DataColumnSpec colSpec : table.getDataTableSpec()) {
            String colName = colSpec.getName();
            if (!exclude.contains(colName)) {
                String value = colHeaders.containsKey(colName) ?
                        colHeaders.get(colName) : colName;
                buffer.append("<th>");
                buffer.append(escapeHtml(value));
                buffer.append("</th>");
            }
        }
        buffer.append("</tr>");

        int r = 0;
        for (DataRow row : table) {
            buffer.append("<tr class=\"");
            buffer.append(r % 2 == 0 ? "odd" : "even");
            buffer.append("\">");
            renderDataCell(row.getCell(rowHeaderI), buffer);
            for (int i = 0; i < row.getNumCells(); i++) {
                if (excludeI.contains(i)) {
                    continue;
                }
                DataCell cell = row.getCell(i);
                renderDataCell(cell, buffer);
            }
            buffer.append("</tr>");
            r++;
        }
        buffer.append("</table>\n");
    }

    /**
     * @param cell the data cell to render
     * @param buffer write to this buffer
     */
    public static void renderDataCell(final DataCell cell,
            final StringBuilder buffer) {
        if (cell.isMissing()) {
            buffer.append("<td></td>");
            return;
        }
        if (cell.getType().isCompatible(IntValue.class)) {
            IntValue value = (IntValue)cell;
            buffer.append("<td class=\"numeric\">");
            buffer.append(value.getIntValue());
            buffer.append("</td>");
        } else if (cell.getType().isCompatible(DoubleValue.class)) {
            DoubleValue value = (DoubleValue)cell;
            buffer.append("<td class=\"numeric\">");
            buffer.append(DoubleFormat.formatDouble(value.getDoubleValue()));
            buffer.append("</td>");
        } else if (cell.getType().isCompatible(StringValue.class)) {
            StringValue value = (StringValue)cell;
            buffer.append("<td class=\"left\">");
            buffer.append(value.getStringValue());
            buffer.append("</td>");
        } else {
            buffer.append("<td class=\"left\">");
            buffer.append(cell.toString());
            buffer.append("</td>");
        }
    }

}
