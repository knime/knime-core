package org.knime.timeseries.node.aggregate;

import java.util.ArrayList;
import java.util.List;

import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnNameSelection;
import org.knime.core.node.defaultnodesettings.DialogComponentString;
import org.knime.core.node.defaultnodesettings.DialogComponentStringSelection;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.timeseries.node.diff.Granularity;
import org.knime.timeseries.node.diff.TimeDifferenceNodeModel;
import org.knime.timeseries.types.TimestampValue;

/**
 * Dialog for the TimeAggregator node, with a column selection for the column 
 * containing the time values, a select box for the aggregation level (year, 
 * quarter, month, week, day) and a text field for the new column name. 
 * 
 * @author KNIME GmbH
 */
public class TimeAggregatorNodeDialog extends DefaultNodeSettingsPane {

	private static final String CFG_COL = "time.column";
	private static final String CFG_NEW_COL = "new.column.name";
	private static final String CFG_LEVEL = "aggregation.level";	
    /**
     * New pane for configuring the TimeAggregator node.
     */
	@SuppressWarnings("unchecked")
    protected TimeAggregatorNodeDialog() {
		// select column containing the time values
    	addDialogComponent(new DialogComponentColumnNameSelection(
    			createColumnModel(), "Select time column", 0, 
    			TimestampValue.class));
    	// get the aggregation granularity from TimeDifference node!
    	List<String>methods = new ArrayList<String>();
    	methods.addAll(Granularity.asStringList());
    	// remove minute and hour (too fine-grained)
    	methods.remove(Granularity.MINUTE.name());
    	methods.remove(Granularity.HOUR.name());
    	// add granularity selection
    	addDialogComponent(new DialogComponentStringSelection(
    			createLevelModel(), "Choose aggregation level", 
    			methods));
    	// add new column name text field
    	addDialogComponent(new DialogComponentString(
    			createNewColNameModel(), "Appended column name"));

    }
    
	/*
	 * Models...
	 */ 
	
    static SettingsModelString createColumnModel() {
    	return new SettingsModelString(CFG_COL, "");
    }
    
    static SettingsModelString createNewColNameModel() {
    	return new SettingsModelString(CFG_NEW_COL, "AGGREGATED TIME");
    }
    
    static SettingsModelString createLevelModel() {
    	return new SettingsModelString(CFG_LEVEL, 
                Granularity.QUARTER.name());
    }
}

