/* 
 * ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2007
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
 * --------------------------------------------------------------------- *
 * 
 * History
 *   20.02.2008 (gabriel): created
 */
package org.knime.base.node.mine.bfn.radial;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.knime.base.node.mine.bfn.BasisFunctionModelContent;
import org.knime.base.node.mine.bfn.BasisFunctionPortObject;
import org.knime.base.node.mine.bfn.BasisFunctionPredictorRow;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.ModelContentRO;
import org.knime.core.node.PortType;

/**
 * 
 * @author Thomas Gabriel, University of Konstanz
 */
public final class RadialBasisFunctionPortObject 
        implements BasisFunctionPortObject {

    /** The <code>PortType</code> for basisfunction models. */
    public static final PortType TYPE = new PortType(DataTableSpec.class, 
            RadialBasisFunctionPortObject.class);
    
    private final BasisFunctionModelContent m_content;
    
    /**
     * {@inheritDoc}
     */
    public DataTableSpec getSpec() {
        return m_content.getSpec();
    }
    
    /**
     * @return basisfunctions rules by class
     */
    public Map<DataCell, List<BasisFunctionPredictorRow>> getBasisFunctions() {
        return m_content.getBasisFunctions();
    }
    
    /**
     * Creates a new basis function model object.
     * @param cont basisfunction model content containing rules and spec
     */
    public RadialBasisFunctionPortObject(final BasisFunctionModelContent cont) {
        m_content = cont;
    }
    
    /**
     * @return Serializer for the {@link RadialBasisFunctionPortObject}
     */
    static PortObjectSerializer<RadialBasisFunctionPortObject> 
        getPortObjectSerializer() {
        return new PortObjectSerializer<RadialBasisFunctionPortObject>() {

            /** {@inheritDoc} */
            @Override
            protected void savePortObject(
                    final RadialBasisFunctionPortObject portObject,
                    final File directory, final ExecutionMonitor exec)
                    throws IOException, CanceledExecutionException {
                portObject.save(directory);
            }

            /** {@inheritDoc} */
            @Override
            protected RadialBasisFunctionPortObject loadPortObject(
                    final File directory, final ExecutionMonitor exec)
                    throws IOException, CanceledExecutionException {
                return RadialBasisFunctionPortObject.load(directory);
            }
        };
    }
    
    /**
     * Save the given rule model and model spec into this model content object.
     * @param bfs the rules to save
     * @param spec the model spec to save
     */
    private void save(final File dir) throws IOException {
        m_content.save(dir);
    }
    
    /**
     * Reads the rule model used for prediction from the
     * <code>ModelContentRO</code> object.
     * @param model predictor model used to create the rules
     * @return a list of basisfunction rules
     * @throws InvalidSettingsException if the model contains invalid settings
     */
    private static RadialBasisFunctionPortObject load(final File dir) 
            throws IOException {
        return new RadialBasisFunctionPortObject(
                BasisFunctionModelContent.load(dir, new RadialCreator()));
    }

    /**
     * {@inheritDoc}
     */
    public BasisFunctionPortObject createPortObject(
            final BasisFunctionModelContent content) {
        return new RadialBasisFunctionPortObject(content);
    }
    
    /**
     * Used to create PNN predictor rows.
     */
    public static class RadialCreator implements Creator {
        /**
         * {@inheritDoc}
         */
        public BasisFunctionPredictorRow createPredictorRow(
                final ModelContentRO pp)
                throws InvalidSettingsException {
            return new RadialBasisFunctionPredictorRow(pp);
        }
    }
    
}
