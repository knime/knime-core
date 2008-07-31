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
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.config.ConfigRO;
import org.knime.core.node.config.ConfigWO;

/**
 * Internal color model used to load, save, validate color settings and 
 * configure, execute  {@link ColorManagerNodeModel} and 
 * {@link ColorAppender2NodeModel} nodes.
 * 
 * @see ColorHandler
 * 
 * @author Thomas Gabriel, University of Konstanz
 */
class ColorNodeModel {
    
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

    /** true if color ranges, false for discrete colors. */
    private boolean m_isNominal;

    private static final DataCell MIN_VALUE = new StringCell("min_value");

    private static final DataCell MAX_VALUE = new StringCell("max_value");

    /**
     * Creates a new model for mapping colors. The model has one input and no
     * output.
     */
    ColorNodeModel() {
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
     */
    BufferedDataTable[] execute(final BufferedDataTable[] data,
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
        BufferedDataTable changedSpecTable = exec.createSpecReplacerTable(
                data[INPORT], createNewSpec(inSpec, columnIndex, colorHdl));
        // return original table with ColorHandler
        return new BufferedDataTable[]{changedSpecTable};
    }
    
    private DataTableSpec createNewSpec(final DataTableSpec inSpec,
            final int columnIndex, final ColorHandler colorHdl) {
        DataColumnSpec[] newColSpecs = 
            new DataColumnSpec[inSpec.getNumColumns()];
        for (int i = 0; i < newColSpecs.length; i++) {
            DataColumnSpecCreator dtsCont = 
                new DataColumnSpecCreator(inSpec.getColumnSpec(i));
            if (i == columnIndex) {
                dtsCont.setColorHandler(colorHdl);
            } else {
                dtsCont.setColorHandler(null);
            }
            newColSpecs[i] = dtsCont.createSpec();
        }
        return new DataTableSpec(newColSpecs);
    }
    
    /**
     * @return the selected column or <code>null</code> if none
     */
    String getSelectedColumn() {
        return m_column;
    }
    
    /**
     * Sets a (new) selected column name to which the color settings
     * are applied. 
     * @param column The column name or null.
     */
    void setSelectedColumn(final String column) {
        m_column = column;
    }

    /**
     * @param inSpecs the input specs passed to the output port
     * @return the same as the input spec
     * 
     * @throws InvalidSettingsException if a column is not available
     */
    DataTableSpec[] configure(final DataTableSpec[] inSpecs) 
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
        // either set colors by ranges or discrete values
        if (m_isNominal) {
            // check if all values set are in the domain of the column spec
            Set<DataCell> list = domain.getValues();
            if (list == null) {
                throw new InvalidSettingsException("Column " + m_column
                        + " has no nominal values set:\n"
                        + "execute predecessor or add Binner.");
            }
            // check if the mapping's values and the possible values match
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
        int columnIndex = inSpecs[INPORT].findColumnIndex(m_column);
        // create new column spec based on color settings
        DataColumnSpec cspec = inSpecs[INPORT].getColumnSpec(m_column);
        // will be set in final table
        ColorHandler colorHdl;
        if (m_isNominal) {
            colorHdl = createNominalColorHandler(m_map);
        } else {
            DataColumnDomain dom = cspec.getDomain();
            DataCell lower = null;
            DataCell upper = null;
            if (dom.hasBounds()) {
                lower = dom.getLowerBound();
                upper = dom.getUpperBound();
            }
            colorHdl = createRangeColorHandler(lower, upper, m_map);
        }
        return new DataTableSpec[]{
                createNewSpec(inSpecs[INPORT], columnIndex, colorHdl)};
    }

    /**
     * Load color settings.
     * @param settings Used to read color settings from.
     * @throws InvalidSettingsException If a color property with the settings
     *         is invalid.
     */
    void loadValidatedSettingsFrom(final ConfigRO settings)
            throws InvalidSettingsException {
        assert (settings != null);
        // remove all color mappings
        resetColorMapping();
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
     * Save color settings. 
     * @param settings Used to write color settings into.
     */
    void saveSettingsTo(final ConfigWO settings) {
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
    void validateSettings(final ConfigRO settings) 
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
    
    /**
     * Resets the color mapping.
     */
    void resetColorMapping() {
        m_map.clear();
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
}
