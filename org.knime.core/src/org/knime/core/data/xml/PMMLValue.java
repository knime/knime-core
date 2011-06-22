/* ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2008 - 2011
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
