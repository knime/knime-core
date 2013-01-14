/*
 *
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
 * -------------------------------------------------------------------
 *
 * History
 *   Nov 23, 2009 (morent): created
 */
package org.knime.base.node.viz.plotter.box;

import java.util.LinkedHashMap;
import java.util.Map;

import org.knime.base.node.viz.condbox.ConditionalBoxPlotNodeModel;
import org.knime.base.node.viz.plotter.AbstractPlotterProperties;
import org.knime.base.util.coordinate.Coordinate;
import org.knime.core.data.DataColumnDomain;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DoubleValue;

/**
 *
 * @author Dominik Morent, KNIME.com, Zurich, Switzerland
 */
public class ConditionalBoxPlotter extends BoxPlotter {

    /**
     * {@inheritDoc}
     */
    @Override
    protected void createNormalizedCoordinates(
            final Map<DataColumnSpec, double[]> statistics) {
        // create y-axis that consider the input domain
        ConditionalBoxPlotNodeModel model =
                (ConditionalBoxPlotNodeModel)getDataProvider();
        DataColumnSpec numColSpec = model.getNumColSpec();

        Map<DataColumnSpec, Coordinate> coordinates =
                new LinkedHashMap<DataColumnSpec, Coordinate>();
        for (DataColumnSpec colSpec : statistics.keySet()) {
            /*
             * Pass the input domain's min and max values of the numerical
             * column to all box plots by providing the column spec of the
             * numerical column to all box plots. In this way they all use the
             * same scale and the whole domain range is displayed.
             */
            coordinates.put(colSpec, Coordinate.createCoordinate(numColSpec));
        }
        setCoordinates(coordinates);

        DataColumnDomain domain = numColSpec.getDomain();
        double min = ((DoubleValue)domain.getLowerBound()).getDoubleValue();
        double max = ((DoubleValue)domain.getUpperBound()).getDoubleValue();
        createYCoordinate(min, max);
    }

    /**
     * @param enabled True to enable the checkbox, false to disable it.
     * @see javax.swing.AbstractButton#setEnabled(boolean)
     */
    public void setNormalizeTabCheckboxEnabled(final boolean enabled) {
        AbstractPlotterProperties properties = getProperties();
        if (properties != null && properties instanceof BoxPlotterProperties) {
            BoxPlotterProperties boxProperties =
                    (BoxPlotterProperties)getProperties();
            boxProperties.setCheckboxEnabled(enabled);
        }

    }

    /**
     * @param text The tooltip text for the normalization checkbox.
     * @see javax.swing.JComponent#setToolTipText(java.lang.String)
     */
    public void setNormalizeTabCheckboxToolTip(final String text) {
        AbstractPlotterProperties properties = getProperties();
        if (properties != null && properties instanceof BoxPlotterProperties) {
            BoxPlotterProperties boxProperties =
                    (BoxPlotterProperties)getProperties();
            boxProperties.setCheckboxToolTipText(text);
        }
    }

    /**
     * @param selected The initial value for the normalization checkbox.
     * @see javax.swing.AbstractButton#setSelected(boolean)
     */
    public void setNormalizeTabCheckboxSelected(final boolean selected) {
        AbstractPlotterProperties properties = getProperties();
        if (properties != null && properties instanceof BoxPlotterProperties) {
            BoxPlotterProperties boxProperties =
                    (BoxPlotterProperties)getProperties();
            boxProperties.setCheckboxSelected(selected);
        }
    }

}
