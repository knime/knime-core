/*
 * ------------------------------------------------------------------------
 *
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
 * ---------------------------------------------------------------------
 *
 * History
 *   Oct 22, 2018 (marcel): created
 */
package org.knime.core.data.convert.map;

/**
 * Interface for {@link CellValueProducer producers} of Java primitive {@code float} values.
 *
 * @param <S> Type of source from which this producer reads.
 * @param <PP> Parameters that can be used to configure this producer.
 * @since 3.7
 * @author Marcel Wiedenmann, KNIME GmbH, Konstanz, Germany
 */
public interface FloatCellValueProducer<S, PP>
    extends PrimitiveCellValueProducer<S, Float, PP> {

    /**
     * Reads an {@code float} value from the given source using the given parameters.
     *
     * @param source The source.
     * @param params The parameters further specifying how to read from the given source, e.g., from which SQL column or
     *            table to read. Specific to the type of source and {@link CellValueProducer} that is being
     *            used.
     * @return The value which was read from source.
     * @throws MappingException If an exception occurs while producing the cell value.
     */
    float produceFloatCellValue(S source, PP params) throws MappingException;

    @Override
    default Float produceCellValue(final S source, final PP params) throws MappingException {
        if (producesMissingCellValue(source, params)) {
            return null;
        } else {
            return produceFloatCellValue(source, params);
        }
    }
}