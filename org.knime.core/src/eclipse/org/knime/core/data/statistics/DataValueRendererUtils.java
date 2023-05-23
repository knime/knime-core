/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by KNIME AG, Zurich, Switzerland
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
 * ---------------------------------------------------------------------
 *
 */
package org.knime.core.data.statistics;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataType;
import org.knime.core.data.DataValue;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.renderer.DataValueRenderer;
import org.knime.core.data.renderer.DefaultDataValueRenderer;
import org.knime.core.data.renderer.DoubleValueRenderer;
import org.knime.core.data.renderer.IntValueRenderer;

/**
 * Utilities to format {@link DataValue}s to strings.
 */
final class DataValueRendererUtils {

    private DataValueRendererUtils() {
        // utility class
    }

    static DoubleValueRenderer getDoubleRenderer() {
        return (DoubleValueRenderer)getNumberRenderer(DoubleCell.TYPE);
    }

    static IntValueRenderer getIntRenderer() {
        return (IntValueRenderer)getNumberRenderer(IntCell.TYPE);
    }

    static DataValueRenderer getNumberRenderer(final DataType dataType) throws IllegalStateException {
        final var rendererFactories = dataType.getRendererFactories();
        for (var factory : rendererFactories) {
            final var renderer =
                factory.createRenderer(new DataColumnSpecCreator(dataType.getName(), dataType).createSpec());
            if (isTextBasedRenderer(renderer)) {
                return renderer;
            }
        }
        throw new IllegalStateException("No applicable number text renderer found.");
    }

    private static boolean isTextBasedRenderer(final DataValueRenderer renderer) {
        return renderer instanceof DefaultDataValueRenderer && ((DefaultDataValueRenderer)renderer).getIcon() == null;
    }

    static String formatNumber(final DoubleValueRenderer renderer, final Double value) {
        return value == null ? null
            : ((DoubleValueRenderer)renderer.getRendererComponent(new DoubleCell(value))).getText();
    }

    static String formatNumber(final IntValueRenderer renderer, final Integer value) {
        return value == null ? null : ((IntValueRenderer)renderer.getRendererComponent(new IntCell(value))).getText();
    }

    private static DoubleValueRenderer getPercentageRenderer() {
        return new DoubleValueRenderer(new DecimalFormat("###0.0#%", new DecimalFormatSymbols(Locale.US)),
            "Percentage");
    }

    static String formatDouble(final Double doubleValue) {
        return formatNumber(getDoubleRenderer(), doubleValue);
    }

    static String formatPercentage(final Double doubleValue) {
        return formatNumber(getPercentageRenderer(), doubleValue);
    }

}
