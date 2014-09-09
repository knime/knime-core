/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by KNIME GmbH, Konstanz, Germany
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
 * ---------------------------------------------------------------------
 *
 * History
 *   20.08.2014 (koetter): created
 */
package org.knime.core.node.port.database.aggregation;

import java.util.List;

import javax.swing.JComponent;

import org.knime.core.data.DataType;

/**
 *
 * @author Tobias Koetter, KNIME.com, Zurich, Switzerland
 * @param <F> the type of {@link AggregationFunction} to return
 * @since 2.11
 */
public interface AggregationFunctionProvider <F extends AggregationFunction> {
    /**
     * @param type the {@link DataType} to check
     * @param sorted <code>true</code> if the compatible methods should be sorted in ascending order by the
     * user displayed label
     * @return all {@link AggregationFunction}s that are compatible with
     * the given {@link DataType} or an empty list if none is compatible
     */
    public List<F> getCompatibleFunctions(final DataType type, final boolean sorted);

    /**
     * @param id the id of the {@link AggregationFunction}
     * @return the {@link AggregationFunction} for the given id or <code>null</code> if none exists
     */
    public F getFunction(String id);

    /**
     * @param type the {@link DataType}
     * @return the default {@link AggregationFunction}
     */
    public F getDefaultFunction(DataType type);

    /**
     * @param sorted <code>true</code> if the list should be sorted by name
     * @return all supported {@link AggregationFunction}s
     */
    public List<F> getFunctions(boolean sorted);

    /**
     * Creates a {@link JComponent} that lists all available aggregation
     * functions including a short description of each.
     *
     * @return a {@link JComponent} that can be added to any dialog to display all available aggregation functions
     * and their description.
     */
    public JComponent getDescriptionPane();
}
