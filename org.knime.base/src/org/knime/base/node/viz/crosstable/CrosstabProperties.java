/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2011
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
 *  propagated with or for interoperation with KNIME. The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ------------------------------------------------------------------------
 *
 * History
 *   07.06.2011 (hofer): created
 */
package org.knime.base.node.viz.crosstable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/**
 * This class defines names and position of the columns in the output table of
 * crosstab node. This class allows to change the naming in further releases
 * while keeping backward compatibility with versioning.
 *
 * @author Heiko Hofer
 */
final class CrosstabProperties {
    private static Map<String, CrosstabProperties> namings;

    private String m_frequencyName;
    private String m_percentName;
    private String m_rowPercentName;
    private String m_columnPercentName;
    private String m_totalRowCountName;
    private String m_totalColumnCountName;
    private String m_totalCountName;
    private String m_cellChiSquareName;
    private String m_expectedName;
    private String m_deviationName;
    private final Map<String, Integer> m_weights;

    /**
     * No instantiations allowed from outside.
     */
    private CrosstabProperties(final String version) {
        m_weights = new IdentityHashMap<String, Integer>();
        if (version.equals("1.0")) {
            m_frequencyName = "Frequency";
            m_weights.put(m_frequencyName, 5);

            m_expectedName = "Expected";
            m_weights.put(m_expectedName, 10);

            m_deviationName = "Deviation";
            m_weights.put(m_deviationName, 15);

            m_percentName = "Percent";
            m_weights.put(m_percentName, 30);

            m_rowPercentName = "Row Percent";
            m_weights.put(m_rowPercentName, 40);

            m_columnPercentName = "Column Percent";
            m_weights.put(m_columnPercentName, 50);

            m_totalRowCountName = "Total Row Count";
            m_weights.put(m_totalRowCountName, 60);

            m_totalColumnCountName = "Total Column Count";
            m_weights.put(m_totalColumnCountName, 70);

            m_totalCountName = "Total Count";
            m_weights.put(m_totalCountName, 80);

            m_cellChiSquareName = "Cell Chi-Square";
            m_weights.put(m_cellChiSquareName, 90);

        }
    }

    /**
     * @param version
     *            the version of the naming
     * @return the naming object for the given version
     */
    static CrosstabProperties create(final String version) {
        if (null == namings) {
            namings = new HashMap<String, CrosstabProperties>();
        }
        if (namings.containsKey(version)) {
            return namings.get(version);
        } else if (getSupportedVersion().contains(version)) {
            final CrosstabProperties naming = new CrosstabProperties(version);
            namings.put(version, naming);
            return naming;
        } else {
            throw new IllegalStateException("Crosstab naming not fount. "
                    + "This is most likely a programming error.");
        }

    }

    /**
     * @return the list of supported Versions
     */
    static List<String> getSupportedVersion() {
        return Arrays.asList(new String[] {"1.0"});
    }

    /**
     * The list of properties.
     * @return the supported properties
     */
    List<String> getProperties() {
        final List<String> sortedNames = new ArrayList<String>();
        sortedNames.addAll(m_weights.keySet());
        Collections.sort(sortedNames, new PropertyComparator());
        return sortedNames;
    }

    /**
     * @return the frequencyName
     */
    String getFrequencyName() {
        return m_frequencyName;
    }

    /**
     * @return the percentName
     */
    String getPercentName() {
        return m_percentName;
    }

    /**
     * @return the rowPercentName
     */
    String getRowPercentName() {
        return m_rowPercentName;
    }

    /**
     * @return the columnPercentName
     */
    String getColPercentName() {
        return m_columnPercentName;
    }

    /**
     * @return the totalRowCountName
     */
    String getTotalRowCountName() {
        return m_totalRowCountName;
    }

    /**
     * @return the totalColumnCountName
     */
    String getTotalColCountName() {
        return m_totalColumnCountName;
    }

    /**
     * @return the totalCountName
     */
    String getTotalCountName() {
        return m_totalCountName;
    }

    /**
     * @return the cellChiSquareName
     */
    String getCellChiSquareName() {
        return m_cellChiSquareName;
    }

    /**
     * @return the expectedName
     */
    String getExpectedFrequencyName() {
        return m_expectedName;
    }

    /**
     * @return the deviationName
     */
    String getDeviationName() {
        return m_deviationName;
    }

    /**
     * Compare to properties by their weights or by the lexical order if the
     * weights are equal.
     *
     * @author Heiko Hofer
     */
    class PropertyComparator implements Comparator<String> {
        /**
         * {@inheritDoc}
         */
        @Override
        public int compare(final String o1, final String o2) {
            final int weight1 = m_weights.get(o1);
            final int weight2 = m_weights.get(o2);
            if (weight1 != weight2) {
                return weight1 - weight2;
            } else {
                return o1.compareTo(o2);
            }
        }

    }
}
