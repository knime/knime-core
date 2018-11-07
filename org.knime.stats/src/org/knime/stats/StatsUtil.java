/*
 * ------------------------------------------------------------------------
 *  Copyright by KNIME AG, Zurich, Switzerland
 *  Website: http://www.knime.com; Email: contact@knime.com
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
 *  KNIME and ECLIPSE being a combined program, KNIME AG herewith grants
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
 *
 * History
 *   26.06.2012 (hofer): created
 */
package org.knime.stats;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.math3.stat.descriptive.SummaryStatistics;
import org.apache.commons.math3.util.FastMath;
import org.knime.core.data.DataColumnProperties;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataType;
import org.knime.core.data.renderer.DataValueRenderer;
import org.knime.core.data.renderer.DoubleValueRenderer.FullPrecisionRendererFactory;

/**
 *
 * @author Heiko Hofer
 */
public final class StatsUtil {

    private StatsUtil() {
    }

    /**
     * Full precision renderer for double values
     */
    public static final String FULL_PRECISION_RENDERER = new FullPrecisionRendererFactory().getDescription();

    /**
     * Computes the standard error. See http://mathworld.wolfram.com/StandardError.html for a definition.
     *
     * @param stats a summary of descriptive statistics
     * @return the standard error
     */
    public static double getStandardError(final SummaryStatistics stats) {
        final double std = stats.getStandardDeviation();
        final double n = stats.getN();
        return std / FastMath.sqrt(n);
    }

    /**
     * Creates a columnspec with the given name and type. Adds the given properties to the spec.
     * 
     * @param name the name of the column
     * @param properties the properties for the column or null for empty properties
     * @param type the data type of the column
     * @return the columnspec
     */
    private static DataColumnSpec createDataColumnSpec(final String name, final Map<String, String> properties,
        final DataType type) {
        final DataColumnSpecCreator columnSpecCreator = new DataColumnSpecCreator(name, type);
        if (properties != null) {
            columnSpecCreator.setProperties(new DataColumnProperties(properties));
        }
        return columnSpecCreator.createSpec();
    }

    /**
     * Creates a columnspec with the given name and type. Adds the preferredRenderer in the properties of the columnspec
     * 
     * @param name the name of the column
     * @param preferredRenderer the preferred renderer for the values of the column
     * @param type the data type of the column
     * @return the columnspec
     */
    public static DataColumnSpec createDataColumnSpec(final String name, final String preferredRenderer,
        final DataType type) {
        final Map<String, String> properties = new HashMap<>(1);
        properties.put(DataValueRenderer.PROPERTY_PREFERRED_RENDERER, preferredRenderer);
        return createDataColumnSpec(name, properties, type);
    }
}
