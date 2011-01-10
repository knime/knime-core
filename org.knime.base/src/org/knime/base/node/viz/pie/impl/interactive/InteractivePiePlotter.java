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
