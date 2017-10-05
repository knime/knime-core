/*
 * ------------------------------------------------------------------------
 *  Copyright by KNIME GmbH, Konstanz, Germany
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
 * ---------------------------------------------------------------------
 *
 * Created on 21.10.2013 by NanoTec
 */
package org.knime.base.node.preproc.rename;

import static org.knime.core.node.util.DataColumnSpecListCellRenderer.isInvalid;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataValue;
import org.knime.core.node.util.DataColumnSpecListCellRenderer;

/**
 * The panel for a rename configuration.
 *
 * @author Marcel Hanser
 */
@SuppressWarnings("serial")
final class RenameColumnPanel extends JPanel {

    /** abbreviate names to this number of letters. */
    private static final int MAX_LETTERS = 25;

    /**
     * fired if the remove button is pressed.
     */
    public static final String REMOVE_ACTION = "REMOVE_ACTION";

    private final RenameColumnSetting m_settings;

    private final DataColumnSpec m_columnSpec;

    /**
     * @param colSet the column settings
     * @param spec the column spec
     * @param hideTypeBox <code>true</code> if the column type select box should be hidden
     */
    @SuppressWarnings("unchecked")
    RenameColumnPanel(final RenameColumnSetting colSet, final DataColumnSpec spec, final boolean hideTypeBox) {

        m_settings = colSet;
        m_columnSpec = spec;
        final String oldColName = colSet.getName();
        final String newColName = colSet.getNewColumnName();
        boolean hasNewName = newColName != null;

        String labelName =
            oldColName.length() > MAX_LETTERS ? oldColName.substring(0, MAX_LETTERS) + "..." : oldColName;

        Class<? extends DataValue>[] possibleTypes = colSet.getPossibleValueClasses();
        possibleTypes = possibleTypes == null ? new Class[0] : possibleTypes;

        int selectedType = colSet.getNewValueClassIndex();
        final JLabel nameLabel = new JLabel(labelName);
        nameLabel.setToolTipText(oldColName);

        JButton removeButton = new JButton("Remove");
        removeButton.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(final ActionEvent e) {
                firePropertyChange(REMOVE_ACTION, null, null);
            }
        });

        final JTextField newNameField = new JTextField(oldColName, 12);

        if (hasNewName) {
            newNameField.setText(newColName);
        }

        // add listeners to all fields that can change: They will update their
        // RenameColumnSetting immediately
        newNameField.addFocusListener(new FocusListener() {
            @Override
            public void focusGained(final FocusEvent e) {
                // no op
            }

            @Override
            public void focusLost(final FocusEvent e) {
                String newText = newNameField.getText();
                colSet.setNewColumnName(newText);
            }
        });
        // Bug 3993: Column Rename doesn't commit value on OK
        newNameField.addCaretListener(new CaretListener() {
            @Override
            public void caretUpdate(final CaretEvent e) {
                String newText = newNameField.getText();
                colSet.setNewColumnName(newText);
            }
        });
        final JCheckBox checker = new JCheckBox("Change: ");
        newNameField.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(final MouseEvent e) {
                if (!newNameField.isEnabled()) {
                    assert (!checker.isSelected());
                    checker.doClick();
                }
            }
        });
        checker.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(final ChangeEvent e) {
                if (checker.isSelected()) {
                    newNameField.setEnabled(true);
                    newNameField.requestFocus();
                } else {
                    newNameField.setText(oldColName);
                    colSet.setNewColumnName(null);
                    newNameField.setEnabled(false);
                }
            }
        });
        checker.setSelected(hasNewName);
        newNameField.setEnabled(hasNewName);

        setBorder(isInvalid(spec) ? BorderFactory.createLineBorder(Color.RED, 2) : BorderFactory.createLineBorder(
            Color.BLACK, 1));

        JPanel northLayout = new JPanel(new BorderLayout());
        northLayout.add(nameLabel, BorderLayout.WEST);
        northLayout.add(removeButton, BorderLayout.EAST);

        JPanel southLayout = new JPanel(new BorderLayout(0, 10));
        southLayout.add(checker, BorderLayout.WEST);
        southLayout.add(newNameField, BorderLayout.CENTER);
        if (!hideTypeBox) {
            final JComboBox typeChooser = new JComboBox(possibleTypes);
            // no need to make it enabled when there is only one choice.
            typeChooser.setEnabled(possibleTypes.length != 1);
            typeChooser.setRenderer(new DataTypeNameRenderer());

            if (possibleTypes.length > 0) {
                typeChooser.setSelectedIndex(selectedType);
            }

            typeChooser.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(final ActionEvent e) {
                    colSet.setNewValueClassIndex(typeChooser.getSelectedIndex());
                }
            });
            southLayout.add(typeChooser, BorderLayout.EAST);
        }

        setLayout(new FlowLayout());

        JPanel moep = new JPanel(new BorderLayout());
        moep.add(northLayout, BorderLayout.NORTH);
        moep.add(southLayout, BorderLayout.SOUTH);
        moep.add(createSpacer(75), BorderLayout.CENTER);
        add(moep);
    }

    /**
     * @return
     */
    private JLabel createSpacer(final int size) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < size; i++) {
            builder.append("L");
        }
        JLabel jLabel = new JLabel(builder.toString());
        jLabel.setForeground(getBackground());
        return jLabel;
    }

    /**
     * @return the settings
     */
    RenameColumnSetting getSettings() {
        return m_settings;
    }

    /**
     * @return the column spec
     */
    DataColumnSpec getColumnSpec() {
        return m_columnSpec;
    }

    /**
     * @return <code>true</code> if {@link DataColumnSpecListCellRenderer#isInvalid(DataColumnSpec)} returns
     *         <code>false</code> for the current spec
     */
    boolean hasValidSpec() {
        return !isInvalid(m_columnSpec);
    }

}
