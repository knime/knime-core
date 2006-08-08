/* 
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2006
 * University of Konstanz, Germany.
 * Chair for Bioinformatics and Information Mining
 * Prof. Dr. Michael R. Berthold
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
 * -------------------------------------------------------------------
 * 
 * History
 *   Jul 17, 2006 (wiswedel): created
 */
package org.knime.core.node;

import org.knime.core.data.DataTableSpec;
import org.knime.core.data.container.ContainerTable;
import org.knime.core.data.container.DataContainer;

/**
 * 
 * @author wiswedel, University of Konstanz
 */
public class BufferedDataContainer extends DataContainer {
    
    private final Node m_node;
    private BufferedDataTable m_resultTable; 

    /**
     * @see DataContainer#DataContainer(DataTableSpec, boolean)
     */
    BufferedDataContainer(final DataTableSpec spec, 
            final boolean initDomain, final Node node) {
        super(spec, initDomain);
        m_node = node;
    }

    /**
     * Returns the content of this container in a BufferedDataTable. The result
     * can be returned, e.g. in a NodeModel's execute method.
     * @see org.knime.core.data.container.DataContainer#getTable()
     */
    @Override
    public BufferedDataTable getTable() {
        if (m_resultTable == null) {
            ContainerTable buffer = getBufferedTable();
            m_resultTable = new BufferedDataTable(buffer);
            m_resultTable.setOwnerRecursively(m_node);
        }
        return m_resultTable;
    }
}
