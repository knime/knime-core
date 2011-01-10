/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2011
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
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.ModelContent;
import org.knime.core.node.ModelContentRO;
import org.knime.core.node.ModelContentWO;
import org.knime.core.node.workflow.ModelContentOutPortView;

/**
 * Abstract implementation of basic port objects that save and load themselves
 * from {@link ModelContentRO} objects. This class should be used in cases where
 * the content of a model can be easily broke up into basic types (such as
 * String, int, double, ...) and array of those.
 * 
 * <p>
 * Subclasses <b>must</b> provide an empty no-arg constructor with public scope
 * (which will be used to restore the content). They are encouraged to also
 * provide a convenience access member such as
 * 
 * <pre>
 * public static final PortType TYPE = new PortType(FooModelPortObject.class);
 * </pre>
 * 
 * and to narrow the return type of the {@link PortObject#getSpec() getSpec()}
 * method (most commonly used are specs of type
 * {@link org.knime.core.data.DataTableSpec}, whereby the columns reflect the
 * required input attributes of a model), or
 * {@link AbstractSimplePortObjectSpec} Derived classes don't need to provide
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
        public AbstractSimplePortObject loadPortObject(
                final PortObjectZipInputStream in,
                final PortObjectSpec spec, final ExecutionMonitor exec)
                throws IOException, CanceledExecutionException {
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
        public void savePortObject(final AbstractSimplePortObject portObject,
                final PortObjectZipOutputStream out, 
                final ExecutionMonitor exec)
                throws IOException, CanceledExecutionException {
            // this is going to throw a runtime exception in case...
            ModelContent model = new ModelContent("model.xml");
            model.addInt("version", 1);
            model.addString("class_name", portObject.getClass().getName());
            ModelContentWO subModel = model.addModelContent("model");
            portObject.save(subModel, exec);
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
        try {
            ModelContent model = new ModelContent("Model Content");
            save(model, new ExecutionMonitor());
            return new JComponent[] {
                    new ModelContentOutPortView((ModelContentRO)model)};
        } catch (CanceledExecutionException cee) {
            // should not be possible
        }
        return null;
    }
    
    /**
     * Method compares both <code>ModelContent</code> objects that first need 
     * to be saved by calling {@link #save(ModelContentWO, ExecutionMonitor)}. 
     * Override this method in order to compare both objects more efficiently.
     * 
     * {@inheritDoc} 
     */
    @Override
    public boolean equals(final Object oport) {
        if (oport == this) {
            return true;
        }
        if (oport == null) {
            return false;
        }
        if (!this.getClass().equals(oport.getClass())) {
            return false;
        }
        ModelContent tcont = new ModelContent("ignored");
        ModelContent ocont = new ModelContent("ignored");
        try {
            this.save(tcont, new ExecutionMonitor());
            ((AbstractSimplePortObject) oport).save(
                   ocont, new ExecutionMonitor());
        } catch (CanceledExecutionException cee) {
            // ignored, should not happen
        }
        return tcont.equals(ocont);
    }
    
    /**
     * Method computes the hash code as defined by the underlying 
     * <code>ModelContent</code> object that first need to be saved by calling
     * {@link #save(ModelContentWO, ExecutionMonitor)}. Override this method in 
     * order to compute the hash code more efficiently.
     * 
     * {@inheritDoc} 
     */
    @Override
    public int hashCode() {
        ModelContent tcont = new ModelContent("ignored");
        try {
            this.save(tcont, new ExecutionMonitor());
        } catch (CanceledExecutionException cee) {
            // ignored, should not happen
        }
        return tcont.hashCode();
    }
}
