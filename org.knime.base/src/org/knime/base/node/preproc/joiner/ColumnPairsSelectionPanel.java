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
 *  propagated with or for interoperation with KNIME. The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ------------------------------------------------------------------------
 *
 * History
 *   18.11.2009 (Heiko Hofer): created
 */
package org.knime.base.node.preproc.joiner;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;

/**
 * A Component used to define a list of column pairs.
 *
 * @author Heiko Hofer
 */
public class ColumnPairsSelectionPanel extends JPanel {
    private static final long serialVersionUID = 1L;

    private DataTableSpec[] m_specs;

    private final List<JComboBox> m_leftComboBoxes;
    private final List<JComboBox> m_rightComboBoxes;
    private final List<JButton> m_addButtons;
    private final List<JButton> m_removeButtons;

    private final ActionListener m_addButtonListener;
    private final ActionListener m_removeButtonListener;

    private final JComponent m_fillComponent;

    private final JButton m_persistentAddButton;


    /**
     * Creates a new instance. Use updateData(...) to initialize the component.
     */
    public ColumnPairsSelectionPanel() {
        super(new GridBagLayout());

        m_leftComboBoxes = new ArrayList<JComboBox>();
        m_rightComboBoxes = new ArrayList<JComboBox>();
        m_addButtons = new ArrayList<JButton>();
        m_removeButtons = new ArrayList<JButton>();

        m_addButtonListener = new AddButtonListener();
        m_removeButtonListener = new RemoveButtonListener();

        m_fillComponent = new JPanel();
        m_fillComponent.setBackground(Color.WHITE);
        m_persistentAddButton = new JButton("Add row");
        m_persistentAddButton.setToolTipText("Append row at the end");
        m_persistentAddButton.addActionListener(new ActionListener() {
            /**  {@inheritDoc} */
            @Override
            public void actionPerformed(final ActionEvent e) {
                addUIControls(m_addButtons.size(), null, null);
                updateLayout();
            }
        });
    }


    /**
     * Updates the component with the given data.
     * leftSelected[i] and rightSelected[i] are a pair of columns out of
     * specs[0] and specs[1], respectively. If not, they will be replaced by
     * Joiner2Settings.ROW_KEY_COL_NAME.
     *
     * UI-Controls are provided to change, add or remove pairs of columns.
     *
     * @param specs an array with two elements
     * @param leftSelected the selected columns of specs[0]
     * @param rightSelected the selected columns of specs[1]
     */
    public void updateData(final DataTableSpec[] specs,
            final String[] leftSelected,
            final String[] rightSelected) {
        m_specs = specs;
        removeAllUIControls();
        if (null != leftSelected) {
            for (int i = 0; i < leftSelected.length; i++) {
                addUIControls(i, leftSelected[i], rightSelected[i]);
            }
        } else {
            addUIControls(0, null, null);
        }
        updateLayout();
    }


    /**
     * Returns an array with elements of type DataColumnSpec or String. If
     * an element is of type it is one column spec of specs[0] (see updataData).
     * If an element is of type string it is equal to
     * Joiner2Settings.ROW_KEY_COL_NAME.
     *
     * @return the selected columns of the left table
     */
    public Object[] getLeftSelectedItems() {
        Object[] r = new Object[m_leftComboBoxes.size()];
        for (int i = 0; i < r.length; i++) {
            r[i] = m_leftComboBoxes.get(i).getSelectedItem();
        }
        return r;
    }

    /**
     * Returns an array with elements of type DataColumnSpec or String. If
     * an element is of type it is one column spec of specs[1] (see updataData).
     * If an element is of type string it is equal to
     * Joiner2Settings.ROW_KEY_COL_NAME.
     *
     * @return the selected columns of the right table
     */
    public Object[] getRightSelectedItems() {
        Object[] r = new Object[m_rightComboBoxes.size()];
        for (int i = 0; i < r.length; i++) {
            r[i] = m_rightComboBoxes.get(i).getSelectedItem();
        }
        return r;
    }

    private void initConstraints(final GridBagConstraints c) {
        if (m_leftComboBoxes.size() > 0) {
            c.anchor = GridBagConstraints.PAGE_START;
            c.fill = GridBagConstraints.BOTH;
            c.weightx = 1;
        }
        c.insets = new Insets(2, 2, 2, 2);
        c.gridx = 0;
        c.gridy = 0;
    }


    private void initComboBox(final DataTableSpec spec,
            final JComboBox comboBox,
            final String selected) {
        DefaultComboBoxModel comboBoxModel =
            (DefaultComboBoxModel)comboBox.getModel();
        comboBoxModel.removeAllElements();
        comboBoxModel.addElement(Joiner2Settings.ROW_KEY_COL_NAME);
        comboBox.setSelectedIndex(0);
        for (DataColumnSpec colSpec : spec) {
            comboBoxModel.addElement(colSpec);
            if (null != selected
                    && colSpec.getName().equals(selected)) {
                comboBoxModel.setSelectedItem(colSpec);
            }

        }
    }

