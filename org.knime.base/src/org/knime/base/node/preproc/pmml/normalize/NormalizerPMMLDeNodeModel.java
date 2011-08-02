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
  */

package org.knime.base.node.preproc.pmml.normalize;

import org.knime.base.data.normalize.AffineTransConfiguration;

/**
 * The nodemodel for the pmml denormalizer.
 * 
 * @author Iris Adae, University of Konstanz
 *
 */
public class NormalizerPMMLDeNodeModel extends NormalizerPMMLApplyNodeModel {
    /**
    *
    */
   public NormalizerPMMLDeNodeModel() {
       super();
   }


   /**
    * {@inheritDoc}
    */
    protected AffineTransConfiguration getAffineTrans(
            final AffineTransConfiguration affineTransConfig) {            
        return NormalizerPMMLDeNodeModel.
                    deNormalizeConfiguration(affineTransConfig);
    }
    
    /**
     * This method takes the given configuration and transforms it into 
     * a denormalization configuration.
     * @param affineTransConfig normalization configuration.
     * @return denormalization configuration.
     */
    public static AffineTransConfiguration deNormalizeConfiguration(
            final AffineTransConfiguration affineTransConfig) {
        // change the transform here
           double[] scales = affineTransConfig.getScales(); 
            double[] translations = affineTransConfig.getTranslations();
             double[] min = affineTransConfig.getMin(); 
             double[] max = affineTransConfig.getMax();
            
            for (int i = 0; i < scales.length; i++) {
                // otherwise:
                double onethroughscale = 1.0 / scales[i];
                scales[i] = onethroughscale;
                translations[i] = -1.0 * translations[i] * onethroughscale;
                min[i] = scales[i] * min[i] + translations[i];
                max[i] = scales[i] * max[i] + translations[i];
            }
            
        return new AffineTransConfiguration(
                affineTransConfig.getNames(), 
                scales, 
                translations, 
                min,
                max, 
                affineTransConfig.getSummary());
    }
}
