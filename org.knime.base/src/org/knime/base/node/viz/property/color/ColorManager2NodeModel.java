/*
 * --------------------------------------------------------------------- *
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
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;

/**
 * Model used to set colors either based on the nominal values or ranges
 * (bounds) retrieved from the {@link org.knime.core.data.DataColumnSpec}.
 * The created {@link org.knime.core.data.property.ColorHandler} is then
 * set in the column spec.
 * 
 * @see ColorManager2NodeDialogPane
 * 
 * @author Thomas Gabriel, University of Konstanz
 */
class ColorManager2NodeModel extends NodeModel {
        
    /** Logger for this package. */
    static final NodeLogger LOGGER = NodeLogger.getLogger("Color Manager");

    /** Stores the mapping from string column value to color. */
    private final Map<DataCell, ColorAttr> m_map;

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

    /** true if color ranges, false for discrete colors. */
    private boolean m_isNominal;

    /** Key for minimum color value. */
    private static final DataCell MIN_VALUE = new StringCell("min_value");

    /** Key for maximum color value. */
    private static final DataCell MAX_VALUE = new StringCell("max_value");
    
    /** ColorHandler generated during executed and save into the model port. */
    private ColorHandler m_colorHandler;

    /**
     * Is invoked during the node's execution to make the color settings.
     * 
     * @param data the input data array
     * @param exec the execution monitor
     * @return the same input data table whereby the RowKeys contain color info
     *         now
     * @throws CanceledExecutionException if user canceled execution
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] data,
            final ExecutionContext exec) throws CanceledExecutionException {
        assert (data != null && data.length == 1 && data[INPORT] != null);
        DataTableSpec inSpec = data[INPORT].getDataTableSpec();
        // if no column has been selected, guess first nominal column
        if (m_column == null) {
            // find first nominal column with possible values
            String column = DataTableSpec.guessNominalClassColumn(inSpec, true);
            super.setWarningMessage(
                    "Selected column \"" + column
                    + "\" with default nominal color mapping.");
            Set<DataCell> set = 
                inSpec.getColumnSpec(column).getDomain().getValues();
            m_colorHandler = createNominalColorHandler(
                    ColorManager2DialogNominal.createColorMapping(set));
            DataTableSpec newSpec =
                appendColorManager(inSpec, column, m_colorHandler);
            BufferedDataTable changedSpecTable = 
                exec.createSpecReplacerTable(data[INPORT], newSpec);
            // return original table with ColorHandler
            return new BufferedDataTable[]{changedSpecTable};
        }
        // find column index
        int columnIndex = inSpec.findColumnIndex(m_column);
        // create new column spec based on color settings
        DataColumnSpec cspec = inSpec.getColumnSpec(m_column);
        if (m_isNominal) {
            m_colorHandler = createNominalColorHandler(m_map);
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
            m_colorHandler = createRangeColorHandler(lower, upper, m_map);
        }
        BufferedDataTable changedSpecTable = exec.createSpecReplacerTable(
                data[INPORT], 
                appendColorManager(inSpec, m_column, m_colorHandler));
        // return original table with ColorHandler
        return new BufferedDataTable[]{changedSpecTable};
    }
    
    /**
     * Appends the given <code>ColorHandler</code> to the given 
     * <code>DataTableSpec</code> for the given column. If the spec
     * already contains a ColorHandler, it will be removed and replaced by
     * the new one.
     * @param spec to which the ColorHandler is appended
     * @param columnName for this column
     * @param colorHdl ColorHandler
     * @return a new spec with ColorHandler
     */
    static final DataTableSpec appendColorManager(final DataTableSpec spec,
            final String columnName, final ColorHandler colorHdl) {
        DataColumnSpec[] cspecs = new DataColumnSpec[spec.getNumColumns()];
        for (int i = 0; i < cspecs.length; i++) {
            DataColumnSpec cspec = spec.getColumnSpec(i);
            DataColumnSpecCreator cr = new DataColumnSpecCreator(cspec);
            if (cspec.getName().equals(columnName)) {
                cr.setColorHandler(colorHdl);
            } else {
                // delete other ColorHandler
                cr.setColorHandler(null);
            }
            cspecs[i] = cr.createSpec();
        }
        return new DataTableSpec(cspecs);
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
            // find first nominal column with possible values
            String column = DataTableSpec.guessNominalClassColumn(
                    inSpecs[INPORT], true);
            if (column == null) {
                throw new InvalidSettingsException("No column selected.");
            }
            super.setWarningMessage(
                    "Selected column \"" + column
                    + "\" with default nominal color mapping.");
            Set<DataCell> set = inSpecs[INPORT].getColumnSpec(column).
                    getDomain().getValues();
            ColorHandler colorHandler = createNominalColorHandler(
                    ColorManager2DialogNominal.createColorMapping(set));
            return new DataTableSpec[]{
                    appendColorManager(inSpecs[INPORT], column, colorHandler)};
        }
        // check column in spec
        if (!inSpecs[INPORT].containsName(m_column)) {
            throw new InvalidSettingsException("Column " + m_column
                    + " not found.");
        }
        // get domain
        DataColumnDomain domain = inSpecs[INPORT].getColumnSpec(m_column)
                .getDomain();
        // either set colors by ranges or discrete values
        if (m_isNominal) {
            // check if all values set are in the domain of the column spec
            Set<DataCell> list = domain.getValues();
            if (list == null) {
                throw new InvalidSettingsException("Column \"" + m_column + "\""
                        + " has no nominal values set: "
                        + "execute predecessor or add Binner.");
            }
            // check if the mapping values and the possible values match
            if (!m_map.keySet().containsAll(list)) {
                throw new InvalidSettingsException("Mapping does not match "
                        + "possible values in spec.");
            }
        } else { // range
            // check if double column is selected
            if (!inSpecs[INPORT].getColumnSpec(m_column).getType()
                    .isCompatible(DoubleValue.class)) {
                throw new InvalidSettingsException("Column is not valid for"
                        + " range color settings: "
                        + inSpecs[INPORT].getColumnSpec(m_column).getType());
            }
            // check map
            if (m_map.size() != 2) {
                throw new InvalidSettingsException(
                        "Color settings not yet available.");
            }   
        }
        
