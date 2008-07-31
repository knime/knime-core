package org.knime.timeseries.node.aggregate;

import java.io.File;
import java.io.IOException;
import java.util.Calendar;
import java.util.Date;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.data.container.SingleCellFactory;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.timeseries.node.diff.Granularity;
import org.knime.timeseries.types.TimestampValue;

/**
 * Appends a time column with the time in another column to a higher aggregation 
 * level (e.g. month to quarter) and as a string.
 *
 * @author KNIME GmbH
 */
public class TimeAggregatorNodeModel extends NodeModel {
    // column containing the time values
	private final SettingsModelString m_col = TimeAggregatorNodeDialog
		.createColumnModel();
	// .. and the referring index
	private int m_colIdx;
	// the new column name
	private final SettingsModelString m_newColName = TimeAggregatorNodeDialog
		.createNewColNameModel();
	// the aggregation level
	private final SettingsModelString m_level 
		= TimeAggregatorNodeDialog.createLevelModel();
	
    /**
     * Constructor for the node model, one in and one out port.
     */
    protected TimeAggregatorNodeModel() {
        super(1, 1);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {
    	// create column rearranger
    	ColumnRearranger rearranger = new ColumnRearranger(
    			inData[0].getDataTableSpec());
    	// this is the factory which returns the value for the new column 
    	// per row - value depends on granularity level
    	SingleCellFactory factory = new SingleCellFactory(createOutputSpec(
    			inData[0].getDataTableSpec())) {

			@Override
			public DataCell getCell(DataRow row) {
				// check if cell with time value is missing
				if (row.getCell(m_colIdx).isMissing()) {
					return DataType.getMissingCell();
				}
				// get the date
				Date d = ((TimestampValue)row.getCell(m_colIdx))
					.getDate();
				// get a calendar
				Calendar c = Calendar.getInstance();
				c.setTime(d);
				// get the selected granularity level
                Granularity level = Granularity.valueOf(
                        m_level.getStringValue());
				// depending on the selected granularity level
				// return different values = use different methods
				if (level.equals(Granularity.YEAR)) {
					return new StringCell(getYear(row, c));
				} else if (level.equals(Granularity.QUARTER)) {
					String s = getQuarter(row, c);
					return new StringCell(s);
				} else if (level.equals(Granularity.MONTH)) {
					return new StringCell(getMonth(row, c));
				} else if (level.equals(Granularity.WEEK)) {
					return new StringCell(getWeek(row, c));
				} else if (level.equals(Granularity.DAY)) {
					return new StringCell(getDay(row, c));
				} else {
					return DataType.getMissingCell();
				}
			}
			
			// extract the year only (yyyy)
			private String getYear(final DataRow row, 
					final Calendar c) {
				int year = c.get(Calendar.YEAR);
				return "" + year;	
			}
			// calculate the quarter q and return yyyy_q
			private String getQuarter(final DataRow row, 
					final Calendar c) {
				int year = c.get(Calendar.YEAR);
				int month = c.get(Calendar.MONTH);
				// month starts with 0!
				month++;
				String s = "" + year;
				s += "_" + (int)Math.ceil(month / 3);	
				return s;
			}
			// extract year and month (yyyy_mm)
			private String getMonth(final DataRow row, 
					final Calendar c) {
				int year = c.get(Calendar.YEAR);
				int month = c.get(Calendar.MONTH);
				// month starts with 0!
				month++;
				String s = "" + year;
				if (month < 10) {
					s += "_0" + month;
				} else {
					s += "_" + month;
				}
				return s;
			}
			
			// extract year and week (yyyy_ww) 
			private String getWeek(final DataRow row, 
					final Calendar c) {
				int year = c.get(Calendar.YEAR);
				int week = c.get(Calendar.WEEK_OF_YEAR);
				String s = "" + year;
				if (week < 10) {
					s += "_0" + week;	
				} else {
					s += "_" + week;
				}
				return s;
			}
			
			// extract year, month, and day (yyyy_mm_dd)
			private String getDay(final DataRow row, 
					final Calendar c) {
				int year = c.get(Calendar.YEAR);
				int month = c.get(Calendar.MONTH);
				// month starts with 0!
				month++;
				int day = c.get(Calendar.DAY_OF_MONTH);
				String s = "" + year; 
				if (month < 10 ) {
					s += "_0" + month;
				} else {
					s += "_" + month;
				}
				if (day < 10 ) {
					s += "_0" + day;	
				} else {
					s += "_" + day;
				}
				return s;
			}
		};
    	rearranger.append(factory);
    	BufferedDataTable out = exec.createColumnRearrangeTable(inData[0], 
    			rearranger, exec);
        return new BufferedDataTable[]{out};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
        // nothing
    }
    
    private DataColumnSpec createOutputSpec(final DataTableSpec spec) {
    	// get a unique column name based on entered new column name
    	m_newColName.setStringValue(DataTableSpec.getUniqueColumnName(
    			spec, m_newColName.getStringValue()));
    	// new spec with type string and chosen (now unique) new column name
    	DataColumnSpecCreator creator = new DataColumnSpecCreator(
    			m_newColName.getStringValue(), 
    			StringCell.TYPE);
    	return creator.createSpec();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
            throws InvalidSettingsException {
    	// check for selected column in spec
    	m_colIdx = inSpecs[0].findColumnIndex(m_col.getStringValue());
    	if (m_colIdx < 0 ) {
    		throw new InvalidSettingsException(
    				"Column " + m_col.getStringValue() 
    				+ " not found in input data");
    	}
        return new DataTableSpec[]{new DataTableSpec(
        		inSpecs[0], new DataTableSpec(createOutputSpec(inSpecs[0])))};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
         m_col.saveSettingsTo(settings);
         m_level.saveSettingsTo(settings);
         m_newColName.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_col.loadSettingsFrom(settings);
        m_level.loadSettingsFrom(settings);
        m_newColName.loadSettingsFrom(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_col.validateSettings(settings);
        m_newColName.validateSettings(settings);
        m_level.validateSettings(settings);
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File internDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        // nothing
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File internDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        // nothing
    }

}

