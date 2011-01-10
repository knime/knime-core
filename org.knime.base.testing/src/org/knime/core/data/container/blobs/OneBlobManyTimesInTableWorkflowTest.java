/* ------------------------------------------------------------------
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
 * ---------------------------------------------------------------------
 * 
 * History
 *   Nov 27, 2008 (wiswedel): created
 */
package org.knime.core.data.container.blobs;

import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.ExecutionContext;
import org.knime.testing.data.blob.LargeBlobCell;

/**
 * Creates a table with 200 blobs (about 1MB each), where the blob cell is
 * always the same. The resulting flow is expected to be about 1MB in size. 
 * @author Bernd Wiswedel, University of Konstanz
 */
public class OneBlobManyTimesInTableWorkflowTest extends AbstractBlobsInWorkflowTest {

    private static final int ROW_COUNT = 200;

    /** {@inheritDoc} */
    @Override
    public BufferedDataTable createBDT(final ExecutionContext exec) {
        BufferedDataContainer c = exec.createDataContainer(
                new DataTableSpec(new DataColumnSpecCreator(
                        "Blobs", LargeBlobCell.TYPE).createSpec()));
        LargeBlobCell cell = new LargeBlobCell("This is a big cell");
        for (int i = 0; i < ROW_COUNT; i++) {
            String s = "someName_" + i;
            c.addRowToTable(new DefaultRow(s, cell));
        }
        c.close();
        return c.getTable();
    }
    
    /** {@inheritDoc} */
    @Override
    protected long getApproximateSize() {
        return LargeBlobCell.SIZE_OF_CELL;
    }
    
}
