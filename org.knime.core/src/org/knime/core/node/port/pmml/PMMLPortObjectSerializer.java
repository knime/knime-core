/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2009
 *  University of Konstanz, Germany and
 *  KNIME GmbH, Konstanz, Germany
 *  Website: http://www.knime.org; Email: contact@knime.org
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License, Version 3, as
 *  published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, see <http://www.gnu.org/licenses>.
 *
 *  Additional permission under GNU GPL version 3 section 7:
 *
 *  KNIME interoperates with ECLIPSE solely via ECLIPSE's plug-in APIs.
 *  Hence, KNIME and ECLIPSE are both independent programs and are not
 *  derived from each other. Should, however, the interpretation of the
 *  GNU GPL Version 3 ("License") under any applicable laws result in
 *  KNIME and ECLIPSE being a combined program, KNIME GMBH herewith grants
 *  you the additional permission to use and propagate KNIME together with
 *  ECLIPSE with only the license terms in place for ECLIPSE applying to
 *  ECLIPSE and the GNU GPL Version 3 applying for KNIME, provided the
 *  license terms of ECLIPSE themselves allow for the respective use and
 *  propagation of ECLIPSE together with KNIME.
 *
 *  Additional permission relating to nodes for KNIME that extend the Node
 *  Extension (and in particular that are based on subclasses of NodeModel,
 *  NodeDialog, and NodeView) and that only interoperate with KNIME through
 *  standard APIs ("Nodes"):
 *  Nodes are deemed to be separate and independent programs and to not be
 *  covered works.  Notwithstanding anything to the contrary in the
 *  License, the License does not apply to Nodes, you are not required to
 *  license Nodes under the License, and you are granted a license to
 *  prepare and propagate Nodes, in each case even if such Nodes are
 *  propagated with or for interoperation with KNIME.  The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ------------------------------------------------------------------------
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
