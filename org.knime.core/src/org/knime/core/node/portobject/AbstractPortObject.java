/*
 * ------------------------------------------------------------------ *
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
 * ---------------------------------------------------------------------
 * 
 * History
 *   Jun 5, 2008 (wiswedel): created
 */
package org.knime.core.node.portobject;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import org.knime.core.eclipseUtil.GlobalClassCreator;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.ModelContent;
import org.knime.core.node.ModelContentRO;
import org.knime.core.node.PortObject;
import org.knime.core.node.PortObjectSpec;

/**
 * Abstract implementation of general port objects. Extending this class
 * (as opposed to just implementing {@link PortObject}) has the advantage that
 * the serializing methods are enforced by abstract methods (rather than 
 * defining a static method with a particular name as given by the interface). 
 * 
 * <p>Subclasses <b>must</b> provide an empty no-arg constructor with public
 * scope (which will be used to restore the content). They are encouraged to
 * also provide a convenience access member such as 
 * <pre>
 *   public static final PortType TYPE = new PortType(FooModelPortObject.class);
 * </pre>
 * and to narrow the return type of the {@link PortObject#getSpec() getSpec()}
 * method. Derived classes don't need to provide a static serializer method as
 * required by the interface {@link PortObject}.
 * 
 * @author Bernd Wiswedel, University of Konstanz
 */
public abstract class AbstractPortObject implements PortObject {

    /** Abstract serializer method as required by interface {@link PortObject}.
     * @return A serializer that reads/writes any implementation of this class.
     */
    public static final PortObjectSerializer<AbstractPortObject> 
        getPortObjectSerializer() {
        return MyPortObjectSerializer.INSTANCE;
    }
    
    /** Public no-arg constructor. Subclasses must also provide such a
     * constructor in order to allow the serializer to instantiate them using
     * reflection. */
    public AbstractPortObject() {
    }
    
    /** Saves this object to clean directory. This method represents the 
     * implementation of {@link PortObjectSerializer#savePortObject}.
     * @param directory A clean directory to write to.
     * @param exec For progress/cancelation.
     * @throws IOException If writing fails
     * @throws CanceledExecutionException If canceled.
     */
    protected abstract void save(
            final File directory, ExecutionMonitor exec) 
        throws IOException, CanceledExecutionException;
    
    /** Loads the content into the freshly instantiated object. This method
     * is called at most once in the life time of the object 
     * (after the serializer has created a new object using the public no-arg
     * constructor.)
     * @param directory To restore from
     * @param spec The accompanying spec (which can be safely cast to the 
     * expected class).
     * @param exec For progress/cancelation.
     * @throws IOException If reading fails.
     * @throws CanceledExecutionException If canceled.
     */
    protected abstract void load(final File directory, 
            final PortObjectSpec spec, final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException;
    
    /** Final implementation of the serializer. */
    private static final class MyPortObjectSerializer extends
            PortObjectSerializer<AbstractPortObject> {

        /** Instance to be used. */
        static final MyPortObjectSerializer INSTANCE =
                new MyPortObjectSerializer();

        private MyPortObjectSerializer() {
        }

        /** {@inheritDoc} */
        @Override
        protected AbstractPortObject loadPortObject(final File directory,
                final PortObjectSpec spec, final ExecutionMonitor exec)
                throws IOException, CanceledExecutionException {
            File metaFile = new File(directory, "meta.xml");
            ModelContentRO meta = ModelContent.loadFromXML(
                    new BufferedInputStream(new FileInputStream(metaFile)));
            String className;
            try {
                className = meta.getString("class_name");
            } catch (InvalidSettingsException e1) {
                throw new IOException("Unable to load settings", e1);
            }
            Class<?> cl;
            try {
                cl = GlobalClassCreator.createClass(className);
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(
                        "Unable to load class " + className, e);
            }
            if (!AbstractPortObject.class.isAssignableFrom(cl)) {
                throw new RuntimeException(
                        "Class \"" + className + "\" is not of"
                        + " type " + AbstractPortObject.class.getSimpleName());
            }
            Class<? extends AbstractPortObject> acl = 
                cl.asSubclass(AbstractPortObject.class);
            AbstractPortObject result;
            try {
                result = acl.newInstance();
            } catch (Exception e) {
                throw new RuntimeException("Failed to instantiate class \""
                        + acl.getSimpleName() 
                        + "\" (failed to invoke no-arg constructor): " 
                        + e.getMessage(), e);
            }
            File subDir = new File(directory, "content");
            result.load(subDir, spec, exec);
            return result;
        }

        /** {@inheritDoc} */
        @Override
        protected void savePortObject(final AbstractPortObject portObject,
                final File directory, final ExecutionMonitor exec)
                throws IOException, CanceledExecutionException {
            // this is going to throw a runtime exception in case...
            ModelContent meta = new ModelContent("meta.xml");
            meta.addInt("version", 1);
            meta.addString("class_name", portObject.getClass().getName());
            File metaFile = new File(directory, "meta.xml");
            meta.saveToXML(new BufferedOutputStream(
                    new FileOutputStream(metaFile)));
            File subDir = new File(directory, "content");
            subDir.mkdir();
            portObject.save(subDir, exec);
        }
    }
}
