
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
 * -------------------------------------------------------------------
 *
 */
package org.knime.base.node.io.database;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;

import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.knime.base.node.io.database.util.DBDialogPane;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.database.DatabaseConnectionPortObjectSpec;
import org.knime.core.node.port.database.DatabaseConnectionSettings;

/**
 * Dialog pane of the database writer.
 *
 * @author Thomas Gabriel, University of Konstanz
 */
final class DBWriterDialogPane extends NodeDialogPane {

    private final DBDialogPane m_loginPane = new DBDialogPane(false);

    private final JTextField m_table = new JTextField("");

    private final JCheckBox m_append = new JCheckBox("... to existing table (if any!)");

    private final JCheckBox m_insertNullForMissing = new JCheckBox("Insert null for missing columns");

    private final JCheckBox m_failOnError = new JCheckBox("Fail if an error occurs");

    private final DBSQLTypesPanel m_typePanel;

    private final JTextField m_batchSize;

    /**
     * Creates new dialog.
     */
    DBWriterDialogPane() {
// add login and table name tab
        JPanel tableAndConnectionPanel = new JPanel(new GridBagLayout());

        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        c.anchor = GridBagConstraints.NORTHWEST;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1;

        tableAndConnectionPanel.add(m_loginPane, c);

        c.gridy++;
        JPanel p = new JPanel(new GridLayout());
        p.add(m_table);
        p.setBorder(BorderFactory.createTitledBorder(" Table Name "));
        tableAndConnectionPanel.add(p, c);

        c.gridy++;
        p = new JPanel(new GridLayout());
        p.add(m_append);
        m_append.addChangeListener(new ChangeListener() {

            @Override
            public void stateChanged(final ChangeEvent e) {
                m_insertNullForMissing.setEnabled(m_append.isSelected());
            }
        });
        m_append.setToolTipText("Data columns from input and database table must match!");
        p.add(m_insertNullForMissing);

        p.setBorder(BorderFactory.createTitledBorder(" Append Data "));
        tableAndConnectionPanel.add(p, c);

        c.gridy++;
        p = new JPanel(new GridLayout());
        p.add(m_failOnError);
        p.setBorder(BorderFactory.createTitledBorder(" Error Handling "));
        tableAndConnectionPanel.add(p, c);

        super.addTab("Settings", tableAndConnectionPanel);

// add SQL Types tab
        m_typePanel = new DBSQLTypesPanel();
        final JScrollPane scroll = new JScrollPane(m_typePanel);
        scroll.setPreferredSize(m_loginPane.getPreferredSize());
        super.addTab("SQL Types", scroll);

// advanced tab with batch size
        final JPanel batchSizePanel = new JPanel(new FlowLayout());
        batchSizePanel.add(new JLabel("Batch Size: "));
        m_batchSize = new JTextField();
        m_batchSize.setPreferredSize(new Dimension(100, 20));
        batchSizePanel.add(m_batchSize);
        super.addTab("Advanced", batchSizePanel);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings,
            final PortObjectSpec[] specs) throws NotConfigurableException {
        // get workflow credentials
        m_loginPane.loadSettingsFrom(settings, specs, getCredentialsProvider());
        // table name
        m_table.setText(settings.getString(DBWriterNodeModel.KEY_TABLE_NAME, ""));
        // append data flag
        m_append.setSelected(settings.getBoolean(DBWriterNodeModel.KEY_APPEND_DATA, true));

        m_insertNullForMissing.setSelected(
            settings.getBoolean(DBWriterNodeModel.KEY_INSERT_NULL_FOR_MISSING_COLS, false));
        m_insertNullForMissing.setEnabled(m_append.isSelected());

        //introduced in KNIME 3.3.1 default behavior was not failing e.g. false
        m_failOnError.setSelected(settings.getBoolean(DBWriterNodeModel.KEY_FAIL_ON_ERROR, false));

        // load SQL Types for each column
        try {
            NodeSettingsRO typeSett = settings.getNodeSettings(DBWriterNodeModel.CFG_SQL_TYPES);
            m_typePanel.loadSettingsFrom(typeSett, (DataTableSpec)specs[0]);
        } catch (InvalidSettingsException ise) {
            m_typePanel.loadSettingsFrom(null, (DataTableSpec)specs[0]);
        }

        // load batch size
        final int batchSize = settings.getInt(DBWriterNodeModel.KEY_BATCH_SIZE,
                                              DatabaseConnectionSettings.BATCH_WRITE_SIZE);
        m_batchSize.setText(Integer.toString(batchSize));

        if ((specs.length > 1) && (specs[1] instanceof DatabaseConnectionPortObjectSpec)) {
            m_loginPane.setVisible(false);
        } else {
            m_loginPane.setVisible(true);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings)
            throws InvalidSettingsException {
        m_loginPane.saveSettingsTo(settings, getCredentialsProvider());

        settings.addString(DBWriterNodeModel.KEY_TABLE_NAME, m_table.getText().trim());
        settings.addBoolean(DBWriterNodeModel.KEY_APPEND_DATA, m_append.isSelected());
        settings.addBoolean(DBWriterNodeModel.KEY_INSERT_NULL_FOR_MISSING_COLS, m_insertNullForMissing.isSelected());
        //introduced in KNIME 3.3.1 legacy behavior is not failing e.g. false
        settings.addBoolean(DBWriterNodeModel.KEY_FAIL_ON_ERROR, m_failOnError.isSelected());

        // save SQL Types for each column
        NodeSettingsWO typeSett = settings.addNodeSettings(DBWriterNodeModel.CFG_SQL_TYPES);
        m_typePanel.saveSettingsTo(typeSett);

        // save batch size
        final String strBatchSite = m_batchSize.getText().trim();
        if (strBatchSite.isEmpty()) {
            throw new InvalidSettingsException("Batch size must not be empty.");
        }
        try {
            final int intBatchSize = Integer.parseInt(strBatchSite);
            settings.addInt(DBWriterNodeModel.KEY_BATCH_SIZE, intBatchSize);
        } catch (final NumberFormatException nfe) {
            throw new InvalidSettingsException("Can't parse batch size \"" + strBatchSite
                                               + "\", reason: " + nfe.getMessage(), nfe);
        }
    }
}
