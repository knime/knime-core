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
 */

package org.knime.core.node.port.database.aggregation.function.column;


import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import javax.swing.JPanel;

import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataValue;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnNameSelection;
import org.knime.core.node.defaultnodesettings.SettingsModelString;


/**
 * {@link JPanel} that allows the user to specify layout mapping settings.
 *
 * @author Tobias Koetter
 * @since 2.11
 */
public class ColumnFuntionSettingsPanel extends JPanel {

    private static final long serialVersionUID = 1;
    private final DialogComponentColumnNameSelection m_columnComponent;
    /**
     * @param settings the {@link ColumnFuntionSettings} to use
     * @param label label for dialog in front of column name select box
     * @param classFilter which classes are available for selection
     */
    @SafeVarargs
    public ColumnFuntionSettingsPanel(final ColumnFuntionSettings settings, final String label,
        final Class<? extends DataValue>... classFilter) {
        final JPanel rootPanel = new JPanel(new GridBagLayout());
//        rootPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory
//                .createEtchedBorder(), " Settings "));
        final GridBagConstraints c = new GridBagConstraints();
        c.anchor = GridBagConstraints.FIRST_LINE_START;
        c.gridx = 0;
        c.gridy = 0;
        final SettingsModelString xModel = settings.getModel();
        m_columnComponent = new DialogComponentColumnNameSelection(xModel, label, 0, classFilter);
        rootPanel.add(m_columnComponent.getComponentPanel(),
            c);
        add(rootPanel);
    }

    /**
     * Read value(s) of this dialog component from the configuration object.
     * This method will be called by the dialog pane only.
     *
     * @param settings the <code>NodeSettings</code> to read from
     * @param spec the input {@link DataTableSpec}
     * @throws NotConfigurableException If there is no chance for the dialog
     *             component to be valid (i.e. the settings are valid), e.g. if
     *             the given spec lacks some important columns or column types.
     */
    public void loadSettingsFrom(final NodeSettingsRO settings, final DataTableSpec spec)
            throws NotConfigurableException {
        m_columnComponent.loadSettingsFrom(settings, new DataTableSpec[] {spec});
    }
}
