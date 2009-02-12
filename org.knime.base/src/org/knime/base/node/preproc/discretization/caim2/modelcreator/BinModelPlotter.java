/*
 * ------------------------------------------------------------------
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
 *   15.11.2006 (sieb): created
 */
package org.knime.base.node.preproc.discretization.caim2.modelcreator;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.swing.JPopupMenu;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.knime.base.node.preproc.discretization.caim2.DiscretizationModel;
import org.knime.base.node.preproc.discretization.caim2.DiscretizationScheme;
import org.knime.base.node.viz.plotter.AbstractPlotter;
import org.knime.base.node.viz.plotter.columns.MultiColumnPlotterProperties;
import org.knime.base.util.coordinate.Coordinate;
import org.knime.base.util.coordinate.DoubleCoordinate;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnDomainCreator;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.property.hilite.KeyEvent;
import org.knime.core.node.util.ColumnFilterPanel;

/**
 * This plotter draws a {@link DiscretizationModel}.
 *
 * @author Christoph Sieb, University of Konstanz
 */
public class BinModelPlotter extends AbstractPlotter {

    private Set<String> m_selectedColumns;

    private List<Coordinate> m_coordinates;

    private int m_hMargin;

    private int m_vMargin;

    private int m_columnDisplayHeight = 80;

    /**
     * The {@link DiscretizationModel} to visualize.
     */
    private DiscretizationModel m_discretizationModel;

    private DataTableSpec m_binnedColumnsSpec;

    /**
     * Creates a bin model plotter.
     */
    public BinModelPlotter() {
        super(new BinModelDrawingPane(), new MultiColumnPlotterProperties());
        /* ------------- listener ------------ */
        // colunm selection
        final ColumnFilterPanel colFilter =
                ((MultiColumnPlotterProperties)getProperties()).getColumnFilter();
        colFilter.addChangeListener(new ChangeListener() {
            /**
             * {@inheritDoc}
             */
            public void stateChanged(final ChangeEvent e) {
                m_selectedColumns = colFilter.getIncludedColumnSet();
                updatePaintModel();
                repaint();
            }
        });
        setAntialiasing(true);
        removeMouseListener(AbstractPlotter.SelectionMouseListener.class);
    }

