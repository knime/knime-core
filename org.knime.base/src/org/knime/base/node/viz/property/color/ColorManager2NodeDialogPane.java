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
package org.knime.base.node.viz.property.color;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridLayout;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JColorChooser;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.colorchooser.DefaultColorSelectionModel;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnDomain;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.property.ColorAttr;
import org.knime.core.data.property.ColorHandler;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.util.DataColumnSpecListCellRenderer;

/**
 * Color manager dialog which shows all columns of the input data and its
 * corresponding values inside two combo boxes divided by range and nominal
 * ones. The color chooser can then be used to select certain colors for each
 * value for one attribute value or range, min or max. If the attribute changes,
 * the color settings are locally saved. During save the settings are saved by
 * the underlying {@link ColorHandler}'s <code>ColorModel</code> which in turn a
 * read by the model.
 * 
 * @see ColorManager2NodeModel
 * 
 * @author Thomas Gabriel, University of Konstanz
 */
final class ColorManager2NodeDialogPane extends NodeDialogPane implements
        ItemListener {
    
    private static final NodeLogger LOGGER = NodeLogger.getLogger(
            ColorManager2NodeDialogPane.class);

    /** Keeps all columns. */
    private final JComboBox m_columns = new JComboBox();

    /** Nominal column. */
    private final JRadioButton m_buttonNominal = new JRadioButton("Nominal");

    /** Range column. */
    private final JRadioButton m_buttonRange = new JRadioButton("Range");

    /** Nominal color panel. */
    private final ColorManager2DialogNominal m_nominal;

    /** Range color panel. */
    private final ColorManager2DialogRange m_range;
    
    private final DefaultAlphaColorPanel m_alphaPanel = 
        new DefaultAlphaColorPanel();

    /**
     * Creates a new color manager dialog; all color settings are empty.
     */
    ColorManager2NodeDialogPane() {
        // create new super node dialog with name
        super();

        m_columns.setRenderer(new DataColumnSpecListCellRenderer());
        JPanel columnPanel = new JPanel(new BorderLayout());
        columnPanel.setBorder(BorderFactory
                .createTitledBorder(" Select one Column "));
        columnPanel.add(m_columns);

        // button group for nominal and numeric color selection
        ButtonGroup buttonGroup = new ButtonGroup();
        buttonGroup.add(m_buttonNominal);
        buttonGroup.add(m_buttonRange);
        
        /**
         * Overwrite default color selection model to throw color event even if
         * the color is the same and no event was created.
         */
        class MyColorSelectionModel extends DefaultColorSelectionModel {
            /**
             * @param color to set.
             * @see DefaultColorSelectionModel#setSelectedColor(java.awt.Color)
             */
            @Override
            public void setSelectedColor(final Color color) {
                super.setSelectedColor(color);
                if (color != null) {
                    colorChanged(color);
                }
            }
        }
        // init color chooser and the value combo box
        final JColorChooser jcc = new JColorChooser(
                new MyColorSelectionModel());
        jcc.addChooserPanel(m_alphaPanel);

        // init nominal and range color selection dialog
        m_nominal = new ColorManager2DialogNominal();
        m_range = new ColorManager2DialogRange();

        // combo holding the values for a certain column
        final Color dftColor = ColorAttr.DEFAULT.getColor();
        jcc.setColor(dftColor);
        // remove preview
        jcc.setPreviewPanel(new JPanel());

        JPanel nominalPanel = new JPanel(new BorderLayout());
        nominalPanel.setBorder(BorderFactory.createTitledBorder(""));
        nominalPanel.add(m_buttonNominal, BorderLayout.NORTH);
        nominalPanel.add(m_nominal, BorderLayout.CENTER);

        JPanel rangePanel = new JPanel(new BorderLayout());
        rangePanel.setBorder(BorderFactory.createTitledBorder(""));
        rangePanel.add(m_buttonRange, BorderLayout.NORTH);
        rangePanel.add(m_range, BorderLayout.CENTER);

        // center panel that is added to the dialog pane's tabs
        JPanel center = new JPanel(new BorderLayout());
        center.add(columnPanel, BorderLayout.NORTH);
        JPanel listPanel = new JPanel(new GridLayout(1, 2));
        listPanel.add(nominalPanel);
        listPanel.add(rangePanel);
        center.add(listPanel, BorderLayout.CENTER);
        center.add(jcc, BorderLayout.SOUTH);
        super.addTab(" Color Settings ", center);
    }

    /* If the color has changed by the color chooser. */
    private void colorChanged(final Color color) {
        String column = getSelectedColumn();
        if (column == null) {
            return;
        }
        if (m_buttonNominal.isSelected()) {
            m_nominal.update(column, ColorAttr.getInstance(color));
        } else {
            if (m_buttonRange.isSelected()) {
                m_range.update(column, color);
            }
        }
    }

    /**
     * Updates this dialog by refreshing all components in the color tab. Inits
     * the column name combo box and sets the values for the default selected
     * one.
     * 
     * @param settings the settings to load
     * @param specs the input table specs
     * @throws NotConfigurableException if no column found for color selection
     * @see NodeDialogPane#loadSettingsFrom(NodeSettingsRO, DataTableSpec[])
     */
    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings,
            final DataTableSpec[] specs) throws NotConfigurableException {
        // remove all columns
        m_columns.removeItemListener(this);
        m_columns.removeAllItems();
        
        // reset nominal and range panel
        m_nominal.removeAllElements();
        m_range.removeAllElements();

        // index of the last column with nominal values
        int hasNominals = -1;
        // index of the last column with numeric ranges defined
        int hasRanges = -1;
        
        // read settings and write into the map
        String target = settings.getString(
                ColorManager2NodeModel.SELECTED_COLUMN, null);
        
        // null = not specified, true = nominal, and false = range
        Boolean nominalSelected = null;
        try {
            nominalSelected = settings.getBoolean(
                    ColorManager2NodeModel.IS_NOMINAL);
        } catch (InvalidSettingsException ise) {
            LOGGER.debug("Nominal/Range selection flag"
                    + " not available.");
        }

        // find last columns for nominal values and numeric ranges defined
        for (int i = 0; i < specs[0].getNumColumns(); i++) {
            DataColumnSpec cspec = specs[0].getColumnSpec(i);
            DataColumnDomain domain = cspec.getDomain();
            // nominal values defined
            if (domain.hasValues()) {
                m_nominal.add(cspec.getName(), domain.getValues());
                // select last possible nominal column
                hasNominals = i;
            }
            // numeric ranges defined
            if (cspec.getType().isCompatible(DoubleValue.class)) {
                DataCell lower = domain.getLowerBound();
                DataCell upper = domain.getUpperBound();
                // lower and upper bound can be null
                m_range.add(cspec.getName(), lower, upper);
                if (hasRanges == -1) { // select first range column found
                    hasRanges = i;
                }
            }
        }

        // check for not configurable: no column found
        if (hasNominals == -1 && hasRanges == -1) {
            throw new NotConfigurableException("Please provide input table"
                    + " with at least one column with either nominal and/or"
                    + " lower and upper bounds defined.");
        }

        // update target column if: (1) null, (2) not in spec, (3+4) does not
        // have possible values defined AND is not compatible with DoubleType
        if (target == null || !specs[0].containsName(target)
                || (!specs[0].getColumnSpec(target).getDomain().hasValues() 
                &&  !specs[0].getColumnSpec(target).getType().isCompatible(
                        DoubleValue.class))) {
            // select first nominal column if nothing could be selected
            if (hasNominals > -1) {
                target = specs[0].getColumnSpec(hasNominals).getName();
                nominalSelected = true;
            } else {
                // otherwise the first range column
                if (hasRanges > -1) { 
                    target = specs[0].getColumnSpec(hasRanges).getName();
                    nominalSelected = false;
                } else { // 
                    assert false : "Both, nominal and range column are not "
                        + "available!";
                }
            }
        } else { // we have a valid target column
            boolean domValues = 
                specs[0].getColumnSpec(target).getDomain().hasValues();
            // nothing selected before
            if (nominalSelected == null) {
                // select nominal, if possible values found
                nominalSelected = domValues;
            } else { // nominal or range?
                // nominal! but no possible values
                if (nominalSelected && !domValues) {
                    // use range column
                    nominalSelected = false;
                }
            }
        }

        // nominal column selected
        if (hasNominals > -1) {
            m_nominal.loadSettings(settings, target);
            if (nominalSelected) {
                m_nominal.select(target);
                m_alphaPanel.setAlpha(m_nominal.getAlpha());
            }
        } else {
            m_nominal.select(null);
        }
        
        // numeric range column selected
        if (hasRanges > -1) {
            m_range.loadSettings(settings, target);
            if (!nominalSelected) {
                m_range.select(target);
                m_alphaPanel.setAlpha(m_range.getAlpha());
            }
        } else {
            m_range.select(null);
        }

        // add all columns
        int cols = specs[0].getNumColumns();
        for (int i = 0; i < cols; i++) {
            DataColumnSpec cspec = specs[0].getColumnSpec(i);
            m_columns.addItem(cspec);
            if (cspec.getName().equals(target)) {
                m_columns.setSelectedIndex(i);
            }
        }
        
        // inform about column change
        columnChanged(target, nominalSelected);
        // register column change listener
        m_columns.addItemListener(this);
    }

    /**
     * Method is invoked by the super class in order to force the dialog to
     * apply its changes.
     * 
     * @param settings the object to write the settings into
     * @throws InvalidSettingsException if either nominal or range selection
     *             could not be saved
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings)
            throws InvalidSettingsException {
        assert (settings != null);
        String cell = getSelectedColumn();
        settings.addString(ColorManager2NodeModel.SELECTED_COLUMN, cell);
        if (cell != null) {
            if (m_buttonNominal.isSelected() && m_buttonNominal.isEnabled()) {
                settings.addBoolean(ColorManager2NodeModel.IS_NOMINAL, true);
                m_nominal.setAlpha(m_alphaPanel.getAlpha());
                m_nominal.saveSettings(settings);
            } else {
                if (m_buttonRange.isSelected() && m_buttonRange.isEnabled()) {
                    settings.addBoolean(
                            ColorManager2NodeModel.IS_NOMINAL, false);
                    m_range.setAlpha(m_alphaPanel.getAlpha());
                    m_range.saveSettings(settings);
                } else {
                    throw new InvalidSettingsException("No color settings for "
                            + cell + " available.");
                }
            }
        }
    }

    /**
     * @param e the source event
     * @see ItemListener#itemStateChanged(java.awt.event.ItemEvent)
     */
    public void itemStateChanged(final ItemEvent e) {
        Object o = e.getItem();
        if (o == null) {
            return;
        }
        String cell = ((DataColumnSpec)o).getName();
        columnChanged(cell, true);
    }
    
    private void columnChanged(final String cell, final boolean nominal) {
        boolean hasRanges = m_range.select(cell);
        boolean hasNominal = m_nominal.select(cell);
        if (hasRanges) {
            m_buttonRange.setEnabled(true);
            if (!nominal || !hasNominal) {
                m_buttonRange.setSelected(true);
            }
        } else {
            m_buttonRange.setEnabled(false);
        }
        if (hasNominal) {
            m_buttonNominal.setEnabled(true);
            if (nominal || !hasRanges) {
                m_buttonNominal.setSelected(true);
            }
        } else {
            m_buttonNominal.setEnabled(false);
        }
    }
    


    /* Find selected column in button group. */
    private String getSelectedColumn() {
        Object o = m_columns.getSelectedItem();
        if (o == null) {
            return null;
        }
        return ((DataColumnSpec)o).getName();
    }
}
