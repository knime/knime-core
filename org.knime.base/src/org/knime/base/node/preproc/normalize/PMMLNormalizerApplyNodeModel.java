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

package org.knime.base.node.preproc.normalize;

import java.util.ArrayList;
import java.util.List;

import org.knime.base.data.normalize.AffineTransConfiguration;
import org.knime.base.data.normalize.AffineTransTable;
import org.knime.base.data.normalize.Normalizer;
import org.knime.base.data.normalize.PMMLPreprocNormalization;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DoubleValue;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.pmml.preproc.PMMLPreprocOperation;
import org.knime.core.node.port.pmml.preproc.PMMLPreprocPortObject;
import org.knime.core.node.port.pmml.preproc.PMMLPreprocPortObjectSpec;

/**
 * @author Dominik Morent, KNIME.com, Zurich, Switzerland
 *
 */
public class PMMLNormalizerApplyNodeModel extends NormalizerApplyNodeModel {
    /**
    *
    */
   public PMMLNormalizerApplyNodeModel() {
       super(PMMLPreprocPortObject.TYPE);
   }

   /**
    * {@inheritDoc}
    */
   @Override
   protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs)
           throws InvalidSettingsException {
       PMMLPreprocPortObjectSpec modelSpec
               = (PMMLPreprocPortObjectSpec)inSpecs[0];
       DataTableSpec dataSpec = (DataTableSpec)inSpecs[1];
       List<String> unknownCols = new ArrayList<String>();
       List<String> knownCols = new ArrayList<String>();
       for (String column : modelSpec.getColumnNames()) {
           DataColumnSpec inDataCol = dataSpec.getColumnSpec(column);
           if (inDataCol == null) {
               unknownCols.add(column);
           } else if (!inDataCol.getType().isCompatible(DoubleValue.class)) {
               throw new InvalidSettingsException("Column \"" + column
                       + "\" is to be normalized, but is not numeric");
           } else {
               knownCols.add(column);
           }
       }
       if (!unknownCols.isEmpty()) {
           setWarningMessage("Some column(s) as specified by the model is not "
                   + "present in the data: " + unknownCols);
       }
       String[] ar = knownCols.toArray(new String[knownCols.size()]);
       DataTableSpec s = Normalizer.generateNewSpec(dataSpec, ar);
       return new DataTableSpec[]{s};
   }

   /**
    * {@inheritDoc}
    */
   @Override
   protected PortObject[] execute(final PortObject[] inData,
           final ExecutionContext exec) throws Exception {
       PMMLPreprocPortObject model = (PMMLPreprocPortObject)inData[0];
       BufferedDataTable table = (BufferedDataTable)inData[1];
       AffineTransConfiguration config = null;
       for (PMMLPreprocOperation op : model.getOperations()) {
           if (op instanceof PMMLPreprocNormalization) {
               config = ((PMMLPreprocNormalization)op).getConfiguration();
           }
       }
       if (config == null) {
           throw new IllegalArgumentException("No normalization configuration "
                   + "found.");
       }
       AffineTransTable t = new AffineTransTable(table, config);
       BufferedDataTable bdt = exec.createBufferedDataTable(t, exec);
       if (t.getErrorMessage() != null) {
           setWarningMessage(t.getErrorMessage());
       }
       return new BufferedDataTable[]{bdt};
   }

}
