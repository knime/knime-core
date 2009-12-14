/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2009
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
package org.knime.base.node.preproc.binner;

import static java.lang.Double.NEGATIVE_INFINITY;
import static java.lang.Double.POSITIVE_INFINITY;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.text.ParseException;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JSplitPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.def.IntCell;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.util.DataColumnSpecListCellRenderer;


/**
 * Binner dialog used to group numeric columns (int or double) into intervals.
 * 
 * @author Thomas Gabriel, University of Konstanz
 */
final class BinnerNodeDialogPane extends NodeDialogPane {
    /** The node logger for this class. */
    private static final NodeLogger LOGGER = NodeLogger
            .getLogger(BinnerNodeDialogPane.class);

    /** List of numeric columns. */
    private final JList m_numList;

    /** The numeric columns' model. */
    private final DefaultListModel m_numMdl;

    /** Keeps shows the currenty selected interval. */
    private final JPanel m_numInterval;

    /** Keeps column data cell to interval panel settings. */
    private final LinkedHashMap<String, IntervalPanel> m_intervals;

    /**
     * Creates a new binner dialog.
     */
    BinnerNodeDialogPane() {
        super();
        m_intervals = new LinkedHashMap<String, IntervalPanel>();

        // numeric panel in tab
        final JPanel numericPanel = new JPanel(new GridLayout(1, 1));

        // numeric column list
        m_numMdl = new DefaultListModel();
        m_numMdl.addElement("<empty>");
        m_numList = new JList(m_numMdl);
        /**
         * Override renderer to plot number of defined bins.
         */
        class BinnerListCellRenderer extends DataColumnSpecListCellRenderer {
            /**
             * {@inheritDoc}
             */
            @Override
            public Component getListCellRendererComponent(final JList list,
                    final Object value, final int index,
                    final boolean isSelected, final boolean cellHasFocus) {
                Component c = super.getListCellRendererComponent(list, value,
                        index, isSelected, cellHasFocus);
                String name = ((DataColumnSpec)value).getName();
                IntervalPanel p = m_intervals.get(name);
                if (p != null) {
                    int bins = p.getNumIntervals();
                    if (bins > 0) {
                        String text = getText() + " (";
                        if (bins == 1) {
                            text += bins + " bin defined";
                        } else {
                            text += bins + " bins defined";
                        }
                        if (p.isAppendedColumn()) {
                            setText(text + ", append new)");
                        } else {
                            setText(text + ", replace this)");
                        }
                    }
                }
                return c;
            }

        }
        m_numList.setCellRenderer(new BinnerListCellRenderer());
        m_numList.addListSelectionListener(new ListSelectionListener() {
            /**
             * 
             */
            public void valueChanged(final ListSelectionEvent e) {
                columnChanged();
                numericPanel.validate();
                numericPanel.repaint();
            }
        });
        m_numList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        final JScrollPane numScroll = new JScrollPane(m_numList);
        numScroll.setMinimumSize(new Dimension(200, 155));
        numScroll
                .setBorder(BorderFactory.createTitledBorder(" Select Column "));

        // numeric column intervals
        m_numInterval = new JPanel(new GridLayout(1, 1));
        m_numInterval.setBorder(BorderFactory.createTitledBorder(" "));
        m_numInterval.setMinimumSize(new Dimension(350, 300));
        m_numInterval.setPreferredSize(new Dimension(350, 300));
        JSplitPane split = new JSplitPane(
                JSplitPane.HORIZONTAL_SPLIT, numScroll, m_numInterval);
        numericPanel.add(split);
        super.addTab(" Intervals ", numericPanel);
    }

    private void columnChanged() {
        m_numInterval.removeAll();
        Object o = m_numList.getSelectedValue();
        if (o == null) {
            m_numInterval.setBorder(BorderFactory
                    .createTitledBorder(" Select Column "));
        } else {
            m_numInterval.setBorder(null);
            m_numInterval.add(createIntervalPanel((DataColumnSpec)o));
        }
    }

    private SpinnerNumberModel createNumberModel(final DataType type) {
        if (IntCell.TYPE.equals(type)) {
            return new SpinnerNumberModel(new Integer(0), null, null,
                    new Integer(1));
        }
        return new SpinnerNumberModel(0.0, NEGATIVE_INFINITY,
                POSITIVE_INFINITY, 0.1);
    }

