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
 *   15 Feb 2021 (Marc Bux, KNIME GmbH, Berlin, Germany): created
 */
package org.knime.core.data.v2.schema;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.knime.core.data.v2.schema.DefaultValueSchemaTest.createDefaultValueSchema;
import static org.knime.core.data.v2.schema.DefaultValueSchemaTest.createSpec;

import java.util.AbstractMap;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.Test;
import org.knime.core.data.DataColumnDomain;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.meta.DataColumnMetaData;
import org.knime.core.data.probability.nominal.NominalDistributionValueMetaData;

/**
 * @author Marc Bux, KNIME GmbH, Berlin, Germany
 */
@SuppressWarnings("javadoc")
public class UpdatedValueSchemaTest {

    @Test(expected = IndexOutOfBoundsException.class)
    public void testGetSpecIndexOutOfBoundsLower() {
        new UpdatedValueSchema(createSpec(0), createDefaultValueSchema(createSpec(0))).getSpec(-1);
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testGetSpecIndexOutOfBoundsUpper() {
        new UpdatedValueSchema(createSpec(0), createDefaultValueSchema(createSpec(0))).getSpec(1);
    }

    @Test
    public void testGetSpec() {
        final ValueSchema delegate = createDefaultValueSchema(createSpec(1));
        final UpdatedValueSchema updated = new UpdatedValueSchema(createSpec(1), delegate);
        assertEquals(delegate.getSpec(0), updated.getSpec(0));
        assertEquals(delegate.getSpec(1), updated.getSpec(1));
    }

    @Test
    public void testSourceSpec() {
        final DataTableSpec delegateSpec = createSpec(1);
        final DataTableSpec updatedSpec = createSpec(1);
        final ValueSchema delegate = createDefaultValueSchema(delegateSpec);
        final UpdatedValueSchema updated = new UpdatedValueSchema(updatedSpec, delegate);
        assertEquals(delegateSpec, delegate.getSourceSpec());
        assertEquals(updatedSpec, updated.getSourceSpec());
    }

    @Test
    public void testUpdateSource() {
        final ValueSchema source = createDefaultValueSchema(createSpec(1));
        final ValueSchema updated =
            ValueSchemaUtils.updateDataTableSpec(source, Collections.emptyMap(), Collections.emptyMap());
        assertEquals(source.getSourceSpec(), updated.getSourceSpec());
    }

    @Test
    public void testUpdateSourceWithMetadata() {
        final ValueSchema source = createDefaultValueSchema(createSpec(1));
        final Map<Integer, DataColumnDomain> domainMap = Collections.emptyMap();
        final Map<Integer, DataColumnMetaData[]> metadataMap = Stream
            .of(new AbstractMap.SimpleImmutableEntry<>(Integer.valueOf(1),
                new DataColumnMetaData[]{new NominalDistributionValueMetaData(new String[]{"test"})}))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        final ValueSchema updated = ValueSchemaUtils.updateDataTableSpec(source, domainMap, metadataMap);
        assertNotEquals(source.getSourceSpec(), updated.getSourceSpec());
    }

}
