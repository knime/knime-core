/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2009
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
 */
package org.knime.base.node.viz.histogram.impl.interactive;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.knime.base.node.viz.aggregation.AggregationMethod;
import org.knime.base.node.viz.histogram.datamodel.AbstractHistogramVizModel;
import org.knime.base.node.viz.histogram.datamodel.InteractiveHistogramVizModel;
import org.knime.base.node.viz.histogram.impl.AbstractHistogramPlotter;
import org.knime.base.node.viz.histogram.impl.AbstractHistogramProperties;
import org.knime.base.node.viz.histogram.util.ColorColumn;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.property.hilite.HiLiteHandler;
import org.knime.core.node.util.ColumnSelectionComboxBox;

/**
 * This class is the controller between the data model of the
 * {@link InteractiveHistogramVizModel}
 * class and the view
 * {@link org.knime.base.node.viz.histogram.impl.HistogramDrawingPane}.
 * It creates the
 * {@link org.knime.base.node.viz.histogram.datamodel.BarDataModel}
 * objects based on the
 * {@link
 * org.knime.base.node.viz.histogram.datamodel.InteractiveHistogramDataModel}
 * class by enhancing these information
 * with information about the size of the drawing space like height and width
 * in pixel.
 *
 * @author Tobias Koetter, University of Konstanz
 */
public class InteractiveHistogramPlotter extends AbstractHistogramPlotter {
    private static final NodeLogger LOGGER = NodeLogger
            .getLogger(InteractiveHistogramPlotter.class);
    private static final long serialVersionUID = -893697601218801524L;


    /**
     * Creates a new PlotterScrolling pane and associates it with the passed
     * view control panel.
     *
     * @param histogramProps the <code>FixedColumnHistogramProperties</code>
     * with the view options for the user
     * @param handler the hilite handler from the input port
     */
    public InteractiveHistogramPlotter(
            final InteractiveHistogramProperties histogramProps,
            final HiLiteHandler handler) {
        super(histogramProps, handler);
        histogramProps.addXColumnChangeListener(
                new ActionListener() {
                    public void actionPerformed(final ActionEvent e) {
                        final ColumnSelectionComboxBox cb =
                            (ColumnSelectionComboxBox)e.getSource();
                        final String xCol = cb.getSelectedColumn();
                        onXColChanged(xCol);
                    }
                });
        histogramProps.addAggrColumnChangeListener(
                new ChangeListener() {
                    public void stateChanged(final ChangeEvent e) {
                        onAggrColChanged();
                    }
                });
    }

    /**
    * Called whenever user changes the x column selection.
    *
    * @param xColName the new selected x column
    */
   protected void onXColChanged(final String xColName) {
       if (xColName == null || xColName.trim().length() < 1) {
           return;
       }
       // if it's the same name we don't need to do anything
       if (getXColName() != null && getXColName().equals(xColName)) {
           return;
       }
       final AbstractHistogramVizModel abstractVizModel =
           getHistogramVizModel();
       if (abstractVizModel == null) {
           LOGGER.debug("VizModel was null");
           return;
       }
       if (abstractVizModel instanceof InteractiveHistogramVizModel) {
           final InteractiveHistogramVizModel vizModel =
               (InteractiveHistogramVizModel)abstractVizModel;
           final DataColumnSpec xColSpec =
               getDataTableSpec().getColumnSpec(xColName);
           if (vizModel.setXColumn(xColSpec)) {
               //set the current hilited keys in the new bins
               vizModel.updateHiliteInfo(delegateGetHiLitKeys(), true);
//             set the new axis
               setXCoordinates();
               setYCoordinates();
               //update the details tab
               getHistogramPropertiesPanel().updateHTMLDetailsPanel(
                       vizModel.getHTMLDetailData());
               // repaint the plotter
               updatePaintModel();

           }
        } else {
            throw new IllegalStateException(
                    "VizModel should be of type interactive");
        }

       // update the slider values and the select boxes
//       getHistogramPropertiesPanel().updateHistogramSettings(this);
   }

