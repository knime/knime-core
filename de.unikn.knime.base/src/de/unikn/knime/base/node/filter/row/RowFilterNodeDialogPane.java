/* @(#)$RCSfile$ 
 * $Revision$ $Date$ $Author$
 * 
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 * 
 * Copyright, 2003 - 2006
 * Universitaet Konstanz, Germany.
 * Lehrstuhl fuer Angewandte Informatik
 * Prof. Dr. Michael R. Berthold
 * 
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner.
 * -------------------------------------------------------------------
 * 
 * History
 *   07.07.2005 (ohl): created
 */
package de.unikn.knime.base.node.filter.row;

import java.awt.Dimension;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import de.unikn.knime.base.node.filter.row.rowfilter.AndRowFilter;
import de.unikn.knime.base.node.filter.row.rowfilter.ColValRowFilter;
import de.unikn.knime.base.node.filter.row.rowfilter.RowFilter;
import de.unikn.knime.base.node.filter.row.rowfilter.RowFilterFactory;
import de.unikn.knime.base.node.filter.row.rowfilter.RowIDRowFilter;
import de.unikn.knime.base.node.filter.row.rowfilter.RowNoRowFilter;
import de.unikn.knime.base.node.filter.row.rowfilter.TrueRowFilter;
import de.unikn.knime.core.data.DataTableSpec;
import de.unikn.knime.core.node.InvalidSettingsException;
import de.unikn.knime.core.node.NodeDialogPane;
import de.unikn.knime.core.node.NodeSettings;

/**
 * 
 * @author ohl, University of Konstanz
 */
public class RowFilterNodeDialogPane extends NodeDialogPane {

    private JComboBox m_rangeBox;

    private JComboBox m_rowIDBox;

    private JComboBox m_colValueBox;

    private RowNoRowFilterPanel m_rangePanel;

    private RowIDRowFilterPanel m_rowIDPanel;

    private ColumnRowFilterPanel m_rowColPanel;

    private JPanel m_frontPanel;

    /**
     * creates a new panel for the row filter node dialog.
     */
    public RowFilterNodeDialogPane() {
        super("Row filter settings");
        // the actual component instantiation is happening during loadSettings.
    }

    private JPanel createFrontPanel() {
        JPanel result = new JPanel();
        result.setLayout(new BoxLayout(result, BoxLayout.Y_AXIS));

        Box box;

        box = Box.createHorizontalBox();
        result.add(Box.createVerticalStrut(7));
        box.add(Box.createHorizontalStrut(5));
        box.add(new JLabel("The result should contain only rows"));
        box.add(Box.createHorizontalGlue());
        result.add(box);
        result.add(Box.createVerticalStrut(7));

        box = Box.createHorizontalBox();
        m_rangeBox = new JComboBox(new String[] {"from the entire table",
                "from within a certain row number range",
                "NOT from within a certain row number range"});
        m_rangeBox.addItemListener(new ItemListener() {
            public void itemStateChanged(final ItemEvent e) {
                selectionChanged();
            }
        });
        m_rangeBox.setPreferredSize(new Dimension(300, 25));
        m_rangeBox.setMaximumSize(new Dimension(300, 25));
        m_rangeBox.setMinimumSize(new Dimension(300, 25));
        box.add(Box.createHorizontalStrut(8));
        box.add(m_rangeBox);
        box.add(Box.createHorizontalGlue());
        result.add(box);
        result.add(Box.createVerticalStrut(7));

        box = Box.createHorizontalBox();
        box.add(Box.createHorizontalStrut(5));
        box.add(new JLabel("and"));
        box.add(Box.createHorizontalGlue());
        result.add(box);
        result.add(Box.createVerticalStrut(3));

        box = Box.createHorizontalBox();
        m_rowIDBox = new JComboBox(new String[] {"with any row ID",
                "whose row ID match a certain regular expression",
                "whose row ID DOESN'T match a certain reg. expression"});
        m_rowIDBox.addItemListener(new ItemListener() {
            public void itemStateChanged(final ItemEvent e) {
                selectionChanged();
            }
        });
        m_rowIDBox.setPreferredSize(new Dimension(300, 25));
        m_rowIDBox.setMaximumSize(new Dimension(300, 25));
        m_rowIDBox.setMinimumSize(new Dimension(300, 25));
        box.add(Box.createHorizontalStrut(8));
        box.add(m_rowIDBox);
        box.add(Box.createHorizontalGlue());
        result.add(box);
        result.add(Box.createVerticalStrut(7));

        box = Box.createHorizontalBox();
        box.add(Box.createHorizontalStrut(8));
        box.add(new JLabel("and that"));
        box.add(Box.createHorizontalGlue());
        result.add(box);
        result.add(Box.createVerticalStrut(3));

        box = Box.createHorizontalBox();
        m_colValueBox = new JComboBox(new String[] {
                "contain any value in any column",
                "contain certain values in a selected column",
                "DON'T contain certain values in a sel. column"});
        m_colValueBox.addItemListener(new ItemListener() {
            public void itemStateChanged(final ItemEvent e) {
                selectionChanged();
            }
        });
        m_colValueBox.setPreferredSize(new Dimension(300, 25));
        m_colValueBox.setMaximumSize(new Dimension(300, 25));
        m_colValueBox.setMinimumSize(new Dimension(300, 25));
        box.add(Box.createHorizontalStrut(8));
        box.add(m_colValueBox);
        box.add(Box.createHorizontalGlue());
        result.add(box);
        result.add(Box.createVerticalStrut(7));

        result.add(Box.createVerticalGlue());

        return result;
    }

