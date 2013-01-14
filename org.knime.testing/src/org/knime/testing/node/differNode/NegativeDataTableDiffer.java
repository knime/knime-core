/* 
 * ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2013
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

import java.util.Iterator;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTable;


/**
 * 
 * @author ritmeier, University of Konstanz
 */
public class NegativeDataTableDiffer implements TestEvaluator {

    /**
     * default Constructor.
     */
    public NegativeDataTableDiffer() {
        super();
    }

    /**
     * 
     * @throws TestEvaluationException
     * @see org.knime.testing.node.differNode.TestEvaluator#compare(
     *      org.knime.core.data.DataTable,
     *      org.knime.core.data.DataTable)
     */
    public void compare(final DataTable table1, final DataTable table2)
            throws TestEvaluationException {
        boolean testResult = true;
        if (!table1.getDataTableSpec().equalStructure(
                table2.getDataTableSpec())) {
            testResult = false;
        }
        Iterator<DataRow> rowIt1 = table1.iterator();
        Iterator<DataRow> rowIt2 = table2.iterator();
        while (rowIt1.hasNext() && rowIt2.hasNext()) {
            Iterator<DataCell> cellIt1 = rowIt1.next().iterator();
            Iterator<DataCell> cellIt2 = rowIt2.next().iterator();
            while (cellIt1.hasNext() && cellIt2.hasNext()) {
                if (!cellIt1.next().equals(cellIt2.next())) {
                    testResult = false;
                }
            }
        }
        if (testResult) {
            throw new TestEvaluationException(
                    "DataTables are the same. They should be differend in negitive testing");
        }
    }

}
