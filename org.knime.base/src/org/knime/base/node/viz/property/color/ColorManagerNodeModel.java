/*
 * --------------------------------------------------------------------- *
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2006
 * University of Konstanz, Germany.
 * Chair for Bioinformatics and Information Mining
 * Prof. Dr. Michael R. Berthold
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
 * --------------------------------------------------------------------- *
 */
package org.knime.base.node.viz.property.color;

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.knime.base.data.statistics.StatisticsTable;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnDomain;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.def.StringCell;
import org.knime.core.data.property.ColorAttr;
import org.knime.core.data.property.ColorHandler;
import org.knime.core.data.property.ColorModelNominal;
import org.knime.core.data.property.ColorModelRange;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.ModelContentWO;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;

/**
 * Model used to set colors either based on the nominal values or ranges
 * (bounds) retrieved from the {@link org.knime.core.data.DataColumnSpec}.
 * The created {@link org.knime.core.data.property.ColorHandler} is then
 * set in the column spec.
 * 
 * @see ColorManagerNodeDialogPane
 * @see ColorHandler
 * 
 * @author Thomas Gabriel, University of Konstanz
 */
class ColorManagerNodeModel extends NodeModel {
    /** Logger for this package. */
    static final NodeLogger LOGGER = NodeLogger.getLogger("Color Manager");

    /** Stores the mapping from string column value to color. */
    private final LinkedHashMap<DataCell, ColorAttr> m_map;

    /** The selected column. */
    private String m_column;

    /** Keeps port number for the single input port. */
    static final int INPORT = 0;

    /** Keeps port number for the single input port. */
    static final int OUTPORT = 0;

    /** Keeps the selected column. */
    static final String SELECTED_COLUMN = "selected_column";

    /** The nominal column values. */
    static final String VALUES = "values";

    /** The minimum column value for range color settings. */
    static final String MIN_COLOR = "min_color";

    /** The maximum column value for range color settings. */
    static final String MAX_COLOR = "max_color";

    /** Type of color setting. */
    static final String IS_NOMINAL = "is_nominal";

    /** true if color ranges, false for descret colors. */
    private boolean m_isNominal;

    private static final DataCell MIN_VALUE = new StringCell("min_value");

    private static final DataCell MAX_VALUE = new StringCell("max_value");

    /**
     * Creates a new model for mapping colors. The model has one input and no
     * output.
     * 
     * @param dataIns number of data ins
     * @param dataOuts number of data outs
     * @param modelIns number of model ins
     * @param modelOuts number of model outs
     */
    ColorManagerNodeModel(final int dataIns, final int dataOuts,
            final int modelIns, final int modelOuts) {
        super(dataIns, dataOuts, modelIns, modelOuts);
        m_map = new LinkedHashMap<DataCell, ColorAttr>();
    }

    /**
     * Is invoked during the node's execution to make the color settings.
     * 
     * @param data the input data array
     * @param exec the execution monitor
     * @return the same input data table whereby the RowKeys contain color info
     *         now
     * @throws CanceledExecutionException if user canceled execution
     * 
     * @see NodeModel#execute(BufferedDataTable[],ExecutionContext)
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] data,
            final ExecutionContext exec) throws CanceledExecutionException {
        assert (data != null && data.length == 1 && data[INPORT] != null);
        // find selected column index
        DataTableSpec inSpec = data[INPORT].getDataTableSpec();
        int columnIndex = inSpec.findColumnIndex(m_column);
        // create new column spec based on color settings
        DataColumnSpec cspec = inSpec.getColumnSpec(m_column);
        // will be set in final table
        ColorHandler colorHdl;
        if (m_isNominal) {
            colorHdl = createNominalColorHandler(m_map);
        } else {
            DataColumnDomain dom = cspec.getDomain();
            DataCell lower, upper;
            if (dom.hasBounds()) {
                lower = dom.getLowerBound();
                upper = dom.getUpperBound();
            } else {
                StatisticsTable stat = new StatisticsTable(data[INPORT], exec);
                lower = stat.getMin(columnIndex);
                upper = stat.getMax(columnIndex);
            }
            colorHdl = createRangeColorHandler(lower, upper, m_map);
        }
        DataColumnSpec[] newColSpecs = new DataColumnSpec[inSpec
                .getNumColumns()];
        for (int i = 0; i < newColSpecs.length; i++) {
            DataColumnSpecCreator dtsCont = new DataColumnSpecCreator(inSpec
                    .getColumnSpec(i));
            if (i == columnIndex) {
                dtsCont.setColorHandler(colorHdl);
            } else {
                dtsCont.setColorHandler(null);
            }
            newColSpecs[i] = dtsCont.createSpec();
        }
        final DataTableSpec newSpec = new DataTableSpec(newColSpecs);
        BufferedDataTable changedSpecTable = exec.createSpecReplacerTable(
                data[INPORT], newSpec);
        // return original table with ColorHandler
        return new BufferedDataTable[]{changedSpecTable};
    }

    /**
     * Saves the color settings to <code>ModelContent</code> object.
     * 
     * @see NodeModel#saveModelContent(int, ModelContentWO)
     */
    @Override
    protected void saveModelContent(final int index,
            final ModelContentWO predParams) throws InvalidSettingsException {
        assert index == 0;
        if (predParams != null) {
            NodeSettings settings = new NodeSettings(predParams.getKey());
            saveSettingsTo(settings);
            settings.copyTo(predParams);
        }
    }

    /**
     * @return the selected column or <code>null</code> if none
     */
    protected String getSelectedColumn() {
        return m_column;
    }

