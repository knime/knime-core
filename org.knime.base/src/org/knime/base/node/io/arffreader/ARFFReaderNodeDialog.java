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
 * -------------------------------------------------------------------
 *
 * History
 *   11.02.2005 (ohl): created
 */
package org.knime.base.node.io.arffreader;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.util.FilesHistoryPanel;
import org.knime.core.node.util.FilesHistoryPanel.LocationValidation;
import org.knime.core.node.workflow.FlowVariable;


/**
 * Contains the dialog for the ARFF file reader.
 *
 * @author Peter Ohl, University of Konstanz
 */
public class ARFFReaderNodeDialog extends NodeDialogPane {

    private final FilesHistoryPanel m_filePanel;

    private final JTextField m_rowPrefix;

    /**
     * Creates a new ARFF file reader dialog.
     */
    public ARFFReaderNodeDialog() {
        super();
        JPanel panel = new JPanel(new BorderLayout());

        m_rowPrefix = new JTextField("Row", 10);
        m_filePanel =
            new FilesHistoryPanel(createFlowVariableModel(ARFFReaderNodeModel.CFGKEY_FILEURL, FlowVariable.Type.STRING),
                "arff_read", LocationValidation.FileInput, ".arff");
        panel.add(m_filePanel, BorderLayout.NORTH);
        JPanel centerPanel = initLayout();
        panel.add(centerPanel, BorderLayout.CENTER);

        addTab("Specify ARFF file", panel);

    }

    private JPanel initLayout() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.WEST;

        gbc.gridy += 1;
        gbc.gridwidth = 1;
        panel.add(new JLabel("RowID prefix: "), gbc);
        gbc.gridx += 1;
        panel.add(m_rowPrefix, gbc);
        return panel;
    }



    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings,
            final DataTableSpec[] specs) throws NotConfigurableException {
        m_filePanel.updateHistory();
        m_filePanel.setSelectedFile(settings.getString(ARFFReaderNodeModel.CFGKEY_FILEURL, ""));
        String rowPrefix = settings.getString(ARFFReaderNodeModel.CFGKEY_ROWPREFIX, "Row");
        m_rowPrefix.setText(rowPrefix != null ? rowPrefix : "Row");

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings)
            throws InvalidSettingsException {
        if (m_filePanel.getSelectedFile() == null || m_filePanel.getSelectedFile().equals("")) {
            throw new InvalidSettingsException("No input selected");
        }
        settings.addString(ARFFReaderNodeModel.CFGKEY_FILEURL, m_filePanel.getSelectedFile().trim());
        settings.addString(ARFFReaderNodeModel.CFGKEY_ROWPREFIX, m_rowPrefix.getText());
    }
}
