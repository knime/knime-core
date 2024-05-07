/**
 * A binding to SLF4J delegating to KNIME's {@link org.knime.core.node.NodeLogger} classes. As suggested by the
 * official <a href="http://www.slf4j.org/faq.html#slf4j_compatible">SLF4J documentation</a> this implementation
 * is copied from the existing <a href="https://search.maven.org/artifact/org.slf4j/slf4j-log4j12">slf4j-log4j12</a>
 * artefact and tailored to using KNIME's logging capability.
 *
 * @author Bernd Wiswedel, KNIME GmbH, Konstanz, Germany
 */
package org.knime.slf4j.binding;