    private IntervalPanel createIntervalPanel(final DataColumnSpec cspec) {
        String name = cspec.getName();
        IntervalPanel p;
        if (m_intervals.containsKey(name)) {
            p = m_intervals.get(name);
        } else {
            p = new IntervalPanel(name, null, m_numList, cspec.getType());
            m_intervals.put(name, p);
        }
        p.validate();
        p.repaint();
        return p;
    }

    /**
     * Creates new panel holding one bin column. 
     */
    final class IntervalPanel extends JPanel {
        /** List of intervals. */
        private final JList m_intervalList;

        /** The intervals' model. */
        private final DefaultListModel m_intervalMdl;

        private final JCheckBox m_appendColumn;

        private final JTextField m_appendName;

        /**
         * Create new interval panel.
         * 
         * @param column the current column name
         * @param appendColumn if a new binned column is append, otherwise the
         *            column is replaced
         * @param parent used to refresh column list is number of bins has
         *            changed
         * @param type the type for the spinner model
         * 
         */
        IntervalPanel(final String column, final String appendColumn,
                final Component parent, final DataType type) {
            super(new BorderLayout());
            setBorder(BorderFactory.createTitledBorder(" " + column + " "));
            m_intervalMdl = new DefaultListModel();
            m_intervalList = new JList(m_intervalMdl);
            Font font = new Font("Monospaced", Font.PLAIN, 12);
            m_intervalList.setFont(font);
            final JButton addButton = new JButton("Add");
            addButton.addActionListener(new ActionListener() {
                /**
                 * 
                 */
                public void actionPerformed(final ActionEvent e) {
                    final int size = m_intervalMdl.getSize();
                    // if the first interval is added
                    if (size == 0) {
                        m_intervalMdl.addElement(new IntervalItemPanel(
                                IntervalPanel.this, null, null, "Bin1", type));
                    } else {
                        // if the first interval needs to be split
                        if (size == 1) {
                            IntervalItemPanel p = new IntervalItemPanel(
                                   IntervalPanel.this, 0.0, null, "Bin2", type);
                            m_intervalMdl.addElement(p);
                            p.updateInterval();
                        } else {
                            Object o = m_intervalList.getSelectedValue();
                            // if non is selected or the last one is selected
                            if (o == null
                                    || m_intervalMdl.indexOf(o) == size - 1) {
                                IntervalItemPanel p1 = 
                                    (IntervalItemPanel)m_intervalMdl
                                        .getElementAt(size - 1);
                                double d = p1.getLeftValue(false);
                                IntervalItemPanel p = new IntervalItemPanel(
                                        IntervalPanel.this, d,
                                        POSITIVE_INFINITY, "Bin" + (size + 1),
                                        type);
                                m_intervalMdl.insertElementAt(p, size);
                                p.updateInterval();
                            } else {
                                IntervalItemPanel p1 = (IntervalItemPanel)o;
                                IntervalItemPanel p2 = 
                                    (IntervalItemPanel) m_intervalMdl
                                        .getElementAt(
                                                m_intervalMdl.indexOf(p1) + 1);
                                double d1 = p1.getRightValue(false);
                                double d2 = p2.getLeftValue(false);
                                IntervalItemPanel p = new IntervalItemPanel(
                                        IntervalPanel.this, d1, d2, "Bin"
                                                + (size + 1), type);
                                m_intervalMdl.insertElementAt(p, m_intervalMdl
                                        .indexOf(p1) + 1);
                                p.updateInterval();
                            }
                        }
                    }
                    parent.validate();
                    parent.repaint();
                }
            });
            final JButton removeButton = new JButton("Remove");
            removeButton.addActionListener(new ActionListener() {
                /**
                 * 
                 */
                public void actionPerformed(final ActionEvent e) {
                    IntervalItemPanel p = (IntervalItemPanel)m_intervalList
                            .getSelectedValue();
                    if (p != null) {
                        int i = m_intervalMdl.indexOf(p);
                        m_intervalMdl.removeElement(p);
                        int size = m_intervalMdl.getSize();
                        if (size > 0) {
                            if (size == 1 || size == i) {
                                m_intervalList.setSelectedIndex(size - 1);
                            } else {
                                m_intervalList.setSelectedIndex(i);
                            }
                            ((IntervalItemPanel)m_intervalList
                                    .getSelectedValue()).updateInterval();
                        }
                        parent.validate();
                        parent.repaint();
                    }
                }
            });
            final JPanel buttonPanel = new JPanel(new GridLayout(1, 2));
            buttonPanel.add(addButton);
            buttonPanel.add(removeButton);
            super.add(buttonPanel, BorderLayout.NORTH);

            //
            // interval list
            //

            final JPanel selInterval = new JPanel(new GridLayout(1, 1));
            selInterval
                    .add(new IntervalItemPanel(this, null, null, null, type));
            selInterval.validate();
            selInterval.repaint();

            m_intervalList
                    .addListSelectionListener(new ListSelectionListener() {
                        /**
                         * 
                         */
                        public void valueChanged(final ListSelectionEvent e) {
                            selInterval.removeAll();
                            Object o = m_intervalList.getSelectedValue();
                            if (o == null) {
                                selInterval.add(new IntervalItemPanel(
                                        IntervalPanel.this, null, null, null,
                                        type));
                            } else {
                                selInterval.add((IntervalItemPanel)o);
                            }
                            m_numInterval.validate();
                            m_numInterval.repaint();
                        }
                    });
            m_intervalList
                    .setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

            final JScrollPane intervalScroll = new JScrollPane(m_intervalList);
            intervalScroll.setMinimumSize(new Dimension(200, 155));
            intervalScroll.setPreferredSize(new Dimension(200, 155));
            super.add(intervalScroll, BorderLayout.CENTER);

            JPanel southPanel = new JPanel(new BorderLayout());
            southPanel.add(selInterval, BorderLayout.CENTER);

            if (appendColumn == null) {
                m_appendName = new JTextField(column.toString() + "_binned");
                m_appendName.setEnabled(false);
                m_appendColumn = new JCheckBox("Append new column", false);
            } else {
                m_appendName = new JTextField(appendColumn);
                m_appendName.setEnabled(true);
                m_appendColumn = new JCheckBox("Append new column", true);
            }
            m_appendColumn.setEnabled(true);
            m_appendColumn.setToolTipText("Check this to append a column "
                    + "instead of replacing the input column.");
            m_appendName.setPreferredSize(new Dimension(150, 20));
            m_appendColumn.addItemListener(new ItemListener() {
                /**
                 * {@inheritDoc}
                 */
                public void itemStateChanged(final ItemEvent e) {
                    if (m_appendColumn.isSelected()) {
                        m_appendName.setEnabled(true);
                    } else {
                        m_appendName.setEnabled(false);
                    }
                    parent.invalidate();
                    parent.repaint();
                }
            });

            JPanel replacedPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
            replacedPanel.add(m_appendColumn);
            replacedPanel.add(m_appendName);
            southPanel.add(replacedPanel, BorderLayout.SOUTH);

            super.add(southPanel, BorderLayout.SOUTH);
        }