    private void addUIControls(final int index, final String leftSelected,
            final String rightSelected) {
        m_leftComboBoxes.add(index, new JComboBox());
        m_leftComboBoxes.get(index).setModel(new DefaultComboBoxModel());
        m_leftComboBoxes.get(index).setRenderer(new ColumnSpecListRenderer());
        initComboBox(m_specs[0], m_leftComboBoxes.get(index), leftSelected);
        m_rightComboBoxes.add(index, new JComboBox());
        m_rightComboBoxes.get(index).setModel(new DefaultComboBoxModel());
        m_rightComboBoxes.get(index).setRenderer(new ColumnSpecListRenderer());
        initComboBox(m_specs[1], m_rightComboBoxes.get(index), rightSelected);
        JButton addButton = new JButton("+");
        addButton.setToolTipText("Add row preceding this.");
        addButton.addActionListener(m_addButtonListener);
        m_addButtons.add(index, addButton);
        JButton removeButton = new JButton("-");
        removeButton.addActionListener(m_removeButtonListener);
        removeButton.setToolTipText("Remove this row.");
        m_removeButtons.add(index, removeButton);

        // if the first row was added
        if (m_leftComboBoxes.size() == 1) {
            m_persistentAddButton.setText("+");
        }
    }

    private void removeUIControls(final int index) {
        Component c = m_leftComboBoxes.remove(index);
        remove(c);
        c = m_rightComboBoxes.remove(index);
        remove(c);
        c = m_addButtons.remove(index);
        remove(c);
        c = m_removeButtons.remove(index);
        remove(c);

        // if the last row was removed
        if (m_leftComboBoxes.size() == 0) {
            m_persistentAddButton.setText("Add row");
        }

    }

    private void removeAllUIControls() {
        for (int i = m_rightComboBoxes.size() - 1; i >= 0; i--) {
            removeUIControls(i);
        }
    }

    private void updateLayout() {
        GridBagConstraints c = new GridBagConstraints();
        initConstraints(c);

        for (int i = 0; i < m_leftComboBoxes.size(); i++) {
            c.weightx = 1;
            addOrUpdate(m_leftComboBoxes.get(i), c);
            c.gridx++;
            c.weightx = 1;
            addOrUpdate(m_rightComboBoxes.get(i), c);
            c.gridx++;
            c.weightx = 0;
            addOrUpdate(m_addButtons.get(i), c);
            c.gridx++;
            c.weightx = 0;
            addOrUpdate(m_removeButtons.get(i), c);
            c.gridx = 0;
            c.gridy++;
        }

        c.gridx = 2;
        addOrUpdate(m_persistentAddButton, c);

        c.gridy++;
        c.weighty = 1;
        addOrUpdate(m_fillComponent, c);

        revalidate();
    }

    private void addOrUpdate(final JComponent component,
            final GridBagConstraints c) {
        List<Component> components = Arrays.asList(
                this.getComponents());
        if (components.contains(component)) {
            ((GridBagLayout)getLayout()).setConstraints(component, c);
        } else {
            add(component, c);
        }
    }


    private class AddButtonListener implements ActionListener {
        /** {@inheritDoc} */
        @Override
        public void actionPerformed(final ActionEvent e) {
            JButton button = (JButton)e.getSource();
            int index = m_addButtons.indexOf(button);
            addUIControls(index, null, null);
            updateLayout();
        }
    }

    private class RemoveButtonListener implements ActionListener {
        /** {@inheritDoc}  */
        @Override
        public void actionPerformed(final ActionEvent e) {
            JButton button = (JButton)e.getSource();
            int index = m_removeButtons.indexOf(button);
            int x = m_leftComboBoxes.get(index).getLocation().x;
            int y = m_leftComboBoxes.get(index).getLocation().y;
            int width = m_removeButtons.get(index).getBounds().x
                + m_removeButtons.get(index).getBounds().width - x;
            int height = m_removeButtons.get(index).getBounds().y
            + m_removeButtons.get(index).getBounds().height - y;

            removeUIControls(index);
            updateLayout();
            // Initiate a repaint on the area of the removed columns.
            // It is a workaround which seems to be a bug in the swing
            // repainting which occurs when the last row is deleted when
            // more than two rows are displayed.
            repaint(x, y, width, height);
        }
    }


    /**
     * Returns a component which is intended to be used as the header view of
     * a scroll pane.
     *
     * @return a component for the header view of a scroll pane.
     */
    public Component getHeaderView() {
        return new ColumnHeaderView();
    }

    /**
     * A component which is intended to be used as the header view of
     * a scroll pane.
     *
     * @author Heiko Hofer
     */
    private class ColumnHeaderView extends JPanel {
        private static final long serialVersionUID = 1L;

        private JLabel m_leftHeader = new JLabel("Left Table");
        private JLabel m_rightHeader = new JLabel("Right Table");

        public ColumnHeaderView() {
            super(null);
            add(m_leftHeader);
            add(m_rightHeader);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void paintComponent(final Graphics g) {
            if (m_leftComboBoxes.size() > 0) {
                int offset = (m_leftComboBoxes.get(0).getSize().width
                        - m_leftHeader.getPreferredSize().width) / 2;
                m_leftHeader.setLocation(new Point(
                        m_leftComboBoxes.get(0).getX() + offset, 0));
                m_leftHeader.setSize(m_leftHeader.getPreferredSize());
            }
            if (m_rightComboBoxes.size() > 0) {
                int offset = (m_rightComboBoxes.get(0).getSize().width
                        - m_rightHeader.getPreferredSize().width) / 2;
                m_rightHeader.setLocation(new Point(
                        m_rightComboBoxes.get(0).getX() + offset, 0));
                m_rightHeader.setSize(m_rightHeader.getPreferredSize());
            }
            super.paintComponent(g);
        }
    }
}
