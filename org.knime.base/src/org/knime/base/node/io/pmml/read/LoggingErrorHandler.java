/* This source code, its documentation and all appendant files
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
 */
package org.knime.base.node.io.pmml.read;

import org.knime.core.node.NodeLogger;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

/**
 * Error handler that throws an exception if an error occurs.
 * 
 * @author Fabian Dill, University of Konstanz
 */
public class LoggingErrorHandler implements ErrorHandler {
 
    private static final NodeLogger LOGGER = NodeLogger.getLogger(
            "PMML Error Handler");
    
    /**
     * 
     * {@inheritDoc}
     */
    @Override
    public void error(final SAXParseException saxe) 
        throws SAXException {
        LOGGER.error("Invalid PMML file: ", saxe);
        throw saxe;
    }

    /**
     * 
     * {@inheritDoc}
     */
    @Override
    public void fatalError(final SAXParseException saxe) 
        throws SAXException {
        LOGGER.fatal("Invalid PMML file: ", saxe);
        throw saxe;
    }

    /**
     * 
     * {@inheritDoc}
     */
    @Override
    public void warning(final SAXParseException saxe) 
        throws SAXException {
        LOGGER.warn("Invalid PMML file: ", saxe);
    }

}
