/*
 * ------------------------------------------------------------------------
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
 * -------------------------------------------------------------------
 *
 * History
 *   19.04.2005 (cebron): created
 */
package org.knime.base.node.preproc.pmml.normalize;

import org.knime.base.data.normalize.Normalizer;
import org.knime.base.data.normalize.Normalizer2;
import org.knime.base.data.normalize.PMMLNormalizeTranslator;
import org.knime.base.node.preproc.normalize2.Normalizer2NodeModel;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;
import org.knime.core.node.port.pmml.PMMLPortObject;
import org.knime.core.node.port.pmml.PMMLPortObjectSpec;
import org.knime.core.node.port.pmml.PMMLPortObjectSpecCreator;
import org.knime.core.node.port.pmml.preproc.DerivedFieldMapper;

/**
 * The NormalizeNodeModel uses the Normalizer to normalize the input DataTable.
 *
 * @see Normalizer
 * @author Nicolas Cebron, University of Konstanz
 * @since 3.0
 */
public abstract class AbstractNormalizerPMMLNodeModel extends Normalizer2NodeModel {

    private boolean m_hasModelIn = false;

    /**
     * @param modelPortType the type of the optional model input port
     */
    public AbstractNormalizerPMMLNodeModel(final PortType modelPortType) {
        super(modelPortType);
        m_hasModelIn = true;
    }

    /**
     * @param modelPortType the type of the optional model input port
     */
    public AbstractNormalizerPMMLNodeModel(final PortType inModelPortType, final PortType outModelPortType) {
        super(inModelPortType, outModelPortType);
        m_hasModelIn = inModelPortType != null;
    }

    /**
     */
    public AbstractNormalizerPMMLNodeModel() {
        super();
    }

    /**
     * @param inSpecs An array of DataTableSpecs (as many as this model has
     *            inputs).
     * @return An array of DataTableSpecs (as many as this model has outputs)
     *
     * @throws InvalidSettingsException if the <code>#configure()</code> failed,
     *             that is, the settings are inconsistent with given
     *             DataTableSpec elements.
     */
    @Override
    protected PortObjectSpec[] prepareConfigure(
            final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
        DataTableSpec spec = (DataTableSpec)inSpecs[0];
        PMMLPortObjectSpecCreator pmmlSpecCreator;
        if (m_hasModelIn) {
            PMMLPortObjectSpec pmmlSpec = (PMMLPortObjectSpec)inSpecs[1];

            // extract selected numeric columns
            updateNumericColumnSelection(spec);
            if (getMode() == NONORM_MODE) {
                return new PortObjectSpec[]{spec, pmmlSpec};
            }
            pmmlSpecCreator
                    = new PMMLPortObjectSpecCreator(pmmlSpec, spec);
        } else {
            // extract selected numeric columns
            updateNumericColumnSelection(spec);
            if (getMode() == NONORM_MODE) {
                return new PortObjectSpec[]{spec};
            }

            pmmlSpecCreator
                    = new PMMLPortObjectSpecCreator(spec);
        }

        return new PortObjectSpec[]{
                Normalizer2.generateNewSpec(spec, getColumns()),
                pmmlSpecCreator.createSpec()};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected PortObject[] execute(final PortObject[] inObjects,
            final ExecutionContext exec) throws Exception {
        CalculationResult result = calculate(inObjects, exec);

        BufferedDataTable outTable = result.getDataTable();

        // the optional PMML in port (can be null)
        PMMLPortObject inPMMLPort = m_hasModelIn ? (PMMLPortObject)inObjects[1] : null;
        PMMLNormalizeTranslator trans = new PMMLNormalizeTranslator(
                result.getConfig(), new DerivedFieldMapper(inPMMLPort));

        DataTableSpec dataTableSpec = (DataTableSpec)inObjects[0].getSpec();
        PMMLPortObjectSpecCreator creator = new PMMLPortObjectSpecCreator(
                inPMMLPort, dataTableSpec);
        PMMLPortObject outPMMLPort = new PMMLPortObject(
               creator.createSpec(), inPMMLPort);
        outPMMLPort.addGlobalTransformations(trans.exportToTransDict());

        return new PortObject[] {outTable, outPMMLPort};
    }
}