        // temp color handler
        ColorHandler colorHandler;
        
        // create new column spec based on color settings
        DataColumnSpec cspec = inSpecs[INPORT].getColumnSpec(m_column);
        if (m_isNominal) {
            colorHandler = createNominalColorHandler(m_map);
        } else {
            DataColumnDomain dom = cspec.getDomain();
            DataCell lower = null;
            DataCell upper = null;
            if (dom.hasBounds()) {
                lower = dom.getLowerBound();
                upper = dom.getUpperBound();
            }
            colorHandler = createRangeColorHandler(lower, upper, m_map);
        }
        return new DataTableSpec[]{
                appendColorManager(inSpecs[INPORT], m_column, colorHandler)};
    }

    /**
     * Load color settings.
     * @param settings Used to read color settings from.
     * @throws InvalidSettingsException If a color property with the settings
     *         is invalid.
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
                            settings.getInt(values[i].toString()), true)));
                }
            } else { // range
                // lower color
                Color c0 = new Color(settings.getInt(MIN_COLOR), true);
                m_map.put(MIN_VALUE, ColorAttr.getInstance(c0));
                // upper color
                Color c1 = new Color(settings.getInt(MAX_COLOR), true);
                m_map.put(MAX_VALUE, ColorAttr.getInstance(c1));
                if (c0.equals(c1)) {
                    LOGGER.info("Lower and upper color are equal: " + c0);
                }
            }
        }
    }

    /**
     * Save color settings. 
     * @param settings Used to write color settings into.
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        settings.addString(SELECTED_COLUMN, m_column);
        if (m_column != null) {
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
     * Validate the color settings, that are, column name must be available, as
     * well as, a color model either nominal or range that contains a color
     * mapping, from each possible value to a color or from min and max
     * value to color, respectively.
     * @param settings Color settings to validate.
     * @throws InvalidSettingsException If a color property read from the
     *         settings is invalid.
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
        double d0 = Double.NaN;
        if (lower != null && !lower.isMissing() 
                && lower.getType().isCompatible(DoubleValue.class)) {
            d0 = ((DoubleValue)lower).getDoubleValue();
        }
        double d1 = Double.NaN;
        if (upper != null && !upper.isMissing() 
                && upper.getType().isCompatible(DoubleValue.class)) {
            d1 = ((DoubleValue)upper).getDoubleValue();
        }
        return new ColorHandler(new ColorModelRange(d0, c0, d1, c1));
    }

    /**
     * Creates a new model for mapping colors. The model has one input and no
     * output.
     * 
     * @param dataIns number of data ins
     * @param dataOuts number of data outs
     * @param modelIns number of model ins
     * @param modelOuts number of model outs
     */
    public ColorManager2NodeModel(final int dataIns, final int dataOuts,
            final int modelIns, final int modelOuts) {
        super(dataIns, dataOuts, modelIns, modelOuts);
        m_map = new LinkedHashMap<DataCell, ColorAttr>();
    }

    /**
     * Resets all color' settings inside this model and the color handler which
     * will then inform the registered views about the changes.
     * 
     * @see NodeModel#reset()
     */
    @Override
    protected void reset() {
        m_colorHandler = null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File nodeInternDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File nodeInternDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveModelContent(final int index, 
            final ModelContentWO predParams) throws InvalidSettingsException {
        m_colorHandler.save(predParams);
    }
    
}
