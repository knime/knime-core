/*
 * ------------------------------------------------------------------------
 *  Copyright by KNIME AG, Zurich, Switzerland
 *  Website: http://www.knime.com; Email: contact@knime.com
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
 *  KNIME and ECLIPSE being a combined program, KNIME AG herewith grants
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

    /**
     * Abstract implementation of the a serializer for all {@link AbstractSimplePortObjectSpec}s. Subclasses can simply
     * extend this class with the appropriate type without implementing any methods.
     *
     * @since 3.0
     */
    public static abstract class AbstractSimplePortObjectSpecSerializer<T extends AbstractSimplePortObjectSpec>
        extends PortObjectSpecSerializer<T> {
        /** {@inheritDoc} */
        @Override
        public T loadPortObjectSpec(
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
            } catch (InvalidSettingsException ex1) {
                throw new IOException("Unable to load settings", ex1);
            }
            try {
                return loadPortObjectSpecFromModelSettings(model);
            } catch (ClassNotFoundException ex) {
                throw new RuntimeException("Unable to load class " + className, ex);
            } catch (ClassCastException ex) {
                throw new RuntimeException(ex.getMessage());
            } catch (InstantiationException | IllegalAccessException ex) {
                throw new RuntimeException("Failed to instantiate class \"" + className
                    + "\" (failed to invoke no-arg constructor): " + ex.getMessage(), ex);
            }
        }

        /** {@inheritDoc} */
        @Override
        public void savePortObjectSpec(final T portObject, final PortObjectSpecZipOutputStream out) throws IOException {
            // this is going to throw a runtime exception in case...
            ModelContent model = new ModelContent("model.xml");
            saveToModelSettings(portObject, model);
            out.putNextEntry(new ZipEntry("content.xml"));
            model.saveToXML(out);
        }

        /**
         * Utility method to create a new spec instance from a {@link ModelContent} object if of type
         * {@link AbstractSimplePortObjectSpec}.
         *
         * @param model the model content to create the spec instance from
         * @return the instantiated and loaded spec
         * @throws IOException if the spec class name is not available or the model content couldn't be loaded
         * @throws ClassNotFoundException if the spec class represented by the model cannot be loaded
         * @throws IllegalAccessException if instantiation failed due to illegal access
         * @throws InstantiationException if the instantiation failed due to other reasons
         * @throws ClassCastException if the model spec is not of type {@link AbstractSimplePortObjectSpec}
         * @since 3.6
         */
        @SuppressWarnings("unchecked")
        public static <T extends AbstractSimplePortObjectSpec> T
            loadPortObjectSpecFromModelSettings(final ModelContentRO model)
                throws IOException, ClassNotFoundException, InstantiationException, IllegalAccessException {
            String className;
            try {
                className = model.getString("class_name");
            } catch (InvalidSettingsException e1) {
                throw new IOException("Unable to load settings", e1);
            }
            Class<?> cl;
            cl = Class.forName(className);
            if (!AbstractSimplePortObjectSpec.class.isAssignableFrom(cl)) {
                throw new ClassCastException(
                    "Class \"" + className + "\" is not of type " + AbstractSimplePortObjectSpec.class.getSimpleName());
            }
            Class<? extends AbstractSimplePortObjectSpec> acl = cl.asSubclass(AbstractSimplePortObjectSpec.class);
            AbstractSimplePortObjectSpec result;
            result = acl.newInstance();
            try {
                ModelContentRO subModel = model.getModelContent("model");
                result.load(subModel);
                return (T)result;
            } catch (InvalidSettingsException e) {
                throw new IOException(
                    "Unable to load model content into \"" + acl.getSimpleName() + "\": " + e.getMessage(), e);
            }
        }

        /**
         * Utility method to save the port object spec to model settings.
         *
         * @param spec the spec to be saved
         * @param model the model to store the content to
         * @since 3.6
         */
        public static <T extends AbstractSimplePortObjectSpec> void savePortObjectSpecToModelSettings(final T spec,
            final ModelContentWO model) {
            saveToModelSettings(spec, model);
        }

        private static <T extends AbstractSimplePortObjectSpec> void saveToModelSettings(final T spec,
            final ModelContentWO model) {
            model.addInt("version", 1);
            model.addString("class_name", spec.getClass().getName());
            ModelContentWO subModel = model.addModelContent("model");
            spec.save(subModel);
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
                    new ModelContentOutPortView(model)};
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