    /**
     * called when one of the selection boxes changes.
     */
    protected void selectionChanged() {
        m_rangePanel.setComponentsVisible(m_rangeBox.getSelectedIndex() > 0);
        m_rowIDPanel.setComponentsVisible(m_rowIDBox.getSelectedIndex() > 0);
        m_rowColPanel.
                setComponentsVisible(m_colValueBox.getSelectedIndex() > 0);

    }

    /**
     * @see de.unikn.knime.core.node.NodeDialogPane
     *      #loadSettingsFrom(de.unikn.knime.core.node.NodeSettings,
     *      de.unikn.knime.core.data.DataTableSpec[])
     */
    protected void loadSettingsFrom(final NodeSettings settings,
            final DataTableSpec[] specs) {

        m_frontPanel = createFrontPanel();
        m_rangePanel = new RowNoRowFilterPanel();
        m_rowIDPanel = new RowIDRowFilterPanel();
        m_rowColPanel = new ColumnRowFilterPanel(specs[0]);

        m_rangeBox.setSelectedIndex(0); // set them all back to include all
        m_rowIDBox.setSelectedIndex(0);
        m_colValueBox.setSelectedIndex(0);

        // default in the front panel is no filtering: disable them all
        m_rangePanel.setComponentsVisible(false);
        m_rowIDPanel.setComponentsVisible(false);
        m_rowColPanel.setComponentsVisible(false);

        removeTab("Criteria"); // remove any tabs from previous openDialog
        addTab("Criteria", m_frontPanel);
        removeTab("by row number");
        addTab("by row number", m_rangePanel);
        removeTab("by row ID");
        addTab("by row ID", m_rowIDPanel);
        removeTab("by column value");
        addTab("by column value", m_rowColPanel);

        /*
         * now read the filters. We support three different filters:
         * RowIDFilter, ColValfilter, and RowNumberFilter. Or any subset of them
         * as long as they are ANDed. Thus we have to be able to read in up to
         * two ANDfilters and forward their input filters to the according
         * panel.
         */
        RowFilter f1 = null;
        RowFilter f2 = null;
        RowFilter f3 = null;
        try {
            // get THE filter
            f1 = RowFilterFactory.createRowFilter(settings.
                    getConfig(RowFilterNodeModel.CFGFILTER));
            // if its an AND filter - get its inputs
            if (f1 instanceof AndRowFilter) {
                f2 = ((AndRowFilter)f1).getInput2();
                f1 = ((AndRowFilter)f1).getInput1();
                // if one of them is still an AND filter - get these inputs too
                if (f1 instanceof AndRowFilter) {
                    f3 = ((AndRowFilter)f1).getInput2();
                    f1 = ((AndRowFilter)f1).getInput1();
                } else if (f2 instanceof AndRowFilter) {
                    f3 = ((AndRowFilter)f2).getInput2();
                    f2 = ((AndRowFilter)f2).getInput1();
                }
            }

            if (!(f1 instanceof TrueRowFilter)
                    && !(f1 instanceof RowIDRowFilter)
                    && !(f1 instanceof RowNoRowFilter)
                    && !(f1 instanceof ColValRowFilter)) {
                // invalid filter in config - silently fall back to default
                return;
            }
            if ((f2 != null) && !(f2 instanceof RowIDRowFilter)
                    && !(f2 instanceof RowNoRowFilter)
                    && !(f2 instanceof ColValRowFilter)) {
                // invalid filter in config - silently fall back to default
                return;
            }
            if ((f3 != null) && !(f3 instanceof RowIDRowFilter)
                    && !(f3 instanceof RowNoRowFilter)
                    && !(f3 instanceof ColValRowFilter)) {
                // invalid filter in config - silently fall back to default
                return;
            }
        } catch (InvalidSettingsException ise) {
            // leave the default settings in the panels
            return;
        }

        if (f1 instanceof TrueRowFilter) {
            return;
        }
        applyFilterToPanel(f1);
        applyFilterToPanel(f2);
        applyFilterToPanel(f3);

        selectionChanged();
    }

