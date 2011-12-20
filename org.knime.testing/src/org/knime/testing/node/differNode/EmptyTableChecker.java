/*
 * ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2011
 * University of Konstanz, Germany
 * Chair for Bioinformatics and Information Mining (Prof. M. Berthold)
 * and KNIME GmbH, Konstanz, Germany
 *
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner or
 * as specified in the license file distributed with this product.
 *
 * If you have any questions please contact the copyright holder:
 * website: www.knime.org
 * email: contact@knime.org
 * --------------------------------------------------------------------- *
 *
 * History
 *   May 18, 2006 (ritmeier): created
 */
package org.knime.testing.node.differNode;

import org.knime.core.data.DataTable;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.RowIterator;

/**
 * Fails if one of the input tables is not empty or the {@link DataTableSpec}s
 * are different.
 *
 * @author Tobias Koetter, University of Konstanz
 */
public class EmptyTableChecker implements TestEvaluator {

    /**
     * default Constructor.
     */
    public EmptyTableChecker() {
        super();
    }

    /**
     *
     * @throws TestEvaluationException
     * @see org.knime.testing.node.differNode.TestEvaluator#compare(
     *      org.knime.core.data.DataTable, org.knime.core.data.DataTable)
     */
    @Override
    public void compare(final DataTable table1, final DataTable table2)
            throws TestEvaluationException {
        int table1RowCount = 0;
        final RowIterator iter1 = table1.iterator();
        while(iter1.hasNext()) {
            table1RowCount++;
            iter1.next();
        }
        int table2RowCount = 0;
        final RowIterator iter2 = table2.iterator();
        while(iter2.hasNext()) {
            table2RowCount++;
            iter2.next();
        }
        String errorMsg = "";
        if (table1RowCount > 0) {
            errorMsg += "Table1 contains " + table1RowCount + " rows. ";
        }
        if (table2RowCount > 0) {
            errorMsg += "Table2 contains " + table2RowCount + " rows.";
        }
        if (!errorMsg.isEmpty()) {
            throw new TestEvaluationException(errorMsg);
        }
    }
}
