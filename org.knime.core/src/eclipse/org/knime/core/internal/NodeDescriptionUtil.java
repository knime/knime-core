package org.knime.core.internal;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.apache.commons.lang3.concurrent.ConcurrentException;
import org.apache.commons.lang3.concurrent.LazyInitializer;
import org.apache.xmlbeans.XmlObject;
import org.apache.xmlbeans.XmlOptions;
import org.eclipse.core.runtime.Platform;
import org.knime.core.node.NodeDescription;
import org.knime.core.node.NodeLogger;

/**
 *
 * @author Benjamin Moser, KNIME GmbH, Konstanz, Germany
 *
 * @since 4.6
 * @noreference This class is not intended to be referenced by clients.
 */
public final class NodeDescriptionUtil {

    private NodeDescriptionUtil() {

    }

    /**
     * Lazily initialization for XSLT transformer.
     *
     * @see #getPrettyXmlText(XmlObject)
     */
    private static final LazyInitializer<Transformer> REMOVE_NAMESPACES_TRANSFORMER = new LazyInitializer<>() {
        @Override
        protected Transformer initialize() {
            try (InputStream xslt = NodeDescriptionUtil.class.getResourceAsStream("removeNamespaces.xslt")) {
                Transformer transformer = TransformerFactory.newInstance().newTransformer(new StreamSource(xslt));
                transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
                transformer.setOutputProperty(OutputKeys.INDENT, "no");
                return transformer;
            } catch (IOException | TransformerConfigurationException e) {
                NodeLogger.getLogger(NodeDescriptionUtil.class)
                    .error("Could not format text, reason: " + e.getMessage(), e);
                e.printStackTrace();
                return null;
            }
        }
    };

    /**
     * If the given String is wrapped by an XML tag, return the String without that tag. Example: {@code <foo>bar</foo>}
     * yields {@code bar}.
     *
     * @param contents a String that is the serialisation of some XML object
     * @return the String without surrounding XML tags.
     */
    public static String stripXmlFragment(final String contents) {
        int first = 0;
        while ((first < contents.length()) && (contents.charAt(first) != '>')) {
            first++;
        }
        int last = contents.length() - 1;
        while ((last >= 0) && (contents.charAt(last) != '<')) {
            last--;
        }

        if (last <= first) {
            return "";
        } else {
            return contents.substring(first + 1, last);
        }
    }

    /**
     * Obtain a String representation of the children of the given XML object. Intended for pretty-printing contents of
     * nodes that resemble HTML-formatted text. Strip comments, namespace prefixes and namespace attributes.
     * If the given String is {@code null}, the method returns {@code null}.
     *
     * @param obj the XML object whose child nodes should be printed to a String
     * @return a string representation of the nodes children, or null if {@code obj} is null.
     */
    public static String getPrettyXmlText(final XmlObject obj) {
        if (obj == null) {
            return null;
        }

        XmlOptions xmlOptions = new XmlOptions().setLoadStripComments() // strip comments
            .setLoadStripProcinsts(); // strip processing instructions

        StringWriter writer = new StringWriter();
        try {
            REMOVE_NAMESPACES_TRANSFORMER.get().transform(new StreamSource(obj.newReader(xmlOptions)),
                new StreamResult(writer));
        } catch (TransformerException | ConcurrentException e) {
            NodeLogger.getLogger(NodeDescriptionUtil.class).error("Could not format text, reason: " + e.getMessage(),
                e);
            e.printStackTrace();
        }

        String s = writer.toString();
        // On Windows, the transformer produces CRLF (\r\n) line endings, on Unix only LF (\n)
        if (Platform.OS_WIN32.equals(Platform.getOS())) {
            s = s.replaceAll("\\r\\n", "\n");
        }
        return NodeDescriptionUtil.stripXmlFragment(s);
    }

    /**
     * Obtain direct children of {@code element} that can be cast to {@code targetClass}.
     *
     * @param element The XML element to get the children from
     * @param targetClass The target class
     * @param <R> The concrete type
     * @return A list of direct children of {@code element} that can be cast to {@code targetClass}
     */
    public static <R> List<R> getDirectChildrenOfType(final XmlObject element, final Class<R> targetClass) {
        if (element == null) {
            return Collections.emptyList();
        }
        return Arrays.stream(element.selectPath("*")).filter(targetClass::isInstance).map(targetClass::cast)
            .collect(Collectors.toList());
    }

    /**
     * Construct a list of {@link NodeDescription.DynamicPortGroupDescription}s from a list of {@link XmlObject}s using the supplied
     * getter.
     *
     * @param elements The input elements to construct output elements from.
     * @param nameGetter A function supplying the name of a given port group
     * @param groupIdentifierGetter A function supplying the group identifier of a given port group
     * @param <E> The concrete type of the {@link XmlObject}
     * @return A list of {@link NodeDescription.DynamicPortGroupDescription}s based on the input elements.
     */
    public static <E extends XmlObject> List<NodeDescription.DynamicPortGroupDescription> extractDynamicPortGroupDescriptions(final List<E> elements,
        final Function<E, String> nameGetter, final Function<E, String> groupIdentifierGetter) {
        return elements.stream().map(e -> new NodeDescription.DynamicPortGroupDescription(nameGetter.apply(e),
            groupIdentifierGetter.apply(e), getPrettyXmlText(e))).collect(Collectors.toList());
    }

