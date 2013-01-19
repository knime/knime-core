/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2013
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
 *   Aug 8, 2008 (wiswedel): created
 */
package org.knime.core.data.container;

import org.knime.core.data.RowKey;

/**
 * Defines some constants commonly used when writing {@link Buffer} files. 
 * @author Bernd Wiswedel, University of Konstanz
 */
interface KNIMEStreamConstants {
    
    /** The key being returned by a {@link NoKeyBuffer}. */
    static final RowKey DUMMY_ROW_KEY = new RowKey("non-existing");
    
    /** The byte being used as block terminate. */
    static final byte TC_TERMINATE = (byte)0x61;

    /**
     * The byte being used to escape the next byte. The next byte will therefore
     * neither be considered as terminate nor as escape byte.
     */
    static final byte TC_ESCAPE = (byte)0x62;
 
    /** The char for missing cells. */
    static final byte BYTE_TYPE_MISSING = Byte.MIN_VALUE;

    /** The char for cell whose type needs serialization. */
    static final byte BYTE_TYPE_SERIALIZATION = BYTE_TYPE_MISSING + 1;

    /** The first used char for the map char --> type. */
    static final byte BYTE_TYPE_START = BYTE_TYPE_MISSING + 2;

    /** Separator for different rows. */
    static final byte BYTE_ROW_SEPARATOR = BYTE_TYPE_MISSING + 3;
    

}
