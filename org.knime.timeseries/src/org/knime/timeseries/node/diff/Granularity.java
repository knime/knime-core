/**
 * 
 */
package org.knime.timeseries.node.diff;

import java.util.ArrayList;
import java.util.List;

import org.knime.timeseries.util.TimeLevelNames;


/**
 * Granularities for time, with a factory to divide milliseconds. 
 *   
 * @author Fabian Dill, KNIME GmbH
 *
 */
public class Granularity {
    
    /** Minute. */
    public static final Granularity MINUTE = new Granularity(
            TimeLevelNames.MINUTE, 1000 * 60);
    /** Hour. */
    public static final Granularity HOUR = new Granularity(
            TimeLevelNames.HOUR, 1000 * 60 * 60);
    /** Day. */
    public static final Granularity DAY = new Granularity(
            TimeLevelNames.DAY, 1000 * 60 * 60 * 24);
    /** Week. */
    public static final Granularity WEEK = new Granularity(
            TimeLevelNames.WEEK, 1000 * 60 * 60 * 24 * 7);
    /** Month. */
    public static final Granularity MONTH = new Granularity(
            TimeLevelNames.MONTH, 1000 * 60 * 60 * 24 * 3 * 30);
    /** Quarter (=three months). */
    public static final Granularity QUARTER = new Granularity(
            TimeLevelNames.QUARTER, 1000 * 60 * 60 * 24 * 3 * 90);
    /** Year. */
    public static final Granularity YEAR = new Granularity(
            TimeLevelNames.YEAR, 1000 * 60 * 60 * 24 * 3 * 365);

    /**
     * 
     * @return the names of the default granularities defined in this class  
     */
    public static final List<String>getDefaultGranularityNames() {
        List<String>names = new ArrayList<String>();
        names.add(YEAR.getName());
        names.add(QUARTER.getName());
        names.add(MONTH.getName());
        names.add(WEEK.getName());
        names.add(DAY.getName());
        names.add(HOUR.getName());
        names.add(MINUTE.getName());
        return names;
    }
    
    
    private final double m_factor;
    
    private final String m_name;
    
    /**
     * Creates a granularity with display level and factor. The factor defines 
     * by what the milliseconds have to be divided in order to achieve the 
     * granularity  defined by the display name.
     * 
     * @param name name of the granularity
     * @param factor factory by which milliseconds must be divided to achieve 
     *  the granularity
     */
    public Granularity(final String name, final double factor) {
        m_factor = factor;
        m_name = name;
    }
    
    /**
     * 
     * @return factory by which milliseconds must be divided to achieve the 
     * given granularity
     */
    public double getFactor() {
        return m_factor;
    }

    /**
     * 
     * @return name of the granularity
     * @see TimeLevelNames
     */
    public String getName() {
        return m_name;
    }
    
    /**
     * 
     * @param name name of the granularity (usually one of 
     *  {@link TimeLevelNames})
     * @return the referring granularity or <code>null</code>
     */
    public static Granularity valueOf(final String name) {
        if (TimeLevelNames.YEAR.equals(name)) {
            return YEAR;
        } else if (TimeLevelNames.QUARTER.equals(name)) {
            return QUARTER;
        } else if (TimeLevelNames.MONTH.equals(name)) {
            return MONTH;
        } else if (TimeLevelNames.WEEK.equals(name)) {
            return WEEK;
        } else if (TimeLevelNames.DAY.equals(name)) {
            return DAY;
        } else if (TimeLevelNames.HOUR.equals(name)) {
            return HOUR;
        } else if (TimeLevelNames.MINUTE.equals(name)) {
            return MINUTE;
        } else {
            return null;
        }
    }

}
