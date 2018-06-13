/*
 * ------------------------------------------------------------------------
 *
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
 *   Jun 04, 2018 (Mor Kalla): created
 */
package org.knime.core.util.binning.numeric;

import static java.lang.Double.NEGATIVE_INFINITY;
import static java.lang.Double.POSITIVE_INFINITY;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.text.ParseException;

import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import org.knime.core.data.DataType;
import org.knime.core.data.def.IntCell;

/**
 * A panel that holding one interval.
 *
 * @author Mor Kalla
 * @since 3.6
 */
public class IntervalItemPanel extends JPanel {

    private static final long serialVersionUID = 8310311102403892850L;

    private static final double DEFAULT_STEP_SIZE = 0.1;

    private static final int SIZE_TEXT_FIELD = 15;

    private static final Dimension DIMENSION_SMALLER_INTERVAL_ITEM = new Dimension(50, 25);

    private static final Dimension DIMENSION_BIGGER_INTERVAL_ITEM = new Dimension(125, 25);

    private final IntervalPanel m_parent;

    private final JComboBox<String> m_borderLeft = new JComboBox<String>();

    private final JSpinner m_left;

    private final JSpinner m_right;

    private final JComboBox<String> m_borderRight = new JComboBox<String>();

    private final JTextField m_bin = new JTextField();

    /**
     * Left/open or right/closed interval bracket.
     */
   static final String LEFT = "]";

    /**
     * Right/open or left/closed interval bracket.
     */
   static final String RIGHT = "[";

    private final JPanel m_numInterval;

    /**
     * Constructs a {@link IntervalItemPanel} with given left right and open values.
     *
     * @param parent the interval item's parent component
     * @param leftOpen initial left open
     * @param left initial left value
     * @param rightOpen initial right open
     * @param right initial right value
     * @param bin the name for this bin
     * @param type the column type of this interval
     * @param numInterval the currently selected interval
     */
    IntervalItemPanel(final IntervalPanel parent, final boolean leftOpen, final Double left, final boolean rightOpen,
        final Double right, final String bin, final DataType type, final JPanel numInterval) {
        this(parent, left, right, bin, type, numInterval);
        setLeftOpen(leftOpen);
        setRightOpen(rightOpen);
    }

    /**
     * Constructs a {@link IntervalItemPanel} with given left right values.
     *
     * @param parent the interval item's parent component
     * @param left initial left value
     * @param right initial right value
     * @param bin the name for this bin
     * @param type the column type of this interval
     * @param numInterval the currently selected interval
     */
    IntervalItemPanel(final IntervalPanel parent, final Double left, final Double right, final String bin,
        final DataType type, final JPanel numInterval) {
        this(parent, type, numInterval);

        if (bin == null) {
            m_bin.setText("");
            m_bin.setEditable(false);
        } else {
            m_bin.setText(bin);
        }

        final JPanel binNamePanel = new JPanel(new BorderLayout());
        binNamePanel.add(m_bin, BorderLayout.CENTER);
        binNamePanel.add(new JLabel(" :  "), BorderLayout.EAST);
        super.add(binNamePanel);

        final JPanel leftIntervalPanel = new JPanel(new BorderLayout());
        leftIntervalPanel.add(m_borderLeft, BorderLayout.WEST);
        leftIntervalPanel.add(m_left, BorderLayout.CENTER);
        leftIntervalPanel.add(new JLabel(" ."), BorderLayout.EAST);
        setLeftValue(left);
        super.add(leftIntervalPanel);

        final JPanel rightIntervalPanel = new JPanel(new BorderLayout());
        rightIntervalPanel.add(new JLabel(". "), BorderLayout.WEST);
        rightIntervalPanel.add(m_right, BorderLayout.CENTER);
        rightIntervalPanel.add(m_borderRight, BorderLayout.EAST);
        setRightValue(right);
        super.add(rightIntervalPanel);

        initListener();
    }

    private IntervalItemPanel(final IntervalPanel parent, final DataType type, final JPanel numInterval) {
        super(new GridLayout(1, 0));
        m_numInterval = numInterval;

        m_parent = parent;

        m_bin.setPreferredSize(DIMENSION_SMALLER_INTERVAL_ITEM);

        m_left = new JSpinner(createNumberModel(type));
        final JSpinner.DefaultEditor editorLeft = new JSpinner.NumberEditor(m_left, "0.0##############");
        editorLeft.getTextField().setColumns(SIZE_TEXT_FIELD);
        m_left.setEditor(editorLeft);
        m_left.setPreferredSize(DIMENSION_BIGGER_INTERVAL_ITEM);

        m_right = new JSpinner(createNumberModel(type));
        final JSpinner.DefaultEditor editorRight = new JSpinner.NumberEditor(m_right, "0.0##############");
        editorRight.getTextField().setColumns(SIZE_TEXT_FIELD);
        m_right.setEditor(editorRight);
        m_right.setPreferredSize(DIMENSION_BIGGER_INTERVAL_ITEM);

        m_borderLeft.setPreferredSize(DIMENSION_SMALLER_INTERVAL_ITEM);
        m_borderLeft.setLightWeightPopupEnabled(false);
        m_borderLeft.addItem(RIGHT);
        m_borderLeft.addItem(LEFT);

        m_borderRight.setPreferredSize(DIMENSION_SMALLER_INTERVAL_ITEM);
        m_borderRight.setLightWeightPopupEnabled(false);
        m_borderRight.addItem(LEFT);
        m_borderRight.addItem(RIGHT);
    }