    private void applyFilterToPanel(final RowFilter f) {
        if (f == null) {
            return;
        }
        if (f instanceof ColValRowFilter) {
            ColValRowFilter filter = (ColValRowFilter)f;
            if (filter.includeMatchingLines()) {
                m_colValueBox.setSelectedIndex(1);
            } else {
                m_colValueBox.setSelectedIndex(2);
            }
            try {
                m_rowColPanel.loadSettingsFromFilter(filter);
            } catch (InvalidSettingsException ise) {
                // should not happen
            }
            return;
        }

        if (f instanceof RowIDRowFilter) {
            RowIDRowFilter filter = (RowIDRowFilter)f;
            if (filter.getInclude()) {
                m_rowIDBox.setSelectedIndex(1);
            } else {
                m_rowIDBox.setSelectedIndex(2);
            }
            try {
                m_rowIDPanel.loadSettingsFromFilter(filter);
            } catch (InvalidSettingsException ise) {
                // should not happen
            }
            return;
        }

        if (f instanceof RowNoRowFilter) {
            RowNoRowFilter filter = (RowNoRowFilter)f;
            if (filter.getInclude()) {
                m_rangeBox.setSelectedIndex(1);
            } else {
                m_rangeBox.setSelectedIndex(2);
            }
            try {
                m_rangePanel.loadSettingsFromFilter(filter);
            } catch (InvalidSettingsException ise) {
                // shouldn't happen.
            }
            return;
        }

    }

    /**
     * @see de.unikn.knime.core.node.NodeDialogPane
     *      #saveSettingsTo(de.unikn.knime.core.node.NodeSettings)
     */
    protected void saveSettingsTo(final NodeSettings settings)
            throws InvalidSettingsException {
        int rangeIdx = m_rangeBox.getSelectedIndex();
        int colValIdx = m_colValueBox.getSelectedIndex();
        int rowIDIdx = m_rowIDBox.getSelectedIndex();

        if ((rangeIdx < 0) || (colValIdx < 0) || (rowIDIdx < 0)) {
            throw new InvalidSettingsException("Make a valid selection in the"
                    + " first tab.");
        }
        if ((rangeIdx > 2) || (colValIdx > 2) || (rowIDIdx > 2)) {
            throw new InvalidSettingsException("Please select one of the first"
                    + "three choises in the boxes only (looks like an internal"
                    + " error from here...)");
        }

        RowFilter rowNoFilter = null;
        RowFilter rowIDFilter = null;
        RowFilter colValFilter = null;
        int filterCount = 0;

        if (rangeIdx > 0) {
            boolean include = (rangeIdx != 2);
            try {
                rowNoFilter = m_rangePanel.createFilter(include);
            } catch (InvalidSettingsException ise) {
                throw new InvalidSettingsException("Row Range: "
                        + ise.getMessage());
            }
            filterCount++;
        }
        if (rowIDIdx > 0) {
            boolean include = (rowIDIdx != 2);
            try {
                rowIDFilter = m_rowIDPanel.createFilter(include);
            } catch (InvalidSettingsException ise) {
                throw new InvalidSettingsException("Row ID pattern: "
                        + ise.getMessage());
            }
            filterCount++;
        }
        if (colValIdx > 0) {
            boolean include = (colValIdx != 2);
            try {
                colValFilter = m_rowColPanel.createFilter(include);
            } catch (InvalidSettingsException ise) {
                throw new InvalidSettingsException("Column Value: "
                        + ise.getMessage());
            }
            filterCount++;
        }

        // now combine them (by ANDing multiples) to one filter:
        RowFilter theFilter;
        switch (filterCount) {
        case 0:
            // if we have no filter we need to include all rows
            theFilter = new TrueRowFilter();
            break;
        case 1:
            // exactly one of the should be NOT null
            if (rowNoFilter != null) {
                theFilter = rowNoFilter;
                break;
            }
            if (rowIDFilter != null) {
                theFilter = rowIDFilter;
                break;
            }
            if (colValFilter != null) {
                theFilter = colValFilter;
                break;
            }
            throw new InvalidSettingsException(
                    "Internal error. Try changing some"
                            + " settings or press cancel. Sorry.");
        case 2:
            // exactly one of the should BE null
            if (rowNoFilter == null) {
                // AND together the two others
                theFilter = new AndRowFilter(rowIDFilter, colValFilter);
                break;
            }
            if (rowIDFilter == null) {
                // AND together the two others
                theFilter = new AndRowFilter(rowNoFilter, colValFilter);
                break;
            }
            if (colValFilter == null) {
                // AND together the two others
                theFilter = new AndRowFilter(rowNoFilter, rowIDFilter);
                break;
            }
            throw new InvalidSettingsException(
                    "Invalid error. Try changing some"
                            + " settings or press cancel. Sorry.");
        case 3:
            // put them all together with ANDs.
            theFilter = new AndRowFilter(colValFilter, new AndRowFilter(
                    rowNoFilter, rowIDFilter));
            break;
        default:
            throw new InvalidSettingsException(
                    "Invalid error. Try changing some"
                            + " settings or press cancel. Sorry.");
        }

        theFilter.saveSettingsTo(settings.
                addConfig(RowFilterNodeModel.CFGFILTER));

    }
}
