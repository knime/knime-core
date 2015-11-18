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
 *   Oct 27, 2008 (wiswedel): created
 */
package org.knime.core.node.exec.dataexchange;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamClass;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataCellDataInput;
import org.knime.core.data.DataCellDataOutput;
import org.knime.core.data.DataCellSerializer;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTypeRegistry;
import org.knime.core.data.container.BlobDataCell;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.eclipseUtil.GlobalObjectInputStream;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.Node;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.util.CheckUtils;

/**
 * Static repository of {@link PortObject PortObjects}. It is used to virtually set output objects of port object
 * in node models.
 *
 * @author Bernd Wiswedel, University of Konstanz
 * @noreference This class is not intended to be referenced by clients.
 * @since 3.1
 */
public final class PortObjectRepository {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(PortObjectRepository.class);

    private static int nextID;
    private static final Map<Integer, PortObject> MAP = new HashMap<Integer, PortObject>();

    private PortObjectRepository() {
        // empty
    }

    /** Add new port object to repository.
     * @param object Object to be added
     * @return the unique id this object is associated with
     * @throws NullPointerException If argument is null.
     */
    public static synchronized int add(final PortObject object) {
        CheckUtils.checkArgumentNotNull(object);
        int id = nextID;
        nextID += 1;
        assert !MAP.containsKey(id) : "Map contains ID " + id;
        MAP.put(id, object);
        LOGGER.debug("Added port object (" + object.getClass().getSimpleName()
                + ") to static repository, assigned ID " + id
                + " (total count " + MAP.size() + ")");
        return id;
    }

    /** Remove the port object that is associated with the given id.
     * @param id The id of the object
     * @return The removed object or null if it was not contained.
     */
    public static synchronized PortObject remove(final int id) {
        PortObject object = MAP.remove(id);
        if (object != null) {
            LOGGER.debug("Removed port object with id " + id + " ("
                    + object.getClass().getSimpleName()
                    + ") from static repository (" + MAP.size()
                    + " remaining)");
        } else {
            LOGGER.debug("Failed to remove port object with id " + id
                    + " from repository, no such id");
        }
        return object;
    }

    /** Get the port object that is associated with the given id.
     * @param id The id of the object
     * @return The object or null if it is not contained.
     */
    public static synchronized PortObject get(final int id) {
        return MAP.get(id);
    }

    /** Copies the argument object by means of the associated serializer.
     * @param object The port object to be copied.
     * @param exec Host for BDTs being created
     * @param progress For progress/cancelation
     * @return The deep copy.
     * @throws IOException In case of exceptions while accessing the streams
     * @throws CanceledExecutionException If canceled.
     */
    public static final PortObject copy(final PortObject object, final ExecutionContext exec,
        final ExecutionMonitor progress) throws IOException, CanceledExecutionException {
        if (object instanceof BufferedDataTable) {
            // need to copy the table cell by cell
            // this is to workaround the standard knime philosophy according
            // to which tables are referenced. A row-based copy will not work
            // as it still will reference blobs
            BufferedDataTable in = (BufferedDataTable)object;
            BufferedDataContainer con = exec.createDataContainer(
                    in.getSpec(), true, 0);
            final long rowCount = in.size();
            long row = 0;
            boolean hasLoggedCloneProblem = false;
            for (DataRow r : in) {
                DataCell[] cells = new DataCell[r.getNumCells()];
                for (int i = 0; i < cells.length; i++) {
                    DataCell c = r.getCell(i); // deserialize blob
                    if (c instanceof BlobDataCell) {
                        try {
                            c = cloneBlobCell(c);
                        } catch (Exception e) {
                            if (!hasLoggedCloneProblem) {
                                LOGGER.warn("Can't clone blob object: " + e.getMessage(), e);
                                hasLoggedCloneProblem = true;
                                LOGGER.debug("Suppressing futher warnings.");
                            }
                        }
                    }
                    cells[i] = c;
                }
                con.addRowToTable(new DefaultRow(r.getKey(), cells));
                progress.setProgress(row / (double)rowCount, "Copied row " + row + "/" + rowCount);
                progress.checkCanceled();
                row++;
            }
            con.close();
            return con.getTable();

        }
        return Node.copyPortObject(object, exec);
    }

