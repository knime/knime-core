/* ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2008 - 2012
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
 *   Sep 6, 2012 (hornm): created
 */

package org.knime.core.data.util.memory;

/**
 * API not public yet
 *
 * Marks an object that is able to free memory on demand.
 *
 * @author Martin Horn, Christian Dietz University of Konstanz
 *
 */
public interface MemoryReleasable {
        /**
         * @return true, if object may be removed from ObjectTracker
         */
        public boolean memoryAlert();

        /**
         * Unique ID in VM
         */
        public int id();

}
