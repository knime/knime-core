/* 
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2011
 *  University of Konstanz, Germany and
 *  KNIME GmbH, Konstanz, Germany
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
 */
package org.knime.base.node.io.def;

import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeView;

/**
 * Its the factory for a
 * {@link DefaultTableNodeModel}. It will
 * produce a {@link org.knime.core.node.NodeModel} with a constant
 * {@link org.knime.core.data.DataTable} which was defined by the
 * parameters passed to the factory's constructor. No
 * {@link org.knime.core.node.NodeDialogPane} or
 * {@link org.knime.core.node.NodeView} is available.
 * 
 * @author Peter Ohl, University of Konstanz
 */
public class DefaultTableNodeFactory extends NodeFactory {

    /*
     * depending on which constructor was used, we need to store two different
     * sets of parameters. We call them set1 and set2.
     */
    private boolean m_set1;

    private DataRow[] m_rows;

    private DataTableSpec m_spec;

    private boolean m_set2;

    private Object[][] m_data;

    private String[] m_rowHeader;

    private String[] m_colHeader;

    /**
     * We provide the same constructors as the
     * {@link org.knime.core.data.def.DefaultTable}.
     * 
     * @see org.knime.core.data.def.DefaultTable
     * @param rows see DefaultTable constructor
     * @param columnNames see DefaultTable constructor
     * @param columnTypes see DefaultTable constructor
     */
    public DefaultTableNodeFactory(final DataRow[] rows,
            final String[] columnNames, final DataType[] columnTypes) {
        this(rows, new DataTableSpec(columnNames, columnTypes));
    }

    /**
     * Also this constructor is available in
     * {@link org.knime.core.data.def.DefaultTable}.
     * 
     * @param rows Passed to constructor of
     *            {@link org.knime.core.data.def.DefaultTable}
     * @param spec Passed to constructor of
     *            {@link org.knime.core.data.def.DefaultTable}
     * @see org.knime.core.data.def.DefaultTable#DefaultTable( DataRow[],
     *      DataTableSpec)
     */
    public DefaultTableNodeFactory(final DataRow[] rows,
            final DataTableSpec spec) {
        m_set1 = true;
        m_rows = rows;
        m_spec = spec;
    }

    /**
     * We provide the same constructors as the
     * {@link org.knime.core.data.def.DefaultTable}.
     * 
     * @see org.knime.core.data.def.DefaultTable
     * @param data see DefaultTable constructor
     * @param rowHeader see DefaultTable constructor
     * @param colHeader see DefaultTable constructor
     */
    public DefaultTableNodeFactory(final Object[][] data,
            final String[] rowHeader, final String[] colHeader) {

        m_set2 = true;

        m_data = data;
        m_rowHeader = rowHeader;
        m_colHeader = colHeader;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NodeModel createNodeModel() {
        if (m_set1) {
            return new DefaultTableNodeModel(m_rows, m_spec);
        }
        if (m_set2) {
            return new DefaultTableNodeModel(m_data, m_rowHeader, m_colHeader);

        }
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getNrNodeViews() {
        return 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NodeView createNodeView(final int i, final NodeModel nodeModel) {
        throw new InternalError();
    }

    /**
     * @return <b>false</b>.
     * @see org.knime.core.node.NodeFactory#hasDialog()
     */
    @Override
    public boolean hasDialog() {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NodeDialogPane createNodeDialogPane() {
        throw new InternalError();
    }
}
