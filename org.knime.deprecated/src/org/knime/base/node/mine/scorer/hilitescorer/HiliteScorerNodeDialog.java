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
 * --------------------------------------------------------------------- *
 */
package org.knime.base.node.mine.scorer.hilitescorer;

import java.awt.FlowLayout;
import java.awt.GridLayout;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JPanel;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.util.DataColumnSpecListCellRenderer;

/**
 * A dialog for the scorer to set the two table columns to score for.
 * 
 * @author Christoph Sieb, University of Konstanz
 * @author Thomas Gabriel, University of Konstanz
 */
public final class HiliteScorerNodeDialog extends NodeDialogPane {
    /*
     * The main panel in this view.
     */
    private final JPanel m_p;

    /*
     * The text field for the first column to compare The first column
     * represents the real classes of the data
     */
    private final JComboBox m_firstColumns;

    /*
     * The text field for the second column to compare The second column
     * represents the predicted classes of the data
     */
    private final JComboBox m_secondColumns;

    /**
     * Creates a new {@link NodeDialogPane} for scoring in order to set the two
     * columns to compare.
     */
    public HiliteScorerNodeDialog() {
        super();

        m_p = new JPanel();
        m_p.setLayout(new BoxLayout(m_p, BoxLayout.Y_AXIS));

        m_firstColumns = new JComboBox();
        m_firstColumns.setRenderer(new DataColumnSpecListCellRenderer());
        m_secondColumns = new JComboBox();
        m_secondColumns.setRenderer(new DataColumnSpecListCellRenderer());

        JPanel firstColumnPanel = new JPanel(new GridLayout(1, 1));
        firstColumnPanel.setBorder(BorderFactory
                .createTitledBorder("First Column"));
        JPanel flowLayout = new JPanel(new FlowLayout());
        flowLayout.add(m_firstColumns);
        firstColumnPanel.add(flowLayout);

        JPanel secondColumnPanel = new JPanel(new GridLayout(1, 1));
        secondColumnPanel.setBorder(BorderFactory
                .createTitledBorder("Second Column"));
        flowLayout = new JPanel(new FlowLayout());
        flowLayout.add(m_secondColumns);
        secondColumnPanel.add(flowLayout);

        m_p.add(firstColumnPanel);

        m_p.add(secondColumnPanel);

        super.addTab("Scorer", m_p);
    } // ScorerNodeDialog(NodeModel)

    /**
     * Fills the two combo boxes with all column names retrieved from the input
     * table spec. The second and last column will be selected by default unless
     * the settings object contains others.
     * 
     * {@inheritDoc}
     */
    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings,
            final DataTableSpec[] specs) throws NotConfigurableException {
        assert (settings != null && specs != null);

        m_firstColumns.removeAllItems();
        m_secondColumns.removeAllItems();

        DataTableSpec spec = specs[HiliteScorerNodeModel.INPORT];

        if ((spec == null) || (spec.getNumColumns() < 2)) {
            throw new NotConfigurableException("Scorer needs an input table "
                    + "with at least two columns");
        }

        int numCols = spec.getNumColumns();
        for (int i = 0; i < numCols; i++) {
            DataColumnSpec c = spec.getColumnSpec(i);
            m_firstColumns.addItem(c);
            m_secondColumns.addItem(c);
        }
        // if at least two columns available
        DataColumnSpec col2 =
                (numCols > 0) ? spec.getColumnSpec(numCols - 1) : null;
        DataColumnSpec col1 =
                (numCols > 1) ? spec.getColumnSpec(numCols - 2) : col2;
        col1 =
                spec.getColumnSpec(settings.getString(
                        HiliteScorerNodeModel.FIRST_COMP_ID, col1.getName()));
        col2 =
                spec.getColumnSpec(settings.getString(
                        HiliteScorerNodeModel.SECOND_COMP_ID, col2.getName()));
        m_firstColumns.setSelectedItem(col1);
        m_secondColumns.setSelectedItem(col2);
    }

    /**
     * Sets the selected columns inside the {@link HiliteScorerNodeModel}.
     * 
     * @param settings the object to write the settings into
     * @throws InvalidSettingsException if the column selection is invalid
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings)
            throws InvalidSettingsException {
        assert (settings != null);

        String firstColumn =
                ((DataColumnSpec)m_firstColumns.getSelectedItem()).getName();
        String secondColumn =
                ((DataColumnSpec)m_secondColumns.getSelectedItem()).getName();

        if ((firstColumn == null) || (secondColumn == null)) {
            throw new InvalidSettingsException("Select two valid column names "
                    + "from the lists (or press cancel).");
        }
        if (m_firstColumns.getItemCount() > 1
                && firstColumn.equals(secondColumn)) {
            throw new InvalidSettingsException(
                    "First and second column cannot be the same.");
        }
        settings.addString(HiliteScorerNodeModel.FIRST_COMP_ID, firstColumn);
        settings.addString(HiliteScorerNodeModel.SECOND_COMP_ID, secondColumn);
    }
}
