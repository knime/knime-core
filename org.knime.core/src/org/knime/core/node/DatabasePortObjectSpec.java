/* 
 * ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2007
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
 *   13.02.2008 (gabriel): created
 */
package org.knime.core.node;

import org.knime.core.data.DataTableSpec;

/**
 * Class used as database port object holding a {@link DataTableSpec}
 * and a <code>ModelContentRO</code> to create a database connection.
 * 
 * @author Thomas Gabriel, University of Konstanz
 */
public class DatabasePortObjectSpec implements PortObjectSpec {

    private final DataTableSpec m_spec;
    
    private final ModelContentRO m_conn;
    
    /**
     * Creates a new database port object spec.
     * @param spec underlying table spec
     * @param conn connection model
     */
    public DatabasePortObjectSpec(final DataTableSpec spec, 
            final ModelContentRO conn) {
        m_spec = spec;
        m_conn = conn;
    }
    
    /**
     * @return underlying table spec
     */
    public DataTableSpec getDataTableSpec() {
        return m_spec;
    }
    
    /**
     * @return connection model
     */
    public ModelContentRO getConnectionModel() {
        return m_conn;
    }
    
}
