/*
 * ------------------------------------------------------------------------
 *  Copyright by KNIME AG, Zurich, Switzerland
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
 * ------------------------------------------------------------------------
 */
package org.knime.base.node.preproc.pmml.normalize;

import org.knime.base.data.normalize.AffineTransConfiguration;
import org.knime.base.data.normalize.AffineTransTable;
import org.knime.base.data.normalize.PMMLNormalizeTranslator;
import org.knime.base.node.preproc.normalize.NormalizerApplyNodeModel;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.pmml.PMMLPortObject;
import org.knime.core.node.streamable.InputPortRole;
import org.knime.core.node.streamable.OutputPortRole;
import org.knime.core.node.streamable.PartitionInfo;
import org.knime.core.node.streamable.StreamableOperator;

public class NormalizerPMMLApplyNodeModel extends NormalizerApplyNodeModel {
    /**
    *
    */
   public NormalizerPMMLApplyNodeModel() {
       super(PMMLPortObject.TYPE, true);
   }

   /**
    * {@inheritDoc}
    */
   @Override
   protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs)
           throws InvalidSettingsException {
      /* So far we can only get all preprocessing fields from the PMML port
       * object spec. There is no way to determine from the spec which
       * of those fields contain normalize operations. Hence we cannot
       * determine the data table output spec at this point.
       * Bug 2985
       *
       */
      return new PortObjectSpec[]{inSpecs[0], null};
   }

   /**
    * {@inheritDoc}
    */
   @Override
   protected PortObject[] execute(final PortObject[] inData,
           final ExecutionContext exec) throws Exception {
       PMMLPortObject model = (PMMLPortObject)inData[0];
       BufferedDataTable table = (BufferedDataTable)inData[1];

       PMMLNormalizeTranslator translator = new PMMLNormalizeTranslator();
       translator.initializeFrom(model.getDerivedFields());
       AffineTransConfiguration config = getAffineTrans(
                 translator.getAffineTransConfig());
       if (config.getNames().length == 0) {
           throw new IllegalArgumentException("No normalization configuration "
                   + "found.");
       }
       AffineTransTable t = new AffineTransTable(table, config);
       BufferedDataTable bdt = exec.createBufferedDataTable(t, exec);
       if (t.getErrorMessage() != null) {
           setWarningMessage(t.getErrorMessage());
       }
       return new PortObject[]{model, bdt};
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public StreamableOperator createStreamableOperator(final PartitionInfo partitionInfo, final PortObjectSpec[] inSpecs)
       throws InvalidSettingsException {
       //PMML normalizer node is not streamable nor distributable since the data table spec is not available during during configure
       //call default implementation of this method (delegated by the super-method)
       return super.createStreamableOperator(partitionInfo, inSpecs);
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public InputPortRole[] getInputPortRoles() {
       return new InputPortRole[]{InputPortRole.NONDISTRIBUTED_NONSTREAMABLE, InputPortRole.NONDISTRIBUTED_NONSTREAMABLE};
   }

    /**
     * {@inheritDoc}
     */
    @Override
    public OutputPortRole[] getOutputPortRoles() {
        return new OutputPortRole[]{OutputPortRole.NONDISTRIBUTED, OutputPortRole.NONDISTRIBUTED};
    }


   /**
    * Return the configuration with possible additional transformations made.
    *
    * @param affineTransConfig the original affine transformation configuration.
    * @return the (possible modified) configuration.
    */
    @Override
    protected AffineTransConfiguration getAffineTrans(
                    final AffineTransConfiguration affineTransConfig) {
              return affineTransConfig;
    }

}
