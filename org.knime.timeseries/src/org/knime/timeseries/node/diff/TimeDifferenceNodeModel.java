package org.knime.timeseries.node.diff;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.data.container.SingleCellFactory;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelInteger;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.timeseries.types.TimestampValue;

/**
 * Appends the difference between two dates with a selected granularity (year, 
 * quarter, month, week, day, hour, minute).
 *
 * @author KNIME GmbH
 */
public class TimeDifferenceNodeModel extends NodeModel {
	
	/**
	 * Granularities for time, with a factory to divide milliseconds. 
	 * 
	 *
	 */
	public enum GRANULARITY {
		/** Minute. */
		MINUTE (1000 * 60),
		/** Hour. */
		HOUR (1000 * 60 * 60),
		/** Day. */
		DAY (1000 * 60 * 60 * 24),
		/** Week. */
		WEEK(1000 * 60 * 60 * 24 * 7),
		/** Month. */
		MONTH(1000 * 60 * 60 * 24 * 3 * 30),
		/** Quarter (=three months). */
		QUARTER (1000 * 60 * 60 * 24 * 3 * 90),
		/** Year. */
		YEAR(1000 * 60 * 60 * 24 * 3 * 365);
		
		GRANULARITY(final double factor) {
			m_factor = factor;
		}
		
		private final double m_factor;
		
		private static List<String>m_valuesAsString;
		
		public double getFactor() {
			return m_factor;
		}
		
		/**
		 * 
		 * @return the values of the enumeration as a list of strings
		 */
		public static List<String> asStringList(){
			if (m_valuesAsString == null) {
				m_valuesAsString = new ArrayList<String>();
				for (GRANULARITY g : values()) {
					m_valuesAsString.add(g.name());
				}				
			}
			return Collections.unmodifiableList(m_valuesAsString);
		}
	}
	// first date column
	private final SettingsModelString m_col1 = TimeDifferenceNodeDialog
		.createColmn1Model();
	// ... and the referring index
	private int m_col1Idx;
	// second date column
	private final SettingsModelString m_col2 = TimeDifferenceNodeDialog.
		createColumn2Model();
	// ... and the referring index
	private int m_col2Idx;
	// new column name
	private final SettingsModelString m_newColName = TimeDifferenceNodeDialog
		.createNewColNameModel();
	// selected granularity level
	private final SettingsModelString m_granularity = TimeDifferenceNodeDialog
		.createGranularityModel();
	// number of fraction digits for rounding
	private final SettingsModelInteger m_rounding 
		= TimeDifferenceNodeDialog.createRoundingModel();
	
    /**
     * Constructor for the node model with one in and one out port.
     */
    protected TimeDifferenceNodeModel() {
        super(1, 1);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {
    	// get the selected granularity level
    	final GRANULARITY g = GRANULARITY.valueOf(
    			m_granularity.getStringValue());
    	// create rearranger
    	ColumnRearranger rearranger = new ColumnRearranger(
    			inData[0].getDataTableSpec());
    	// append the new column with single cell factory
    	rearranger.append(new SingleCellFactory(createOutputColumnSpec(
    			inData[0].getDataTableSpec(), m_newColName.getStringValue())) {
    		/**
    		 * Value for the new column is based on the values of 
    		 * two column of the row (first and second date column), the 
    		 * selected granularity, and the fraction digits for rounding.
    		 * 
    		 * @param row the current row
    		 * @return the difference between the two date values with the given
    		 * 	granularity and rounding
    		 */
			@Override
			public DataCell getCell(DataRow row) {
				Date first = ((TimestampValue)row.getCell(m_col1Idx)).getDate();
				Date last = ((TimestampValue)row.getCell(m_col2Idx)).getDate();
				Date diff = new Date(last.getTime() - first.getTime());
				double diffTime = diff.getTime() / g.getFactor();
				BigDecimal bd = new BigDecimal(diffTime);
				bd = bd.setScale(m_rounding.getIntValue(), 
						BigDecimal.ROUND_CEILING);
				return new DoubleCell(bd.doubleValue());
			}
    	});
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

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
            throws InvalidSettingsException {
    	m_col1Idx = inSpecs[0].findColumnIndex(m_col1.getStringValue());
    	m_col2Idx = inSpecs[0].findColumnIndex(m_col2.getStringValue());
    	// check for first date column in input spec
    	if (m_col1Idx < 0) {
    		throw new InvalidSettingsException("Column " 
    				+ m_col1.getStringValue() 
    				+ " not found in input table");
    	}
//    	 check for second date column in input spec
    	if (m_col2Idx < 0) {
    		throw new InvalidSettingsException("Column " 
    				+ m_col2.getStringValue() 
    				+ " not found in input table");
    	}
    	// return new spec with appended column 
    	// (time and chosen new column name)
        return new DataTableSpec[]{ new DataTableSpec(inSpecs[0], 
        		new DataTableSpec(
        				createOutputColumnSpec(inSpecs[0], 
        				m_newColName.getStringValue())))};
    }

    

	private DataColumnSpec createOutputColumnSpec(final DataTableSpec spec, 
			final String newColName) {
		// get unique column name based on the entered column name
		m_newColName.setStringValue(DataTableSpec.getUniqueColumnName(
				spec, newColName));
		// create column spec with type date and new (now uniwue) column name
		DataColumnSpecCreator creator = new DataColumnSpecCreator(
				m_newColName.getStringValue(), DoubleCell.TYPE);
		return creator.createSpec();
	}

	/**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
    	m_col1.saveSettingsTo(settings);
    	m_col2.saveSettingsTo(settings);
    	m_newColName.saveSettingsTo(settings);
    	m_granularity.saveSettingsTo(settings);
    	m_rounding.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
    	m_col1.loadSettingsFrom(settings);
    	m_col2.loadSettingsFrom(settings);
    	m_newColName.loadSettingsFrom(settings);
    	m_granularity.loadSettingsFrom(settings);
    	m_rounding.loadSettingsFrom(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
    	m_col1.validateSettings(settings);
    	m_col2.validateSettings(settings);
    	m_newColName.validateSettings(settings);
    	m_granularity.validateSettings(settings);
    	m_rounding.validateSettings(settings);
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

