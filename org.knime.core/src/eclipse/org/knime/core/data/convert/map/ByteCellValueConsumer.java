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
 * Interface for {@link CellValueConsumer consumers} of Java primitive {@code byte} values.
 *
 * @param <D> Type of destination to which this consumer writes.
 * @param <CP> Subtype of parameters that can be used to configure this consumer.
 * @since 3.7
 * @author Marcel Wiedenmann, KNIME GmbH, Konstanz, Germany
 */
public interface ByteCellValueConsumer<D, CP>
    extends PrimitiveCellValueConsumer<D, Byte, CP> {

    /**
     * Writes the given {@code byte} value to the given destination using the given parameters.
     *
     * @param destination The destination.
     * @param value The value to write.
     * @param params The parameters further specifying how to write to the destination, e.g., to which SQL column or
     *            table to write. Specific to the type of destination and {@link CellValueConsumer} that is
     *            being used.
     * @throws MappingException If an exception occurs while consuming the cell value.
     */
    void consumeByteCellValue(D destination, byte value, CP params) throws MappingException;

    @Override
    default void consumeCellValue(final D destination, final Byte value, final CP consumerParams)
        throws MappingException {
        if (value != null) {
            consumeByteCellValue(destination, value, consumerParams);
        } else {
            consumeMissingCellValue(destination, consumerParams);
        }
    }
}
