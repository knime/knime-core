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
 * website: www.knime.org
 * email: contact@knime.org
 * ---------------------------------------------------------------------
 * 
 * History
 *   28.07.2009 (Fabian Dill): created
 */
package org.knime.timeseries.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * 
 * @author Fabian Dill, KNIME.com, Zurich, Switzerland
 */
public final class TimeLevelNames {
    
    private TimeLevelNames() {
        // utility class
    }
    
    /** Key for aggregation level year. */ 
    public static final String YEAR = "Year";
    /** Key for aggregation level quarter. */
    public static final String QUARTER = "Quarter";
    /** Key for aggregation level month. */
    public static final String MONTH = "Month";
    /** Key for aggregation level week. */
    public static final String WEEK = "Week";
    /** Key for aggregation level day. */
    public static final String DAY = "Day";
    /** Key for aggregation level hour. */
    public static final String HOUR = "Hour";
    /** Key for aggregation level minute. */
    public static final String MINUTE = "Minute";

    /**
     * 
     * @return a list of aggregation level names. May be subclassed.
     */
    public static Collection<String>getAggregationLevels() {
        List<String>levelNames = new ArrayList<String>(); 
        levelNames.add(TimeLevelNames.YEAR);
        levelNames.add(TimeLevelNames.QUARTER);
        levelNames.add(TimeLevelNames.MONTH);
        levelNames.add(TimeLevelNames.WEEK);
        levelNames.add(TimeLevelNames.DAY);
        levelNames.add(TimeLevelNames.HOUR);
        levelNames.add(TimeLevelNames.MINUTE);
        return levelNames;
    }
    
}
