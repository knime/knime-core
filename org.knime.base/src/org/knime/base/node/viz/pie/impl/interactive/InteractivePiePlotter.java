/*
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2009
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
 *    23.09.2007 (Tobias Koetter): created
 */

package org.knime.base.node.viz.pie.impl.interactive;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import org.knime.base.node.viz.pie.datamodel.interactive.InteractivePieVizModel;
import org.knime.base.node.viz.pie.impl.PiePlotter;
import org.knime.base.node.viz.pie.util.TooManySectionsException;
import org.knime.core.node.property.hilite.HiLiteHandler;


/**
 * The interactive implementation of the pie plotter which allows the user
 * to change the pie and aggregation column in the view and supports hiliting.
 * @author Tobias Koetter, University of Konstanz
 */
public class InteractivePiePlotter
    extends PiePlotter<InteractivePieProperties, InteractivePieVizModel> {

    private static final long serialVersionUID = -7592208071888135451L;

    private boolean m_ignoreChanges = false;

    /**Constructor for class InteractivePiePlotter.
     * @param properties the properties panel
     * @param handler the hilite handler
     */
    public InteractivePiePlotter(final InteractivePieProperties properties,
            final HiLiteHandler handler) {
        super(properties, handler);
        properties.addPieColumnChangeListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                final InteractivePieProperties props = getPropertiesPanel();
                if (props == null) {
                    return;
                }
                setPieColumn(props.getSelectedPieColumn());
            }
        });
        properties.addAggrColumnChangeListener(new ActionListener() {

            public void actionPerformed(final ActionEvent e) {
                final InteractivePieProperties props = getPropertiesPanel();
                if (props == null) {
                    return;
                }
                setAggregationColumn(props.getSelectedAggrColumn());
            }
        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void updatePropertiesPanel(
            final InteractivePieVizModel vizModel) {
        m_ignoreChanges = true;
        super.updatePropertiesPanel(vizModel);
        m_ignoreChanges = false;
    }

    /**
     * @param colName the name of the new pie column
     */
    protected void setPieColumn(final String colName) {
        if (m_ignoreChanges) {
            return;
        }
        final InteractivePieVizModel vizModel = getVizModel();
        if (vizModel == null) {
            throw new NullPointerException("vizModel must not be null");
        }
        boolean changed = true;
        try {
            changed = vizModel.setPieColumn(colName);
            resetInfoMsg();
        } catch (final TooManySectionsException e) {
            setInfoMsg(e.getMessage());
        }
        if (changed) {
            modelChanged();
        }
    }

    /**
     * @param colName the name of the new aggregation column
     */
    protected void setAggregationColumn(final String colName) {
        if (m_ignoreChanges) {
            return;
        }
        final InteractivePieVizModel vizModel = getVizModel();
        if (vizModel == null) {
            throw new NullPointerException("vizModel must not be null");
        }
        //update the properties panel as well
        final InteractivePieProperties properties = getPropertiesPanel();
        if (properties == null) {
            throw new NullPointerException("Properties must not be null");
        }
        boolean changed = true;
        try {
            changed = vizModel.setAggrColumn(colName);
            resetInfoMsg();
        } catch (final TooManySectionsException e) {
            setInfoMsg(e.getMessage());
        }
        if (changed) {
          modelChanged();
        }
    }
}
