/* ------------------------------------------------------------------
  * This source code, its documentation and all appendant files
  * are protected by copyright law. All rights reserved.
  *
  * Copyright, 2008 - 2013
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
  *   May 11, 2011 (morent): created
  */

package org.knime.workbench.editor2;

import java.net.URL;

import org.knime.core.node.ContextAwareNodeFactory;
import org.knime.core.node.NodeModel;

/**
 *
 * @author Dominik Morent, KNIME.com, Zurich, Switzerland
 *
 */
public class ReaderNodeSettings {
    private final ContextAwareNodeFactory<NodeModel> m_factory;
    private final URL m_url;

    /**
     * @param factory the node factory of the node
     * @param url the url of the file to be read
     */
    public ReaderNodeSettings(final ContextAwareNodeFactory<NodeModel> factory,
            final URL url) {
        super();
        m_factory = factory;
        m_url = url;
    }

    /**
     * @return the factory of the node
     */
    public ContextAwareNodeFactory<NodeModel> getFactory() {
        return m_factory;
    }

    /**
     * @return the url of the file to be read
     */
    public URL getUrl() {
        return m_url;
    }




}
