/* 
 * ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2008
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
package org.knime.base.node.io.database;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.ModelContent;
import org.knime.core.node.ModelContentRO;
import org.knime.core.node.config.ConfigRO;
import org.knime.core.node.config.ConfigWO;

/**
 * 
 * @author Thomas Gabriel, University of Konstanz
 */
public class DBQueryConnection extends DBConnection {
    
    /** Place holder <code>&lttable&gt</code>. */
    public static final String TABLE_PLACEHOLDER = "<table>"; 
    
    private String m_query = null;
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void validateConnection(final ConfigRO settings)
            throws InvalidSettingsException {
        String query = settings.getString(CFG_STATEMENT);
        if (query != null && query.contains(TABLE_PLACEHOLDER)) {
            throw new InvalidSettingsException(
            "Database table place holder not replaced.");
        }
        super.validateConnection(settings);
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public boolean loadValidatedConnection(final ConfigRO settings)
            throws InvalidSettingsException {
        m_query = settings.getString(DBConnection.CFG_STATEMENT);
        return super.loadValidatedConnection(settings);
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void saveConnection(final ConfigWO settings) {
        settings.addString(CFG_STATEMENT, m_query);
        super.saveConnection(settings);
    }
    
    /**
     * Set new SQL statement. 
     * @param query new SQL statement
     */
    public void setQuery(final String query) {
        m_query = query;
    }
    
    /**
     * @return SQL statement 
     */
    public String getQuery() {
        return m_query;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public ModelContentRO createConnectionModel() {
        ModelContent cont = new ModelContent("database_query_connection_model");
        saveConnection(cont);
        return cont;
    }        

}
