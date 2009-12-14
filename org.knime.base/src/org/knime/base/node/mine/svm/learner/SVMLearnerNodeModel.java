/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2009
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
 *   27.09.2007 (cebron): created
 */
package org.knime.base.node.mine.svm.learner;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TimerTask;
import java.util.Vector;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.knime.base.node.mine.svm.PMMLSVMPortObject;
import org.knime.base.node.mine.svm.Svm;
import org.knime.base.node.mine.svm.kernel.Kernel;
import org.knime.base.node.mine.svm.kernel.KernelFactory;
import org.knime.base.node.mine.svm.kernel.KernelFactory.KernelType;
import org.knime.base.node.mine.svm.util.BinarySvmRunnable;
import org.knime.base.node.mine.svm.util.DoubleVector;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.NominalValue;
import org.knime.core.data.StringValue;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.KNIMEConstants;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelDouble;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;
import org.knime.core.node.port.pmml.PMMLPortObjectSpec;
import org.knime.core.node.port.pmml.PMMLPortObjectSpecCreator;
import org.knime.core.util.KNIMETimer;
import org.knime.core.util.ThreadPool;

/**
 *
 * @author cebron, University of Konstanz
 */
public class SVMLearnerNodeModel extends NodeModel {

    private static final NodeLogger LOGGER =
            NodeLogger.getLogger(SVMLearnerNodeModel.class);

    /**
     * Key to store the parameter c in the NodeSettings.
     */
    public static final String CFG_PARAMC = "c_parameter";

    /**
     * Key to store the class column in the NodeSettings.
     */
    public static final String CFG_CLASSCOL = "classcol";

    /**
     * Key to store kernel parameters in the NodeSettings ATTENTION: this key
     * name is used together with an index. So the i'th parameter will be in
     * KEY_KERNELPARAM + i.toString()
     */
    public static final String CFG_KERNELPARAM = "kernel_param";

    /**
     * Key to store the kernel type in the NodeSettings.
     */
    public static final String CFG_KERNELTYPE = "kernel_type";

    /** Keys under which to save the parameters. */
    public static final String KEY_CATEG_COUNT = "Category count";

    /** key to save the DataTableSpec .*/
    public static final String KEY_SPEC = "DataTableSpec";

    /** Key to save the DataTableSpec .*/
    public static final String KEY_CLASSCOL = "classcol";

    /** Default c parameter. */
    public static final double DEFAULT_PARAMC = 1.0;

    /*
     * The c parameter value.
     */
    private SettingsModelDouble m_paramC =
            new SettingsModelDouble(CFG_PARAMC, DEFAULT_PARAMC);

    /*
     * Class column
     */
    private SettingsModelString m_classcol =
            new SettingsModelString(CFG_CLASSCOL, "");

    /*
     * Position of class column
     */
    private int m_classpos;

    /*
     * The chosen kernel
     */
    private KernelType m_kernelType = KernelFactory.getDefaultKernelType();

    private HashMap<KernelType, Vector<SettingsModelDouble>> m_kernelParameters;

    /*
     * For each category, a BinarySvm that splits the category from the others.
     */
    private Svm[] m_svms;

    /*
     * The DataTableSpec we have learned with
     */
    private DataTableSpec m_spec;

    /*
     * String containing info about the trained SVM's
     */
    private String m_svmInfo = "";

    /**
     * creates the kernel parameter SettingsModels.
     *
     * @return HashMap containing the kernel and its assigned SettingsModels.
     */
    static LinkedHashMap<KernelType, Vector<SettingsModelDouble>>
    createKernelParams() {
        LinkedHashMap<KernelType, Vector<SettingsModelDouble>>
        kernelParameters =
                new LinkedHashMap<KernelType, Vector<SettingsModelDouble>>();
        for (KernelType kerneltype : KernelType.values()) {
            Kernel kernel = KernelFactory.getKernel(kerneltype);
            Vector<SettingsModelDouble> settings =
                    new Vector<SettingsModelDouble>();
            for (int i = 0; i < kernel.getNumberParameters(); i++) {
                settings.add(new SettingsModelDouble(CFG_KERNELPARAM + "_"
                        + kernel.getParameterName(i), kernel
                        .getDefaultParameter(i)));
            }
            kernelParameters.put(kerneltype, settings);
        }
        return kernelParameters;
    }

