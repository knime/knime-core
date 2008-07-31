/*
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2008
 * University of Konstanz, Germany
 * Chair for Bioinformatics and Information Mining (Prof. M. Berthold)
 * and KNIME GmbH, Konstanz, Germany
 *
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner or
 * as specified in the license file distributed with this product.
 *
 * If you have any questions please contact the copyright holder:
 * website: www.knime.org
 * email: contact@knime.org
 * -------------------------------------------------------------------
 * 
 * History
 *    16.03.2007 (Tobias Koetter): created
 */

package org.knime.base.node.viz.histogram.util;

import java.awt.Dimension;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.knime.core.data.DataTableSpec;
import org.knime.core.node.defaultnodesettings.DialogComponent;
import org.knime.core.node.util.ColumnFilter;


/**
 * 
 * @author Tobias Koetter, University of Konstanz
 */
public class AggregationColumnDialogComponent extends DialogComponent {

    private final AggregationColumnFilterPanel m_panel;
    
    /**Constructor for class AggregationColumnDialogComponent.
     * @param label the label of this component
     * @param model holds the selected aggregation columns
     * @param listDimension the dimension of the list fields
     * @param filter the column filter to use
     */
    public AggregationColumnDialogComponent(final String label,
            final SettingsModelColorNameColumns model, 
            final Dimension listDimension,
            final ColumnFilter filter) {
        super(model);
        m_panel = 
            new AggregationColumnFilterPanel(label, listDimension, filter);
        getComponentPanel().add(m_panel);
//      when the user input changes we need to update the model.
        m_panel.addChangeListener(new ChangeListener() {
            public void stateChanged(final ChangeEvent e) {
                updateModel();
            }
        });
        // update the components, when the value in the model changes
        model.prependChangeListener(new ChangeListener() {
            public void stateChanged(final ChangeEvent e) {
                updateComponent();
            }
        });
    }
    /**
     * transfers the settings from the component into the settings model.
     */
    protected void updateModel() {
        final ColorColumn[] includedColumns = 
            m_panel.getIncludedColorNameColumns();
        SettingsModelColorNameColumns colModel =
                (SettingsModelColorNameColumns)getModel();
        colModel.setColorNameColumns(includedColumns);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void checkConfigurabilityBeforeLoad(final DataTableSpec[] specs) {
        // nothing to do
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void setEnabledComponents(final boolean enabled) {
        m_panel.setEnabled(enabled);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setToolTipText(final String text) {
        m_panel.setToolTipText(text);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void updateComponent() {
        SettingsModelColorNameColumns colModel =
            (SettingsModelColorNameColumns)getModel();
        final ColorColumn[] inclCols = colModel.getColorNameColumns();
        if (inclCols == null) {
            m_panel.update(getLastTableSpec(0), new ColorColumn[0]);
        } else {
            m_panel.update(getLastTableSpec(0), inclCols);
        }
        // update the enable status
        setEnabledComponents(colModel.isEnabled());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettingsBeforeSave() {
//      just in case we didn't get notified about the last selection ...
        updateModel();
    }

}
