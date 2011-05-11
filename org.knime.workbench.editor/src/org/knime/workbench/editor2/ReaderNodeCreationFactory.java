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
  *   May 11, 2011 (morent): created
  */

package org.knime.workbench.editor2;

import org.eclipse.gef.requests.CreationFactory;

/**
 *
 * @author Dominik Morent, KNIME.com, Zurich, Switzerland
 *
 */
public class ReaderNodeCreationFactory implements CreationFactory {
    private ReaderNodeSettings m_settings;

    /**
     * {@inheritDoc}
     */
    @Override
    public Object getNewObject() {
        return m_settings;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Class<?>  getObjectType() {
       return ReaderNodeSettings.class;
    }

    /**
     * @param settings the reader node settings to be saved
     */
    void setReaderNodeSettings(final ReaderNodeSettings settings) {
        m_settings = settings;
    }
}