        private void addIntervalItem(final IntervalItemPanel item) {
            m_intervalMdl.addElement(item);
            // m_intervalMdl.insertElementAt(item, m_intervalMdl.getSize());
            // item.updateInterval();
        }

        /**
         * @param item the current interval item
         * @return the previous one in the list or <code>null</code>
         */
        private IntervalItemPanel getPrevious(final IntervalItemPanel item) {
            int i = m_intervalMdl.indexOf(item);
            if (i > 0) {
                return (IntervalItemPanel)m_intervalMdl.getElementAt(i - 1);
            }
            return null;
        }

        /**
         * @param item the current interval item
         * @return the next one in the list or <code>null</code>
         */
        private IntervalItemPanel getNext(final IntervalItemPanel item) {
            int i = m_intervalMdl.indexOf(item);
            if (i >= 0 && i + 1 < m_intervalMdl.getSize()) {
                return (IntervalItemPanel)m_intervalMdl.getElementAt(i + 1);
            }
            return null;
        }

        /**
         * @return number of interval specified for binning
         */
        public int getNumIntervals() {
            return m_intervalMdl.getSize();
        }

        /**
         * @param i index for interval
         * @return the interval item
         */
        public IntervalItemPanel getInterval(final int i) {
            return (IntervalItemPanel)m_intervalMdl.get(i);
        }

