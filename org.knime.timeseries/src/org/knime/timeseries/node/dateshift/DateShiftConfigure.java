/*
 * ------------------------------------------------------------------------
 *  Copyright by KNIME GmbH, Konstanz, Germany
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
 * ------------------------------------------------------------------------
 */
package org.knime.timeseries.node.dateshift;

import java.util.TimeZone;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.data.container.SingleCellFactory;
import org.knime.core.data.date.DateAndTimeCell;
import org.knime.core.data.date.DateAndTimeValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.timeseries.node.diff.Granularity;
import org.knime.timeseries.util.SettingsModelCalendar;

/**
 * The configuration object for the Date/Time Shift node model.
 *
 * @author Iris Adae, Universitaet Konstanz
 */
public class DateShiftConfigure {

    // numerical column
    private final SettingsModelString m_numcol = createNumColmodel();

    /**
     * @return the settings model for the numerical column.
     */
    public SettingsModelString getNumColumnModel() {
        return m_numcol;
    }

    //  date column
    private final SettingsModelString m_dateCol = createDateColumnModel();

    /**
     * @return the settings model for the second column (Date time).
     */
    public SettingsModelString getDateColumnModel() {
        return m_dateCol;
    }

    // new column name
    private final SettingsModelString m_newColName = createNewColNameModel();

    /**
     * @return the settingsmodel for the new column name.
     */
    public SettingsModelString getNewColumnName() {
        return m_newColName;
    }

    // selected granularity level
    private final SettingsModelString m_granularity = createGranularityModel();

    /**
     * @return the settings model for the granularity.
     */
    public SettingsModelString getGranularity() {
        return m_granularity;
    }

    private final SettingsModelString m_typeofreference = getReferenceTypeModel();

    /**
     * @return the settings model for the type.
     */
    public SettingsModelString gettypeofreference() {
        return m_typeofreference;
    }

    private final SettingsModelCalendar m_timemodel = getCalendarModel();

    /**
     * @return the settings model for the calender.
     */
    public SettingsModelCalendar getTimeModel() {
        return m_timemodel;
    }

    private final SettingsModelBoolean m_hasDate = createHasDateModel();

    /**
     * @return the settings model for using date.
     */
    public SettingsModelBoolean getHasDate() {
        return m_hasDate;
    }

    private final SettingsModelBoolean m_hasTime = createHasTimeModel();

    /**
     * @return the settings model for using time.
     */
    public SettingsModelBoolean getHasTime() {
        return m_hasTime;
    }

    private final SettingsModelBoolean m_hasMiliSeconds = createHasMiliSecondsModel();

    /**
     * @return the settings model for using milliseconds.
     */
    public SettingsModelBoolean getHasMiliSeconds() {
        return m_hasMiliSeconds;
    }

    private static final String CFG_COL1 = "column.lower";

    private static final String CFG_COL2 = "column.upper";

    private static final String CFG_NEW_COL_NAME = "new.column.name";

    private static final String CFG_GRANULARITY = "granularity";

    private static final String CFG_REF_TYPE = "seconddate.reference.type";

    private static final String CFG_TIME = "cfg.time";

    /*
     * Models...
     */

    /**
     * @return settings model for the selected time
     */
    public static SettingsModelCalendar getCalendarModel() {
        return new SettingsModelCalendar(CFG_TIME, null);
    }

    /**
     * @return settings model for the type of the used reference column.
     */
    public static SettingsModelString getReferenceTypeModel() {
        return new SettingsModelString(CFG_REF_TYPE, DateShiftNodeDialog.CFG_NOW);
    }

    /**
     * @return settings model for the first time column
     */
    public static SettingsModelString createNumColmodel() {
        return new SettingsModelString(CFG_COL1, "");
    }

    /**
     *
     * @return settings model for the second time column
     */
    public static SettingsModelString createDateColumnModel() {
        return new SettingsModelString(CFG_COL2, "");
    }

    /**
     *
     * @return settings model for the new column name
     */
    public static SettingsModelString createNewColNameModel() {
        return new SettingsModelString(CFG_NEW_COL_NAME, "ShiftDate");
    }

    /**
     * @return settings model for the granularity
     */
    public static SettingsModelString createGranularityModel() {
        return new SettingsModelString(CFG_GRANULARITY, Granularity.DAY.getName());
    }

