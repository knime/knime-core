/*
 * ------------------------------------------------------------------------
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
 * ------------------------------------------------------------------------
 */

package org.knime.base.node.mine.bayes.naivebayes.datamodel2;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.CharEncoding;
import org.apache.xmlbeans.SchemaType;
import org.apache.xmlbeans.impl.util.Base64;
import org.dmg.pmml.ExtensionDocument.Extension;
import org.dmg.pmml.PMMLDocument;
import org.dmg.pmml.PMMLDocument.PMML;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.port.pmml.PMMLMiningSchemaTranslator;
import org.knime.core.node.port.pmml.PMMLPortObjectSpec;
import org.knime.core.node.port.pmml.PMMLTranslator;
import org.knime.core.node.port.pmml.preproc.DerivedFieldMapper;

/**
 * Helper class translate between the internal used naive Bayes model and the PMML standard.
 * The class also provides helper methods to read/write extensions.
 * @author Tobias Koetter
 */
public class PMMLNaiveBayesModelTranslator implements PMMLTranslator {

    private NaiveBayesModel m_model;

    /**Default constructor.*/
    public PMMLNaiveBayesModelTranslator() {

    }

    /**
     * @param model {@link NaiveBayesModel} to translate
     */
    public PMMLNaiveBayesModelTranslator(final NaiveBayesModel model) {
        m_model = model;
    }

