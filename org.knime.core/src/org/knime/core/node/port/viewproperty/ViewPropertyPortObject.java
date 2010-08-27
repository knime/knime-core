/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2010
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
 * 
 * History
 *   Sep 4, 2008 (wiswedel): created
 */
package org.knime.core.node.port.viewproperty;

import org.knime.core.data.DataTableSpec;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.ModelContentRO;
import org.knime.core.node.ModelContentWO;
import org.knime.core.node.port.AbstractSimplePortObject;
import org.knime.core.node.port.PortObjectSpec;

/**
 * PortObject used to represent visual properties such as color, shape and
 * size information attached to a table. The content of this object resides in 
 * the accompanying {@link DataTableSpec}.
 * @author Bernd Wiswedel, University of Konstanz
 */
public abstract class ViewPropertyPortObject extends AbstractSimplePortObject {
    
    private DataTableSpec m_spec;
    private String m_summary;
    
    /** Public no arg constructor required by super class. 
     * <p>
     * <b>This constructor should only be used by the framework.</b> */
    protected ViewPropertyPortObject() {
    }
    
    /** Constructor used to instantiate this object during a node's execute
     * method.
     * @param spec The accompanying spec
     * @param portSummary A summary returned in the {@link #getSummary()}
     * method.
     * @throws NullPointerException If spec argument is <code>null</code>.
     */
    protected ViewPropertyPortObject(final DataTableSpec spec, 
            final String portSummary) {
        if (spec == null) {
            throw new NullPointerException("Spec must not be null");
        }
        m_spec = spec;
        m_summary = portSummary;
    }

    /** {@inheritDoc} */
    @Override
    protected void load(final ModelContentRO model, final PortObjectSpec spec,
            final ExecutionMonitor exec) throws InvalidSettingsException,
            CanceledExecutionException {
        m_summary = model.getString("summary");
        m_spec = (DataTableSpec)spec;
    }

    /** {@inheritDoc} */
    @Override
    protected void save(final ModelContentWO model, final ExecutionMonitor exec)
            throws CanceledExecutionException {
        model.addString("summary", m_summary);
    }

    /** {@inheritDoc} */
    @Override
    public final DataTableSpec getSpec() {
        return m_spec;
    }

    /** {@inheritDoc} */
    @Override
    public final String getSummary() {
        return m_summary;
    }

}
