/* ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright by KNIME AG, Zurich, Switzerland
 *
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner or
 * as specified in the license file distributed with this product.
 *
 * If you have any questions please contact the copyright holder:
 * website: www.knime.com
 * email: contact@knime.com
 * ---------------------------------------------------------------------
 *
 * History
 *   Created on 20.06.2016 by koetter
 */
package org.knime.base.node.io.database.connection.util;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.config.Config;
import org.knime.core.node.config.ConfigRO;
import org.knime.core.node.config.ConfigWO;
import org.knime.core.node.workflow.CredentialsProvider;

/**
 * Class that extends the {@link DefaultDatabaseConnectionSettings} to also provide a SSL option.
 * @author Tobias Koetter, KNIME.com
 * @since 3.2
 */
public class ParameterizedDatabaseConnectionSettings extends DefaultDatabaseConnectionSettings {

    private static final String CFG_PARAMETER = "parameter";
    private static final String CFG_KEY = "parameter";
    private String m_parameter = "";

    /**
     * @return the additional parameter or an empty string
     */
    public String getParameter() {
        return m_parameter;
    }

    /**
     * @param parameter the additional parameter
     */
    public void setParameter(final String parameter) {
        m_parameter = parameter;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void saveConnection(final ConfigWO settings) {
        super.saveConnection(settings);
        final Config config = settings.addConfig(CFG_KEY);
        config.addString(CFG_PARAMETER, m_parameter);
    }

    //introduced with KNIME 3.2
//    /**
//     * {@inheritDoc}
//     */
//    @Override
//    public void validateConnection(final ConfigRO settings, final CredentialsProvider cp)
//            throws InvalidSettingsException {
//        super.validateConnection(settings, cp);
//        final Config config = settings.getConfig(CFG_KEY);
//        config.getBoolean(CFG_USE_SSL);
//    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean loadValidatedConnection(final ConfigRO settings, final CredentialsProvider cp)
            throws InvalidSettingsException {
        boolean b = super.loadValidatedConnection(settings, cp);
        if (settings.containsKey(CFG_KEY)) {
            final Config config = settings.getConfig(CFG_KEY);
            m_parameter = config.getString(CFG_PARAMETER);
        } else {
            //parameter support added with KNIME 3.2
            m_parameter = "";
        }
        return b;
    }

}