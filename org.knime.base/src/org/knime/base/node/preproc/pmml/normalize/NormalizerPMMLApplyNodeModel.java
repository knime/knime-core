/* ------------------------------------------------------------------
  * This source code, its documentation and all appendant files
  * are protected by copyright law. All rights reserved.
  *
  * Copyright, 2008 - 2011
  * KNIME.com, Zurich, Switzerland
  *
  * You may not modify, publish, transmit, transfer or sell, reproduce,
  * create derivative works from, distribute, perform, display, or in
  * any way exploit any of the content, in whole or in part, except as
  * otherwise expressly permitted in writing by the copyright owner or
  * as specified in the license file distributed with this product.
  *
  * If you have any questions please contact the copyright holder:
  * website: www.knime.com
  * email: contact@knime.com
  * ---------------------------------------------------------------------
  *
  * History
  *   Apr 6, 2011 (morent): created
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

/**
 * @author Dominik Morent, KNIME.com, Zurich, Switzerland
 *
 */
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
