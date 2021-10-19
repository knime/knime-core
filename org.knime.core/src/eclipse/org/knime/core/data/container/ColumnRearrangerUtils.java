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
 *   Jul 29, 2021 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.core.data.container;

import org.knime.core.data.DataTableSpec;
import org.knime.core.data.container.ColumnRearranger.SpecAndFactoryObject;
import org.knime.core.node.util.CheckUtils;

/**
 * Utility class used by the framework to extract information from a {@link ColumnRearranger}.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 * @noreference This class is not intended to be referenced by clients.
 * @since 4.5
 */
public final class ColumnRearrangerUtils {

    /**
     * Checks if the provided rearranger and spec are compatible i.e. their structure (names and types) match.
     *
     * @param rearranger to check
     * @param spec to check
     */
    public static void checkSpecCompatibility(final ColumnRearranger rearranger, final DataTableSpec spec) {
        CheckUtils.checkArgument(isCompatible(rearranger, spec),
            "The argument table's spec does not match the original spec passed in the constructor.");
    }

    private static boolean isCompatible(final ColumnRearranger rearranger, final DataTableSpec spec) {
        final DataTableSpec originalSpec = rearranger.getOriginalSpec();
        return originalSpec.equalStructure(spec);
    }

    /**
     * Checks if the rearranger only performs permutation and filter operations.
     *
     * @param rearranger to check
     * @return {@code true} if the rearranger performs only permutation and filter operations
     */
    public static boolean addsNoNewColumns(final ColumnRearranger rearranger) {
        return rearranger.getIncludes().stream()//
            .noneMatch(SpecAndFactoryObject::isNewColumn);
    }

    /**
     * Extracts the original column indices (= index in the original table) for the columns in the rearranger.
     * NOTE: Only use if the rearranger doesn't add new columns
     *
     * @param rearranger to extract the column indices from
     * @return the original column indices
     * @throws IllegalArgumentException if the rearranger adds any new columns
     * @see #addsNoNewColumns(ColumnRearranger)
     */
    public static int[] extractOriginalIndicesOfIncludedColumns(final ColumnRearranger rearranger) {
        CheckUtils.checkArgument(addsNoNewColumns(rearranger),
            "Coding error: Only use this method if no new columns were added.");
        return rearranger.getIncludes().stream()//
            .mapToInt(SpecAndFactoryObject::getOriginalIndex)//
            .toArray();
    }

    /**
     * Extracts a boolean array from the provided rearranger that indicates whether a column is from the original table
     * or from the append table.
     *
     * @param rearranger to extract the information from
     * @return array indicating whether columns are from the original table or the append table
     */
    public static boolean[] extractIsFromOriginal(final ColumnRearranger rearranger) {
        final var fromOriginal = new boolean[rearranger.getColumnCount()];
        int idx = 0;
        for (var s : rearranger.getIncludes()) {
            fromOriginal[idx] = s.isNewColumn();
            idx++;
        }
        return fromOriginal;
    }

    private ColumnRearrangerUtils() {

    }
}
