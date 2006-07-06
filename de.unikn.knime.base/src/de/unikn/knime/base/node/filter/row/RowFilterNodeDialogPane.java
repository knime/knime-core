/* -------------------------------------------------------------------
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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JPanel;
import javax.swing.JRadioButton;

import de.unikn.knime.base.node.filter.row.rowfilter.ColValRowFilter;
import de.unikn.knime.base.node.filter.row.rowfilter.RowFilter;
import de.unikn.knime.base.node.filter.row.rowfilter.RowFilterFactory;
import de.unikn.knime.base.node.filter.row.rowfilter.RowIDRowFilter;
import de.unikn.knime.base.node.filter.row.rowfilter.RowNoRowFilter;
import de.unikn.knime.core.data.DataTableSpec;
import de.unikn.knime.core.node.InvalidSettingsException;
import de.unikn.knime.core.node.NodeDialogPane;
import de.unikn.knime.core.node.NodeSettings;
import de.unikn.knime.core.node.NotConfigurableException;

/**
 * 
 * @author ohl, University of Konstanz
 */
public class RowFilterNodeDialogPane extends NodeDialogPane {

    // private JComboBox m_rangeBox;
    //
    // private JComboBox m_rowIDBox;
    //
    // private JComboBox m_colValueBox;

    private JRadioButton m_rangeInclRadio;

    private JRadioButton m_rangeExclRadio;

    private JRadioButton m_rowIDInclRadio;

    private JRadioButton m_rowIDExclRadio;

    private JRadioButton m_colValInclRadio;

    private JRadioButton m_colValExclRadio;

    private RowNoRowFilterPanel m_rangePanel;

    private RowIDRowFilterPanel m_rowIDPanel;

    private ColumnRowFilterPanel m_colValPanel;

    private JPanel m_filterPanel;

    /**
     * creates a new panel for the row filter node dialog.
     */
    public RowFilterNodeDialogPane() {
        super("Row filter settings");
        JPanel dlg = createDialogPanel();
        // the actual filter panel instantiations happen during loadSettings.
        addTab("Filter Criteria", dlg);
    }