    /**
     * Construct a list of {@link NodeDescription.DescriptionLink}s from a list of {@link XmlObject}s using the supplied getter.
     *
     * @param elements The input elements to construct output elements from.
     * @param targetGetter A function supplying the target of a given link.
     * @param <E> The concrete type of the {@link XmlObject}
     * @return A list of {@link NodeDescription.DescriptionLink} based on the input elements.
     */
    public static <E extends XmlObject> List<NodeDescription.DescriptionLink> extractDescriptionLinks(
            final List<E> elements,
            final Function<E, String> targetGetter) {
        return elements.stream()
            .map(e -> new NodeDescription.DescriptionLink(stripXmlFragment(targetGetter.apply(e)),
                getPrettyXmlText(e)))
            .collect(Collectors.toList());
    }

    /**
     * Construct a list of {@link NodeDescription.DialogOption}s from a list of {@link XmlObject}s using the supplied getter.
     *
     * @param elements The input elements to construct output elements from.
     * @param nameGetter A function supplying the name of the new dialog option.
     * @param <E> The concrete type of the {@link XmlObject}
     * @return A list of {@link NodeDescription.DialogOption} based on the input elements.
     */
    public static <E extends XmlObject> List<NodeDescription.DialogOption> extractDialogOptions(
            final List<E> elements,
            final Function<E, String> nameGetter,
            final Function<E, Boolean> optionalGetter
    ) {
        return elements.stream()
            .map(e -> new NodeDescription.DialogOption(
                    nameGetter.apply(e),
                    getPrettyXmlText(e),
                    optionalGetter.apply(e)
                    )
            ).collect(Collectors.toList());
    }

    /**
     * Construct a list of {@link NodeDescription.DialogOptionGroup}s from a list of {@link XmlObject}s using the
     * supplied getter.
     *
     * @param ungroupedOptions List of elements that describe ungrouped options
     * @param groups List of elements that describe option groups
     * @param groupNameGetter A function supplying the name of the new dialog option group
     * @param groupDescGetter A function supplying the description of the dialog option group
     * @param optionGroupGetter A function supplying the XML elements that describe the options in this group
     * @param optionNameGetter A function supplying the description of a given option
     * @param <G> The concrete type of an {@link XmlObject} that holds option groups
     * @param <O> The concrete type of an {@link XmlObject} that describes a single option
     * @return A list of {@link NodeDescription.DialogOptionGroup}s based on the input elements.
     */
    public static <G extends XmlObject, O extends XmlObject> List<NodeDescription.DialogOptionGroup> extractDialogOptionGroups(
            List<O> ungroupedOptions,
            List<G> groups,
            Function<G, String> groupNameGetter,
            Function<G, XmlObject> groupDescGetter,
            Function<G, List<O>> optionGroupGetter,
            Function<O, String> optionNameGetter,
            Function<O, Boolean> optionOptionalGetter) {
        return Stream.concat(
                extractUngroupedOptions(ungroupedOptions, optionNameGetter, optionOptionalGetter).stream(),
                groups.stream().map(g -> new NodeDescription.DialogOptionGroup(
                                groupNameGetter.apply(g),
                                getPrettyXmlText(groupDescGetter.apply(g)),
                                extractDialogOptions(optionGroupGetter.apply(g), optionNameGetter, optionOptionalGetter))
                        )
        ).collect(Collectors.toList());
    }

    /**
     * Construct a {@link NodeDescription.DialogOptionGroup} from a list of {@link XmlObject}s describing ungrouped
     * dialog options. The produced option group is "anonymous" in the sense that it has no name or description.
     * @param ungroupedOptions List of elements that describe ungrouped options
     * @param optionNameGetter A function supplying the name of some dialog option
     * @param <O> The concrete type of an {@link XmlObject} that describes a single option
     * @return An {@link Optional} that contains the produced option group, or an empty Optional if there are no
     *      ungrouped options.
     */
    public static <O extends XmlObject> Optional<NodeDescription.DialogOptionGroup> extractUngroupedOptions(
            List<O> ungroupedOptions,
            Function<O, String> optionNameGetter,
            Function<O, Boolean> optionOptionalGetter
    ) {
        if (!ungroupedOptions.isEmpty()) {
            return Optional.of(new NodeDescription.DialogOptionGroup(
                            null,
                            null,
                            extractDialogOptions(ungroupedOptions, optionNameGetter, optionOptionalGetter)
                    )
            );
        } else {
            return Optional.empty();
        }
    }

}
