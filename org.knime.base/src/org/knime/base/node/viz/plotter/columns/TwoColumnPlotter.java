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
 *   30.08.2006 (Fabian Dill): created
 */
package org.knime.base.node.viz.plotter.columns;

import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.knime.base.node.viz.plotter.AbstractDrawingPane;
import org.knime.base.node.viz.plotter.AbstractPlotterProperties;
import org.knime.base.node.viz.plotter.Axis;
import org.knime.base.node.viz.plotter.DataProvider;
import org.knime.base.node.viz.plotter.basic.BasicDrawingPane;
import org.knime.base.node.viz.plotter.basic.BasicPlotter;
import org.knime.base.util.coordinate.Coordinate;
import org.knime.base.util.coordinate.NumericCoordinate;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.NodeLogger;

/**
 * Wraps the functionality where the data of two columns have to be displayed.
 * It registeres the appropriate listeners to the column selection and calls the
 * corresponding methods dependend on whether the model has changed
 * ({@link #updatePaintModel()}) or the ranges have changed 
 * ({@link #updateSize()}). If only columns which are compatible to certain 
 * {@link org.knime.core.data.DataValue}s should be selectable, an instance of 
 * the {@link org.knime.base.node.viz.plotter.columns.TwoColumnProperties} has 
 * to be created, where the restricting {@link org.knime.core.data.DataValue}s
 *  can be passed to the constructor.
 * 
 * @author Fabian Dill, University of Konstanz
 */
public abstract class TwoColumnPlotter extends BasicPlotter {
    
    private static final NodeLogger LOGGER = NodeLogger.getLogger(
            TwoColumnPlotter.class);
    
    private DataColumnSpec m_selectedXColumn;
    
    private DataColumnSpec m_selectedYColumn;
    
    private DataTableSpec m_spec;
    

    /**
     * Constructor for extending classes.
     * 
     * @param panel the drawing pane
     * @param properties the properties
     */
    public TwoColumnPlotter(final AbstractDrawingPane panel, 
            final AbstractPlotterProperties properties) {
        super(panel, properties);
        if (properties instanceof TwoColumnProperties) {
            TwoColumnProperties properties2D = 
                (TwoColumnProperties)properties;
            properties2D.addXColumnListener(new ItemListener() {

                /**
                 * {@inheritDoc}
                 */
                public void itemStateChanged(final ItemEvent e) {
                    DataColumnSpec x = ((TwoColumnProperties)
                            getProperties()).getSelectedXColumn();
                    if (x != null) {
                        xColumnChanged(x);
                    }
                }
            });
            properties2D.addYColumnListener(new ItemListener() {

                /**
                 * {@inheritDoc}
                 */
                public void itemStateChanged(final ItemEvent e) {
                    DataColumnSpec y = ((TwoColumnProperties)
                            getProperties()).getSelectedYColumn();
                    if (y != null) {
                        yColumnChanged(y);
                    }
                }
            });
        }
        setXAxis(new Axis(Axis.HORIZONTAL, getDrawingPaneDimension().width));
        setYAxis(new Axis(Axis.VERTICAL, getDrawingPaneDimension().height));
        addRangeListener();
    }
    
    /**
     * Default two column plotter with 
     * {@link org.knime.base.node.viz.plotter.basic.BasicDrawingPane} and 
     * {@link TwoColumnProperties}.
     *
     */
    public TwoColumnPlotter() {
        this(new BasicDrawingPane(), new TwoColumnProperties());
    }
    
    /**
     * Adds the range listener to the range adjustment component of the 
     * properties.
     *
     */
    private void addRangeListener() {
        if (!(getProperties() instanceof TwoColumnProperties)) {
            return;
        }
        final TwoColumnProperties props = (TwoColumnProperties)getProperties();
        props.addXMinListener(new ChangeListener() {
            /**
             * {@inheritDoc}
             */
            public void stateChanged(final ChangeEvent e) {
                double newXMin = props.getXMinValue();
                ((NumericCoordinate)getXAxis().getCoordinate())
                    .setMinDomainValue(newXMin);
                updateSize();
                getXAxis().repaint();
            }
            
        });
        props.addXMaxListener(new ChangeListener() {
            /**
             * {@inheritDoc}
             */
            public void stateChanged(final ChangeEvent e) {
                double newXMax = props.getXMaxValue();
                ((NumericCoordinate)getXAxis().getCoordinate())
                    .setMaxDomainValue(newXMax);
                updateSize();
                getXAxis().repaint();
            }
            
        });
        props.addYMinListener(new ChangeListener() {
            /**
             * {@inheritDoc}
             */
            public void stateChanged(final ChangeEvent e) {
                double newYMin = props.getYMinValue();
                ((NumericCoordinate)getYAxis().getCoordinate())
                    .setMinDomainValue(newYMin);
                updateSize();
                getYAxis().repaint();
            }
            
        });
        props.addYMaxListener(new ChangeListener() {
            /**
             * {@inheritDoc}
             */
            public void stateChanged(final ChangeEvent e) {
                double newYMax = props.getYMaxValue();
                ((NumericCoordinate)getYAxis().getCoordinate())
                    .setMaxDomainValue(newYMax);
                updateSize();
                getYAxis().repaint();
            }
            
        });
    }
    
