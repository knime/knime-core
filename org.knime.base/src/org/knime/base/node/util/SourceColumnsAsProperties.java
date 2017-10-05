/*
 * ------------------------------------------------------------------------
 *  Copyright by KNIME GmbH, Konstanz, Germany
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
 * ---------------------------------------------------------------------
 *
 * Created on 2014.01.15. by gabor
 */
package org.knime.base.node.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.regex.Pattern;

import org.knime.core.data.DataColumnProperties;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.defaultnodesettings.SettingsModelColumnFilter2;
import org.knime.core.node.util.filter.NameFilterConfiguration.FilterResult;

/**
 * A helper class to create {@link DataColumnProperties} from the input column names for possible reverse of the
 * process.
 *
 * @author Gabor Bakos
 * @since 2.10
 */
public class SourceColumnsAsProperties {
    /** The key for the property containing the original column indices. */
    public static final String PROPKEY_SOURCE_COLUMN_INDICES = "Source column indices";

    /**
     * Creates the {@link DataColumnProperties} with the
     * {@link #PROPKEY_SOURCE_COLUMN_INDICES} keys to the column names and column values respectively.
     *
     * @param selection The model for the selected columns.
     * @param input The input {@link DataTableSpec}.
     * @return The properties with the column names and column indices encoded as a string value for the specified keys.
     */
    public static DataColumnProperties toProperties(final SettingsModelColumnFilter2 selection,
        final DataTableSpec input) {
        Map<String, String> map = new HashMap<String, String>();
        FilterResult filterResult = selection.applyTo(input);
        map.put(PROPKEY_SOURCE_COLUMN_INDICES, indicesAsString(filterResult, input));
        return new DataColumnProperties(map);
    }

    /// Delimiter used to separate the values.
    private static final String DELIMITER = ", ";
    private static final String DELIMITER_FOR_REGEX = Pattern.quote(DELIMITER);
    static {
        assert DELIMITER.charAt(0) != '\\';
    }

    /**
     * @param filterResult The selected columns.
     * @param input The input table.
     * @return The indices of the selected columns as a {@link String}.
     */
    private static String indicesAsString(final FilterResult filterResult, final DataTableSpec input) {
        int[] indices = indices(filterResult, input);
        return indicesAsString(indices);
    }

    /**
     * @param indices An array of ints.
     * @return {@code indices} encoded as a {@link String}.
     */
    public static String indicesAsString(final int[] indices) {
        StringBuilder sb = new StringBuilder(indices.length * 5);
        for (int i : indices) {
            sb.append(i).append(DELIMITER);
        }
        if (sb.length() > 0) {
            sb.setLength(sb.length() - DELIMITER.length());
        }
        return sb.toString();
    }

    /**
     * Finds the column indices for the included column names.
     *
     * @param filterResult A {@link FilterResult}.
     * @param input A {@link DataTableSpec}.
     * @return An array of ints where the values are the indices of columns in {@code input}, else (not found case:)
     *         {@code -1}
     */
    public static int[] indices(final FilterResult filterResult, final DataTableSpec input) {
        String[] includes = filterResult.getIncludes();
        return indices(includes, input);
    }

    /**
     * Finds the indices for the {@code includes} column names.
     *
     * @param includes The name of the columns.
     * @param input A {@link DataTableSpec}.
     * @return An array of ints where the values are the indices of columns in {@code input}, else (not found case:)
     *         {@code -1}
     */
    public static int[] indices(final String[] includes, final DataTableSpec input) {
        int[] ret = new int[includes.length];
        for (int i = ret.length; i-- > 0;) {
            ret[i] = input.findColumnIndex(includes[i]);
        }
        return ret;
    }

    /**
     * @param inputSpec The input column spec, that contains the properties for indices.
     * @return The indices specified, or an empty array if there was an error.
     */
    public static int[] indicesFrom(final DataColumnSpec inputSpec) {
        String property = inputSpec.getProperties().getProperty(PROPKEY_SOURCE_COLUMN_INDICES, "");
        String[] strings = property.split(DELIMITER_FOR_REGEX, -1);

        int[] ret = new int[strings.length];
        try {
            for (int i = 0; i < ret.length; i++) {
                ret[i] = Integer.parseInt(strings[i]);
            }
        } catch (NumberFormatException e) {
            return new int[0];
        }
        return ret;
    }

    /**
     * Sorts the {@code names} according to the {@code indices} ascending order.
     *
     * @param names Some {@link String}s.
     * @param indices The (unique!) indices.
     * @return The {@code names} ordered by {@code indices}, if there are less {@code indices}, the rest of the {@code names} are copied to the end.
     * @throws IllegalArgumentException There were duplicate values in {@code indices}.
     */
    public static List<String> sortNamesAccordingToIndex(final List<String> names, final int[] indices) throws IllegalArgumentException {
        List<String> ns = new ArrayList<>(names);
        int[] is = indices.clone();
        int min = Math.min(ns.size(), is.length);
        SortedMap<Integer, String> map = new TreeMap<Integer, String>();
        for (int i = min; i-->0;) {
            String previous = map.put(is[i], ns.get(i));
            if (previous != null) {
                throw new IllegalArgumentException("The index " + is[i] + " occures multiple times in: " + Arrays.toString(is));
            }
        }
        int i = 0;
        for (Entry<Integer, String> entry : map.entrySet()) {
            ns.set(i++, entry.getValue());
        }
        return ns;
    }
}
