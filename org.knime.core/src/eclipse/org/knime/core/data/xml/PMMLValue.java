/* ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright by
 * KNIME.com, Zurich, Switzerland
 *
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner or
 * as specified in the license file distributed with this product.
 *
 * If you have any questions please contact the copyright holder:
 * website: www.knime.com
 * email: contact@knime.com
 * ---------------------------------------------------------------------
 *
 * History
 *   Mar 29, 2011 (morent): created
 */

package org.knime.core.data.xml;

import java.util.List;
import java.util.Set;

import javax.swing.Icon;

import org.knime.core.data.DataValue;
import org.knime.core.data.ExtensibleUtilityFactory;
import org.knime.core.pmml.PMMLModelType;
import org.w3c.dom.Document;
import org.w3c.dom.Node;


/**
 * This value encapsulates a PMML {@link Document}.
 *
 * @author morent
 *
 */
public interface PMMLValue extends XMLValue {
    /**
     * Meta information to this value type.
     *
     * @see DataValue#UTILITY
     * @since 2.10
     */
    UtilityFactory UTILITY = new PMMLUtilityFactory();

    /**
     * Implementations of the meta information of this value class.
     * @since 2.10
     */
    class PMMLUtilityFactory extends ExtensibleUtilityFactory {
        /** Singleton icon to be used to display this cell type. */
        private static final Icon ICON = loadIcon(PMMLValue.class,
                "/icons/pmmlicon.png");

        /** Only subclasses are allowed to instantiate this class. */
        protected PMMLUtilityFactory() {
            super(PMMLValue.class);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Icon getIcon() {
            if (null != ICON) {
                return ICON;
            } else {
                return super.getIcon();
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String getName() {
            return "PMML";
        }
    }

    /**
     * @return the PMML Version
     */
    public String getPMMLVersion();

    /**
     *
     * @return the types of the contained PMML models
     * @see PMMLModelType
     */
    public Set<PMMLModelType> getModelTypes();

    /**
     * @param type the model type of the models
     * @return a list with all nodes of the specified model type or an empty
     *          list if there is no model of such a type
     */
    public List<Node> getModels(final PMMLModelType type);

    /**
     * @return a list with all pmml models
     */
    public List<Node> getModels();

}