    /**
     * Resets all color' settings inside this model and the color handler which
     * will then inform the registered views about the changes.
     * 
     * @see NodeModel#reset()
     */
    @Override
    protected void reset() {

    }

    /**
     * @see org.knime.core.node.NodeModel#loadInternals(File,
     *      ExecutionMonitor)
     */
    @Override
    protected void loadInternals(final File nodeInternDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {

    }

    /**
     * @see org.knime.core.node.NodeModel#saveInternals(java.io.File,
     *      ExecutionMonitor)
     */
    @Override
    protected void saveInternals(final File nodeInternDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {

    }

    /**
     * @param inSpecs the input specs passed to the output port
     * @return the same as the input spec
     * 
     * @throws InvalidSettingsException if a column is not available
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
            throws InvalidSettingsException {
        assert (inSpecs.length == 1);
        // check null column
        if (m_column == null) {
            throw new InvalidSettingsException("No column selected.");
        }
        // check column in spec
        if (!inSpecs[INPORT].containsName(m_column)) {
            throw new InvalidSettingsException("Column " + m_column
                    + " not found.");
        }
        // get domain
        DataColumnDomain domain = inSpecs[INPORT].getColumnSpec(m_column)
                .getDomain();
        // either set colors by ranges or descret values
        if (m_isNominal) {
            // check if all values set are in the domain of the column spec
            Set<DataCell> list = domain.getValues();
            if (list == null) {
                throw new InvalidSettingsException("Column " + m_column
                        + " has no nominal values set:\n"
                        + "execute predecessor or add Binner.");
            }
            // check if the mapping's values and the poss values match
            if (!m_map.keySet().containsAll(list)) {
                throw new InvalidSettingsException("Mapping does not match "
                        + "possible values in spec.");
            }
        } else { // range
            // check if double column is selcted
            if (!inSpecs[INPORT].getColumnSpec(m_column).getType()
                    .isCompatible(DoubleValue.class)) {
                throw new InvalidSettingsException("Column is not valid for"
                        + " range color settings: "
                        + inSpecs[INPORT].getColumnSpec(m_column).getType());
            }
        }
        return new DataTableSpec[]{inSpecs[INPORT]};
    }

    /**
     * @see NodeModel#loadValidatedSettingsFrom(NodeSettingsRO)
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        assert (settings != null);
        // remove all color mappings
        m_map.clear();
        // read settings and write into the map
        m_column = settings.getString(SELECTED_COLUMN, null);
        if (m_column != null) {
            m_isNominal = settings.getBoolean(IS_NOMINAL);
            // nominal
            if (m_isNominal) {
                DataCell[] values = settings.getDataCellArray(VALUES,
                        new DataCell[0]);
                for (int i = 0; i < values.length; i++) {
                    m_map.put(values[i], ColorAttr.getInstance(new Color(
                            settings.getInt(values[i].toString()))));
                }
            } else { // range
                // lower color
                Color c0 = new Color(settings.getInt(MIN_COLOR));
                m_map.put(MIN_VALUE, ColorAttr.getInstance(c0));
                // upper color
                Color c1 = new Color(settings.getInt(MAX_COLOR));
                m_map.put(MAX_VALUE, ColorAttr.getInstance(c1));
                if (c0.equals(c1)) {
                    LOGGER.info("Lower and upper color are equal: " + c0);
                }
            }
        }
    }

    /**
     * @see NodeModel#saveSettingsTo(NodeSettingsWO)
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        if (m_column != null) {
            settings.addString(SELECTED_COLUMN, m_column);
            settings.addBoolean(IS_NOMINAL, m_isNominal);
            // nominal
            if (m_isNominal) {
                DataCell[] values = new DataCell[m_map.size()];
                int id = -1;
                for (DataCell c : m_map.keySet()) {
                    settings.addInt(c.toString(), m_map.get(c).getColor()
                            .getRGB());
                    values[++id] = c;
                }
                settings.addDataCellArray(VALUES, values);
            } else { // range
                assert m_map.size() == 2;
                settings.addInt(MIN_COLOR, m_map.get(MIN_VALUE).getColor()
                        .getRGB());
                settings.addInt(MAX_COLOR, m_map.get(MAX_VALUE).getColor()
                        .getRGB());
            }
        }
    }

    /**
     * @see NodeModel#validateSettings(NodeSettingsRO)
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        String column = settings.getString(SELECTED_COLUMN, null);
        if (column != null) {
            boolean nominalSelected = settings.getBoolean(IS_NOMINAL);
            if (nominalSelected) {
                DataCell[] values = settings.getDataCellArray(VALUES,
                        new DataCell[0]);
                for (int i = 0; i < values.length; i++) {
                    new Color(settings.getInt(values[i].toString()));
                }
            } else {
                new Color(settings.getInt(MIN_COLOR));
                new Color(settings.getInt(MAX_COLOR));
            }
        }
    }

    private static final ColorHandler createNominalColorHandler(
            final Map<DataCell, ColorAttr> map) {
        return new ColorHandler(new ColorModelNominal(map));
    }

    private static final ColorHandler createRangeColorHandler(
            final DataCell lower, final DataCell upper,
            final Map<DataCell, ColorAttr> map) {
        assert map.size() == 2;
        Color c0 = map.get(MIN_VALUE).getColor();
        Color c1 = map.get(MAX_VALUE).getColor();
        double d0 = ((DoubleValue)lower).getDoubleValue();
        double d1 = ((DoubleValue)upper).getDoubleValue();
        return new ColorHandler(new ColorModelRange(d0, c0, d1, c1));
    }
}
