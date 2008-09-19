/* ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2008
 * University of Konstanz, Germany
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
 *   18.09.2008 (ohl): created
 */
package org.knime.base.node.io.filereader;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.workflow.ScopeVariable;

/**
 *
 * @author ohl, University of Konstanz
 */
class VariableFileReaderNodeSettings extends FileReaderNodeSettings {

    private static final String VAR_NAME = "variable_name";

    private String m_variableName;

    /**
     * Creates an empty settings object.
     */
    public VariableFileReaderNodeSettings() {
        m_variableName = "";
    }

    /**
     * @param variableName the name of the scope variable to read the file
     *            location from
     *
     */
    VariableFileReaderNodeSettings(final String variableName) {
        assert variableName != null;
        m_variableName = variableName;
    }

    /**
     * Creates a clone.
     *
     * @param clonee the thing to clone.
     */
    public VariableFileReaderNodeSettings(
            final VariableFileReaderNodeSettings clonee) {
        super(clonee);
        m_variableName = clonee.m_variableName;
    }

    public VariableFileReaderNodeSettings(final FileReaderNodeSettings clonee) {
        super(clonee);
        m_variableName = "";
    }

    /**
     * Creates a new settings object initialized with the values from the passed
     * object.
     *
     * @param cfg the config to read the values from
     * @throws InvalidSettingsException if cfg object is invalid
     */
    public VariableFileReaderNodeSettings(final NodeSettingsRO cfg)
            throws InvalidSettingsException {
        super(cfg);
        m_variableName = cfg.getString(VAR_NAME);
    }

    /**
     * @param variableName the variableName to set
     */
    public void setVariableName(final String variableName) {
        assert variableName != null;
        m_variableName = variableName;
    }

    /**
     * @return the variableName
     */
    public String getVariableName() {
        return m_variableName;
    }

    /**
     * Creates a clone from this object replacing the dataURL location by the
     * value from stack.
     *
     * @param stack the map containing all currently available variables and
     *            their values
     * @return a copy of this settings object with the file location replaced by
     *         the value of the variable
     * @throws IllegalArgumentException if the variable is not on the stack
     * @throws MalformedURLException if the value of the variable is not a valid
     *             URL
     */
    VariableFileReaderNodeSettings createSettingsFrom(
            final Map<String, ScopeVariable> stack)
            throws MalformedURLException {
        VariableFileReaderNodeSettings result =
                new VariableFileReaderNodeSettings(this);
        ScopeVariable var = stack.get(m_variableName);
        if (var == null) {
            throw new IllegalArgumentException("File location variable ("
                    + m_variableName + ") is not on the stack.");
        }
        if (!var.getType().equals(ScopeVariable.Type.STRING)) {
            throw new IllegalArgumentException(
                    "Selected file location variable (" + m_variableName
                            + ") is not of type string.");
        }
        if (var.getStringValue() == null || var.getStringValue().isEmpty()) {
            throw new IllegalArgumentException("File location variable ("
                    + m_variableName + ") is not set. Execute predecessor.");
        }
        URL loc = FileReaderNodeDialog.textToURL(var.getStringValue());

        result.setDataFileLocationAndUpdateTableName(loc);
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void saveToConfiguration(final NodeSettingsWO cfg) {
        super.saveToConfiguration(cfg);
        cfg.addString(VAR_NAME, m_variableName);
    }
}