    private JPanel createDialogPanel() {

        JPanel result = new JPanel();
        result.setLayout(new BoxLayout(result, BoxLayout.X_AXIS));

        // the panel on the left side for the filter selection
        Box selectionBox = Box.createVerticalBox();
        selectionBox.setMaximumSize(new Dimension(300, 500));
        m_rangeInclRadio = new JRadioButton("include rows by number");
        m_rangeExclRadio = new JRadioButton("exclude rows by number");
        m_rangeInclRadio.setActionCommand("range");
        m_rangeExclRadio.setActionCommand("range");
        addActionListener(m_rangeInclRadio);
        addActionListener(m_rangeExclRadio);
        m_rowIDInclRadio = new JRadioButton("include rows by row ID");
        m_rowIDExclRadio = new JRadioButton("exclude rows by row ID");
        m_rowIDInclRadio.setActionCommand("id");
        m_rowIDExclRadio.setActionCommand("id");
        addActionListener(m_rowIDInclRadio);
        addActionListener(m_rowIDExclRadio);
        m_colValInclRadio = new JRadioButton("include rows by attribute value");
        m_colValExclRadio = new JRadioButton("exclude rows by attribute value");
        m_colValInclRadio.setActionCommand("colval");
        m_colValExclRadio.setActionCommand("colval");
        addActionListener(m_colValInclRadio);
        addActionListener(m_colValExclRadio);

        // only one at a time should be selected
        ButtonGroup group = new ButtonGroup();
        group.add(m_rangeInclRadio);
        group.add(m_rangeExclRadio);
        group.add(m_rowIDInclRadio);
        group.add(m_rowIDExclRadio);
        group.add(m_colValInclRadio);
        group.add(m_colValExclRadio);
        selectionBox.add(Box.createVerticalGlue());
        selectionBox.add(m_colValInclRadio);
        selectionBox.add(m_colValExclRadio);
        selectionBox.add(Box.createVerticalStrut(3));
        selectionBox.add(m_rangeInclRadio);
        selectionBox.add(m_rangeExclRadio);
        selectionBox.add(Box.createVerticalStrut(3));
        selectionBox.add(m_rowIDInclRadio);
        selectionBox.add(m_rowIDExclRadio);
        selectionBox.add(Box.createVerticalGlue());

        // the panel containing the filter panels. They will be instantiated
        // in loadSettings
        m_filterPanel = new JPanel();
        m_filterPanel.setLayout(new BoxLayout(m_filterPanel, BoxLayout.Y_AXIS));
        m_filterPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory
                .createEtchedBorder(), "Set filter parameter:"));
        m_filterPanel.setMaximumSize(new Dimension(450, 500));
        result.add(selectionBox);
        result.add(Box.createHorizontalStrut(7));
        result.add(m_filterPanel);

        return result;
    }

    private void addActionListener(final JRadioButton rb) {
        rb.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                filterSelectionChanged(e.getActionCommand());
            }
        });
    }

    /**
     * activates the corresponding panel. Called by the radio button listeners.
     * 
     * @param activeFilterMethod the active filter method
     */
    protected void filterSelectionChanged(final String activeFilterMethod) {

        // remove the old filter panels
        m_filterPanel.remove(m_rangePanel);
        m_filterPanel.remove(m_rowIDPanel);
        m_filterPanel.remove(m_colValPanel);
        
        if (activeFilterMethod.equals("id")) {
            m_filterPanel.add(m_rowIDPanel);
        }
        if (activeFilterMethod.equals("range")) {
            m_filterPanel.add(m_rangePanel);
        }
        if (activeFilterMethod.equals("colval")) {
            m_filterPanel.add(m_colValPanel);
        }
        m_filterPanel.invalidate();
        m_filterPanel.validate();
        m_filterPanel.repaint();
    }

    /**
     * @see de.unikn.knime.core.node.NodeDialogPane
     *      #loadSettingsFrom(de.unikn.knime.core.node.NodeSettings,
     *      de.unikn.knime.core.data.DataTableSpec[])
     */
    protected void loadSettingsFrom(final NodeSettings settings,
            final DataTableSpec[] specs) throws NotConfigurableException {

        if ((specs[0] == null) || (specs[0].getNumColumns() < 1)) {
            throw new NotConfigurableException("Cannot be configured without"
                    + " input table");
        }
        // remove the old filter panels, if any
        if (m_rangePanel != null) {
            m_filterPanel.remove(m_rangePanel);
        }
        if (m_rowIDPanel != null) {
            m_filterPanel.remove(m_rowIDPanel);
        }
        if (m_colValPanel != null) {
            m_filterPanel.remove(m_colValPanel);
        }
        // now create new filter panels
        m_rangePanel = new RowNoRowFilterPanel();
        m_rowIDPanel = new RowIDRowFilterPanel();
        m_colValPanel = new ColumnRowFilterPanel(specs[0]);

        /*
         * now read the filters. We support three different filters:
         * RowIDFilter, ColValfilter, and RowNumberFilter. But only one at a
         * time.
         */
        RowFilter filter = null;
        try {
            // get the filter
            filter = RowFilterFactory.createRowFilter(settings
                    .getConfig(RowFilterNodeModel.CFGFILTER));
        } catch (InvalidSettingsException ise) {
            // silently ignore invalid filters. 
        }

        String actionCommand = "colval";

       
        if (filter == null) {
            // set the default
            m_colValInclRadio.setSelected(true);
            filterSelectionChanged("colval");
            return;
        }
        
        if (filter instanceof ColValRowFilter) {
            ColValRowFilter f = (ColValRowFilter)filter;
            actionCommand = "colval";
            // activate the corresponding radio button
            if (f.includeMatchingLines()) {
                m_colValInclRadio.setSelected(true);
            } else {
                m_colValExclRadio.setSelected(true);
            }
            try {
                m_colValPanel.loadSettingsFromFilter(f);
            } catch (InvalidSettingsException ise) {
                // ignore failure
            }
        } else if (filter instanceof RowIDRowFilter) {
            RowIDRowFilter f = (RowIDRowFilter)filter;
            actionCommand = "id";
            // activate the corresponding radio button
            if (f.getInclude()) {
                m_rowIDInclRadio.setSelected(true);
            } else {
                m_rowIDExclRadio.setSelected(true);
            }
            try {
                m_rowIDPanel.loadSettingsFromFilter(f);
            } catch (InvalidSettingsException ise) {
                // ignore failure
            }
        } else if (filter instanceof RowNoRowFilter) {
            RowNoRowFilter f = (RowNoRowFilter)filter;
            actionCommand = "range";
            // activate the corresponding radio button
            if (f.getInclude()) {
                m_rangeInclRadio.setSelected(true);
            } else {
                m_rangeExclRadio.setSelected(true);
            }
            try {
                m_rangePanel.loadSettingsFromFilter(f);
            } catch (InvalidSettingsException ise) {
                // ignore failure
            }
        } else {
            // we silently ignore unsupported filter and leave default settings.
        }

        filterSelectionChanged(actionCommand);
        
    }

    /**
     * @see de.unikn.knime.core.node.NodeDialogPane
     *      #saveSettingsTo(de.unikn.knime.core.node.NodeSettings)
     */
    protected void saveSettingsTo(final NodeSettings settings)
            throws InvalidSettingsException {

        RowFilter theFilter = null;
        if (m_rangeInclRadio.isSelected()) {
            theFilter = m_rangePanel.createFilter(true);
        }
        if (m_rangeExclRadio.isSelected()) {
            theFilter = m_rangePanel.createFilter(false);            
        }
        if (m_colValInclRadio.isSelected()) {
            theFilter = m_colValPanel.createFilter(true);
        }        
        if (m_colValExclRadio.isSelected()) {
            theFilter = m_colValPanel.createFilter(false);
        }
        if (m_rowIDInclRadio.isSelected()) {
            theFilter = m_rowIDPanel.createFilter(true);
        }
        if (m_rowIDExclRadio.isSelected()) {
            theFilter = m_rowIDPanel.createFilter(false);
        }
        assert theFilter != null;

        theFilter.saveSettingsTo(settings
                .addConfig(RowFilterNodeModel.CFGFILTER));

    }
}
