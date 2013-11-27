/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2013
 *  University of Konstanz, Germany and
 *  KNIME GmbH, Konstanz, Germany
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
 *   18.09.2008 (ohl): created
 */
package org.knime.base.node.io.filereader;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.workflow.FlowVariable;

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

    /**
     * Create new settings object for the variable file reader based on the
     * given settings object.
     * @param clonee clone this settings object
     */
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
     * @throws InvalidSettingsException if the variable is not on the stack
     * @throws MalformedURLException if the value of the variable is not a valid
     *             URL
     */
    VariableFileReaderNodeSettings createSettingsFrom(
            final Map<String, FlowVariable> stack)
            throws MalformedURLException, InvalidSettingsException {
        VariableFileReaderNodeSettings result =
                new VariableFileReaderNodeSettings(this);
        FlowVariable var = stack.get(m_variableName);
        if (var == null) {
            throw new InvalidSettingsException("File location variable ("
                    + m_variableName + ") is not on the stack.");
        }
        if (!var.getType().equals(FlowVariable.Type.STRING)) {
            throw new InvalidSettingsException(
                    "Selected file location variable (" + m_variableName
                            + ") is not of type string.");
        }
        if (var.getStringValue() == null || var.getStringValue().isEmpty()) {
            throw new InvalidSettingsException("File location variable ("
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