    /**
     * @return settings model to have a date.
     */
    public static SettingsModelBoolean createHasDateModel() {
        return new SettingsModelBoolean("Has_DATE", true);
    }

    /**
     * @return settings model to have a TIME.
     */
    public static SettingsModelBoolean createHasTimeModel() {
        return new SettingsModelBoolean("Has_TIME", true);
    }

    /**
     * @return settings model to have a MiliSeconds.
     */
    public static SettingsModelBoolean createHasMiliSecondsModel() {
        return new SettingsModelBoolean("Has_MiliSeconds", true);
    }

    /**
     *
     * @param settings the node settings object.
     * @throws InvalidSettingsException if the settings cannot be validated.
     */
    public void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_numcol.validateSettings(settings);
        m_dateCol.validateSettings(settings);
        m_newColName.validateSettings(settings);
        m_granularity.validateSettings(settings);
        m_typeofreference.validateSettings(settings);
        m_timemodel.validateSettings(settings);
        m_hasDate.validateSettings(settings);
        m_hasMiliSeconds.validateSettings(settings);
        m_hasTime.validateSettings(settings);

    }

    /**
     *
     * @param settings the node settings.
     */
    public void saveSettingsTo(final NodeSettingsWO settings) {
        m_numcol.saveSettingsTo(settings);
        m_dateCol.saveSettingsTo(settings);
        m_newColName.saveSettingsTo(settings);
        m_granularity.saveSettingsTo(settings);
        m_typeofreference.saveSettingsTo(settings);
        m_timemodel.saveSettingsTo(settings);
        m_hasDate.saveSettingsTo(settings);
        m_hasMiliSeconds.saveSettingsTo(settings);
        m_hasTime.saveSettingsTo(settings);
    }

    /**
     *
     * @param settings the node settings.
     * @throws InvalidSettingsException if the settings cannot be load.
     */
    public void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_numcol.loadSettingsFrom(settings);
        m_dateCol.loadSettingsFrom(settings);
        m_newColName.loadSettingsFrom(settings);
        m_granularity.loadSettingsFrom(settings);
        m_typeofreference.loadSettingsFrom(settings);
        m_timemodel.loadSettingsFrom(settings);
        m_hasDate.loadSettingsFrom(settings);
        m_hasMiliSeconds.loadSettingsFrom(settings);
        m_hasTime.loadSettingsFrom(settings);
    }

    /**
     *
     * @param conf the configuration object
     * @param spec the input data table spec
     * @return the column rearranger as defied in the configuration object.
     */
    public static ColumnRearranger getTimeToValueRearranger(final DateShiftConfigure conf, final DataTableSpec spec) {

        int col1Idx = spec.findColumnIndex(conf.getNumColumnModel().getStringValue());
        int col2Idx = spec.findColumnIndex(conf.getDateColumnModel().getStringValue());

        ColumnRearranger rearranger = new ColumnRearranger(spec);
        final Granularity g = Granularity.valueOf(conf.getGranularity().getStringValue());

        String typeofref = conf.gettypeofreference().getStringValue();
        if (typeofref.equals(DateShiftNodeDialog.CFG_COLUMN)) {
            // append the new column with single cell factory
            rearranger.append(getColumnbasedCellFactory(spec, col1Idx, col2Idx, g.getFactor(), conf));
        } else if (typeofref.equals(DateShiftNodeDialog.CFG_11970)) {
            //returning the number since 1.1.1970
            rearranger.append(get1970CellFactory(spec, col1Idx, g.getFactor(), conf));
        } else {
            long time = System.currentTimeMillis() + TimeZone.getDefault().getOffset(System.currentTimeMillis());
            if (typeofref.equals(DateShiftNodeDialog.CFG_FIXDATE)) {
                time = conf.getTimeModel().getCalendar().getTimeInMillis();
            }
            // append the new column with single cell factory
            rearranger.append(getTimeBasedCellFactory(spec, col1Idx, g.getFactor(), conf, time));
        }

        return rearranger;
    }

    /**
     *
     * @param spec the input data table spec.
     * @param newColName the new column name
     * @return a new DTS containing the old spec plus the new date and time cell with the given name
     */
    public static DataColumnSpec createOutputColumnSpec(final DataTableSpec spec, final String newColName) {
        // create column spec with type date and new (now uniwue) column name
        DataColumnSpecCreator creator = new DataColumnSpecCreator(newColName, DateAndTimeCell.TYPE);
        return creator.createSpec();
    }

    /**
     *
     * @param spec the previous data table spec
     * @param col1Idx the column index of the numerical column to add
     * @param g the granularity
     * @param conf the configuration object
     * @param col2Idx the time column
     * @return the cell factory
     */
    public static SingleCellFactory getColumnbasedCellFactory(final DataTableSpec spec, final int col1Idx,
        final int col2Idx, final double g, final DateShiftConfigure conf) {
        return new SingleCellFactory(createOutputColumnSpec(spec, conf.getNewColumnName().getStringValue())) {
            /**
             * Value for the new column is based on the values of two column of the row (first and second date column),
             * the selected granularity, and the fraction digits for rounding.
             *
             * @param row the current row
             * @return the difference between the two date values with the given granularity and rounding
             */
            @Override
            public DataCell getCell(final DataRow row) {
                DataCell cell1 = row.getCell(col1Idx);
                DataCell cell2 = row.getCell(col2Idx);
                if ((cell1.isMissing()) || (cell2.isMissing())) {
                    return DataType.getMissingCell();
                }
                double d = ((DoubleValue)cell1).getDoubleValue();
                d *= g;
                long first = Math.round(d);

                long last = ((DateAndTimeValue)cell2).getUTCTimeInMillis();

                return new DateAndTimeCell(first + last, conf.getHasDate().getBooleanValue(), conf.getHasTime()
                    .getBooleanValue(), conf.getHasMiliSeconds().getBooleanValue());

            }
        };
    }

    /**
     *
     * @param spec the previous data table spec
     * @param col1Idx the column index of the numerical column to add
     * @param g the granularity
     * @param conf the configuration object
     * @return the cell factory
     */
    public static SingleCellFactory get1970CellFactory(final DataTableSpec spec, final int col1Idx, final double g,
        final DateShiftConfigure conf) {

        return new SingleCellFactory(createOutputColumnSpec(spec, conf.getNewColumnName().getStringValue())) {

            /**
             * Value for the new column is based on the values of the current row and the value of the previous row.
             * Therefore both rows must contain a DateAndTimeValue, the selected granularity, and the fraction digits
             * for rounding.
             *
             * @param row the current row
             * @return the difference between the two date values with the given granularity and rounding
             */
            @Override
            public DataCell getCell(final DataRow row) {
                DataCell cell1 = row.getCell(col1Idx);
                // the cell is missing or not compatible to double
                // value
                if ((cell1.isMissing()) || !cell1.getType().isCompatible(DoubleValue.class)) {
                    return DataType.getMissingCell();
                }
                double d = ((DoubleValue)cell1).getDoubleValue();
                d *= g;
                return new DateAndTimeCell(Math.round(d), conf.getHasDate().getBooleanValue(), conf.getHasTime()
                    .getBooleanValue(), conf.getHasMiliSeconds().getBooleanValue());

            }
        };
    }

    /**
     *
     * @param spec the previous data table spec
     * @param col1Idx the column index of the numerical column to add
     * @param g the granularity
     * @param conf the configuration object
     * @param time the configured time in UTC milliceonds
     * @return the cell factory
     */
    public static SingleCellFactory getTimeBasedCellFactory(final DataTableSpec spec, final int col1Idx,
        final double g, final DateShiftConfigure conf, final long time) {

        return new SingleCellFactory(createOutputColumnSpec(spec, conf.getNewColumnName().getStringValue())) {

            /**
             * Value for the new column is based on the values of two column of the row (first and second date column),
             * the selected granularity, and the fraction digits for rounding.
             *
             * @param row the current row
             * @return the difference between the two date values with the given granularity and rounding
             */
            @Override
            public DataCell getCell(final DataRow row) {
                DataCell cell1 = row.getCell(col1Idx);
                if ((cell1.isMissing())) {
                    return DataType.getMissingCell();
                }
                double d = ((DoubleValue)cell1).getDoubleValue();
                d *= g;
                return new DateAndTimeCell(time + Math.round(d), conf.getHasDate().getBooleanValue(), conf.getHasTime()
                    .getBooleanValue(), conf.getHasMiliSeconds().getBooleanValue());
            }
        };
    }

}
