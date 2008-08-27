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

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.PortObjectSpec.PortObjectSpecSerializer;

/**
 * 
 * @author Fabian Dill, University of Konstanz
 */
public class PMMLPortObjectSpecSerializer 
    extends PortObjectSpecSerializer<PMMLPortObjectSpec> {
    
    /*
    private static final String FILE = "pmmlSpec.xml";
    private TransformerHandler m_handler;
    private FileOutputStream m_fos;
    */
    
    /**
     * 
     */
    public PMMLPortObjectSpecSerializer() {
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected PMMLPortObjectSpec loadPortObjectSpec(final File directory)
            throws IOException {
        try {
            return PMMLPortObjectSpec.loadFrom(directory);
        } catch (InvalidSettingsException e) {
            throw new IOException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void savePortObjectSpec(final PMMLPortObjectSpec portObjectSpec,
            final File directory) throws IOException {
        portObjectSpec.saveTo(directory);
        
    }
    

    /*
    private void init(final File file)
            throws TransformerConfigurationException, SAXException,
            FileNotFoundException {
        SAXTransformerFactory fac =
                (SAXTransformerFactory)TransformerFactory.newInstance();
        m_handler = fac.newTransformerHandler();

        Transformer t = m_handler.getTransformer();
        t.setOutputProperty(OutputKeys.METHOD, "xml");
        t.setOutputProperty(OutputKeys.INDENT, "yes");

        m_fos = new FileOutputStream(file);
        m_handler.setResult(new StreamResult(m_fos));

        // PMML root element, namespace declaration, etc.
        m_handler.startDocument();
        AttributesImpl attr = new AttributesImpl();
        attr.addAttribute(null, null, "version", PMMLPortObjectSpec.CDATA, 
                "3.1");
        attr.addAttribute(null, null, "xmlns", PMMLPortObjectSpec.CDATA,
                "http://www.dmg.org/PMML-3_1");
        attr.addAttribute(null, null, "xmlns:xsi", PMMLPortObjectSpec.CDATA,
                "http://www.w3.org/2001/XMLSchema-instance");
        m_handler.startElement(null, null, "PMML", attr);
    }
    */

}
