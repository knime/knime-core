/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2011
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
 *   11.02.2008 (thor): created
 */
package org.knime.base.node.viz.roc;

import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Set;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.Border;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.NominalValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.util.ColumnFilterPanel;
import org.knime.core.node.util.ColumnSelectionComboxBox;

/**
 * This dialog lets the user set the necessary values for drawing ROC curves.
 *
 * @author Thorsten Meinl, University of Konstanz
 */
public class ROCNodeDialog extends NodeDialogPane {
    private static final Color ORANGE = new Color(230, 180, 0);

    private final ROCSettings m_settings = new ROCSettings();

    private DataTableSpec m_spec;

    @SuppressWarnings("unchecked")
    private final ColumnSelectionComboxBox m_classColumn =
            new ColumnSelectionComboxBox((Border)null, NominalValue.class);

    private final JComboBox m_positiveClass =
            new JComboBox(new DefaultComboBoxModel());

    @SuppressWarnings("unchecked")
    private final ColumnFilterPanel m_sortColumns =
            new ColumnFilterPanel(false, DoubleValue.class);

    private final JLabel m_warningLabel = new JLabel();

    /**
     * Creates a new dialog.
     */
    public ROCNodeDialog() {
        final JPanel p = new JPanel(new GridBagLayout());

        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        c.insets = new Insets(2, 2, 2, 2);
        c.anchor = GridBagConstraints.NORTHWEST;

        p.add(new JLabel("Class column   "), c);
        c.gridx++;
        p.add(m_classColumn, c);
        m_classColumn.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                changeClassColumn(p);
            }
        });

        c.gridx = 0;
        c.gridy++;
        p.add(new JLabel("Positive class value   "), c);
        c.gridx++;
//        m_positiveClass.setMinimumSize(new Dimension(100, m_positiveClass
//                .getHeight()));
        p.add(m_positiveClass, c);

        c.gridx++;
        c.anchor = GridBagConstraints.WEST;
        p.add(m_warningLabel, c);
        c.anchor = GridBagConstraints.NORTHWEST;

        c.gridy++;
        c.gridx = 0;
        c.gridwidth = 3;
        p.add(
                new JLabel(
                        "Columns containing the positive class probabilities"),
                c);

        c.gridy++;
        c.gridx = 0;
        c.gridwidth = 3;
        p.add(m_sortColumns, c);

        addTab("Standard Settings", p);
    }

    /**
     * Called if the user changed the class column.
     *
     * @param parent the panel which is the parent for message boxes
     */
    private void changeClassColumn(final JComponent parent) {
        String selCol = m_classColumn.getSelectedColumn();
        ((DefaultComboBoxModel)m_positiveClass.getModel()).removeAllElements();
        if ((selCol != null) && (m_spec != null)) {
            DataColumnSpec cs = m_spec.getColumnSpec(selCol);
            Set<DataCell> values = cs.getDomain().getValues();
            if (values == null) {
                m_warningLabel.setForeground(Color.RED);
                m_warningLabel.setText(" Column '" + selCol
                        + "' contains no possible values");
                return;
            }

            if (values.size() > 2) {
                m_warningLabel.setForeground(ORANGE);
                m_warningLabel.setText(" Column '" + selCol
                        + "' contains more than two possible values");
            } else {
                m_warningLabel.setText("");
            }
            for (DataCell cell : values) {
                m_positiveClass.addItem(cell);
            }
            parent.revalidate();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings,
            final DataTableSpec[] specs) throws NotConfigurableException {
        m_settings.loadSettingsForDialog(settings);

        m_spec = specs[0];
        m_classColumn.update(specs[0], m_settings.getClassColumn());
        m_positiveClass.setSelectedItem(m_settings.getPositiveClass());
        m_sortColumns.update(specs[0], false, m_settings.getCurves());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings)
            throws InvalidSettingsException {
        m_settings.setClassColumn(m_classColumn.getSelectedColumn());
        m_settings
                .setPositiveClass((DataCell)m_positiveClass.getSelectedItem());
        m_settings.getCurves().clear();
        m_settings.getCurves().addAll(m_sortColumns.getIncludedColumnSet());
        m_settings.saveSettings(settings);
    }

}