    /**
     * @return the {@link NaiveBayesModel}
     */
    public NaiveBayesModel getModel() {
        return m_model;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void initializeFrom(final PMMLDocument pmmlDoc) {
        final PMML pmml = pmmlDoc.getPMML();
        try {
            m_model = new NaiveBayesModel(pmml);
        } catch (InvalidSettingsException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SchemaType exportTo(final PMMLDocument pmmlDoc, final PMMLPortObjectSpec spec) {
        if (m_model == null) {
            throw new NullPointerException("No model found to serialize");
        }
        DerivedFieldMapper mapper = new DerivedFieldMapper(pmmlDoc);
        final PMML pmml = pmmlDoc.getPMML();
        final org.dmg.pmml.NaiveBayesModelDocument.NaiveBayesModel bayesModel = pmml.addNewNaiveBayesModel();
        PMMLMiningSchemaTranslator.writeMiningSchema(spec, bayesModel);
        m_model.exportToPMML(bayesModel, mapper);
        return org.dmg.pmml.NaiveBayesModelDocument.NaiveBayesModel.type;
    }

    /**
     * @param extension the {@link Extension} to write to
     * @param fieldName the filed name
     * @param fieldValue the value
     */
    static void setStringExtension(final Extension extension, final String fieldName, final String fieldValue) {
        extension.setName(fieldName);
        extension.setValue(fieldValue);
    }

    /**
     * @param extensionMap the {@link Map} to read from
     * @param fieldName the field name
     * @return the value
     * @throws InvalidSettingsException if the Map does not contain the field
     */
    static String getStringExtension(final Map<String, String> extensionMap, final String fieldName)
            throws InvalidSettingsException {
        final String val = extensionMap.get(fieldName);
        if (val == null) {
            throw new InvalidSettingsException("Extension with name " + fieldName + " not found in PMML model");
        }
        return val;
    }

    /**
     * @param extension the {@link Extension} to write to
     * @param fieldName the filed name
     * @param fieldValue the value
     */
    static void setObjectExtension(final Extension extension, final String fieldName, final Object fieldValue) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutput out = null;
        try {
          out = new ObjectOutputStream(bos);
          out.writeObject(fieldValue);
          byte[] bytes = bos.toByteArray();
          final String valString = new String(Base64.encode(bytes), CharEncoding.UTF_8);
          setStringExtension(extension, fieldName, valString);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        } finally {
          try {
            if (out != null) {
              out.close();
            }
          } catch (IOException ex) {
            // ignore close exception
          }
          try {
            bos.close();
          } catch (IOException ex) {
            // ignore close exception
          }
        }
    }

    /**
     * @param extensionMap the {@link Map} to read from
     * @param fieldName the field name
     * @return the value
     * @throws InvalidSettingsException if the Map does not contain the field
     */
    static Object getObjectExtension(final Map<String, String> extensionMap, final String fieldName)
            throws InvalidSettingsException {
        ByteArrayInputStream bis = null;
        ObjectInput in = null;
        try {
            final String valString = getStringExtension(extensionMap, fieldName);
            final byte[] bytes = Base64.decode(valString.getBytes(CharEncoding.UTF_8));
            bis = new ByteArrayInputStream(bytes);
          in = new ObjectInputStream(bis);
          return in.readObject();
        } catch (IOException | ClassNotFoundException e) {
            throw new IllegalStateException(e);
        } finally {
          try {
              if (bis != null) {
                  bis.close();
              }
          } catch (IOException ex) {
            // ignore close exception
          }
          try {
            if (in != null) {
              in.close();
            }
          } catch (IOException ex) {
            // ignore close exception
          }
        }
    }

    /**
     * @param extension the {@link Extension} to write to
     * @param fieldName the filed name
     * @param fieldValue the value
     */
    static void setIntExtension(final Extension extension, final String fieldName, final int fieldValue) {
        setStringExtension(extension, fieldName, Integer.toString(fieldValue));
    }

    /**
     * @param extensionMap the {@link Map} to read from
     * @param fieldName the field name
     * @return the value
     * @throws InvalidSettingsException if the Map does not contain the field
     */
    static int getIntExtension(final Map<String, String> extensionMap, final String fieldName)
            throws InvalidSettingsException {
        final String val = getStringExtension(extensionMap, fieldName);
        try {
            return Integer.parseInt(val);
        } catch (NumberFormatException e) {
            throw new InvalidSettingsException(e.getMessage());
        }
    }


     /**
      * @param extension the {@link Extension} to write to
      * @param fieldName the filed name
      * @param fieldValue the value
      */
    static void setBooleanExtension(final Extension extension, final String fieldName,
        final boolean fieldValue) {
        setStringExtension(extension, fieldName, Boolean.toString(fieldValue));
    }

    /**
     * @param extensionMap the {@link Map} to read from
     * @param fieldName the field name
     * @return the value
     * @throws InvalidSettingsException if the Map does not contain the field
     */
    static boolean getBooleanExtension(final Map<String, String> extensionMap, final String fieldName)
            throws InvalidSettingsException {
        final String val = getStringExtension(extensionMap, fieldName);
        return Boolean.parseBoolean(val);
    }

    /**
     * @param extension the {@link Extension} to write to
     * @param fieldName the filed name
     * @param fieldValue the value
     */
    static void setDoubleExtension(final Extension extension, final String fieldName, final double fieldValue) {
        setStringExtension(extension, fieldName, Double.toString(fieldValue));
    }

    /**
     * @param extensionMap the {@link Map} to read from
     * @param fieldName the field name
     * @return the value
     * @throws InvalidSettingsException if the Map does not contain the field
     */
    static double getDoubleExtension(final Map<String, String> extensionMap, final String fieldName)
            throws InvalidSettingsException {
        final String val = getStringExtension(extensionMap, fieldName);
        try {
            return Double.parseDouble(val);
        } catch (NumberFormatException e) {
            throw new InvalidSettingsException(e.getMessage());
        }
    }

    /**
     * @param extensionList the {@link List} of {@link Extension}s to convert to a {@link Map}
     * @return the extensions as key value map with the name as key and the value as value
     */
    static Map<String, String> convertToMap(final List<Extension> extensionList) {
        if (extensionList == null || extensionList.isEmpty()) {
            return Collections.EMPTY_MAP;
        }
        final Map<String, String> extensionMap = new LinkedHashMap<>(extensionList.size());
        for (Extension extension : extensionList) {
            extensionMap.put(extension.getName(), extension.getValue());
        }
        return extensionMap;
    }
}
