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

import java.util.Calendar;
import java.util.TimeZone;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.IntValue;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.data.container.SingleCellFactory;
import org.knime.core.data.date.DateAndTimeCell;
import org.knime.core.data.date.DateAndTimeValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelInteger;
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

    private final SettingsModelBoolean m_replaceCol = createReplaceColumnModel();

    /**
     * @return the settings model that determines whether the column should be replaced
     */
    public SettingsModelBoolean getReplaceColumnModel() {
        return m_replaceCol;
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
    private final SettingsModelString m_newColName = createNewColNameModel(m_replaceCol);

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

    private final SettingsModelString m_typeofreference = createReferenceTypeModel();

    /**
     * @return the settings model for the type.
     */
    public SettingsModelString gettypeofreference() {
        return m_typeofreference;
    }

    private final SettingsModelString m_typeofshift = createShiftTypeModel();

    /**
     * @return the settings model for the type of shift.
     */
    public SettingsModelString gettypeofshift() {
        return m_typeofshift;
    }

    private final SettingsModelInteger m_shiftvalue = createShiftValueModel();

    /**
     *  @return the settings model for the type of shift.
     */
    public SettingsModelInteger getvalueofshift() {
            return m_shiftvalue;
    }



    private final SettingsModelCalendar m_timemodel = createCalendarModel();

    /**
     * @return the settings model for the calendar.
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

    private static final String CFG_REPLACE_COLUMN = "replace.column";

    private static final String CFG_COL1 = "column.lower";

    private static final String CFG_COL2 = "column.upper";

    private static final String CFG_NEW_COL_NAME = "new.column.name";

    private static final String CFG_GRANULARITY = "granularity";

    private static final String CFG_REF_TYPE = "seconddate.reference.type";

    private static final String CFG_TIME = "cfg.time";

    private static final String CFG_SHIFT_TYPE = "cfg.shift.type";

    /*
     * Models...
     */

    /**
     * @return settings model for the selected time
     */
    public static SettingsModelCalendar createCalendarModel() {
        Calendar cal = DateAndTimeCell.getUTCCalendar();
        cal.setTimeInMillis(System.currentTimeMillis());
        SettingsModelCalendar smc = new SettingsModelCalendar(CFG_TIME, cal);
        smc.setEnabled(false);
        return smc;
    }

    /**
     * @return settings model for the type of the used reference column.
     */
    public static SettingsModelString createReferenceTypeModel() {
        return new SettingsModelString(CFG_REF_TYPE, DateShiftNodeDialog.CFG_COLUMN);
    }



    /**
     * @return settings model for the type of shift.
     */
    public static SettingsModelString createShiftTypeModel() {
        return new SettingsModelString(CFG_SHIFT_TYPE, DateShiftNodeDialog.CFG_VALUE_SHIFT);
    }



    /**
     * @return settings model for the shift value.
     */
    public static SettingsModelInteger createShiftValueModel() {
        return new SettingsModelInteger("cfg.shift.value", 1);
    }

    /**
     * @return settings model for the first time column
     */
    public static SettingsModelString createNumColmodel() {
        SettingsModelString sms = new SettingsModelString(CFG_COL1, null);
        sms.setEnabled(false);
        return sms;
    }

    /**
     *
     * @return settings model for the second time column
     */
    public static SettingsModelString createDateColumnModel() {
        return new SettingsModelString(CFG_COL2, null);
    }

    /**
     *
     * @param replaceColumnModel TODO
     * @return settings model for the new column name
     */
    public static SettingsModelString createNewColNameModel(final SettingsModelBoolean replaceColumnModel) {
        final SettingsModelString result = new SettingsModelString(CFG_NEW_COL_NAME, "ShiftDate");
        replaceColumnModel.addChangeListener((e) -> result.setEnabled(!replaceColumnModel.getBooleanValue()));
        result.setEnabled(!replaceColumnModel.getBooleanValue());
        return result;
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
     * @return settings model for determining whether the column should be replaced
     */
    public static SettingsModelBoolean createReplaceColumnModel() {
        return new SettingsModelBoolean(CFG_REPLACE_COLUMN, false);
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
        m_typeofshift.validateSettings(settings);
        m_shiftvalue.validateSettings(settings);
        if (settings.containsKey(CFG_REPLACE_COLUMN)) {
            m_replaceCol.validateSettings(settings);
        }
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
        m_typeofshift.saveSettingsTo(settings);
        m_shiftvalue.saveSettingsTo(settings);
        m_replaceCol.saveSettingsTo(settings);
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
        m_typeofshift.loadSettingsFrom(settings);
        m_shiftvalue.loadSettingsFrom(settings);
        if (settings.containsKey(CFG_REPLACE_COLUMN)) {
            m_replaceCol.loadSettingsFrom(settings);
        }
    }

    /**
     *
     * @param conf the configuration object
     * @param spec the input data table spec
     * @return the column rearranger as defied in the configuration object.
     * @throws InvalidSettingsException if settings are not valid.
     */
    public static ColumnRearranger getTimeToValueRearranger(final DateShiftConfigure conf,
                                            final DataTableSpec spec) throws InvalidSettingsException {

        int col1Idx = spec.findColumnIndex(conf.getNumColumnModel().getStringValue());
        ColumnRearranger rearranger = new ColumnRearranger(spec);

        int unit = getDateTimeUnit(conf);

        DataColumnSpec out = createOutputColumnSpec(spec,
            DataTableSpec.getUniqueColumnName(spec, conf.getNewColumnName().getStringValue()));

        String typeofref = conf.gettypeofreference().getStringValue();
        if (typeofref.equals(DateShiftNodeDialog.CFG_COLUMN)) {
            int col2Idx = spec.findColumnIndex(conf.getDateColumnModel().getStringValue());

            // append the new column with single cell factory
            if (conf.getReplaceColumnModel().getBooleanValue()) {
                out = createOutputColumnSpec(spec, conf.getDateColumnModel().getStringValue());
                rearranger.replace(getColumnbasedCellFactory(out, col1Idx, col2Idx, unit, conf), col2Idx);
            } else {
                rearranger.append(getColumnbasedCellFactory(out, col1Idx, col2Idx, unit, conf));
            }
        } else {
            Calendar time = Calendar.getInstance();
            time.setTimeInMillis(System.currentTimeMillis()
                            + TimeZone.getDefault().getOffset(System.currentTimeMillis()));
            if (typeofref.equals(DateShiftNodeDialog.CFG_FIXDATE)) {
                time = conf.getTimeModel().getCalendar();
            }
            // append the new column with single cell factory
            rearranger.append(getTimeBasedCellFactory(out, col1Idx, unit, conf, time));
        }

        return rearranger;
    }

    private static int getDateTimeUnit(final DateShiftConfigure conf) throws InvalidSettingsException {
        String gran = conf.getGranularity().getStringValue();
        if (gran.equals(Granularity.SECOND.getName())) {
            return Calendar.SECOND;
        } else if (gran.equals(Granularity.MINUTE.getName())) {
            return Calendar.MINUTE;
        } else if (gran.equals(Granularity.HOUR.getName())) {
            return Calendar.HOUR;
        } else if (gran.equals(Granularity.DAY.getName())) {
            return Calendar.DAY_OF_YEAR;
        } else if (gran.equals(Granularity.WEEK.getName())) {
            return Calendar.WEEK_OF_YEAR;
        } else if (gran.equals(Granularity.MONTH.getName())) {
            return Calendar.MONTH;
        } else if (gran.equals(Granularity.YEAR.getName())) {
            return Calendar.YEAR;
        } else if (gran.equals(Granularity.MILLISECOND.getName())) {
            return Calendar.MILLISECOND;
        } else {
            throw new InvalidSettingsException("Invalid granularity " + gran);
        }
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
     * @param spec the  output column spec
     * @param col1Idx the column index of the numerical column to add
     * @param g the time field to modify (as defined by calendar constants)
     * @param conf the configuration object
     * @param col2Idx the time column
     * @return the cell factory
     */
    public static SingleCellFactory getColumnbasedCellFactory(final DataColumnSpec spec, final int col1Idx,
        final int col2Idx, final int g, final DateShiftConfigure conf) {
        return new SingleCellFactory(spec) {
            /**
             * Value for the new column is based on the values of two column of the row (first and second date column),
             * the selected granularity, and the fraction digits for rounding.
             *
             * @param row the current row
             * @return the difference between the two date values with the given granularity and rounding
             */
            @Override
            public DataCell getCell(final DataRow row) {
                final int value;
                DataCell cell2 = row.getCell(col2Idx);
                if (cell2.isMissing()) {
                    return DataType.getMissingCell();
                }

                String typeofshift = conf.gettypeofshift().getStringValue();
                if (typeofshift.equals(DateShiftNodeDialog.CFG_COLUMN_SHIFT)) {
                    DataCell cell1 = row.getCell(col1Idx);
                    if ((cell1.isMissing())) {
                        return DataType.getMissingCell();
                    }
                    value = ((IntValue)cell1).getIntValue();
                } else {
                    value = conf.getvalueofshift().getIntValue();
                }

                Calendar c = ((DateAndTimeValue)cell2).getUTCCalendarClone();
                c.add(g, value);

                return new DateAndTimeCell(c.getTimeInMillis(), conf.getHasDate().getBooleanValue(),
                    conf.getHasTime().getBooleanValue(), conf.getHasMiliSeconds().getBooleanValue());

            }
        };
    }

    /**
     *
     * @param spec the  output column spec
     * @param col1Idx the column index of the numerical column to add
     * @param g the time field to modify (as defined by calendar constants)
     * @param conf the configuration object
     * @param time the configured time as Calendar
     * @return the cell factory
     */
    public static SingleCellFactory getTimeBasedCellFactory(final DataColumnSpec spec, final int col1Idx,
        final int g, final DateShiftConfigure conf, final Calendar time) {
        return new SingleCellFactory(spec) {

            /**
             * Value for the new column is based on the values of two column of the row (first and second date column),
             * the selected granularity, and the fraction digits for rounding.
             *
             * @param row the current row
             * @return the difference between the two date values with the given granularity and rounding
             */
            @Override
            public DataCell getCell(final DataRow row) {
                final int value;

                String typeofshift = conf.gettypeofshift().getStringValue();
                if (typeofshift.equals(DateShiftNodeDialog.CFG_COLUMN_SHIFT)) {
                    DataCell cell1 = row.getCell(col1Idx);
                    if ((cell1.isMissing())) {
                        return DataType.getMissingCell();
                    }
                    value = ((IntValue)cell1).getIntValue();
                } else {
                    value = conf.getvalueofshift().getIntValue();
                }
                Calendar c = (Calendar)time.clone();
                c.add(g, value);

                return new DateAndTimeCell(c.getTimeInMillis(),
                    conf.getHasDate().getBooleanValue(), conf.getHasTime().getBooleanValue(),
                    conf.getHasMiliSeconds().getBooleanValue());
            }
        };
    }



    /**
     *
     * @param spec the previous data table spec
     * @param col1Idx the column index of the numerical column to add
     * @param g the time field to modify (as defined by calendar constants)
     * @param conf the configuration object
     * @param col2Idx the time column
     * @return the cell factory
     */
    public static SingleCellFactory getColumnValuebasedCellFactory(final DataTableSpec spec, final int col1Idx,
        final int col2Idx, final int g, final DateShiftConfigure conf) {
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
                Calendar c = ((DateAndTimeValue)cell2).getUTCCalendarClone();
                c.add(g, ((IntValue)cell1).getIntValue());

                return new DateAndTimeCell(c.getTimeInMillis(), conf.getHasDate().getBooleanValue(), conf.getHasTime()
                    .getBooleanValue(), conf.getHasMiliSeconds().getBooleanValue());

            }
        };
    }

    /**
     *
     * @param spec the previous data table spec
     * @param col1Idx the column index of the numerical column to add
     * @param g the time field to modify (as defined by calendar constants)
     * @param conf the configuration object
     * @param time the configured time as Calendar
     * @return the cell factory
     */
    public static SingleCellFactory getTimeBasedValueCellFactory(final DataTableSpec spec, final int col1Idx,
        final int g, final DateShiftConfigure conf, final Calendar time) {

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
                Calendar c = (Calendar)time.clone();
                c.add(g, ((IntValue)cell1).getIntValue());

                return new DateAndTimeCell(c.getTimeInMillis(),
                    conf.getHasDate().getBooleanValue(), conf.getHasTime().getBooleanValue(),
                    conf.getHasMiliSeconds().getBooleanValue());
            }
        };
    }


}
