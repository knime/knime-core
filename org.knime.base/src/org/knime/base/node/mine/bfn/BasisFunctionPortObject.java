/* 
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2013
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
 * --------------------------------------------------------------------- *
 * 
 * History
 *   14.03.2008 (gabriel): created
 */
package org.knime.base.node.mine.bfn;

import java.util.List;
import java.util.Map;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.ModelContentRO;
import org.knime.core.node.ModelContentWO;
import org.knime.core.node.port.AbstractSimplePortObject;
import org.knime.core.node.port.PortObjectSpec;

/**
 * 
 * @author Thomas Gabriel, University of Konstanz
 */
public abstract class BasisFunctionPortObject extends AbstractSimplePortObject {
    
    
    private BasisFunctionModelContent m_content;
    
    /**
     * Creates a new abstract <code>BasisFunctionPortObject</code>.
     */
    public BasisFunctionPortObject() {
        
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public final DataTableSpec getSpec() {
        return m_content.getSpec();
    }
    
    /**
     * @return basisfunctions rules by class
     */
    public final Map<DataCell, 
            List<BasisFunctionPredictorRow>> getBasisFunctions() {
        return m_content.getBasisFunctions();
    }
    
    /**
     * Creates a new basis function model object.
     * @param cont basisfunction model content containing rules and spec
     */
    public BasisFunctionPortObject(final BasisFunctionModelContent cont) {
        m_content = cont;
    }
    
    /**
     * Creator used to instantiate basisfunction predictor rows.
     */
    public interface Creator {
        /**
         * Return specific predictor row for the given 
         * <code>ModelContent</code>.
         * 
         * @param pp the content the read the predictive row from
         * @return a new predictor row
         * @throws InvalidSettingsException if the rule can be read from model
         *             content
         */
        BasisFunctionPredictorRow createPredictorRow(
                ModelContentRO pp) throws InvalidSettingsException;
    }
    
    /**
     * Create a new basisfunction port object given the model content.
     * @param content basisfunction model content with spec and rules.
     * @return a new basisfunction port object
     */
    public abstract BasisFunctionPortObject createPortObject(
            final BasisFunctionModelContent content);
    
    /** {@inheritDoc} */
    @Override
    public String getSummary() {
        return m_content.toString();
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected void save(final ModelContentWO model, final ExecutionMonitor exec)
            throws CanceledExecutionException {
        m_content.save(model);
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected void load(final ModelContentRO model, final PortObjectSpec spec,
            final ExecutionMonitor exec) throws InvalidSettingsException,
            CanceledExecutionException {
        m_content = new BasisFunctionModelContent(model, getCreator());
    }
    
    /**
     * @return a creator for the desired basisfunction rows
     */
    public abstract Creator getCreator(); 
}
