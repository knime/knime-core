/*
 * ------------------------------------------------------------------------
 *  Copyright by KNIME GmbH, Konstanz, Germany
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
 *   10.11.2013 (thor): created
 */
package org.knime.testing.core;

import java.util.Arrays;
import java.util.List;

import org.knime.core.data.DataValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.DialogComponent;
import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;

/**
 * Abstract base class for difference checkers that already implements some common functionality. Subclasses
 * should take care of calling the superclass implementations when overwriting methods.
 *
 * @param <T> value type that this checker handles
 * @author Thorsten Meinl, KNIME.com, Zurich, Switzerland
 * @since 2.9
 */
public abstract class AbstractDifferenceChecker<T extends DataValue> implements DifferenceChecker<T> {
    private final SettingsModelBoolean m_ignoreColumnProperties = new SettingsModelBoolean("ignoreColumnProperties", false);

    private final DialogComponentBoolean m_component = new DialogComponentBoolean(m_ignoreColumnProperties,
            "Ignore column properties");

    /**
     * Creates a new abstract difference checker.
     */
    protected AbstractDifferenceChecker() {
        m_component.setToolTipText("Ignores all properties in the data column spec");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void loadSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_ignoreColumnProperties.loadSettingsFrom(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void loadSettingsForDialog(final NodeSettingsRO settings) {
        try {
            m_ignoreColumnProperties.loadSettingsFrom(settings);
        } catch (InvalidSettingsException ex) {
            // ignore and use default
            m_ignoreColumnProperties.setBooleanValue(false);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void saveSettings(final NodeSettingsWO settings) {
        m_ignoreColumnProperties.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_ignoreColumnProperties.validateSettings(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<? extends DialogComponent> getDialogComponents() {
        return Arrays.asList(m_component);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean ignoreColumnProperties() {
        return m_ignoreColumnProperties.getBooleanValue();
    }
}
