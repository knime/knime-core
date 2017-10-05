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
 *   May 3, 2015 (wiswedel): created
 */
package org.knime.base.data.filter.column;

import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.streamable.RowInput;
import org.knime.core.node.util.CheckUtils;

/**
 * A {@link RowInput} that wraps another input and hides/re-orders columns according to an int[].
 * @author Bernd Wiswedel, KNIME.com, Zurich, Switzerland
 * @since 2.12
 */
public final class FilterColumnRowInput extends RowInput {

    private final RowInput m_input;
    private final DataTableSpec m_spec;
    private final int[] m_includes;

    /**
     * New input only containing the columns as per array argument.
     *
     * @param input The non-null input to wrap.
     * @param includes An non-null array with unique indices of columns to have in this input.
     */
    public FilterColumnRowInput(final RowInput input, final int... includes) {
        m_input = CheckUtils.checkArgumentNotNull(input);
        m_includes = CheckUtils.checkArgumentNotNull(includes);
        m_spec = FilterColumnTable.createFilterTableSpec(input.getDataTableSpec(), includes);
    }

    /**
     * New input only containing the columns as per array argument.
     *
     * @param input The non-null input to wrap.
     * @param includes An non-null array with unique names of columns to have in this input.
     */
    public FilterColumnRowInput(final RowInput input, final String... includes) {
        this(input, FilterColumnTable.findColumnIndices(input.getDataTableSpec(), includes));
    }

    /** {@inheritDoc} */
    @Override
    public DataTableSpec getDataTableSpec() {
        return m_spec;
    }

    /** {@inheritDoc} */
    @Override
    public DataRow poll() throws InterruptedException {
        final DataRow reference = m_input.poll();
        return reference == null ? null : new FilterColumnRow(reference, m_includes);
    }

    /** {@inheritDoc} */
    @Override
    public void close() {
        m_input.close();
    }

}
