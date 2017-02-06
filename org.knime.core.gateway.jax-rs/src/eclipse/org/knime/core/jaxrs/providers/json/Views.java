package org.knime.core.jaxrs.providers.json;
/* ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright by KNIME.com, Zurich, Switzerland
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
 *   Created on 11.04.2016 by thor
 */


/**
 * Class that lists views used by Jackson.
 *
 * Note: copied from 'com.knime.enterprise.server.rest.providers.json' and modified.
 *
 * @author Thorsten Meinl, KNIME.com, Zurich, Switzerland
 */
public final class Views {
    /**
     * The default view. Doesn't need to be specified explictly because properties without view annotation are always
     * serialized.
     */
    public interface Default {}

    /**
     * View for all data including the Mason metadata (e.g. controls, namespaces). Metadata is included by default
     * except if a minimal representation is requested by the client. Then all properties annotated with this view
     * are <b>excluded</b>.
     */
    public interface Complete {}
}
