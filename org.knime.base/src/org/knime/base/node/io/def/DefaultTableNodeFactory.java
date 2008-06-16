/* 
 * ---------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2008
 * University of Konstanz, Germany
 * Chair for Bioinformatics and Information Mining (Prof. M. Berthold)
 * and KNIME GmbH, Konstanz, Germany
 *
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner or
 * as specified in the license file distributed with this product.
 *
 * If you have any questions please contact the copyright holder:
 * website: www.knime.org
 * email: contact@knime.org
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