        /**
         * @return if a new column should be appended
         */
        public boolean isAppendedColumn() {
            return m_appendColumn.isSelected();
        }

        /**
         * @return <code>true</code> if the binned column should be appended,
         *         otherwise the column is replaced
         */
        public String getColumnName() {
            return m_appendName.getText().trim();
        }
    }

    /**
     * Creates a new panel holding one interval.
     */
    final class IntervalItemPanel extends JPanel {
        private final IntervalPanel m_parent;

        private final JComboBox m_borderLeft = new JComboBox();

        private final JSpinner m_left;

        private final JSpinner m_right;

        private final JComboBox m_borderRight = new JComboBox();

        private final JTextField m_bin = new JTextField();

        /** Left/open or right/closed interval bracket. */
        static final String LEFT = "]";

        /** Right/open or left/closed interval bracket. */
        static final String RIGHT = "[";

        /**
         * @param parent the interval item's parent component
         * @param leftOpen initial left open
         * @param left initial left value
         * @param rightOpen initial right open
         * @param right initial right value
         * @param bin the name for this bin
         * @param type the column type of this interval
         */
        IntervalItemPanel(final IntervalPanel parent, final boolean leftOpen,
                final Double left, final boolean rightOpen, final Double right,
                final String bin, final DataType type) {
            this(parent, left, right, bin, type);
            setLeftOpen(leftOpen);
            setRightOpen(rightOpen);
        }

        /**
         * @param parent the interval item's parent component
         * @param left initial left value
         * @param right initial right value
         * @param bin the name for this bin
         * @param type the column type of this interval
         */
        IntervalItemPanel(final IntervalPanel parent, final Double left,
                final Double right, final String bin, final DataType type) {
            this(parent, type);
            if (bin == null) {
                m_bin.setText("");
                m_bin.setEditable(false);
            } else {
                m_bin.setText(bin);
            }
            JPanel p1 = new JPanel(new BorderLayout());
            p1.add(m_bin, BorderLayout.CENTER);
            p1.add(new JLabel(" :  "), BorderLayout.EAST);
            super.add(p1);
            JPanel p2 = new JPanel(new BorderLayout());
            p2.add(m_borderLeft, BorderLayout.WEST);
            p2.add(m_left, BorderLayout.CENTER);
            p2.add(new JLabel(" ."), BorderLayout.EAST);
            setLeftValue(left);
            super.add(p2);
            JPanel p3 = new JPanel(new BorderLayout());
            p3.add(new JLabel(". "), BorderLayout.WEST);
            p3.add(m_right, BorderLayout.CENTER);
            p3.add(m_borderRight, BorderLayout.EAST);
            setRightValue(right);
            super.add(p3);
            initListener();
        }

        /*
         * @param parent the interval item's parent component
         */
        private IntervalItemPanel(final IntervalPanel parent,
                final DataType type) {
            super(new GridLayout(1, 0));
            m_parent = parent;

            m_bin.setPreferredSize(new Dimension(50, 25));

            m_left = new JSpinner(createNumberModel(type));
            JSpinner.DefaultEditor editorLeft = 
                new JSpinner.NumberEditor(m_left, "0.0##############");
            editorLeft.getTextField().setColumns(15);
            m_left.setEditor(editorLeft);
            m_left.setPreferredSize(new Dimension(125, 25));

            m_right = new JSpinner(createNumberModel(type));
            JSpinner.DefaultEditor editorRight = 
                new JSpinner.NumberEditor(m_right, "0.0##############");
            editorRight.getTextField().setColumns(15);
            m_right.setEditor(editorRight);
            m_right.setPreferredSize(new Dimension(125, 25));

            m_borderLeft.setPreferredSize(new Dimension(50, 25));
            m_borderLeft.setLightWeightPopupEnabled(false);
            m_borderLeft.addItem(RIGHT);
            m_borderLeft.addItem(LEFT);

            m_borderRight.setPreferredSize(new Dimension(50, 25));
            m_borderRight.setLightWeightPopupEnabled(false);
            m_borderRight.addItem(LEFT);
            m_borderRight.addItem(RIGHT);
        }