    /**
     *
     */
    public SVMLearnerNodeModel() {
        super(new PortType[]{BufferedDataTable.TYPE},
                new PortType[]{PMMLSVMPortObject.TYPE});
        m_kernelParameters = createKernelParams();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs)
            throws InvalidSettingsException {
        DataTableSpec myspec = (DataTableSpec)inSpecs[0];
        StringBuilder errormessage = new StringBuilder();
        DataColumnSpec targetcol = null;
        List<DataColumnSpec> validCols = new LinkedList<DataColumnSpec>();
        if (myspec.getNumColumns() > 0) {
            if (m_classcol.getStringValue().equals("")) {
                throw new InvalidSettingsException("Class column not set");
            } else {
                int validColumns = 0;
                boolean found = false;
                for (DataColumnSpec colspec : myspec) {
                    if (colspec.getName().equals(m_classcol.getStringValue())) {
                      if (!colspec.getType().isCompatible(NominalValue.class)) {
                            throw new InvalidSettingsException("Target column "
                                    + colspec.getName() + " must be nominal.");
                        }
                      found = true;
                      targetcol = colspec;
                        m_classpos =
                                myspec.findColumnIndex(m_classcol
                                        .getStringValue());
                    } else {
                      if (!colspec.getType().isCompatible(DoubleValue.class)) {
                            errormessage.append(colspec.getName() + ",");
                        } else {
                            validCols.add(colspec);
                            validColumns++;
                        }
                    }
                }
                if (!found) {
                    throw new InvalidSettingsException("Class column "
                            + m_classcol.getStringValue() + " not found"
                            + " in DataTableSpec.");
                }
                if (validColumns == 0) {
                    throw new InvalidSettingsException("Input DataTable does"
                            + " not contain one single valid column.");
                }
                if (errormessage.length() > 0) {
                    // remove last ','
                    int pos = errormessage.length();
                    errormessage.replace(pos - 1, pos, " ");
                    errormessage.append(": incompatible type."
                            + " Will be ignored.");
                    setWarningMessage(errormessage.toString());
                }
            }
        }
        PMMLPortObjectSpecCreator pmmlcreate =
                new PMMLPortObjectSpecCreator(myspec);
        pmmlcreate.setTargetCol(targetcol);
        pmmlcreate.setLearningCols(validCols);
        return new PortObjectSpec[]{pmmlcreate.createSpec()};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected PortObject[] execute(final PortObject[] inData,
            final ExecutionContext exec) throws Exception {
        BufferedDataTable traintable = (BufferedDataTable)inData[0];
        // Clean input data...
        StringBuilder errormessage = new StringBuilder();
        DataTableSpec inputSpec = traintable.getDataTableSpec();
        Vector<Integer> excludeVector = new Vector<Integer>();
        for (int i = 0; i < inputSpec.getNumColumns(); i++) {
            DataColumnSpec colspec = inputSpec.getColumnSpec(i);
            if (!colspec.getType().isCompatible(DoubleValue.class)
                    && !colspec.getName().equals(m_classcol.getStringValue())) {
                errormessage.append(colspec.getName() + ",");
                excludeVector.add(i);
            }
        }
        // ...if necessary
        if (excludeVector.size() > 0) {
            int[] exclude = new int[excludeVector.size()];
            for (int e = 0; e < exclude.length; e++) {
                exclude[e] = excludeVector.get(e);
            }
            ColumnRearranger colre =
                    new ColumnRearranger(traintable.getDataTableSpec());
            colre.remove(exclude);
            traintable =
                    exec.createColumnRearrangeTable(traintable, colre, exec);
        }
        m_spec = traintable.getDataTableSpec();
        m_classpos = m_spec.findColumnIndex(m_classcol.getStringValue());

        // convert input data
        ArrayList<DoubleVector> inputData = new ArrayList<DoubleVector>();
        ArrayList<String> categories = new ArrayList<String>();
        StringValue classvalue = null;
        for (DataRow row : traintable) {
            exec.checkCanceled();
            ArrayList<Double> values = new ArrayList<Double>();
            boolean add = true;
            for (int i = 0; i < row.getNumCells(); i++) {
                if (row.getCell(i).isMissing()) {
                    add = false;
                    break;
                }
                if (i != m_classpos) {
                    DoubleValue cell = (DoubleValue)row.getCell(i);
                    values.add(cell.getDoubleValue());
                } else {
                    classvalue = (StringValue)row.getCell(m_classpos);
                    if (!categories.contains(classvalue.getStringValue())) {
                        categories.add(classvalue.getStringValue());
                    }
                }
            }
            if (add) {
                inputData.add(new DoubleVector(row.getKey(), values, classvalue
                        .getStringValue()));
            }
        }
        DoubleVector[] inputDataArr = new DoubleVector[inputData.size()];
        inputDataArr = inputData.toArray(inputDataArr);
        Kernel kernel = KernelFactory.getKernel(m_kernelType);
        Vector<SettingsModelDouble> kernelparams =
                m_kernelParameters.get(m_kernelType);
        for (int i = 0; i < kernel.getNumberParameters(); ++i) {
            kernel.setParameter(i, kernelparams.get(i).getDoubleValue());
        }

        m_svms = new Svm[categories.size()];
        exec.setMessage("Training SVM");
        final BinarySvmRunnable[] bst =
                new BinarySvmRunnable[categories.size()];
        for (int i = 0; i < categories.size(); i++) {
            bst[i] =
                    new BinarySvmRunnable(inputDataArr, categories.get(i),
                            kernel, m_paramC.getDoubleValue(),
                            exec.createSubProgress((1.0 / categories.size())));

        }
        ThreadPool pool = KNIMEConstants.GLOBAL_THREAD_POOL;
        final Future<?>[] fut = new Future<?>[bst.length];
        KNIMETimer timer = KNIMETimer.getInstance();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                try {
                    exec.checkCanceled();
                } catch (final CanceledExecutionException ce) {
                    for (int i = 0; i < fut.length; i++) {
                        if (fut[i] != null) {
                            fut[i].cancel(true);
                        }
                    }
                    super.cancel();
                }

            }
        }, 0, 3000);
        for (int i = 0; i < bst.length; i++) {
            fut[i] = pool.enqueue(bst[i]);
        }

        try {
            pool.runInvisible(new Callable<Void>() {
                @Override
                public Void call() throws Exception {
                    for (int i = 0; i < fut.length; ++i) {
                        fut[i].get();
                        bst[i].ok();
                        m_svms[i] = bst[i].getSvm();
                    }
                    return null;
                }
            });
        } catch (ExecutionException ex) {
            throw (Exception) ex.getCause();
        }

        if (errormessage.length() > 0) {
            // remove last ','
            int pos = errormessage.length();
            errormessage.replace(pos - 1, pos, " ");
            errormessage.append(": incompatible type. Ignored.");
            setWarningMessage(errormessage.toString());
        }

        PMMLPortObjectSpecCreator pmmlcreate =
                new PMMLPortObjectSpecCreator(traintable.getDataTableSpec());
        pmmlcreate.setTargetCol(m_spec.getColumnSpec(m_classcol
                .getStringValue()));
        PMMLPortObjectSpec pmmlspec = pmmlcreate.createSpec();
        PMMLSVMPortObject pmml =
                new PMMLSVMPortObject(pmmlspec, kernel, m_svms);
        return new PortObject[]{pmml};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File nodeInternDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        File f = new File(nodeInternDir, "SVM_Internals");
        FileReader in = new FileReader(f);
        StringBuffer sb = new StringBuffer();
        char c;
        while (in.ready()) {
            c = (char)in.read();
            sb.append(c);
        }
        m_svmInfo = sb.toString();
        in.close();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_paramC.loadSettingsFrom(settings);
        m_classcol.loadSettingsFrom(settings);
        if (settings.containsKey(CFG_KERNELTYPE)) {
            m_kernelType =
                    KernelType.valueOf(settings.getString(CFG_KERNELTYPE));
        }
        for (Map.Entry<KernelType, Vector<SettingsModelDouble>>
        entry : m_kernelParameters
                .entrySet()) {
            Vector<SettingsModelDouble> kernelsettings = entry.getValue();
            for (SettingsModelDouble smd : kernelsettings) {
               try {
                    smd.loadSettingsFrom(settings);
                } catch (InvalidSettingsException ise) {
                    // it's not bad if a parameter is missing. This may be
                    // an old version, but inform the user.
                    LOGGER.warn("Did not find " + smd.toString() + " in the"
                            + " NodeSettings. Using default value instead.");
                }
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
        m_svms = null;
        m_svmInfo = "";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File nodeInternDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        File f = new File(nodeInternDir, "SVM_Internals");
        FileWriter out = new FileWriter(f);
        out.write(m_svmInfo);
        out.close();
    }

    /**
     * @return a string containing all SVM infos in HTML for the view.
     */
    String getSVMInfos() {
        if (m_svmInfo.length() > 0) {
            return m_svmInfo;
        }
        StringBuffer sb = new StringBuffer();
        if (m_svms != null) {
            sb.append("<html>\n");
            sb.append("<body>\n");
            for (int i = 0; i < m_svms.length; i++) {
                if (m_svms[i] != null) {
                    sb.append("<h1> SVM " + i + " Class: "
                            + m_svms[i].getPositive() + "</h1>");
                    sb.append("<b> Support Vectors: </b><br>");
                    DoubleVector[] supvecs = m_svms[i].getSupportVectors();
                    for (DoubleVector vec : supvecs) {
                        for (int s = 0; s < vec.getNumberValues(); s++) {
                            sb.append(vec.getValue(s) + ", ");
                        }
                        sb.append(vec.getClassValue() + "<br>");
                    }
                }
            }
            sb.append("</body>\n");
            sb.append("</html>\n");
        }
        m_svmInfo = sb.toString();
        return m_svmInfo;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        settings.addString(CFG_KERNELTYPE, m_kernelType.toString());
        m_paramC.saveSettingsTo(settings);
        m_classcol.saveSettingsTo(settings);
        for (Map.Entry<KernelType, Vector<SettingsModelDouble>>
        entry : m_kernelParameters
                .entrySet()) {
            Vector<SettingsModelDouble> kernelsettings = entry.getValue();
            for (SettingsModelDouble smd : kernelsettings) {
                smd.saveSettingsTo(settings);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        if (settings.containsKey(CFG_KERNELTYPE)) {
            String tmpKernel = settings.getString(CFG_KERNELTYPE);
            boolean found = false;
            for (String kernel : KernelFactory.getKernelNames()) {
                if (tmpKernel.equals(kernel)) {
                    found = true;
                }
            }
            if (!found) {
                throw new InvalidSettingsException("Unknown kernel type: "
                        + tmpKernel);
            }

        }

        for (Map.Entry<KernelType, Vector<SettingsModelDouble>> entry
            : m_kernelParameters.entrySet()) {
            Vector<SettingsModelDouble> kernelsettings = entry.getValue();
            for (SettingsModelDouble smd : kernelsettings) {
                try {
                    smd.validateSettings(settings);
                } catch (InvalidSettingsException ise) {
                    // it's not bad if a parameter is missing. This may be
                    // an old version, but inform the user.
                    LOGGER.warn("Did not find " + smd.toString() + " in the"
                            + " NodeSettings. Using default value instead.");
                }
            }
        }

        m_paramC.validateSettings(settings);
        m_classcol.validateSettings(settings);
    }
}
