package org.knime.timeseries.node.diff;

import org.knime.core.data.date.DateAndTimeValue;
import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnNameSelection;
import org.knime.core.node.defaultnodesettings.DialogComponentNumber;
import org.knime.core.node.defaultnodesettings.DialogComponentString;
import org.knime.core.node.defaultnodesettings.DialogComponentStringSelection;
import org.knime.core.node.defaultnodesettings.SettingsModelInteger;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

/**
 * 
 * Dialog for the TimeDifference node with a column selection for the first date
 * column, one for the other date column, a text field for the new column, a
 * selection list for the desired granularity (year, quarter, month, week, day,
 * hour, minute) of the difference and a spinner to chosse the rounding of the
 * fraction digits of the result.
 * 
 * 
 * @author KNIME GmbH
 */
public class TimeDifferenceNodeDialog extends DefaultNodeSettingsPane {

    private static final String CFG_COL1 = "column.lower";

    private static final String CFG_COL2 = "column.upper";

    private static final String CFG_NEW_COL_NAME = "new.column.name";

    private static final String CFG_GRANULARITY = "granularity";

    private static final String CFG_ROUND = "round.numbers";

    /**
     * New pane for configuring the TimeDifference node.
     */
    @SuppressWarnings("unchecked")
    protected TimeDifferenceNodeDialog() {
        // first date column
        addDialogComponent(new DialogComponentColumnNameSelection(
                createColmn1Model(), "Select first date column", 0,
                DateAndTimeValue.class));
        // second date column
        addDialogComponent(new DialogComponentColumnNameSelection(
                createColumn2Model(), "Select second date column", 0,
                org.knime.core.data.date.DateAndTimeValue.class));
        // granularity selection
        addDialogComponent(new DialogComponentStringSelection(
                createGranularityModel(),
                "Select granularity of time difference", Granularity
                        .getDefaultGranularityNames()));
        // fraction digits for rounding
        addDialogComponent(new DialogComponentNumber(createRoundingModel(),
                "Rounding to .. digits", 1));
        // new column name
        addDialogComponent(new DialogComponentString(createNewColNameModel(),
                "Appended column name:"));
    }
    

    /*
     * Models...
     */

    /**
     * @return settings model for the first time column
     */
    static SettingsModelString createColmn1Model() {
        return new SettingsModelString(CFG_COL1, "");
    }

    /**
     * 
     * @return settings model for the second time column
     */
    static SettingsModelString createColumn2Model() {
        return new SettingsModelString(CFG_COL2, "");
    }

    /**
     * 
     * @return settings model for the new column name
     */
    static SettingsModelString createNewColNameModel() {
        return new SettingsModelString(CFG_NEW_COL_NAME, "time diff");
    }

    /**
     * 
     * @return settings model for the granularity
     */
    static SettingsModelString createGranularityModel() {
        return new SettingsModelString(CFG_GRANULARITY, 
                Granularity.DAY.getName());
    }

    /**
     * 
     * @return settings model for the rounding model
     */
    static SettingsModelInteger createRoundingModel() {
        return new SettingsModelInteger(CFG_ROUND, 0);
    }
}
