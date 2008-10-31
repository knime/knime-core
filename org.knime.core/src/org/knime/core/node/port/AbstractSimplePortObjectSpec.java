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
package org.knime.core.node.port;

import java.io.IOException;
import java.util.zip.ZipEntry;

import javax.swing.JComponent;

import org.knime.core.data.util.NonClosableInputStream;
import org.knime.core.eclipseUtil.GlobalClassCreator;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.ModelContent;
import org.knime.core.node.ModelContentRO;
import org.knime.core.node.ModelContentWO;
import org.knime.core.node.workflow.ModelContentOutPortView;

/**
 * Abstract implementation of basic port object specs that save and load
 * themselves from {@link ModelContentRO} objects. This class should be used in
 * cases where the content of a model can be easily broke up into basic types
 * (such as String, int, double, ...) and array of those.
 * 
 * <p>
 * Subclasses <b>must</b> provide an empty no-arg constructor with public scope
 * (which will be used to restore the content). The do not need to provide a
 * static serializer method as required by the interface {@link PortObjectSpec}.
 * 
 * @author Bernd Wiswedel, University of Konstanz
 */
public abstract class AbstractSimplePortObjectSpec implements PortObjectSpec {

    /**
     * Abstract serializer method as required by interface
     * {@link PortObjectSpec}.
     * 
     * @return A serializer that reads/writes any implementation of this class.
     */
    public static final PortObjectSpecSerializer<AbstractSimplePortObjectSpec> 
    getPortObjectSpecSerializer() {
        return MyPortObjectSerializer.INSTANCE;
    }
    
    /** Public no-arg constructor. Subclasses must also provide such a
     * constructor in order to allow the serializer to instantiate them using
     * reflection. */
    public AbstractSimplePortObjectSpec() {
    }
    
    /** Saves this object to model content object. 
     * @param model To save to.
     */
    protected abstract void save(final ModelContentWO model);
    
    /** Loads the content into the freshly instantiated object. This method
     * is called at most once in the life time of the object 
     * (after the serializer has created a new object using the public no-arg
     * constructor.)
     * @param model To load from.
     * @throws InvalidSettingsException If settings are incomplete/deficient.
     */
    protected abstract void load(final ModelContentRO model)
        throws InvalidSettingsException;
    
    /** Final implementation of the serializer. */
    private static final class MyPortObjectSerializer extends
            PortObjectSpecSerializer<AbstractSimplePortObjectSpec> {

        /** Instance to be used. */
        static final MyPortObjectSerializer INSTANCE =
                new MyPortObjectSerializer();

        private MyPortObjectSerializer() {
        }

        /** {@inheritDoc} */
        @Override
        public AbstractSimplePortObjectSpec loadPortObjectSpec(
                final PortObjectSpecZipInputStream in) throws IOException {
            ZipEntry entry = in.getNextEntry();
            if (!"content.xml".equals(entry.getName())) {
                throw new IOException("Expected zip entry content.xml, got "
                        + entry.getName());
            }
            ModelContentRO model = ModelContent.loadFromXML(
                    new NonClosableInputStream.Zip(in));
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
            if (!AbstractSimplePortObjectSpec.class.isAssignableFrom(cl)) {
                throw new RuntimeException(
                        "Class \"" + className + "\" is not of type " 
                        + AbstractSimplePortObjectSpec.class.getSimpleName());
            }
            Class<? extends AbstractSimplePortObjectSpec> acl = 
                cl.asSubclass(AbstractSimplePortObjectSpec.class);
            AbstractSimplePortObjectSpec result;
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
                result.load(subModel);
                return result;
            } catch (InvalidSettingsException e) {
                throw new IOException("Unable to load model content into \""
                        + acl.getSimpleName() + "\": " + e.getMessage(), e);
            }
        }

        /** {@inheritDoc} */
        @Override
        public void savePortObjectSpec(
                final AbstractSimplePortObjectSpec portObject,
                final PortObjectSpecZipOutputStream out) throws IOException {
            // this is going to throw a runtime exception in case...
            ModelContent model = new ModelContent("model.xml");
            model.addInt("version", 1);
            model.addString("class_name", portObject.getClass().getName());
            ModelContentWO subModel = model.addModelContent("model");
            portObject.save(subModel);
            out.putNextEntry(new ZipEntry("content.xml"));
            model.saveToXML(out);
        }
    }
    
    /**
     * 
     * {@inheritDoc}
     */
    @Override
    public JComponent[] getViews() {
            ModelContent model = new ModelContent("Model Content Spec");
            save(model);
            return new JComponent[] {
                    new ModelContentOutPortView((ModelContentRO)model)};
    }
    
    /** {@inheritDoc} */
    @Override
    public boolean equals(final Object ospec) {
        if (ospec == this) {
            return true;
        }
        if (ospec == null) {
            return false;
        }
        if (!this.getClass().equals(ospec.getClass())) {
            return false;
        }
        ModelContent tcont = new ModelContent("ignored");
        ModelContent ocont = new ModelContent("ignored");
        this.save(tcont);
        ((AbstractSimplePortObjectSpec) ospec).save(ocont);
        return tcont.equals(ocont);
    }
    
    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        ModelContent tcont = new ModelContent("ignored");
        this.save(tcont);
        return tcont.hashCode();
    }
}