   /**
     * Called whenever the user changes the aggregation column.
     */
    protected void onAggrColChanged() {
        final AbstractHistogramVizModel abstractVizModel =
            getHistogramVizModel();
        if (abstractVizModel == null) {
            LOGGER.debug("VizModel was null");
            return;
        }
        final AbstractHistogramProperties abstractHistogramProperties =
            getHistogramPropertiesPanel();
        if (abstractHistogramProperties == null) {
            LOGGER.debug("ProeprtiesPanel was null");
            return;
        }
        if (abstractVizModel instanceof InteractiveHistogramVizModel) {
            final InteractiveHistogramVizModel vizModel =
                (InteractiveHistogramVizModel)abstractVizModel;
            if (abstractHistogramProperties
                    instanceof InteractiveHistogramProperties) {
                final InteractiveHistogramProperties props =
                (InteractiveHistogramProperties) abstractHistogramProperties;
                final List<ColorColumn> aggrCols =
                    props.getSelectedAggrColumns();
                if (vizModel.setAggregationColumns(aggrCols)) {
                    //show the bar outline automatically depending on the
                    //number of selected aggregation columns
                    vizModel.setShowBarOutline(aggrCols != null
                            && aggrCols.size() > 1);
                    setYCoordinates();
                    //set the current hilited keys in the new bins
                    vizModel.updateHiliteInfo(delegateGetHiLitKeys(), true);
                    if (vizModel.containsNotPresentableBin()) {
                        vizModel.setBinWidth(vizModel.getMaxBinWidth());
                    }
                    //update the details tab
                    getHistogramPropertiesPanel().updateHTMLDetailsPanel(
                            vizModel.getHTMLDetailData());
                    updatePaintModel();
                }
            } else {
                throw new IllegalStateException(
                        " PropertiesPanel should be of type interactive");
            }
        } else {
            throw new IllegalStateException(
                    "VizModel should be of type interactive");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean setAggregationMethod(final AggregationMethod aggrMethod) {
        if (aggrMethod == null) {
            throw new IllegalArgumentException("Aggregation method must not"
                    + " be null");
        }
        final AbstractHistogramVizModel vizModel = getHistogramVizModel();
        final Collection<? extends ColorColumn> oldAggrCols =
            vizModel.getAggrColumns();
        if (oldAggrCols == null || oldAggrCols.size() < 1) {
            //check if the user hasn't defined any aggregation column yet we
            //have to set a default one if he has changed the aggregation
            //method form sum to something else where we need an
            //aggregation column
            if (vizModel instanceof InteractiveHistogramVizModel) {
                final InteractiveHistogramVizModel interactiveVizModel =
                    (InteractiveHistogramVizModel)vizModel;
                final AbstractHistogramProperties abstHistoProps =
                    getHistogramPropertiesPanel();
                if (abstHistoProps
                        instanceof InteractiveHistogramProperties) {
                    final InteractiveHistogramProperties props =
                        (InteractiveHistogramProperties)abstHistoProps;

                    final DataTableSpec spec =
                        interactiveVizModel.getTableSpec();
                    final int numColumns = spec.getNumColumns();
                    boolean found = false;
                    for (int i = 0; i < numColumns; i++) {
                        final DataColumnSpec colSpec = spec.getColumnSpec(i);
                        if (AbstractHistogramPlotter.AGGREGATION_COLUMN_FILTER.
                                includeColumn(colSpec)) {
                            final ColorColumn aggrColumn =
                                new ColorColumn(Color.LIGHT_GRAY,
                                        colSpec.getName());
                            final ArrayList<ColorColumn> aggrCols =
                                new ArrayList<ColorColumn>(1);
                            aggrCols.add(aggrColumn);
                            props.updateColumnSelection(
                                    spec, getXColName(), aggrCols, aggrMethod);
                            final List<ColorColumn> selectedAggrCols =
                                props.getSelectedAggrColumns();
                            interactiveVizModel.setAggregationColumns(
                                    selectedAggrCols);
                            //set the current hilited keys in the new bins
                            vizModel.updateHiliteInfo(delegateGetHiLitKeys(),
                                    true);
                            found = true;
                            break;
                        }
                    }
                    if (!found) {
                        props.updateHistogramSettings(interactiveVizModel);
                        return false;
                    }
                } else {
                    throw new IllegalStateException(
                            "ProeprtiesPanel should  be of interactive type.");
                }
            } else {
                throw new IllegalStateException("Visualization model should "
                        + " be of interactive type.");
            }
        }
        return super.setAggregationMethod(aggrMethod);
    }
}