    /** Deep-clones a data cell. Most important for blob cell to get rid
     * of their blob address.
     * @param blobCell The cell to clone.
     * @return A clone copy of the arg
     * @throws IOException If that fails for any reason.
     */
    private static DataCell cloneBlobCell(final DataCell blobCell)
        throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        DataCellCloneObjectOutputStream out =
            new DataCellCloneObjectOutputStream(bos);
        Optional<DataCellSerializer<DataCell>> cellSerializer =
            DataTypeRegistry.getInstance().getSerializer(blobCell.getClass());
        if (cellSerializer.isPresent()) {
            cellSerializer.get().serialize(blobCell, out);
        } else {
            out.writeObject(blobCell);
        }
        out.close();

        try (DataCellCloneObjectInputStream in = new DataCellCloneObjectInputStream(
            new ByteArrayInputStream(bos.toByteArray()), blobCell.getClass().getClassLoader())) {
            if (cellSerializer.isPresent()) {
                return cellSerializer.get().deserialize(in);
            } else {
                try {
                    return (DataCell)in.readObject();
                } catch (ClassNotFoundException e) {
                    throw new IOException("Can't read from object input stream: " + e.getMessage(), e);
                }
            }
        }
    }

    /** Input stream used for cloning the a data cell. */
    private static final class DataCellCloneObjectInputStream
        extends GlobalObjectInputStream implements DataCellDataInput {

        private final ClassLoader m_loader;

        /** Create new stream.
         * @param in to read from
         * @param loader class loader for restoring cell.
         * @throws IOException if super constructor throws it.
         */
        DataCellCloneObjectInputStream(final InputStream in,
                final ClassLoader loader) throws IOException {
            super(in);
            m_loader = loader;
        }

        /** {@inheritDoc} */
        @Override
        public DataCell readDataCell() throws IOException {
            try {
                return readDataCellImpl();
            } catch (IOException e) {
                throw e;
            } catch (Exception e) {
                throw new IOException("Can't read nested cell: "
                        + e.getMessage(), e);
            }
        }

        private DataCell readDataCellImpl() throws Exception {
            String clName = readUTF();
            Class<? extends DataCell> cellClass = DataTypeRegistry.getInstance().getCellClass(clName)
                    .orElseThrow(() -> new IOException("No implementation for cell class '" + clName + "' found."));
            Optional<DataCellSerializer<DataCell>> cellSerializer =
                DataTypeRegistry.getInstance().getSerializer(cellClass);
            if (cellSerializer.isPresent()) {
                return cellSerializer.get().deserialize(this);
            } else {
                return (DataCell)readObject();
            }
        }

        /** {@inheritDoc} */
        @Override
        protected Class<?> resolveClass(final ObjectStreamClass desc)
                throws IOException, ClassNotFoundException {
            if (m_loader != null) {
                try {
                    return Class.forName(desc.getName(), true, m_loader);
                } catch (ClassNotFoundException cnfe) {
                    // ignore and let super do it.
                }
            }
            return super.resolveClass(desc);
        }

    }

    /** Output stream used for cloning a data cell. */
    private static final class DataCellCloneObjectOutputStream
        extends ObjectOutputStream implements DataCellDataOutput {

        /** Call super.
         * @param out To delegate
         * @throws IOException If super throws it.
         *
         */
        DataCellCloneObjectOutputStream(
                final OutputStream out) throws IOException {
            super(out);
        }

        /** {@inheritDoc} */
        @Override
        public void writeDataCell(final DataCell cell) throws IOException {
            writeUTF(cell.getClass().getName());
            Optional<DataCellSerializer<DataCell>> cellSerializer =
                    DataTypeRegistry.getInstance().getSerializer(cell.getClass());
            if (cellSerializer.isPresent()) {
                cellSerializer.get().serialize(cell, this);
            } else {
                writeObject(cell);
            }
        }
    }
}
