/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by KNIME GmbH, Konstanz, Germany
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
 *   04.10.2014 (Marcel): created
 */
package org.knime.base.node.preproc.datavalidator.dndpanel;

import static org.knime.core.node.util.CheckUtils.checkArgument;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.knime.core.data.DataColumnSpec;

/**
 * A {@link Transferable} object used for drag and drop, which carries {@link DataColumnSpec}s. Target components may
 * use {@link #extractColumnSpecs(Transferable)} to get the {@link DataColumnSpec} contained in a transferable.
 *
 * @author Marcel Hanser
 * @since 2.11
 */
public class DnDColumnSpecTransferable implements Transferable {
    /**
     * The data flavor for {@link DataColumnSpec}s.
     */
    public static final DataFlavor DATA_COLUMN_SPEC_FLAVOR = new DataFlavor(DataColumnSpec.class, "Data Column Specs");

    private List<DataColumnSpec> m_specs;

    /**
     * Constructor.
     *
     * @param specs the data column specs to transfer.
     */
    public DnDColumnSpecTransferable(final Collection<DataColumnSpec> specs) {
        super();
        this.m_specs = Collections.unmodifiableList(new ArrayList<DataColumnSpec>(specs));
    }

    /**
     * Constructor.
     *
     * @param specs the data column specs to transfer.
     */
    public DnDColumnSpecTransferable(final DataColumnSpec... specs) {
        this(Arrays.asList(specs));
    }

    /**
     * Extracts the {@link DataColumnSpec}s contained in the given transferable. The client code is responsible to check
     * the data flavor before calling this method.
     * <pre>
     * if(transferable.isDataFlavorSupported(DATA_COLUMN_SPEC_FLAVOR)){
     *    List<DataColumnSpec> draggedItems = extractColumnSpecs(transferable);
     * }
     * </pre>
     *
     * @param transferable the transferable
     * @return in the transferable contained {@link DataColumnSpec}s
     * @throws IllegalArgumentException if the transferable does not support the {@link #DATA_COLUMN_SPEC_FLAVOR} data
     *             flavor
     */
    @SuppressWarnings("unchecked")
    public static List<DataColumnSpec> extractColumnSpecs(final Transferable transferable) {
        checkArgument(transferable.isDataFlavorSupported(DATA_COLUMN_SPEC_FLAVOR),
            "column specs should be extracted but the transferable do not support the data flavor.");
        List<DataColumnSpec> data = null;
        try {
            data =
                (List<DataColumnSpec>)transferable.getTransferData(DnDColumnSpecTransferable.DATA_COLUMN_SPEC_FLAVOR);
        } catch (Exception e) {
            //NOOP, should not happen anyway
        }
        return data;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DataFlavor[] getTransferDataFlavors() {
        return new DataFlavor[]{DATA_COLUMN_SPEC_FLAVOR};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isDataFlavorSupported(final DataFlavor flavor) {
        return DATA_COLUMN_SPEC_FLAVOR.equals(flavor);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object getTransferData(final DataFlavor flavor) throws UnsupportedFlavorException, IOException {
        if (!isDataFlavorSupported(flavor)) {
            throw new UnsupportedFlavorException(flavor);
        }
        return m_specs;
    }
}
