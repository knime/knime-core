/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2010
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
 * -------------------------------------------------------------------
 *
 * History
 *   07.07.2005 (ohl): created
 */
package org.knime.base.node.preproc.filter.row;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JPanel;
import javax.swing.JRadioButton;

import org.knime.base.node.preproc.filter.row.rowfilter.AttrValueRowFilter;
import org.knime.base.node.preproc.filter.row.rowfilter.ColValFilterOldObsolete;
import org.knime.base.node.preproc.filter.row.rowfilter.RowFilter;
import org.knime.base.node.preproc.filter.row.rowfilter.RowFilterFactory;
import org.knime.base.node.preproc.filter.row.rowfilter.RowIDRowFilter;
import org.knime.base.node.preproc.filter.row.rowfilter.RowNoRowFilter;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;

/**
 *
 * @author Peter Ohl, University of Konstanz
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
     * Creates a new panel for the row filter node dialog.
     */
    public RowFilterNodeDialogPane() {
        super();
        JPanel dlg = createDialogPanel();
        // the actual filter panel instantiations happen during loadSettings.
        addTab("Filter Criteria", dlg);
    }

    protected JPanel createDialogPanel() {

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
     * Activates the corresponding panel. Called by the radio button listeners.
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
     * {@inheritDoc}
     */
    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings,
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
        m_colValPanel = new ColumnRowFilterPanel(this, specs[0]);

        /*
         * now read the filters. We support three different filters:
         * RowIDFilter, AttrValfilter, and RowNumberFilter. But only one at a
         * time.
         */
        RowFilter filter = null;
        try {
            // get the filter
            filter = RowFilterFactory.createRowFilter(settings
                    .getNodeSettings(RowFilterNodeModel.CFGFILTER));
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

        if (filter instanceof ColValFilterOldObsolete) {
            // support the obsolete filter for backward compatibility
            ColValFilterOldObsolete f = (ColValFilterOldObsolete)filter;
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
        } else if (filter instanceof AttrValueRowFilter) {
            // this covers all the attribute value filters:
            // range, string compare, missing value filter
            AttrValueRowFilter f = (AttrValueRowFilter)filter;
            actionCommand = "colval";
            // activate the corresponding radio button
            if (f.getInclude()) {
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
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings)
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
                .addNodeSettings(RowFilterNodeModel.CFGFILTER));
    }
}
