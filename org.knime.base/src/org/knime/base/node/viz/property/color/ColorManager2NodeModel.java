/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2010
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
import org.knime.core.node.NodeModel;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;
import org.knime.core.node.port.viewproperty.ColorHandlerPortObject;

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

    /** The selected column. */
    private String m_column;
    /** true if color ranges, false for discrete colors. */
    private boolean m_isNominal;
    /** Stores the mapping from string column value to color. */
    private final Map<DataCell, ColorAttr> m_map;

    /** The selected column. */
    private String m_columnGuess;
    /** true if color ranges, false for discrete colors. */
    private boolean m_isNominalGuess;
    /** Stores the mapping from string column value to color. */
    private final Map<DataCell, ColorAttr> m_mapGuess;

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


    /** Key for minimum color value. */
    private static final DataCell MIN_VALUE = new StringCell("min_value");

    /** Key for maximum color value. */
    private static final DataCell MAX_VALUE = new StringCell("max_value");
    
    /**
     * Creates a new model for mapping colors. The model has one input and two
     * outputs.
     * 
     */
    public ColorManager2NodeModel() {
        super(new PortType[]{BufferedDataTable.TYPE}, new PortType[]{
                BufferedDataTable.TYPE, ColorHandlerPortObject.TYPE});
        m_map = new LinkedHashMap<DataCell, ColorAttr>();
        m_mapGuess = new LinkedHashMap<DataCell, ColorAttr>();
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
    @Override
    protected PortObject[] execute(final PortObject[] data,
            final ExecutionContext exec) throws CanceledExecutionException {
        assert (data != null && data.length == 1 && data[INPORT] != null);
        BufferedDataTable in = (BufferedDataTable)data[0];
        DataTableSpec inSpec = in.getDataTableSpec();
        ColorHandler colorHandler;
        // if no column has been selected, guess first nominal column
        if (m_column == null) {
            // find first nominal column with possible values
            String column = 
                DataTableSpec.guessNominalClassColumn(inSpec, false);
            m_columnGuess = column;
            m_isNominalGuess = true;
            super.setWarningMessage(
                    "Selected column \"" + column
                    + "\" with default nominal color mapping.");
            Set<DataCell> set = 
                inSpec.getColumnSpec(column).getDomain().getValues();
            m_mapGuess.clear();
            m_mapGuess.putAll(
                    ColorManager2DialogNominal.createColorMapping(set));
            colorHandler = createNominalColorHandler(m_mapGuess);
            DataTableSpec newSpec = getOutSpec(inSpec, column, colorHandler);
            BufferedDataTable changedSpecTable = 
                exec.createSpecReplacerTable(in, newSpec);
            DataTableSpec modelSpec = 
                new DataTableSpec(newSpec.getColumnSpec(column));
            ColorHandlerPortObject viewModel = new ColorHandlerPortObject(
                    modelSpec, colorHandler.toString() 
                    + " based on column \"" + m_column + "\"");
            return new PortObject[]{changedSpecTable, viewModel};
        }
        // find column index
        int columnIndex = inSpec.findColumnIndex(m_column);
        // create new column spec based on color settings
        DataColumnSpec cspec = inSpec.getColumnSpec(m_column);
        if (m_isNominal) {
            colorHandler = createNominalColorHandler(m_map);
        } else {
            DataColumnDomain dom = cspec.getDomain();
            DataCell lower, upper;
            if (dom.hasBounds()) {
                lower = dom.getLowerBound();
                upper = dom.getUpperBound();
            } else {
                StatisticsTable stat = new StatisticsTable(in, exec);
                lower = stat.getMin(columnIndex);
                upper = stat.getMax(columnIndex);
            }
            colorHandler = createRangeColorHandler(lower, upper, m_map);
        }
        DataTableSpec newSpec =
            getOutSpec(inSpec, m_column, colorHandler);
        DataTableSpec modelSpec = 
            new DataTableSpec(newSpec.getColumnSpec(m_column));
        BufferedDataTable changedSpecTable = 
            exec.createSpecReplacerTable(in, newSpec);
        ColorHandlerPortObject viewModel = new ColorHandlerPortObject(
                modelSpec, "Coloring on \"" + m_column + "\"");
        return new PortObject[]{changedSpecTable, viewModel};
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
    static final DataTableSpec getOutSpec(final DataTableSpec spec, 
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
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs) 
            throws InvalidSettingsException {
        DataTableSpec spec = (DataTableSpec)inSpecs[INPORT];
        if (spec == null) {
            throw new InvalidSettingsException("No input");
        }
        // check null column
        if (m_column == null) {
            // find first nominal column with possible values
            String column = DataTableSpec.guessNominalClassColumn(spec, false);
            if (column == null) {
                throw new InvalidSettingsException(
                    "No column selected and no categorical column available.");
            }
            m_columnGuess = column;
            m_isNominalGuess = true;
            Set<DataCell> set = spec.getColumnSpec(column).
                    getDomain().getValues();
            m_mapGuess.clear();
            m_mapGuess.putAll(
                    ColorManager2DialogNominal.createColorMapping(set));
            ColorHandler colorHandler = createNominalColorHandler(m_mapGuess);
            DataTableSpec dataSpec = getOutSpec(spec, column, colorHandler);
            DataTableSpec modelSpec = 
                new DataTableSpec(dataSpec.getColumnSpec(column));
            super.setWarningMessage(
                    "Selected column \"" + column
                    + "\" with default nominal color mapping.");
            return new DataTableSpec[]{dataSpec, modelSpec};
        }
        // check column in spec
        if (!spec.containsName(m_column)) {
            throw new InvalidSettingsException("Column \"" + m_column
                    + "\" not found.");
        }
        // get domain
        DataColumnDomain domain = spec.getColumnSpec(m_column).getDomain();
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
                throw new InvalidSettingsException(
                       "Color mapping does not match possible values.");
            }
        } else { // range
            // check if double column is selected
            if (!spec.getColumnSpec(m_column).getType()
                    .isCompatible(DoubleValue.class)) {
                throw new InvalidSettingsException("Column is not valid for"
                        + " range color settings: "
                        + spec.getColumnSpec(m_column).getType());
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
        DataColumnSpec cspec = spec.getColumnSpec(m_column);
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
        DataTableSpec dataSpec = getOutSpec(spec, m_column, colorHandler);
        DataTableSpec modelSpec = 
            new DataTableSpec(dataSpec.getColumnSpec(m_column));
        return new DataTableSpec[]{dataSpec, modelSpec};
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
        m_column = settings.getString(SELECTED_COLUMN);
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
        } else {
            if (m_columnGuess != null) {
                assert (m_isNominalGuess);
                settings.addString(SELECTED_COLUMN, m_columnGuess);
                settings.addBoolean(IS_NOMINAL, m_isNominalGuess);
                DataCell[] values = new DataCell[m_mapGuess.size()];
                int id = -1;
                for (DataCell c : m_mapGuess.keySet()) {
                    settings.addInt(c.toString(), m_mapGuess.get(c).getColor()
                            .getRGB());
                    values[++id] = c;
                }
                settings.addDataCellArray(VALUES, values);
            } else {
                settings.addString(SELECTED_COLUMN, m_column);
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
        String column = settings.getString(SELECTED_COLUMN);
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

    /** {@inheritDoc} */
    @Override
    protected void reset() {
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
    
}