        private void initListener() {
            m_left.addChangeListener(new ChangeListener() {
                public void stateChanged(final ChangeEvent e) {
                    repairLeft();
                }
            });
            final JSpinner.DefaultEditor editorLeft = 
                (JSpinner.DefaultEditor)m_left.getEditor();
            editorLeft.getTextField().addFocusListener(new FocusAdapter() {
                @Override
                public void focusLost(final FocusEvent e) {
                    getLeftValue(true);
                    repairLeft();
                }

                @Override
                public void focusGained(final FocusEvent e) {
                }
            });

            m_right.addChangeListener(new ChangeListener() {
                public void stateChanged(final ChangeEvent e) {
                    repairRight();
                }
            });
            final JSpinner.DefaultEditor editorRight = 
                (JSpinner.DefaultEditor)m_right.getEditor();
            editorRight.getTextField().addFocusListener(new FocusAdapter() {
                @Override
                public void focusLost(final FocusEvent e) {
                    getRightValue(true);
                    repairRight();
                }

                @Override
                public void focusGained(final FocusEvent e) {
                }
            });

            m_borderLeft.addItemListener(new ItemListener() {
                public void itemStateChanged(final ItemEvent e) {
                    IntervalItemPanel prev = m_parent
                            .getPrevious(IntervalItemPanel.this);
                    if (prev != null && prev.isRightOpen() == isLeftOpen()) {
                        prev.setRightOpen(!isLeftOpen());
                    }
                    myRepaint();
                }
            });

            m_borderRight.addItemListener(new ItemListener() {
                public void itemStateChanged(final ItemEvent e) {
                    IntervalItemPanel next = m_parent
                            .getNext(IntervalItemPanel.this);
                    if (next != null && next.isLeftOpen() == isRightOpen()) {
                        next.setLeftOpen(!isRightOpen());
                    }
                    myRepaint();
                }
            });

            m_bin.getDocument().addDocumentListener(new DocumentListener() {
                public void changedUpdate(final DocumentEvent e) {
                    myRepaint();
                }

                public void insertUpdate(final DocumentEvent e) {
                    changedUpdate(e);
                }

                public void removeUpdate(final DocumentEvent e) {
                    changedUpdate(e);
                }
            });

        }

        private void repairLeft() {
            double l = getLeftValue(false);
            double r = getRightValue(true);
            if (l > r) {
                setRightValue(l);
                repairNext(l);
            }
            repairPrev(l);
            myRepaint();
        }

        private void repairRight() {
            double r = getRightValue(false);
            double l = getLeftValue(true);
            if (l > r) {
                repairPrev(r);
                setLeftValue(r);
            }
            repairNext(r);
            myRepaint();

        }

        /**
         * @return the name for this interval bin
         */
        public String getBin() {
            return m_bin.getText().trim();
        }

        /**
         * Checks the current, previous, and next interval for consistency; and
         * updates the intervals if necessary.
         */
        public void updateInterval() {
            IntervalItemPanel prev = m_parent.getPrevious(this);
            IntervalItemPanel next = m_parent.getNext(this);
            if (prev == null && next == null) {
                this.setLeftValue(null);
                this.setRightValue(null);
                this.setLeftOpen(true);
                this.setRightOpen(true);
            } else {
                repairPrev(getLeftValue(true));
                repairNext(getRightValue(true));
            }
            myRepaint();
        }

        private void myRepaint() {
            m_numInterval.validate();
            m_numInterval.repaint();
        }

        private void repairPrev(final double value) {
            IntervalItemPanel prev = m_parent.getPrevious(this);
            if (prev != null) {
                if (prev.getRightValue(false) != value) {
                    prev.setRightValue(value);
                    if (prev.getLeftValue(false) > value) {
                        prev.setLeftValue(value);
                    }
                }
                if (prev.isRightOpen() == isLeftOpen()) {
                    prev.setRightOpen(!isLeftOpen());
                }
            } else {
                setLeftValue(null);
                setLeftOpen(true);
            }
        }

        private void repairNext(final double value) {
            IntervalItemPanel next = m_parent.getNext(this);
            if (next != null) {
                if (next.getLeftValue(false) != value) {
                    next.setLeftValue(value);
                    if (next.getRightValue(false) < value) {
                        next.setRightValue(value);
                    }
                }
                if (next.isLeftOpen() == isRightOpen()) {
                    next.setLeftOpen(!isRightOpen());
                }
            } else {
                setRightValue(null);
                setRightOpen(true);
            }
        }

