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

import static java.lang.Double.POSITIVE_INFINITY;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionListener;

import org.knime.core.data.DataType;

/**
 * A panel that holding one bin column.
 *
 * @author Mor Kalla
 */
public class IntervalPanel extends JPanel {

    private static final long serialVersionUID = 3606933345831183776L;

    private static final Dimension DIMENSION_NUMERIC_PANEL = new Dimension(200, 155);

    private static final Dimension DIMENSION_NAME_TEXTFIELD = new Dimension(150, 20);

    private final JList<IntervalItemPanel> m_intervalPanelList;

    private final DefaultListModel<IntervalItemPanel> m_intervalPanelModel;

    private final JCheckBox m_appendColumn;

    private final JTextField m_appendName;

    private static final int DEFAULT_FONT_SIZE = 12;

    /**
     * Constructs a {@link IntervalPanel} object.
     *
     * @param column the current column name
     * @param appendColumn if a new binned column is append, otherwise the column is replaced
     * @param parent used to refresh column list is number of bins has changed
     * @param type the type for the spinner model
     * @param numericIntervalsPanel the numeric intervals panel
     *
     */
    IntervalPanel(final String column, final String appendColumn, final Component parent, final DataType type,
        final JPanel numericIntervalsPanel) {
        super(new BorderLayout());

        setBorder(BorderFactory.createTitledBorder(" " + column + " "));
        m_intervalPanelModel = new DefaultListModel<IntervalItemPanel>();
        m_intervalPanelList = new JList<IntervalItemPanel>(m_intervalPanelModel);
        final Font font = new Font("Monospaced", Font.PLAIN, DEFAULT_FONT_SIZE);
        m_intervalPanelList.setFont(font);

        final JButton addButton = new JButton("Add");
        addButton.addActionListener(getAddButtonAction(parent, type, numericIntervalsPanel));

        final JButton removeButton = new JButton("Remove");
        removeButton.addActionListener(getRemoveAction(parent));

        final JPanel buttonPanel = new JPanel(new GridLayout(1, 2));
        buttonPanel.add(addButton);
        buttonPanel.add(removeButton);
        super.add(buttonPanel, BorderLayout.NORTH);

        final JPanel selInterval = new JPanel(new GridLayout(1, 1));
        selInterval.add(new IntervalItemPanel(this, null, null, null, type, numericIntervalsPanel));
        selInterval.validate();
        selInterval.repaint();

        m_intervalPanelList.addListSelectionListener(getSelectionListener(type, numericIntervalsPanel, selInterval));
        m_intervalPanelList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        final JScrollPane intervalScroll = new JScrollPane(m_intervalPanelList);
        intervalScroll.setMinimumSize(DIMENSION_NUMERIC_PANEL);
        intervalScroll.setPreferredSize(DIMENSION_NUMERIC_PANEL);
        super.add(intervalScroll, BorderLayout.CENTER);

        final JPanel southPanel = new JPanel(new BorderLayout());
        southPanel.add(selInterval, BorderLayout.CENTER);

        final boolean isAppendColumn = appendColumn != null;
        m_appendName = new JTextField(isAppendColumn ? appendColumn : column + "_binned");
        m_appendName.setEnabled(isAppendColumn);
        m_appendColumn = new JCheckBox("Append new column", isAppendColumn);
        m_appendColumn.setEnabled(true);
        m_appendColumn.setToolTipText("Check this to append a column " + "instead of replacing the input column.");
        m_appendName.setPreferredSize(DIMENSION_NAME_TEXTFIELD);
        m_appendColumn.addItemListener(e -> {
            m_appendName.setEnabled(m_appendColumn.isSelected());
            parent.invalidate();
            parent.repaint();
        });

        final JPanel replacedPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        replacedPanel.add(m_appendColumn);
        replacedPanel.add(m_appendName);
        southPanel.add(replacedPanel, BorderLayout.SOUTH);

        super.add(southPanel, BorderLayout.SOUTH);
    }

    private ListSelectionListener getSelectionListener(final DataType type, final JPanel numericIntervalsPanel,
        final JPanel selInterval) {
        return e -> {
            selInterval.removeAll();
            final IntervalItemPanel intervalItemPanel = m_intervalPanelList.getSelectedValue();
            if (intervalItemPanel == null) {
                selInterval
                    .add(new IntervalItemPanel(IntervalPanel.this, null, null, null, type, numericIntervalsPanel));
            } else {
                selInterval.add(intervalItemPanel);
            }
            numericIntervalsPanel.validate();
            numericIntervalsPanel.repaint();
        };
    }

