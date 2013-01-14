/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2013
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
 * -------------------------------------------------------------------
 *
 * History
 *   02.02.2006 (sieb): created
 */
package org.knime.base.util.coordinate;

import org.knime.core.data.DataValue;

/**
 * Abstract class describing a coordinate mapping. A coordinate mapping
 * describes the mapping from a real data domain to a fixed length coordinate
 * axis.
 *
 * @author Christoph Sieb, University of Konstanz
 */
public abstract class CoordinateMapping {

    /**
     * The mapping of the domain value.
     */
    private double m_mappingValue;

    /**
     * Domain value as string.
     */
    private String m_stringDomainValue;

    /**
     * Text of the tool tip.
     */
    private DataValue[] m_values;

    /**
     * Constructs a coordinate mapping from a string representation of the
     * domain value and its mapping value.
     *
     * @param stringDomValue the domain value as a string
     * @param mappingValue the corresponding mapping
     */
    CoordinateMapping(final String stringDomValue, final double mappingValue) {
        m_stringDomainValue = stringDomValue;
        m_mappingValue = mappingValue;
    }

    /**
     * Each coordinate mapping must return the mapping value as a double.
     *
     * @return the mapping value
     */
    public double getMappingValue() {
        return m_mappingValue;
    }

    /**
     * A coordinate mapping must also return the corresponding domain value as a
     * string.
     *
     * @return the domain value as string
     */
    public String getDomainValueAsString() {
        return m_stringDomainValue;
    }

    /**
     * @return the string representation of the domain value.
     */
    String getStringDomainValue() {
        return m_stringDomainValue;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return ("Dom. String Value: " + m_stringDomainValue + ", "
                + "Map. Value: " + m_mappingValue);
    }

    /**
     * Returns the values if set.
     *
     * @return the values
     */
    public DataValue[] getValues() {
        return m_values;
    }

    /**
     * Sets values of this mapping.
     *
     * @param values the values
     */
    public void setValues(final DataValue... values) {
        m_values = values;
    }
}