        /**
         * @param left new left value
         */
        public void setLeftValue(final Double left) {
            if (left == null || left.doubleValue() == NEGATIVE_INFINITY) {
                m_borderLeft.setSelectedItem(LEFT);
                m_borderLeft.setEnabled(false);
                m_left.setValue(NEGATIVE_INFINITY);
                m_left.setEnabled(false);
            } else {
                m_left.setValue(left);
                m_left.setEnabled(true);
                m_borderLeft.setEnabled(true);
            }
        }

        /**
         * @return left value
         * @param commit if the value has to be committed first
         */
        public double getLeftValue(final boolean commit) {
            if (commit) {
                double old = ((Number)m_left.getValue()).doubleValue();
                try {
                    m_left.commitEdit();
                } catch (ParseException pe) {
                    return old;
                }
            }
            return ((Number)m_left.getValue()).doubleValue();

        }

        /**
         * @param left <code>true</code> if the left interval bound is open
         *            otherwise <code>false</code>
         */
        public void setLeftOpen(final boolean left) {
            if (left) {
                m_borderLeft.setSelectedItem(LEFT);
            } else {
                m_borderLeft.setSelectedItem(RIGHT);
            }
        }

        /**
         * @return <code>true</code> if left side open
         */
        public boolean isLeftOpen() {
            return LEFT.equals(m_borderLeft.getSelectedItem());
        }

        /**
         * @param right new right value
         */
        public void setRightValue(final Double right) {
            if (right == null || right.doubleValue() == POSITIVE_INFINITY) {
                m_borderRight.setSelectedItem(RIGHT);
                m_borderRight.setEnabled(false);
                m_right.setValue(POSITIVE_INFINITY);
                m_right.setEnabled(false);

            } else {
                m_right.setValue(right);
                m_right.setEnabled(true);
                m_borderRight.setEnabled(true);
            }
        }

        /**
         * @param right <code>true</code> if the right interval bound is open
         *            otherwise <code>false</code>
         */
        public void setRightOpen(final boolean right) {
            if (right) {
                m_borderRight.setSelectedItem(RIGHT);
            } else {
                m_borderRight.setSelectedItem(LEFT);
            }
        }

        /**
         * @return right value
         * @param commit if the value has to be committed first
         */
        public double getRightValue(final boolean commit) {
            if (commit) {
                double old = ((Number)m_right.getValue()).doubleValue();
                try {
                    m_right.commitEdit();
                } catch (ParseException pe) {
                    return old;
                }
            }
            return ((Number)m_right.getValue()).doubleValue();
        }

        /**
         * @return <code>true</code> if right open
         */
        public boolean isRightOpen() {
            return RIGHT.equals(m_borderRight.getSelectedItem());
        }

        /**
         * @return string containing left and right border, and open/not open
         */
        @Override
        public String toString() {
            double left = getLeftValue(false);
            double right = getRightValue(false);
            String leftString, rightString;
            JComponent editor = m_left.getEditor();
            if (editor instanceof JSpinner.NumberEditor) {
                JSpinner.NumberEditor numEdit = (JSpinner.NumberEditor)editor;
                leftString = numEdit.getFormat().format(left);
                rightString = numEdit.getFormat().format(right);
            } else {
                leftString = Double.toString(left);
                rightString = Double.toString(right);
            }
            String rightBorder = m_borderRight.getSelectedItem().toString();
            String leftBorder = m_borderLeft.getSelectedItem().toString();
            return getBin() + " : " + leftBorder + " " + leftString + " ... "
                    + rightString + " " + rightBorder;
        }

    }

