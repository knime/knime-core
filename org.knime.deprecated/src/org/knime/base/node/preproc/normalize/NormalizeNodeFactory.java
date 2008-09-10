/* 
 * -------------------------------------------------------------------
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
 * -------------------------------------------------------------------
 * 
 * History
 *   19.04.2005 (cebron): created
 */
package org.knime.base.node.preproc.normalize;


/**
 * This class has been replaced by the 
 * {@link org.knime.base.node.preproc.normalize.NormalizerNodeFactory}.
 * 
 * <p>As of 2.0 this class extends the new NormalizerNodeFactory class. That 
 * means old flows containing instances of this node will magically load a 
 * normalizer node that has a model outport.
 * @author Nicolas Cebron, University of Konstanz
 * @deprecated use 
 * {@link org.knime.base.node.preproc.normalize.NormalizerNodeFactory}
 */
@Deprecated 
public class NormalizeNodeFactory extends NormalizerNodeFactory {
}
