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
 * ------------------------------------------------------------------------
 */
package org.knime.base.node.io.database.sampling;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
import org.knime.core.node.defaultnodesettings.DialogComponentButtonGroup;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnNameSelection;
import org.knime.core.node.defaultnodesettings.DialogComponentNumber;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelDoubleBounded;
import org.knime.core.node.defaultnodesettings.SettingsModelIntegerBounded;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.database.DatabasePortObjectSpec;

/**
 * The node dialog of the database sampling node.
 *
 * @author Lara Gorini
 */
final class DBSamplingNodeDialog extends NodeDialogPane {

    private DialogComponentButtonGroup m_countComp;

    private DialogComponentNumber m_absoluteComp;

    private DialogComponentNumber m_relativeComp;

    private DialogComponentButtonGroup m_samplingComp;

    private DialogComponentBoolean m_stratifiedComp;

    private DialogComponentColumnNameSelection m_columnComp;

    private SettingsModelString m_samplingMethod = DBSamplingNodeModel.createSamplingModel();

    private SettingsModelBoolean m_stratified = DBSamplingNodeModel.createStratifiedModel();

    private SettingsModelString m_classColumnName = DBSamplingNodeModel.createColumnModel();

    private SettingsModelString m_countMethod = DBSamplingNodeModel.createCountModel();

    private SettingsModelIntegerBounded m_absoluteCount = DBSamplingNodeModel.createAbsoluteModel();

    private SettingsModelDoubleBounded m_relativeFraction = DBSamplingNodeModel.createRelativeModel();

    /**
     * Create query dialog.
     */
    @SuppressWarnings("unchecked")
    DBSamplingNodeDialog() {
        JPanel samplingPanel = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();

        m_countComp =
            new DialogComponentButtonGroup(m_countMethod, null, true, DBSamplingNodeModel.CountMethod.values());
        m_absoluteComp = new DialogComponentNumber(m_absoluteCount, "Absolute:     ", 50, 10);
        m_relativeComp = new DialogComponentNumber(m_relativeFraction, "Relative[%]:", 10, 10);

        m_samplingComp =
            new DialogComponentButtonGroup(m_samplingMethod, null, true, DBSamplingNodeModel.SamplingMethod.values());
        m_stratifiedComp = new DialogComponentBoolean(m_stratified, "Stratified sampling");
        m_columnComp = new DialogComponentColumnNameSelection(m_classColumnName, null, 0, DataValue.class);

        m_countMethod.addChangeListener(new ChangeListener() {

            @Override
            public void stateChanged(final ChangeEvent e) {
                boolean absolute =
                    m_countMethod.getStringValue().equals(DBSamplingNodeModel.CountMethod.ABSOLUTE.name());
                m_absoluteCount.setEnabled(absolute);
                m_relativeFraction.setEnabled(!absolute);

            }

        });
        m_stratified.addChangeListener(new ChangeListener() {

            @Override
            public void stateChanged(final ChangeEvent e) {
                m_classColumnName.setEnabled(m_stratifiedComp.isSelected());

            }
        });

        c.gridx = 0;
        c.gridy = 0;
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(2, 1, 2, 1);

        samplingPanel.add(m_countComp.getComponentPanel(), c);

        c.gridy++;
        samplingPanel.add(m_absoluteComp.getComponentPanel(), c);
        c.gridy++;
        samplingPanel.add(m_relativeComp.getComponentPanel(), c);

        c.gridx = 0;
        c.gridy++;
        c.gridwidth = 2;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1;
        samplingPanel.add(new JSeparator(), c);

        c.gridy++;
        c.gridwidth = 1;
        c.fill = GridBagConstraints.NONE;
        samplingPanel.add(m_samplingComp.getComponentPanel(), c);

        c.gridy++;
        samplingPanel.add(m_stratifiedComp.getComponentPanel(), c);

        c.gridx++;
        samplingPanel.add(m_columnComp.getComponentPanel(), c);

        super.addTab("Sampling Method", samplingPanel);
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("null")
    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings, final PortObjectSpec[] ports)
        throws NotConfigurableException {
        DatabasePortObjectSpec dbSpec = (DatabasePortObjectSpec)ports[0];
        final DataTableSpec[] specs;
        if (dbSpec == null) {
            specs = new DataTableSpec[]{null};
        } else {
            specs = new DataTableSpec[]{dbSpec.getDataTableSpec()};
        }
        boolean random;
        try {
            random = dbSpec.getConnectionSettings(getCredentialsProvider()).getUtility().supportsRandomSampling();
        } catch (InvalidSettingsException e) {
            throw new NotConfigurableException(e.getMessage());
        }

        m_countComp.loadSettingsFrom(settings, specs);
        m_absoluteComp.loadSettingsFrom(settings, specs);
        m_relativeComp.loadSettingsFrom(settings, specs);
        m_samplingComp.loadSettingsFrom(settings, specs);
        m_stratifiedComp.loadSettingsFrom(settings, specs);
        m_columnComp.loadSettingsFrom(settings, specs);

        if (!random) {
            m_samplingComp.setToolTipText("Connected database does not support random sampling");
            m_samplingMethod.setStringValue(DBSamplingNodeModel.SamplingMethod.FIRST.getActionCommand());
        } else {
            m_samplingComp.setToolTipText(null);
        }
        m_samplingMethod.setEnabled(random);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) throws InvalidSettingsException {
        m_countComp.saveSettingsTo(settings);
        m_absoluteComp.saveSettingsTo(settings);
        m_relativeComp.saveSettingsTo(settings);
        m_samplingComp.saveSettingsTo(settings);
        m_stratifiedComp.saveSettingsTo(settings);
        m_columnComp.saveSettingsTo(settings);
    }
}