    /**
     * @param settings to read intervals from
     * @param specs The input table spec
     * @see NodeDialogPane#loadSettingsFrom(NodeSettingsRO, DataTableSpec[])
     * @throws NotConfigurableException if the spec contains no columns
     */
    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings,
            final DataTableSpec[] specs) throws NotConfigurableException {
        // numeric columns' settings
        m_intervals.clear();
        m_numMdl.removeAllElements();
        for (int i = 0; i < specs[0].getNumColumns(); i++) {
            DataColumnSpec cspec = specs[0].getColumnSpec(i);
            if (cspec.getType().isCompatible(DoubleValue.class)) {
                m_numMdl.addElement(cspec);
            }
        }
        // no column found for binning
        if (m_numMdl.getSize() == 0) {
            throw new NotConfigurableException(
                    "No column found to define intervals.");
        }
        String[] columns = settings.getStringArray(
                BinnerNodeModel.NUMERIC_COLUMNS, (String[])null);
        // if numeric columns in settings, select first
        if (columns != null && columns.length > 0) {
            for (int i = 0; i < columns.length; i++) {
                if (!specs[0].containsName(columns[i])) {
                    continue;
                }
                NodeSettingsRO col;

                DataType type = specs[0].getColumnSpec(columns[i]).getType();
                try {
                    col = settings.getNodeSettings(columns[i].toString());
                } catch (InvalidSettingsException ise) {
                    LOGGER.warn("NodeSettings not available for column: "
                            + columns[i]);
                    continue;
                }
                String appendedColumn = null;
                if (settings.containsKey(columns[i].toString() 
                        + BinnerNodeModel.IS_APPENDED)) {
                    appendedColumn = settings.getString(columns[i].toString()
                            + BinnerNodeModel.IS_APPENDED, null);
                }
                IntervalPanel p = new IntervalPanel(columns[i], appendedColumn,
                        m_numList, type);
                m_intervals.put(columns[i], p);
                for (String binId : col.keySet()) {
                    NumericBin theBin = null;
                    try {
                        theBin = new NumericBin(col.getNodeSettings(binId));
                    } catch (InvalidSettingsException ise) {
                        LOGGER.warn("NodeSettings not available for "
                                + "interval bin: " + binId);
                        continue;
                    }
                    String binName = theBin.getBinName();
                    boolean leftOpen = theBin.isLeftOpen();
                    double left = theBin.getLeftValue();
                    boolean rightOpen = theBin.isRightOpen();
                    double right = theBin.getRightValue();
                    IntervalItemPanel item = new IntervalItemPanel(p, leftOpen,
                            left, rightOpen, right, binName, type);
                    p.addIntervalItem(item);
                }
                DataColumnSpec cspec = specs[0].getColumnSpec(columns[i]);
                // select column and scroll to position
                m_numList.setSelectedValue(cspec, true);
            }
        }
        getPanel().validate();
        getPanel().repaint();
    }

    /**
     * @param settings write intervals to
     * @throws InvalidSettingsException if a bin name is empty
     * @see NodeDialogPane#saveSettingsTo(NodeSettingsWO)
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings)
            throws InvalidSettingsException {
        LinkedHashSet<String> colList = new LinkedHashSet<String>();
        for (String cell : m_intervals.keySet()) {
            IntervalPanel p = m_intervals.get(cell);
            // only if at least 1 bin is defined
            if (p.getNumIntervals() > 0) {
                colList.add(cell);
                NodeSettingsWO set = settings.addNodeSettings(cell.toString());
                if (p.isAppendedColumn()) {
                    String appendedName = p.getColumnName();
                    Enumeration<?> e = m_numMdl.elements();
                    while (e.hasMoreElements()) {
                        DataColumnSpec cspec = (DataColumnSpec)e.nextElement();
                        if (cspec.getName().equals(appendedName)) {
                            throw new InvalidSettingsException(
                                    "New appended column " + appendedName
                                            + " matches other column.");
                        }
                    }
                    settings.addString(cell.toString() 
                            + BinnerNodeModel.IS_APPENDED, appendedName);
                } else {
                    settings.addString(cell.toString() 
                            + BinnerNodeModel.IS_APPENDED, null);
                }
                for (int j = 0; j < p.getNumIntervals(); j++) {
                    IntervalItemPanel item = p.getInterval(j);
                    String binName = item.getBin();
                    if (binName == null || binName.length() == 0) {
                        throw new InvalidSettingsException("Name for bin " + j
                                + " not set: " + item);
                    }
                    NodeSettingsWO bin = set.addNodeSettings(binName + "_" + j);
                    NumericBin theBin = new NumericBin(binName, item
                            .isLeftOpen(), item.getLeftValue(false), item
                            .isRightOpen(), item.getRightValue(false));
                    theBin.saveToSettings(bin);
                }
            }
        }
        // add binned columns
        String[] columns = colList.toArray(new String[0]);
        settings.addStringArray(BinnerNodeModel.NUMERIC_COLUMNS, columns);
    }
}