    /**
     * Updates the coordinates for both columns and calls  
     * {@link #updatePaintModel()}.
     * 
     * @param newXColumn the new {@link org.knime.core.data.DataColumnSpec}. 
     */
    private synchronized void xColumnChanged(final DataColumnSpec newXColumn) {
        if (!newXColumn.equals(m_selectedXColumn)) {
            LOGGER.debug("x column changed: " 
                    + newXColumn.getName());
            m_selectedXColumn = newXColumn;
            getXAxis().setCoordinate(Coordinate.createCoordinate(
                    m_selectedXColumn));
            getYAxis().setCoordinate(Coordinate.createCoordinate(
                    m_selectedYColumn));
            ((TwoColumnProperties)getProperties()).updateRangeSpinner(
                    m_selectedXColumn, m_selectedYColumn);
            updatePaintModel();
        }
    }
    
    /**
     * Updates the coordinates for both columns and calls  
     * {@link #updatePaintModel()}.
     * 
     * @param newYColumn the new {@link org.knime.core.data.DataColumnSpec}. 
     */
    private synchronized void yColumnChanged(final DataColumnSpec newYColumn) {
        if (!newYColumn.equals(m_selectedYColumn)) {
            LOGGER.debug("y column changed: " 
                    + newYColumn.getName());
            m_selectedYColumn = newYColumn;
//            m_yAxis.setPreferredLength(getDrawingPaneDimension().height);
            getYAxis().setCoordinate(Coordinate.createCoordinate(
                    m_selectedYColumn));
            getXAxis().setCoordinate(Coordinate.createCoordinate(
                    m_selectedXColumn));
            ((TwoColumnProperties)getProperties()).updateRangeSpinner(
                    m_selectedXColumn, m_selectedYColumn);
            updatePaintModel();
        }
    }
    
    /**
     * 
     * @return the currently selecteed x column.
     */
    public DataColumnSpec getSelectedXColumn() {
        return m_selectedXColumn;
    }
    
    /**
     * Returns the selected column index or 0 by default, that is if no 
     * {@link org.knime.core.data.DataTableSpec} is set and no column was 
     * selected before. If the stored {@link org.knime.core.data.DataTableSpec}
     * doesn't find the selected column name (which should never happen), 
     * then -1 is returned. 
     * 
     * @return selected x column index or -1
     */
    public int getSelectedXColumnIndex() {
        if (m_spec != null && getSelectedXColumn() != null) {
            return m_spec.findColumnIndex(getSelectedXColumn().getName());
        }
        return 0;
    }
    
    /**
     * Returns the selected column index or 0 by default, that is if no 
     * {@link org.knime.core.data.DataTableSpec} is set and no column was 
     * selected before. If the stored {@link org.knime.core.data.DataTableSpec}
     * doesn't find the selected column name (which should never happen), 
     * then -1 is returned. 
     * 
     * @return selected y column index or -1
     */
    public int getSelectedYColumnIndex() {
        if (m_spec != null && getSelectedYColumn() != null) {
            return m_spec.findColumnIndex(getSelectedYColumn().getName());    
        }
        return 1;
    }
    
    
    /**
     * 
     * @return the currently selected y column
     */
    public DataColumnSpec getSelectedYColumn() {
        return m_selectedYColumn;
    }
    
    /**
     * Updates the select boxes for the x and y columns.
     * @param spec the current data table spec.
     */
    public void setSelectableColumns(final DataTableSpec spec) {
        m_spec = spec;
        if (getProperties() instanceof TwoColumnProperties) {
            ((TwoColumnProperties)getProperties()).update(spec);
        }
    }
    
    
    /**
     * Updates the column selection with the 
     * {@link org.knime.core.data.DataTableSpec} of the 
     * {@link org.knime.base.node.util.DataArray} with index 0.
     * 
     * @see org.knime.base.node.viz.plotter.AbstractPlotter#setDataProvider(
     * DataProvider)
     */
    @Override
    public void setDataProvider(final DataProvider provider) {
        super.setDataProvider(provider);
        if (getDataProvider() != null 
                && getDataProvider().getDataArray(getDataArrayIdx()) != null) {
            setSelectableColumns(getDataProvider().getDataArray(
                    getDataArrayIdx()).getDataTableSpec());
        }
    }
        
    
    /**
     * This method is called whenever the column selection has changed and
     * the view model has to be adapted to the currently selected columns.
     * There might occur a problem with the selected indices if the 
     * underlying data spec was changed and both, the x and the y, indices
     * are changed, then this method is called for the changed x index although
     * the y index is still obsolete. Thus, one have to check that the
     * {@link #getSelectedXColumnIndex()} and the 
     * {@link #getSelectedYColumnIndex()} are both > -1.
     *
     */
    @Override
    public abstract void updatePaintModel();
     
    
}
