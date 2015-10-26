/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by KNIME GmbH, Konstanz, Germany
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
 *  propagated with or for interoperation with KNIME.  The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ---------------------------------------------------------------------
 *
 * History
 *   Sep 30, 2015 (Lara): created
 */
package org.knime.core.node.port.database.binning;

import java.util.Map;
import java.util.Map.Entry;

import org.knime.core.node.port.database.StatementManipulator;

/**
 *
 * @author Lara
 */
public class CaseBinningStatementGenerator extends DefaultBinningStatementGenerator {

    private static final CaseBinningStatementGenerator INSTANCE = new CaseBinningStatementGenerator();

    /**
     * @return the iNSTANCE
     */
    public static CaseBinningStatementGenerator getINSTANCE() {
        return INSTANCE;
    }

    @Override
    public String getBinnerStatement(final StatementManipulator sm, final String query, final String[] includeCols,
        final String[] excludeCols, final Map<String, Double[][]> limitsMap, final Map<String, Boolean[][]> includeMap,
        final Map<String, String[]> namingMap, final Map<String, String> appendMap) {

        StringBuilder selectQuery = new StringBuilder();
        selectQuery.append("SELECT ");
        for (int i = 0; i < excludeCols.length; i++) {
            selectQuery.append(sm.quoteIdentifier(excludeCols[i]) + ", ");
        }

        StringBuilder caseQuery = new StringBuilder();
        int counter = 0;

        for (Entry<String, Double[][]> entry : limitsMap.entrySet()) {

            String selColumn = entry.getKey();
            Double[][] limits = entry.getValue();
            Boolean[][] include = includeMap.get(selColumn);
            String[] naming = namingMap.get(selColumn);
            String appendColumn = appendMap.get(selColumn);

            boolean append = appendColumn != null;

            if (append) {
                selectQuery.append(sm.quoteIdentifier(selColumn) + ", ");
            }

            caseQuery.append("(CASE");

            String leftBorder;
            String rightBorder;
            for (int i = 0; i < limits.length; i++) {
                if (include[i][0]) {
                    leftBorder = ">=";
                } else {
                    leftBorder = ">";
                }
                if (include[i][1]) {
                    rightBorder = "<=";
                } else {
                    rightBorder = "<";
                }

                if (limits[0][0] == Double.NEGATIVE_INFINITY && limits[0][1] == Double.POSITIVE_INFINITY) { //(inf, inf)
                    caseQuery.append(" WHEN " + sm.quoteIdentifier(selColumn) + " = " + sm.quoteIdentifier(selColumn)
                        + " THEN " + naming[0]);
                } else if (limits[i][0] == Double.NEGATIVE_INFINITY) {//(inf, x] or (inf, x)
                    caseQuery.append(
                        " WHEN " + sm.quoteIdentifier(selColumn) + rightBorder + limits[i][1] + " THEN " + naming[i]);
                } else if (limits[i][1] == Double.POSITIVE_INFINITY) { //[x, inf) or (x, inf)
                    caseQuery.append(
                        " WHEN " + sm.quoteIdentifier(selColumn) + leftBorder + limits[i][0] + " THEN " + naming[i]);
                } else {
                    caseQuery.append(" WHEN " + sm.quoteIdentifier(selColumn) + leftBorder + limits[i][0] + " AND "
                        + sm.quoteIdentifier(selColumn) + rightBorder + limits[i][1] + " THEN " + naming[i]);
                }
            }

            caseQuery.append(" END) ");
            if (append) {
                caseQuery.append(sm.quoteIdentifier(appendColumn));
            } else {
                caseQuery.append(sm.quoteIdentifier(selColumn));
            }

            counter++;
            if (counter < limitsMap.size()) {
                caseQuery.append(", ");
            }

        }

        selectQuery.append(caseQuery);
        selectQuery.append(" FROM (" + query + ") T");
        return selectQuery.toString();
    }

}
