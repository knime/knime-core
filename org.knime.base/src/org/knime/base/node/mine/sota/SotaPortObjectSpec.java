/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2009
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
 *   16.09.2008 (thiel): created
 */
package org.knime.base.node.mine.sota;

import org.knime.core.data.DataTableSpec;
import org.knime.core.data.StringValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.ModelContentRO;
import org.knime.core.node.ModelContentWO;
import org.knime.core.node.port.AbstractSimplePortObjectSpec;

/**
 * Provides the spec of the sota port model and a validation method, to 
 * validate if a certain data table spec is compatible to the model spec.
 * 
 * @author Kilian Thiel, University of Konstanz
 */
public class SotaPortObjectSpec extends AbstractSimplePortObjectSpec {    
    
    private DataTableSpec m_spec;
    
    private int m_indexOfClassCol;
    
    /**
     * 
     */
    public SotaPortObjectSpec() { }
    
    /**
     * Creates a new instance of <code>SotaPortObjectSpec</code> with the given
     * data table spec, as the main part of the model spec and the index of the
     * class column.
     * 
     * @param spec The data table spec to store.
     * @param indexOfClassCol The index of the class column.
     */
    public SotaPortObjectSpec(final DataTableSpec spec,
            final int indexOfClassCol) {
        m_spec = spec;
        m_indexOfClassCol = indexOfClassCol;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void load(final ModelContentRO model) 
    throws InvalidSettingsException {
        ModelContentRO subContent = model.getModelContent(
                SotaPortObject.CFG_KEY_SPEC);
        m_spec = DataTableSpec.load(subContent);
        m_indexOfClassCol = model.getInt(SotaPortObject.CFG_KEY_CLASSCOL_INDEX);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void save(final ModelContentWO model) {
        if (m_spec != null) {
            ModelContentWO subContent = model.addModelContent(
                    SotaPortObject.CFG_KEY_SPEC);
            m_spec.save(subContent);
            model.addInt(SotaPortObject.CFG_KEY_CLASSCOL_INDEX, 
                    m_indexOfClassCol);
        }
    }

    /**
     * @return the spec
     */
    public DataTableSpec getSpec() {
        return m_spec;
    }
    
    /**
     * Validates if the given data table spec is compatible to the model port 
     * spec and returns <code>true</code> if so, otherwise <code>false</code>.
     * 
     * @param spec The data table spec to validate.
     * @return <code>true</code> if given spec is compatible to the model port 
     * spec, otherwise <code>false</code>.
     */
    public boolean validateSpec(final DataTableSpec spec) {
        if (m_spec == null || spec == null) {
            return false;
        }
        if (spec.getNumColumns() > m_spec.getNumColumns()
                || spec.getNumColumns() < m_spec.getNumColumns() - 1) {
            return false;
        }
        for (int i = 0; i < m_spec.getNumColumns(); i++) {
            if (spec.getNumColumns() == m_spec.getNumColumns()) {
                if (!m_spec.getColumnSpec(i).getType().equals(
                        spec.getColumnSpec(i).getType())) {
                    return false;
                }
            } else {
                if (i < spec.getNumColumns()) {
                    if (!m_spec.getColumnSpec(i).getType().equals(
                            spec.getColumnSpec(i).getType())) {
                        return false;
                    }
                }                
            }
        }
        
        return true;
    }
    
    /**
     * @return <code>true</code> if internal data table spec contains a column
     * which is compatible to string value.
     */
    public boolean hasClassColumn() {
        if (m_spec == null) {
            return false;
        }
        for (int i = 0; i < m_spec.getNumColumns(); i++) {
            if (m_spec.getColumnSpec(i).getType().isCompatible(
                    StringValue.class)) {
                return true;
            }
        }
        return false;
    }
}
