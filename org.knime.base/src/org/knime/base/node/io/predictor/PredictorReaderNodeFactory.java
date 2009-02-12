/*
 * ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2009
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
 *   30.10.2005 (mb): created
 */
package org.knime.base.node.io.predictor;

import org.knime.base.node.io.portobject.PortObjectReaderNodeFactory;
import org.knime.core.node.port.AbstractSimplePortObject;
import org.knime.core.node.port.PortType;

/** Node that connects to arbitrary model ports and reads the model as
 * ModelContent from a chosen file.
 * 
 * @author M. Berthold, University of Konstanz
 */
public class PredictorReaderNodeFactory extends PortObjectReaderNodeFactory {

    /** 
     * 
     */
    public PredictorReaderNodeFactory() {
        super(new PortType(AbstractSimplePortObject.class));
    }
}
