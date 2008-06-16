/* 
 * ------------------------------------------------------------------
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
 * --------------------------------------------------------------------- *
 * 
 * History
 *   20.02.2008 (gabriel): created
 */
package org.knime.base.node.mine.bfn.fuzzy;

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
import org.knime.core.node.PortObjectSpec;
import org.knime.core.node.PortType;

/**
 * 
 * @author Thomas Gabriel, University of Konstanz
 */
public final class FuzzyBasisFunctionPortObject 
        implements BasisFunctionPortObject {

    /** The <code>PortType</code> for basisfunction models. */
    public static final PortType TYPE = new PortType(
            FuzzyBasisFunctionPortObject.class);
    
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
    public FuzzyBasisFunctionPortObject(final BasisFunctionModelContent cont) {
        m_content = cont;
    }
    
    /**
     * {@inheritDoc}
     */
    public BasisFunctionPortObject createPortObject(
            final BasisFunctionModelContent content) {
        return new FuzzyBasisFunctionPortObject(content);
    }
    
    /**
     * @return Serializer for the {@link FuzzyBasisFunctionPortObject}
     */
    static PortObjectSerializer<FuzzyBasisFunctionPortObject> 
        getPortObjectSerializer() {
        return new PortObjectSerializer<FuzzyBasisFunctionPortObject>() {

            /** {@inheritDoc} */
            @Override
            protected void savePortObject(
                    final FuzzyBasisFunctionPortObject portObject,
                    final File directory, final ExecutionMonitor exec)
                    throws IOException, CanceledExecutionException {
                portObject.save(directory);
            }

            /** {@inheritDoc} */
            @Override
            protected FuzzyBasisFunctionPortObject loadPortObject(
                    final File directory, final PortObjectSpec spec, 
                    final ExecutionMonitor exec)
                    throws IOException, CanceledExecutionException {
                return FuzzyBasisFunctionPortObject.load(directory,
                        (DataTableSpec) spec);
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
    private static FuzzyBasisFunctionPortObject load(final File dir,
            final DataTableSpec spec) throws IOException {
        // TODO use spec
        return new FuzzyBasisFunctionPortObject(
                BasisFunctionModelContent.load(dir, new FuzzyCreator()));
    }
    
    /**
     * Used to create fuzzy predictor rows.
     */
    public static class FuzzyCreator implements Creator {
        /**
         * {@inheritDoc}
         */
        public BasisFunctionPredictorRow createPredictorRow(
                final ModelContentRO pp)
                throws InvalidSettingsException {
            return new FuzzyBasisFunctionPredictorRow(pp);
        }
    }
    
    
    
}
