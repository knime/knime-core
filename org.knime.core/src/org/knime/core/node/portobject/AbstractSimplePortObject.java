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
import org.knime.core.node.ModelContentWO;
import org.knime.core.node.PortObject;
import org.knime.core.node.PortObjectSpec;

/**
 * Abstract implementation of basic port objects that save and load themselves
 * from {@link ModelContentRO} objects. This class should be used in cases where
 * the content of a model can be easily broke up into basic types (such as 
 * String, int, double, ...) and array of those.
 * 
 * <p>Subclasses <b>must</b> provide an empty no-arg constructor with public
 * scope (which will be used to restore the content). They are encouraged to
 * also provide a convenience access member such as 
 * <pre>
 *   public static final PortType TYPE = new PortType(FooModelPortObject.class);
 * </pre>
 * and to narrow the return type of the {@link PortObject#getSpec() getSpec()}
 * method (most commonly used are specs of type 
 * {@link org.knime.core.data.DataTableSpec}, whereby the columns reflect the 
 * required input attributes of a model). Derived classes don't need to provide
 * a static serializer method as required by the interface {@link PortObject}.
 * 
 * @author Bernd Wiswedel, University of Konstanz
 */
public abstract class AbstractSimplePortObject implements PortObject {

    /** Abstract serializer method as required by interface {@link PortObject}.
     * @return A serializer that reads/writes any implementation of this class.
     */
    public static final PortObjectSerializer<AbstractSimplePortObject> 
    getPortObjectSerializer() {
        return MyPortObjectSerializer.INSTANCE;
    }
    
    /** Public no-arg constructor. Subclasses must also provide such a
     * constructor in order to allow the serializer to instantiate them using
     * reflection. */
    public AbstractSimplePortObject() {
    }
    
    /** Saves this object to model content object. 
     * @param model To save to.
     * @param exec For progress/cancelation.
     * @throws CanceledExecutionException If canceled.
     */
    protected abstract void save(final ModelContentWO model, 
            final ExecutionMonitor exec) 
    throws CanceledExecutionException;
    
    /** Loads the content into the freshly instantiated object. This method
     * is called at most once in the life time of the object 
     * (after the serializer has created a new object using the public no-arg
     * constructor.)
     * @param model To load from.
     * @param spec The accompanying spec (which can be safely cast to the 
     * expected class).
     * @param exec For progress/cancelation.
     * @throws InvalidSettingsException If settings are incomplete/deficient.
     * @throws CanceledExecutionException If canceled.
     */
    protected abstract void load(final ModelContentRO model, 
            final PortObjectSpec spec, final ExecutionMonitor exec)
    throws InvalidSettingsException, CanceledExecutionException;
    
    /** Final implementation of the serializer. */
    private static final class MyPortObjectSerializer extends
            PortObjectSerializer<AbstractSimplePortObject> {

        /** Instance to be used. */
        static final MyPortObjectSerializer INSTANCE =
                new MyPortObjectSerializer();

        private MyPortObjectSerializer() {
        }

        /** {@inheritDoc} */
        @Override
        protected AbstractSimplePortObject loadPortObject(final File directory,
                final PortObjectSpec spec, final ExecutionMonitor exec)
                throws IOException, CanceledExecutionException {
            File modelFile = new File(directory, "model.xml.gz");
            ModelContentRO model = ModelContent.loadFromXML(
                    new BufferedInputStream(new FileInputStream(modelFile)));
            String className;
            try {
                className = model.getString("class_name");
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
            if (!AbstractSimplePortObject.class.isAssignableFrom(cl)) {
                throw new RuntimeException(
                        "Class \"" + className + "\" is not of type " 
                        + AbstractSimplePortObject.class.getSimpleName());
            }
            Class<? extends AbstractSimplePortObject> acl = 
                cl.asSubclass(AbstractSimplePortObject.class);
            AbstractSimplePortObject result;
            try {
                result = acl.newInstance();
            } catch (Exception e) {
                throw new RuntimeException("Failed to instantiate class \""
                        + acl.getSimpleName() 
                        + "\" (failed to invoke no-arg constructor): " 
                        + e.getMessage(), e);
            }
            try {
                ModelContentRO subModel = model.getModelContent("model");
                result.load(subModel, spec, exec);
                return result;
            } catch (InvalidSettingsException e) {
                throw new IOException("Unable to load model content into \""
                        + acl.getSimpleName() + "\": " + e.getMessage(), e);
            }
        }

        /** {@inheritDoc} */
        @Override
        protected void savePortObject(final AbstractSimplePortObject portObject,
                final File directory, final ExecutionMonitor exec)
                throws IOException, CanceledExecutionException {
            // this is going to throw a runtime exception in case...
            ModelContent model = new ModelContent("model.xml.gz");
            model.addInt("version", 1);
            model.addString("class_name", portObject.getClass().getName());
            ModelContentWO subModel = model.addModelContent("model");
            portObject.save(subModel, exec);
            File modelFile = new File(directory, "model.xml.gz");
            model.saveToXML(new BufferedOutputStream(
                    new FileOutputStream(modelFile)));
        }
    }
}
