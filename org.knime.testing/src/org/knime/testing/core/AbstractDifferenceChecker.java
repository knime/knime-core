/*
 * ------------------------------------------------------------------------
 *  Copyright by KNIME AG, Zurich, Switzerland
 *  Website: http://www.knime.com; Email: contact@knime.com
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
 *  KNIME and ECLIPSE being a combined program, KNIME AG herewith grants
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
 * @author Thorsten Meinl, KNIME AG, Zurich, Switzerland
 * @since 2.9
 */
public abstract class AbstractDifferenceChecker<T extends DataValue> implements DifferenceChecker<T> {
    private final SettingsModelBoolean m_ignoreColumnProperties = new SettingsModelBoolean("ignoreColumnProperties",
        false);
    private final SettingsModelBoolean m_ignoreColumnElementNames = new SettingsModelBoolean("ignoreColumnElementNames",
        false);

    private final SettingsModelBoolean m_ignoreDomain = new SettingsModelBoolean("ignoreDomain", false);

    // introduced as part of AP-12523
    private final SettingsModelBoolean m_ignoreDomainPossibleValuesOrdering =
        createIgnoreDomainPossibleValuesOrderingModel(m_ignoreDomain);

    private static final SettingsModelBoolean createIgnoreDomainPossibleValuesOrderingModel(
        final SettingsModelBoolean ignoreDomainsModel) {
        SettingsModelBoolean result = new SettingsModelBoolean("ignoreDomainPossibleValuesOrdering", false);
        ignoreDomainsModel.addChangeListener(e -> result.setEnabled(!ignoreDomainsModel.getBooleanValue()));
        result.setEnabled(!ignoreDomainsModel.getBooleanValue());
        return result;
    }

    private DialogComponentBoolean m_ignorePropertiesComponent;
    private DialogComponentBoolean m_ignoreDomainComponent;
    private DialogComponentBoolean m_ignoreElementNamesComponent;
    private DialogComponentBoolean m_ignoreDomainPossibleValuesOrderingComponent;

    /**
     * {@inheritDoc}
     */
    @Override
    public void loadSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_ignoreColumnProperties.loadSettingsFrom(settings);

        // added in 2.11
        try {
            m_ignoreDomain.loadSettingsFrom(settings);
        } catch (InvalidSettingsException ex) {
            // ignore and use default
            m_ignoreDomain.setBooleanValue(false);
        }

        // added in 3.3
        try {
            m_ignoreColumnElementNames.loadSettingsFrom(settings);
        } catch (InvalidSettingsException ex) {
            // ignore and use default
            m_ignoreColumnElementNames.setBooleanValue(false);
        }

        try {
            // added in 4.1
            m_ignoreDomainPossibleValuesOrdering.loadSettingsFrom(settings);
        } catch (InvalidSettingsException ex) {
            // true here because old instances of the node ignore it (backward compatibility)
            m_ignoreDomainPossibleValuesOrdering.setBooleanValue(true);
        }

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
        try {
            m_ignoreColumnElementNames.loadSettingsFrom(settings);
        } catch (InvalidSettingsException ex) {
            // ignore and use default
            m_ignoreColumnElementNames.setBooleanValue(false);
        }

        try {
            m_ignoreDomain.loadSettingsFrom(settings);
        } catch (InvalidSettingsException ex) {
            // ignore and use default
            m_ignoreDomain.setBooleanValue(false);
        }

        try {
            // added in 4.1
            m_ignoreDomainPossibleValuesOrdering.loadSettingsFrom(settings);
        } catch (InvalidSettingsException ex) {
            // false here because new instance of the node check ordering
            m_ignoreDomainPossibleValuesOrdering.setBooleanValue(false);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void saveSettings(final NodeSettingsWO settings) {
        m_ignoreColumnProperties.saveSettingsTo(settings);
        m_ignoreColumnElementNames.saveSettingsTo(settings);
        m_ignoreDomain.saveSettingsTo(settings);
        m_ignoreDomainPossibleValuesOrdering.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_ignoreColumnProperties.validateSettings(settings);
        // don't check m_ignoreDomain, it was added in 2.11
        // don't check m_ignoreColumnElementNames, it was added in 3.3
        // don't check m_ignoreDomainPossibleValuesOrdering, it was added in 4.1
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<? extends DialogComponent> getDialogComponents() {
        if (m_ignorePropertiesComponent == null) {
            m_ignorePropertiesComponent = new DialogComponentBoolean(m_ignoreColumnProperties, "Ignore column properties");
            m_ignorePropertiesComponent.setToolTipText("Ignores all properties in the data column spec");
        }
        if (m_ignoreElementNamesComponent == null) {
            m_ignoreElementNamesComponent = new DialogComponentBoolean(m_ignoreColumnElementNames,
                "Ignore column element names (for collection columns)");
            m_ignoreElementNamesComponent.setToolTipText("Ignores element names for collection columns");
        }
        if (m_ignoreDomainComponent == null) {
            m_ignoreDomainComponent = new DialogComponentBoolean(m_ignoreDomain, "Ignore column domain");
            m_ignoreDomainComponent.setToolTipText("Ignores the domain (e.g. possible values, upper and lower bounds) "
                + "in the data column spec");
        }
        if (m_ignoreDomainPossibleValuesOrderingComponent == null) {
            m_ignoreDomainPossibleValuesOrderingComponent = new DialogComponentBoolean(
                m_ignoreDomainPossibleValuesOrdering, "Ignore ordering of possible values in domain");
            m_ignoreDomainPossibleValuesOrderingComponent.setToolTipText(
                "Ignores the ordering of values in the domain -- the set must be the same but ordering is ignored");
        }
        return Arrays.asList(m_ignorePropertiesComponent, m_ignoreElementNamesComponent, m_ignoreDomainComponent,
            m_ignoreDomainPossibleValuesOrderingComponent);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean ignoreColumnProperties() {
        return m_ignoreColumnProperties.getBooleanValue();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean ignoreColumnElementNames() {
        return m_ignoreColumnElementNames.getBooleanValue();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean ignoreDomain() {
        return m_ignoreDomain.getBooleanValue();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean ignoreDomainPossibleValuesOrdering() {
        return m_ignoreDomainPossibleValuesOrdering.getBooleanValue();
    }
}
