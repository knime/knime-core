/* ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2008 - 2009
 * KNIME.com, Zurich, Switzerland
 *
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner or
 * as specified in the license file distributed with this product.
 *
 * If you have any questions please contact the copyright holder:
 * website: www.knime.com
 * email: contact@knime.com
 * ---------------------------------------------------------------------
 * 
 * History
 *   05.10.2009 (Fabian Dill): created
 */
package org.knime.timeseries.node.extract.date;

import org.knime.core.data.date.DateAndTimeValue;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnNameSelection;
import org.knime.timeseries.node.extract.AbstractFieldExtractorNodeDialog;

/**
 * Node dialog for the date field extractor node that configures which of the 
 * date fields (year, quarter, month, week, day, day of week) should be 
 * appended as an int column.
 * 
 * @author Fabian Dill, KNIME.com, Zurich, Switzerland
 */
public class DateFieldExtractorNodeDialog 
    extends AbstractFieldExtractorNodeDialog {

    /** Year name. */
    static final String YEAR = "Year";
    /** Quarter name.*/
    static final String QUARTER = "Quarter";
    /** Month name. */
    static final String MONTH = "Month";
    /** Day of month name. */
    static final String DAY_OF_MONTH = "Day of month";
    /** Day of week name. */
    static final String DAY_OF_WEEK = "Day of week";
    

    /**
     * 
     */
    @SuppressWarnings("unchecked")
    public DateFieldExtractorNodeDialog() {
        addDialogComponent(new DialogComponentColumnNameSelection(
                createColumnSelectionModel(), 
                "Column to extract time fields from:", 0, 
                DateAndTimeValue.class));
        createUIComponentFor(YEAR);
        createUIComponentFor(QUARTER);
        // the month UI component looks differently because of 
        // the string or int radio buttons
        createUIComponentWithFormatSelection(MONTH);
        createUIComponentFor(DAY_OF_MONTH);
        createUIComponentWithFormatSelection(DAY_OF_WEEK);
    }
    
}
