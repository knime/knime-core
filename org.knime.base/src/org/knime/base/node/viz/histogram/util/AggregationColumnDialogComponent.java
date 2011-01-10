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
 *  propagated with or for interoperation with KNIME.  The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
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
import org.knime.core.node.port.PortObjectSpec;
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
    protected void checkConfigurabilityBeforeLoad(final PortObjectSpec[] specs) {
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
        DataTableSpec thisSpec = null;
        try {
            thisSpec = (DataTableSpec)getLastTableSpec(0);
        } catch (ClassCastException cce) {
            throw new RuntimeException("Expected DataTableSpec at port 0 of"
                    + " AggregationColumnDialogComponent!");
        }
        if (inclCols == null) {
            m_panel.update(thisSpec, new ColorColumn[0]);
        } else {
            m_panel.update(thisSpec, inclCols);
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
