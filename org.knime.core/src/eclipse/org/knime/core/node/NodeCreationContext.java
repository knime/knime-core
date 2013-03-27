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
 */
package org.knime.core.node;

import java.net.URL;

/**
 * @author ohl, University of Konstanz
 */
public class NodeCreationContext {

    private final URL m_url;

    /**
         *
         */
    public NodeCreationContext(final URL url) {
        m_url = url;
    }

    /**
     * @return the url
     */
    public URL getUrl() {
        return m_url;
    }
}
