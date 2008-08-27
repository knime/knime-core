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
package org.knime.core.node.port.pmml;

import java.io.File;
import java.io.IOException;

import javax.xml.transform.TransformerConfigurationException;

import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.PortObjectSpec;
import org.knime.core.node.PortObject.PortObjectSerializer;
import org.xml.sax.SAXException;

/**
 *  
 * @author Fabian Dill, University of Konstanz
 */
public class PMMLPortObjectSerializer<T extends PMMLPortObject> 
    extends PortObjectSerializer<T> {

    private static final String FILE_NAME = "model.pmml";
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected T loadPortObject(final File directory, 
            final PortObjectSpec spec,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        PMMLPortObject o = new PMMLPortObject();
        try {
            return (T)o.loadFrom(new File(directory, FILE_NAME));
        } catch (Exception e) {
            throw new IOException(e);
        }
    }
    

    /**
     * {@inheritDoc}
     */
    @Override
    protected void savePortObject(final T portObject, final File directory,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        try {
            portObject.save(new File(directory, FILE_NAME));
        } catch (SAXException e) {
            throw new IOException(e);
        } catch (TransformerConfigurationException e) {
            throw new IOException(e);
        }
    }
   

}
