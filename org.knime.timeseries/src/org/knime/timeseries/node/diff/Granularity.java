/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2010
 *  University of Konstanz, Germany and
 *  KNIME GmbH, Konstanz, Germany
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
    
    /** Millisecond. */
    public static final Granularity MILLISECOND = new Granularity(
            TimeLevelNames.MILLISECOND, 1);
    /** Second. */
    public static final Granularity SECOND = new Granularity(
            TimeLevelNames.SECOND, 1000);
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
            TimeLevelNames.MONTH, 1000 * 60 * 60 * 24 * 365.25 / 12);
    /** Quarter (=three months). */
    public static final Granularity QUARTER = new Granularity(
            TimeLevelNames.QUARTER, 1000 * 60 * 60 * 24 * 365.25 / 4);
    /** Year. */
    public static final Granularity YEAR = new Granularity(
            TimeLevelNames.YEAR, 1000 * 60 * 60 * 24 * 365.25);

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
        names.add(SECOND.getName());
        names.add(MILLISECOND.getName());
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
     * @param name of the granularity (usually one of 
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
        } else if (TimeLevelNames.SECOND.equals(name)) {
            return SECOND;
        } else if (TimeLevelNames.MILLISECOND.equals(name)) {
            return MILLISECOND;
        } else {
            return null;
        }
    }

}
