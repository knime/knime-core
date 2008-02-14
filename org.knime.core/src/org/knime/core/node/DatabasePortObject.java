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

/**
 * Class used as database port object holding a {@link BufferedDataTable}
 * and a <code>ModelContentRO</code> to create a database connection.
 * 
 * @author Thomas Gabriel, University of Konstanz
 */
public class DatabasePortObject implements PortObject {

    /**
     * Database port type formed <code>PortObjectSpec.class</code> and 
     * <code>PortObject.class</code> from this class.
     */
    public static final PortType TYPE = 
        new PortType(DatabasePortObjectSpec.class, DatabasePortObject.class);
    
    private final BufferedDataTable m_data;
    
    private final ModelContentRO m_conn;
    
    /**
     * Creates a new database port object.
     * @param data underlying data
     * @param conn connection model
     */
    public DatabasePortObject(final BufferedDataTable data, 
            final ModelContentRO conn) {
        m_data = data;
        m_conn = conn;
    }
    
    /**
     * @return underlying data
     */
    public BufferedDataTable getDataTable() {
        return m_data;
    }
    
    /**
     * @return connection model
     */
    public ModelContentRO getConnectionModel() {
        return m_conn;
    }
    
    
}
