/*
 * ------------------------------------------------------------------------
 *
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
 * History
 *   24.05.2016 (Adrian Nembach): created
 */
package org.knime.base.node.meta.feature.selection;

import java.util.Collection;
import java.util.List;

import org.knime.core.data.DataTableSpec;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.util.Pair;

/**
 * Classes that implement this interface can be used in a {@link FeatureSelector} in order
 * to handle columns of different types in a different way (type refers here to ordinary vs. vector type). <br>
 * The idea behind this concept is that vector columns can either be treated as single feature or as multiple features during
 * the feature selection process. <br>
 * The communication with the ColumnHandler works via indices that identify single features.
 *
 * @author Adrian Nembach, KNIME.com
 */
interface ColumnHandler {

    public BufferedDataTable[] getTables(final ExecutionContext exec, final BufferedDataTable[] inTables,
        final Collection<Integer> included, final boolean includeConstantColumns) throws CanceledExecutionException;

    /**
     *
     * @return a list of Integers that represent the available features.
     */
    public List<Integer> getAvailableFeatures();

    /**
     *
     * @return A list containing the names of the constant columns
     */
    public List<String> getConstantColumns();

    /**
     * Returns the names for the columns of the included features and if specified the constant columns
     *
     * @param features the indices of the features that should be included
     * @param includeConstantColumns set to true if the constant columns should be included
     * @return a collection containing the names of the included columns
     */
    public Collection<String> getIncludedColumns(final Collection<Integer> features, final boolean includeConstantColumns);

    /**
     * Returns the names for the columns corresponding to the features that correspond to the indices in <b>features</b>.
     *
     * @param features the indices of the features whose names should be returned
     * @return the names of the features corresponding to the indices contained in <b>features</b>
     */
    public Collection<String> getColumnNamesFor(final Collection<Integer> features);

    /**
     *
     * @param feature
     * @return name of the feature with id <b>feature</b>
     */
    public String getColumnNameFor(final Integer feature);

    /**
     * @param features the indices of the features that should be included
     * @param inSpec the incoming {@link DataTableSpec}
     * @param includeConstantColumns whether constant columns should be included
     * @return a {@link DataTableSpec} that only contains the features specified by <b>features</b> and if desired the constant columns.
     */
    public DataTableSpec getOutSpec(final List<Integer> features, final DataTableSpec inSpec,
        final boolean includeConstantColumns);

    /**
     * Returns a Pair that contains a warning message if some column is missing in <b>inSpec</b> and the corresponding outSpec
     *
     * @param includedFeatures
     * @param inSpec
     * @param includeConstantColumns
     * @return a Pair consisting of a warning message (null if no warning exists) and a DataTableSpec
     * @throws InvalidSettingsException
     */
    public Pair<String, DataTableSpec> getOutSpecAndWarning(final Collection<Integer> includedFeatures, final DataTableSpec inSpec, final boolean includeConstantColumns) throws InvalidSettingsException;

}
