/* This source code, its documentation and all appendant files
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
 */
package org.knime.core.node.port.pmml;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.zip.ZipEntry;

import javax.xml.transform.TransformerConfigurationException;

import org.knime.core.eclipseUtil.GlobalClassCreator;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortObjectZipInputStream;
import org.knime.core.node.port.PortObjectZipOutputStream;
import org.knime.core.node.port.PortObject.PortObjectSerializer;
import org.xml.sax.SAXException;

/**
 *  
 * @author Fabian Dill, University of Konstanz
 */
public final class PMMLPortObjectSerializer 
    extends PortObjectSerializer<PMMLPortObject> {

    private static final String FILE_NAME = "model.pmml";
    private static final String CLAZZ_FILE_NAME = "clazz";

    
    /**
     * {@inheritDoc}
     */
    @Override
    public PMMLPortObject loadPortObject(final PortObjectZipInputStream in, 
            final PortObjectSpec spec, final ExecutionMonitor exec) 
        throws IOException, CanceledExecutionException {
        String entryName = in.getNextEntry().getName();
        if (!entryName.equals(CLAZZ_FILE_NAME)) {
            throw new IOException(
                    "Found unexpected zip entry " + entryName 
                    + "! Expected " + CLAZZ_FILE_NAME);
        }
        BufferedReader reader = new BufferedReader(new InputStreamReader(in));
        String clazzName = reader.readLine();
        if (clazzName == null) {
            throw new IllegalArgumentException(
                    "No port object class found! Cannot load port object!");
        } 
        try {
            Class<?> clazz = GlobalClassCreator.createClass(clazzName);
            if (!PMMLPortObject.class.isAssignableFrom(clazz)) {
                // throw exception
                throw new IllegalArgumentException(
                        "Class " + clazz.getName() 
                        + " must extend PMMLPortObject! Loading failed!");
            }
            PMMLPortObject portObj = (PMMLPortObject)clazz.newInstance();
            entryName = in.getNextEntry().getName();
            if (!entryName.equals(FILE_NAME)) {
                throw new IOException(
                        "Found unexpected zip entry " + entryName 
                        + "! Expected " + FILE_NAME);
            }
            portObj.loadFrom((PMMLPortObjectSpec)spec, in, 
                    PMMLPortObject.PMML_V3_1);
            return (PMMLPortObject)portObj;
        } catch (Exception e) {
            throw new IOException(e);
        }
    }
    

    /**
     * {@inheritDoc}
     */
    @Override
    public void savePortObject(final PMMLPortObject portObject, 
            final PortObjectZipOutputStream out,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        OutputStreamWriter writer = null;
        try {
            out.putNextEntry(new ZipEntry(CLAZZ_FILE_NAME));
            writer = new OutputStreamWriter(out);
            writer.write(portObject.getClass().getName());
            writer.flush();
            out.putNextEntry(new ZipEntry(FILE_NAME));
            portObject.save(out);
        } catch (SAXException e) {
            throw new IOException(e);
        } catch (TransformerConfigurationException e) {
            throw new IOException(e);
        } 
    }
   

}