    /**
     * Sets the {@link DiscretizationModel} to be visulized by this view.
     *
     * @param model the {@link DiscretizationModel} to visualize
     */
    public void setDiscretizationModel(final DiscretizationModel model) {
        m_discretizationModel = model;

        // create a spec for the binned columns; used for the column selection
        // panel
        if (model == null) {
            return;
        }
        String[] binnedColumnNames = model.getIncludedColumnNames();
        DataColumnSpec[] columnSpecs =
                new DataColumnSpec[binnedColumnNames.length];
        for (int i = 0; i < columnSpecs.length; i++) {
            columnSpecs[i] =
                    new DataColumnSpecCreator(binnedColumnNames[i],
                            StringCell.TYPE).createSpec();
        }

        m_binnedColumnsSpec = new DataTableSpec(columnSpecs);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void reset() {
        m_selectedColumns = null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized void updatePaintModel() {

        if (m_discretizationModel == null) {
            return;
        }

        // clear the drawing pane
        ((BinModelDrawingPane)getDrawingPane()).setBinningSchemes(null);

        // get the first columns
        if (m_selectedColumns == null) {
            m_selectedColumns = new LinkedHashSet<String>();
            String[] binnedColumnNames =
                    m_discretizationModel.getIncludedColumnNames();
            for (int i = 0; i < binnedColumnNames.length; i++) {
                // add them to the selected columns
                m_selectedColumns.add(binnedColumnNames[i]);
            }
            ((MultiColumnPlotterProperties)getProperties()).updateColumnSelection(
                    m_binnedColumnsSpec, m_selectedColumns);
        }

        if (m_selectedColumns.size() == 0) {
            getDrawingPane().repaint();
            return;
        }

        Set<DataCell> selectedColumnCells = new LinkedHashSet<DataCell>();
        m_coordinates = new ArrayList<Coordinate>();
        List<Integer> columnIndices = new ArrayList<Integer>();
        for (String name : m_selectedColumns) {
            int idx = m_binnedColumnsSpec.findColumnIndex(name);
            if (idx >= 0) {
                selectedColumnCells.add(new StringCell(name));
                DataColumnSpec colSpec = m_binnedColumnsSpec.getColumnSpec(idx);
                columnIndices.add(idx);
                Coordinate coordinate = Coordinate.createCoordinate(colSpec);
                m_coordinates.add(coordinate);
            }
        }

        // get the binning schemes for the selected columns
        DiscretizationScheme[] selectedSchemes = getSelectedSchemes();
        String[] selectedColumnNames = getSelectedColumnNames();

        // calculate the display coordinates for the drawing pane
        BinRuler[] binRulers = new BinRuler[selectedSchemes.length];

        // determine the width available for a bin ruler
        int rulerWidth = getDrawingPaneDimension().width - 2 * m_hMargin;

        // set the height of the plotter dependent on the number of bin models
        // to display
        // TODO: do it

        for (int i = 0; i < selectedSchemes.length; i++) {
            double[] bounds = selectedSchemes[i].getBounds();
            double min = bounds[0];
            double max = bounds[bounds.length - 1];
            // first create a colum spec from the schemes
            DataColumnSpecCreator columnSpecCreator =
                    new DataColumnSpecCreator("", DoubleCell.TYPE);
            columnSpecCreator.setDomain(new DataColumnDomainCreator(
                    new DoubleCell(min), new DoubleCell(max)).createDomain());
            DoubleCoordinate coordinate =
                    (DoubleCoordinate)Coordinate
                            .createCoordinate(columnSpecCreator.createSpec());

            Point leftStart =
                    new Point(m_hMargin, m_vMargin + (i + 1)
                            * m_columnDisplayHeight);

            int[] binPositions = new int[bounds.length];
            String[] binLabels = new String[bounds.length];
            int count = 0;
            for (double bound : bounds) {
                binPositions[count] =
                        (int)coordinate.calculateMappedValue(new DoubleCell(
                                bound), rulerWidth, true);
                binLabels[count] = coordinate.formatNumber(bounds[count]);

                count++;
            }

            binRulers[i] =
                    new BinRuler(leftStart, rulerWidth, binPositions,
                            binLabels, selectedColumnNames[i]);
        }

        ((BinModelDrawingPane)getDrawingPane()).setBinningSchemes(binRulers);

        m_hMargin = 10;
        m_vMargin = 10;
        ((BinModelDrawingPane)getDrawingPane()).setHorizontalMargin(m_hMargin);
        setHeight(
                binRulers[binRulers.length - 1]
                        .getLeftStartPoint().y + 40);

    }


    /**
     * Creates an array of {@link DiscretizationScheme}s that contains all
     * schemes for the selected columns.
     *
     * @return the selected discretization schemes
     */
    private DiscretizationScheme[] getSelectedSchemes() {

        String[] includedColumns =
                m_discretizationModel.getIncludedColumnNames();
        DiscretizationScheme[] result =
                new DiscretizationScheme[m_selectedColumns.size()];
        int counter = 0;
        for (String column : m_selectedColumns) {

            for (int i = 0; i < includedColumns.length; i++) {
                if (includedColumns[i].equals(column)) {
                    result[counter] = m_discretizationModel.getSchemes()[i];
                    counter++;
                }
            }
        }

        return result;
    }

    /**
     * Creates an array of {@link String}s that contains all column names for
     * the selected columns.
     *
     * @return the selected discretization schemes
     */
    private String[] getSelectedColumnNames() {

        String[] includedColumns =
                m_discretizationModel.getIncludedColumnNames();
        String[] result = new String[m_selectedColumns.size()];
        int counter = 0;
        for (String column : m_selectedColumns) {

            for (int i = 0; i < includedColumns.length; i++) {
                if (includedColumns[i].equals(column)) {
                    result[counter] = includedColumns[i];
                    counter++;
                }
            }
        }

        return result;
    }



    @Override
	public void fillPopupMenu(final JPopupMenu popupMenu) {
		// let popup menu empty
	}

	/**
	 * {@inheritDoc}
	 */
    @Override
    public void updateSize() {
        updatePaintModel();
    }

	@Override
	public void clearSelection() {
		// no selection supported
	}

	@Override
	public void hiLite(final KeyEvent event) {
		// no hilite supported
	}

	@Override
	public void hiLiteSelected() {
		// no hilite supported
	}

	@Override
	public void selectClickedElement(final Point clicked) {
		// no selection supported
	}

	@Override
	public void selectElementsIn(final Rectangle selectionRectangle) {
		// no selection supported
	}

	@Override
	public void unHiLite(final KeyEvent event) {
		// no hilite supported
	}

	@Override
	public void unHiLiteSelected() {
		// no hilite supported
	}

	public void unHiLiteAll(final KeyEvent event) {
		// no hilite supported
	}

}
