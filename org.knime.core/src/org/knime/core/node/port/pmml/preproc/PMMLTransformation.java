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
  *   Apr 12, 2011 (morent): created
  */

package org.knime.core.node.port.pmml.preproc;

import java.util.Map;
import java.util.TreeMap;

/**
 *
 * @author Dominik Morent, KNIME.com, Zurich, Switzerland
 *
 */
public enum PMMLTransformation {
    Constant,
    FieldRef,
    NormContinuous,
    NormDiscrete,
    Discretize,
    MapValues,
    Apply,
    Aggregate;

    private static final Map<String, PMMLTransformation> LOOKUP;

    static {
        LOOKUP = new TreeMap<String, PMMLTransformation>();
        for (PMMLTransformation trans : PMMLTransformation.values()) {
            LOOKUP.put(trans.toString(), trans);
        }
    }
};
