/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by KNIME AG, Zurich, Switzerland
 *  Website: http://www.knime.com; Email: contact@knime.com
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
 *  KNIME and ECLIPSE being a combined program, KNIME AG herewith grants
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
 *   Dec 9, 2019 (hornm): created
 */
package org.knime.core.node.workflow.capture;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.zip.ZipEntry;

import javax.swing.JComponent;

import org.knime.core.data.DataTable;
import org.knime.core.data.container.DataContainer;
import org.knime.core.data.util.NonClosableInputStream;
import org.knime.core.data.util.NonClosableOutputStream;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.ModelContent;
import org.knime.core.node.ModelContentRO;
import org.knime.core.node.ModelContentWO;
import org.knime.core.node.port.AbstractPortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortObjectZipInputStream;
import org.knime.core.node.port.PortObjectZipOutputStream;
import org.knime.core.node.port.PortType;
import org.knime.core.node.port.PortTypeRegistry;
import org.knime.core.node.workflow.BufferedDataTableView;

/**
 * The worfklow port object.
 *
 * @author Martin Horn, KNIME GmbH, Konstanz, Germany
 * @since 4.2
 */
public class WorkflowPortObject extends AbstractPortObject {

    /**
     * Convenience accessor for the port type.
     */
    public static final PortType TYPE = PortTypeRegistry.getInstance().getPortType(WorkflowPortObject.class);

    /**
     * @noreference This class is not intended to be referenced by clients.
     */
    public static final class Serializer extends AbstractPortObjectSerializer<WorkflowPortObject> {}

    private WorkflowPortObjectSpec m_spec;

    private Map<String, DataTable> m_inputData = null;

    /** Empty framework constructor. <b>Do not use!</b> */
    public WorkflowPortObject() {
        // no op
    }

    /**
     * Creates a new port object from a {@link WorkflowFragment}.
     *
     * @param spec
     */
    public WorkflowPortObject(final WorkflowPortObjectSpec spec) {
        this(spec, null);
    }

    /**
     * Creates a new port object from a {@link WorkflowFragment} plus input data.
     *
     * @param spec the spec
     * @param inputData input data mapped to inputs (by id) of the workflow fragment
     */
    public WorkflowPortObject(final WorkflowPortObjectSpec spec, final Map<String, DataTable> inputData) {
        m_spec = spec;
        m_inputData = inputData;
    }

    /**
     * Returns stored input data for a given input if available.
     *
     * @param id the id of the input
     * @return the input data or an empty optional if there is no input data for the input (either none has been stored
     *         or the input doesn't represent data table)
     */
    public Optional<DataTable> getInputDataFor(final String id) {
        if (m_inputData != null) {
            return Optional.ofNullable(m_inputData.get(id));
        }
        return Optional.empty();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void save(final PortObjectZipOutputStream out, final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException {
        if (m_inputData != null && !m_inputData.isEmpty()) {
            List<String> ids = new ArrayList<>(m_inputData.size());
            List<DataTable> tables = new ArrayList<>(m_inputData.size());
            for (Entry<String, DataTable> entry : m_inputData.entrySet()) {
                ids.add(entry.getKey());
                tables.add(entry.getValue());
            }
            out.putNextEntry(new ZipEntry("input_data_ports.xml"));
            ModelContent model = new ModelContent("input_data_ports.xml");
            saveIDs(model, ids);
            try (final NonClosableOutputStream.Zip zout = new NonClosableOutputStream.Zip(out)) {
                model.saveToXML(zout);
            }

            for (int i = 0; i < tables.size(); i++) {
                out.putNextEntry(new ZipEntry("input_table_" + i + ".bin"));
                DataContainer.writeToStream(tables.get(i), out, exec);
            }
        }
    }

    private static void saveIDs(final ModelContentWO model, final List<String> ids) {
        model.addInt("num_ids", ids.size());
        for (int i = 0; i < ids.size(); i++) {
            model.addString("id_" + i, ids.get(i));
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void load(final PortObjectZipInputStream in, final PortObjectSpec spec, final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException {
        m_spec = (WorkflowPortObjectSpec)spec;
        ZipEntry entry = in.getNextEntry();
        if (entry != null && "input_data_ports.xml".equals(entry.getName())) {
            try (InputStream nonCloseIn = new NonClosableInputStream.Zip(in)) {
                ModelContentRO model = ModelContent.loadFromXML(nonCloseIn);
                List<String> ports = loadIDs(model);
                m_inputData = new HashMap<>(ports.size());
                for (int i = 0; i < ports.size(); i++) {
                    entry = in.getNextEntry();
                    assert entry.getName().equals("input_table_" + i + ".bin");
                    DataTable table = DataContainer.readFromStream(nonCloseIn);
                    m_inputData.put(ports.get(i), table);
                }
            } catch (InvalidSettingsException ex) {
                throw new RuntimeException(ex);
            }
        }
    }

    private static List<String> loadIDs(final ModelContentRO model) throws InvalidSettingsException {
        int size = model.getInt("num_ids");
        List<String> ids = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            ids.add(model.getString("id_" + i));
        }
        return ids;
    }

    @Override
    public String getSummary() {
        return getSpec().getWorkflowFragment().getName();
    }

    @Override
    public WorkflowPortObjectSpec getSpec() {
        return m_spec;
    }

    @Override
    public JComponent[] getViews() {
        if (m_inputData != null) {
            return m_inputData.entrySet().stream().map(e -> {
                return new BufferedDataTableView(e.getValue()) {
                    @Override
                    public String getName() {
                        return "Input data for '" + e.getKey() + "'";
                    }
                };
            }).toArray(s -> new JComponent[s]);
        } else {
            return new JComponent[0];
        }
    }

}