    private ActionListener getAddButtonAction(final Component parent, final DataType type,
        final JPanel numericIntervalsPanel) {
        return e -> {
            final int size = m_intervalPanelModel.getSize();
            // if the first interval is added
            if (size == 0) {
                m_intervalPanelModel.addElement(getTheFirstInterval(type, numericIntervalsPanel));
            } else {
                // if the first interval needs to be split
                if (size == 1) {
                    final IntervalItemPanel secondIntervalPanel =
                        new IntervalItemPanel(IntervalPanel.this, 0.0, null, "Bin2", type, numericIntervalsPanel);
                    m_intervalPanelModel.addElement(secondIntervalPanel);
                    secondIntervalPanel.updateInterval();
                } else {
                    final IntervalItemPanel selectedIntervalPanel = m_intervalPanelList.getSelectedValue();

                    // if non is selected or the last one is selected
                    if (selectedIntervalPanel == null
                        || m_intervalPanelModel.indexOf(selectedIntervalPanel) == size - 1) {
                        final IntervalItemPanel lastIntervalPanel = m_intervalPanelModel.getElementAt(size - 1);
                        final double left = lastIntervalPanel.getLeftValue(false);
                        final IntervalItemPanel positiveInfinityPanel = new IntervalItemPanel(IntervalPanel.this, left,
                            POSITIVE_INFINITY, "Bin" + (size + 1), type, numericIntervalsPanel);
                        m_intervalPanelModel.insertElementAt(positiveInfinityPanel, size);
                        positiveInfinityPanel.updateInterval();
                    } else {
                        //final IntervalItemPanel selectedIntervalPanel = intervalItemPanel;

                        final IntervalItemPanel afterSelectedIntervalPanel =
                            m_intervalPanelModel.getElementAt(m_intervalPanelModel.indexOf(selectedIntervalPanel) + 1);
                        final double right = selectedIntervalPanel.getRightValue(false);
                        final double left = afterSelectedIntervalPanel.getLeftValue(false);
                        final IntervalItemPanel p5 = new IntervalItemPanel(IntervalPanel.this, right, left,
                            "Bin" + (size + 1), type, numericIntervalsPanel);
                        m_intervalPanelModel.insertElementAt(p5,
                            m_intervalPanelModel.indexOf(selectedIntervalPanel) + 1);
                        p5.updateInterval();
                    }
                }
            }
            parent.validate();
            parent.repaint();
        };
    }

    private ActionListener getRemoveAction(final Component parent) {
        return e -> {
            final IntervalItemPanel intervalItemPanel = m_intervalPanelList.getSelectedValue();
            if (intervalItemPanel != null) {
                int i = m_intervalPanelModel.indexOf(intervalItemPanel);
                m_intervalPanelModel.removeElement(intervalItemPanel);
                final int size = m_intervalPanelModel.getSize();
                if (size > 0) {
                    if (size == 1 || size == i) {
                        m_intervalPanelList.setSelectedIndex(size - 1);
                    } else {
                        m_intervalPanelList.setSelectedIndex(i);
                    }
                    m_intervalPanelList.getSelectedValue().updateInterval();
                }
                parent.validate();
                parent.repaint();
            }
        };
    }

    private IntervalItemPanel getTheFirstInterval(final DataType type, final JPanel numericIntervalsPanel) {
        return new IntervalItemPanel(IntervalPanel.this, null, null, "Bin1", type, numericIntervalsPanel);
    }

    /**
     * Adds an interval item.
     *
     * @param item the item to add
     */
    void addIntervalItem(final IntervalItemPanel item) {
        m_intervalPanelModel.addElement(item);
    }

    /**
     * Gets the previous interval item panel from the list.
     *
     * @param item the current interval item
     * @return the previous {@link IntervalItemPanel} from the list or {@code null}
     */
    IntervalItemPanel getPrevious(final IntervalItemPanel item) {
        int i = m_intervalPanelModel.indexOf(item);
        if (i > 0) {
            return m_intervalPanelModel.getElementAt(i - 1);
        }
        return null;
    }

    /**
     * Gets the next interval item panel from the list.
     *
     * @param item the current interval item
     * @return the next {@link IntervalItemPanel} from the list or {@code null}
     */
    IntervalItemPanel getNext(final IntervalItemPanel item) {
        int i = m_intervalPanelModel.indexOf(item);
        if (i >= 0 && i + 1 < m_intervalPanelModel.getSize()) {
            return m_intervalPanelModel.getElementAt(i + 1);
        }
        return null;
    }

    /**
     * Gets the size of intervals.
     *
     * @return number of interval specified for binning
     */
    public int getNumericIntervalSize() {
        return m_intervalPanelModel.getSize();
    }

    /**
     * Gets the panel that contains the selected interval.
     *
     * @param i index for interval
     * @return the {@link IntervalItemPanel} item
     */
    public IntervalItemPanel getInterval(final int i) {
        return m_intervalPanelModel.get(i);
    }

    /**
     * Gets whether a new column should be appended or not.
     *
     * @return {@code true} a new column should be appended
     */
    public boolean isAppendedColumn() {
        return m_appendColumn.isSelected();
    }

    /**
     * Gets the appended column name.
     *
     * @return the appended column name
     */
    public String getColumnName() {
        return m_appendName.getText().trim();
    }
}
