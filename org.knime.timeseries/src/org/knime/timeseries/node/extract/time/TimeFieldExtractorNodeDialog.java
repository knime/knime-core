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
 *   24.09.2009 (Fabian Dill): created
 */
package org.knime.timeseries.node.extract.time;

import org.knime.core.data.date.DateAndTimeValue;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnNameSelection;
import org.knime.timeseries.node.extract.AbstractFieldExtractorNodeDialog;

/**
 * Node dialog for the time extractor node that configures which of the time 
 * fields (hour, minute, second, millisecond) should be appended as an int 
 * column.
 * 
 * @author Fabian Dill, KNIME.com, Zurich, Switzerland
 */
public class TimeFieldExtractorNodeDialog 
    extends AbstractFieldExtractorNodeDialog {


    /** Hour name. */
    static final String HOUR = "Hour";
    /** Minute name. */
    static final String MINUTE = "Minute";
    /** Second name. */
    static final String SECOND = "Second";
    /** Millisecond. */
    static final String MILLISECOND = "Millisecond";
    
    /**
     * 
     */
    @SuppressWarnings("unchecked")
    public TimeFieldExtractorNodeDialog() {
        // create the UI components
        addDialogComponent(new DialogComponentColumnNameSelection(
                createColumnSelectionModel(), 
                "Column to extract time fields from:", 0, 
                DateAndTimeValue.class));
        createUIComponentFor(HOUR);
        createUIComponentFor(MINUTE);
        createUIComponentFor(SECOND);
        createUIComponentFor(MILLISECOND);
    }
    
}
