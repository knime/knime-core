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
package org.knime.core.data.v2;

import static org.knime.core.data.v2.TableExtractorUtil.restrictToIndex;

import java.util.function.BiFunction;
import java.util.function.Function;

import org.apache.commons.lang3.NotImplementedException;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataType;
import org.knime.core.data.DataValue;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.IntValue;
import org.knime.core.data.MissingValueException;
import org.knime.core.data.StringValue;

/**
 * Utility methods to extract values from {@link RowRead} instances.
 *
 * @author Paul Bärnreuther
 * @since 5.1
 * @noreference Not public API, for internal use only.
 */
public final class RowReadUtil {

    private RowReadUtil() {
        // utility class
    }

    /**
     * @param row a {@link RowRead}.
     * @return the {@link String} id of the row.
     */
    public static String readRowId(final RowRead row) {
        return row.getRowKey().getString();
    }

    /**
     * A method used to convert an entry of a {@link RowValueRead} to a String. Note that the compatibility of the
     * column with {@link StringValue} is assumed and not checked here.
     *
     * @param row the {@link RowValueRead}.
     * @param colIndex the index of the column that is to be read.
     * @return A {@link String} extracted from the DataValue at the {@colIndex} of the {@row}.
     */
    public static String readStringValue(final RowValueRead row, final int colIndex) {
        if (row.isMissing(colIndex)) {
            return null;
        }
        StringValue value = row.getValue(colIndex);
        return value.getStringValue();
    }

    /**
     * A method used to convert an entry of a {@link RowValueRead} to a Double. Note that the compatibility of the
     * column with {@link DoubleValue} is assumed and not checked here.
     *
     * @param row the {@link RowValueRead}.
     * @param colIndex the index of the column that is to be read.
     * @return A {@link Double} extracted from the DataValue at the {@colIndex} of the {@row}.
     */
    public static Double readDoubleValue(final RowValueRead row, final int colIndex) {
        if (row.isMissing(colIndex)) {
            return null;
        }
        DoubleValue value = row.getValue(colIndex);
        return value.getDoubleValue();
    }

    /**
     * A method used to convert an entry of a {@link RowValueRead} to an Integer. Note that the compatibility of the
     * column with {@link DoubleValue} is assumed and not checked here.
     *
     * @param row the {@link RowValueRead}.
     * @param colIndex the index of the column that is to be read.
     * @return A {@link Integer} extracted from the DataValue at the {@colIndex} of the {@row}.
     */
    public static Integer readIntValue(final RowValueRead row, final int colIndex) {
        if (row.isMissing(colIndex)) {
            return null;
        }
        IntValue value = row.getValue(colIndex);
        return value.getIntValue();
    }

    /**
     * A method used to convert an entry of a {@link RowValueRead} to a primitive double. Note that the compatibility of
     * the column with {@link DoubleValue} is assumed and not checked here.
     *
     * @param row the {@link RowValueRead}.
     * @param colIndex the index of the column that is to be read.
     * @return A primitive double extracted from the DataValue at the {@colIndex} of the {@row}.
     */
    public static int readPrimitiveIntValue(final RowValueRead row, final int colIndex) {
        if (row.isMissing(colIndex)) {
            throw new MissingValueException("Cell is missing.");
        }
        IntValue value = row.getValue(colIndex);
        return value.getIntValue();
    }

    /**
     * @param value the value retrieved from a {@link RowValueRead}
     * @return the materialized DataCell
     */
    public static DataCell getCell(final DataValue value) {
        if (value instanceof DataCell cell) {
            return cell;
        } else if (value instanceof ReadValue readValue) {
            return readValue.getDataCell();
        } else {
            throw new IllegalArgumentException(
                "The DataValue '%s' is neither a DataCell nor a ReadValue.".formatted(value));
        }
    }

    /**
     * Returns a reader function that maps the value into a string value for the given {@link DataType} and column
     * index, only numeric and text based types are supported
     *
     * @param type {@link DataType} of the column the returned function will read
     * @param colIndex index of the column the returned function will read from
     * @return A function that reads an specific column in a row, and returns the value mapped onto String
     */
    public static Function<RowRead, String> getValueReader(final DataType type, final int colIndex) {
        BiFunction<RowRead, Integer, String> readValue = null;
        if (type.isCompatible(StringValue.class)) {
            readValue = RowReadUtil::readStringValue;
        } else if (type.isCompatible(DoubleValue.class)) {
            final BiFunction<RowRead, Integer, Double> readDoubleValue = RowReadUtil::readDoubleValue;
            readValue = readDoubleValue.andThen(String::valueOf);
        } else {
            throw new NotImplementedException(String.format("There is no reader for type %s implemented yet", type));
        }
        return restrictToIndex(readValue, colIndex);
    }

    /**
     * A method used to convert an entry of a {@link RowValueRead} to a primitive double. Note that the compatibility of
     * the column with {@link DoubleValue} is assumed and not checked here.
     *
     * @param row the {@link RowValueRead}.
     * @param colIndex the index of the column that is to be read.
     * @return A primitive double extracted from the DataValue at the {@colIndex} of the {@row}.
     */
    public static double readPrimitiveDoubleValue(final RowValueRead row, final int colIndex) {
        if (row.isMissing(colIndex)) {
            throw new MissingValueException("Cell is missing.");
        }
        DoubleValue value = row.getValue(colIndex);
        return value.getDoubleValue();
    }
}
