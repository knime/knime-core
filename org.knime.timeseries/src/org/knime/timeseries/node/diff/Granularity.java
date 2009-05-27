/**
 * 
 */
package org.knime.timeseries.node.diff;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


/**
 * Granularities for time, with a factory to divide milliseconds. 
 *   
 * @author Fabian Dill, KNIME GmbH
 *
 */
public enum Granularity {
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
    
    Granularity(final double factor) {
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
            for (Granularity g : values()) {
                m_valuesAsString.add(g.name());
            }               
        }
        return Collections.unmodifiableList(m_valuesAsString);
    }
}