    private static SpinnerNumberModel createNumberModel(final DataType type) {
        if (IntCell.TYPE.equals(type)) {
            return new SpinnerNumberModel(0, null, null, 1);
        }
        return new SpinnerNumberModel(0.0, NEGATIVE_INFINITY, POSITIVE_INFINITY, DEFAULT_STEP_SIZE);
    }

    private void initListener() {
        m_left.addChangeListener(e -> repairLeft());
        final JSpinner.DefaultEditor editorLeft = (JSpinner.DefaultEditor)m_left.getEditor();
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

        m_right.addChangeListener(e -> repairRight());
        final JSpinner.DefaultEditor editorRight = (JSpinner.DefaultEditor)m_right.getEditor();
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

        m_borderLeft.addItemListener(e -> {
            final IntervalItemPanel prev = m_parent.getPrevious(IntervalItemPanel.this);
            if (prev != null && prev.isRightOpen() == isLeftOpen()) {
                prev.setRightOpen(!isLeftOpen());
            }
            myRepaint();
        });

        m_borderRight.addItemListener(e -> {
            final IntervalItemPanel next = m_parent.getNext(IntervalItemPanel.this);
            if (next != null && next.isLeftOpen() == isRightOpen()) {
                next.setLeftOpen(!isRightOpen());
            }
            myRepaint();
        });

        m_bin.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void changedUpdate(final DocumentEvent e) {
                myRepaint();
            }

            @Override
            public void insertUpdate(final DocumentEvent e) {
                changedUpdate(e);
            }

            @Override
            public void removeUpdate(final DocumentEvent e) {
                changedUpdate(e);
            }
        });

    }

    private void repairLeft() {
        final double left = getLeftValue(false);
        final double right = getRightValue(true);
        if (left > right) {
            setRightValue(left);
            repairNext(left);
        }
        repairPrev(left);
        myRepaint();
    }

    private void repairRight() {
        final double right = getRightValue(false);
        final double left = getLeftValue(true);
        if (left > right) {
            repairPrev(right);
            setLeftValue(right);
        }
        repairNext(right);
        myRepaint();

    }

    /**
     * Gets the name for this interval bin.
     *
     * @return the name for this interval bin
     */
    public String getBin() {
        return m_bin.getText().trim();
    }

    /**
     * Checks the current, previous, and next interval for consistency; and updates the intervals if necessary.
     */
    public void updateInterval() {
        final IntervalItemPanel prev = m_parent.getPrevious(this);
        final IntervalItemPanel next = m_parent.getNext(this);
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
        final IntervalItemPanel prev = m_parent.getPrevious(this);
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
        final IntervalItemPanel next = m_parent.getNext(this);
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
     * Sets the left value.
     *
     * @param left the value to set
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
     * Gets the left value.
     *
     * @param commit if the value has to be committed first
     * @return the left value
     */
    public double getLeftValue(final boolean commit) {
        if (commit) {
            final double old = ((Number)m_left.getValue()).doubleValue();
            try {
                m_left.commitEdit();
            } catch (ParseException pe) {
                return old;
            }
        }
        return ((Number)m_left.getValue()).doubleValue();

    }

    /**
     * Sets the left open.
     *
     * @param left {@code true} if the left interval bound is open otherwise {@code false}
     */
    public void setLeftOpen(final boolean left) {
        m_borderLeft.setSelectedItem(left ? LEFT : RIGHT);
    }

    /**
     * Gets if the left side is open.
     *
     * @return {@code true} if left side open
     */
    public boolean isLeftOpen() {
        return LEFT.equals(m_borderLeft.getSelectedItem());
    }

    /**
     * Sets the right value.
     *
     * @param right the value to set
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
     * Sets the right open.
     *
     * @param right {@code true} if the right interval bound is open otherwise {@code false}
     */
    public void setRightOpen(final boolean right) {
        m_borderRight.setSelectedItem(right ? RIGHT : LEFT);
    }

    /**
     * Gets the right value.
     *
     * @param commit if the value has to be committed first
     * @return right value
     */
    public double getRightValue(final boolean commit) {
        if (commit) {
            final double old = ((Number)m_right.getValue()).doubleValue();
            try {
                m_right.commitEdit();
            } catch (ParseException pe) {
                return old;
            }
        }
        return ((Number)m_right.getValue()).doubleValue();
    }

    /**
     * Gets if the right side is open.
     *
     * @return {@code true} if right open
     */
    public boolean isRightOpen() {
        return RIGHT.equals(m_borderRight.getSelectedItem());
    }

    @Override
    public String toString() {
        final double left = getLeftValue(false);
        final double right = getRightValue(false);
        final String leftString, rightString;
        final JComponent editor = m_left.getEditor();
        if (editor instanceof JSpinner.NumberEditor) {
            final JSpinner.NumberEditor numEdit = (JSpinner.NumberEditor)editor;
            leftString = numEdit.getFormat().format(left);
            rightString = numEdit.getFormat().format(right);
        } else {
            leftString = Double.toString(left);
            rightString = Double.toString(right);
        }
        final String rightBorder = m_borderRight.getSelectedItem().toString();
        final String leftBorder = m_borderLeft.getSelectedItem().toString();
        return getBin() + " : " + leftBorder + " " + leftString + " ... " + rightString + " " + rightBorder;
    }

}
