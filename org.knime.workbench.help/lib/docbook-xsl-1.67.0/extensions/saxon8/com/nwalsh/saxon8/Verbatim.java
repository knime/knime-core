// Verbatim.java - Saxon extensions supporting DocBook verbatim environments

package com.nwalsh.saxon8;

import java.util.Vector;
import java.util.StringTokenizer;
import java.util.Collections;
import javax.xml.transform.TransformerException;

import net.sf.saxon.Controller;
import net.sf.saxon.Configuration;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.om.DocumentInfo;
import net.sf.saxon.om.Axis;
import net.sf.saxon.om.AxisIterator;
import net.sf.saxon.om.Item;
import net.sf.saxon.om.SequenceIterator;
import net.sf.saxon.om.NamePool;
import net.sf.saxon.om.NodeInfo;
import net.sf.saxon.type.Type;
import net.sf.saxon.tinytree.TinyBuilder;
import net.sf.saxon.event.NamespaceReducer;
import net.sf.saxon.event.Receiver;

/**
 * <p>Saxon extensions supporting DocBook verbatim environments</p>
 *
 * <p>$Id$</p>
 *
 * <p>Copyright (C) 2000 Norman Walsh.</p>
 *
 * <p>This class provides a
 * <a href="http://saxon.sourceforge.net/">Saxon</a> 8.x
 * implementation of two features that would be impractical to
 * implement directly in XSLT: line numbering and callouts.</p>
 *
 * <p><b>Line Numbering</b></p>
 * <p>The <tt>numberLines</tt> method takes a tree
 * (assumed to contain the contents of a formatted verbatim
 * element in DocBook: programlisting, screen, address, literallayout,
 * or synopsis) and returns a tree decorated with
 * line numbers.</p>
 *
 * <p><b>Callouts</b></p>
 * <p>The <tt>insertCallouts</tt> method takes an
 * <tt>areaspec</tt> and a tree
 * (assumed to contain the contents of a formatted verbatim
 * element in DocBook: programlisting, screen, address, literallayout,
 * or synopsis) and returns a tree decorated with
 * callouts.</p>
 *
 * <p><b>Change Log:</b></p>
 * <dl>
 * <dt>1.0</dt>
 * <dd><p>Initial release.</p></dd>
 * </dl>
 *
 * @author Norman Walsh
 * <a href="mailto:ndw@nwalsh.com">ndw@nwalsh.com</a>
 *
 * @version $Id$
 *
 */
public class Verbatim {
  private static final int CALLOUT_TEXT = 1;
  private static final int CALLOUT_UNICODE = 2;
  private static final int CALLOUT_GRAPHICS = 3;

  /** The modulus for line numbering (every 'modulus' line is numbered). */
  private static int modulus = 5;
  /** The width (in characters) of line numbers (for padding). */
  private static int width = 0;
  /** The current line number. */
  private static int lineNumber = 1;
  /** The separator between the line number and the verbatim text. */
  private static String separator = " | ";

  /** The number of lines in the verbatim environment. */
  private static int lineCount = 0;

  /** The line number wrapper element. */
  private static NodeInfo wrapper = null;

  /** List of open elements. */
  private static Vector openElements = null;

  /** List of callouts. */
  private static Vector calloutList = null;

  /** The current column number. */
  private static int colNumber = 1;

  /** The default column for callouts that have only a line or line range */
  private static int defaultColumn = 60;

  /** The type of callout to use. */
  private static int calloutStyle = CALLOUT_TEXT;

  /** The prefix for text callouts. */
  private static String textPrefix = "(";

  /** The suffix for text callouts. */
  private static String textSuffix = ")";

  /** The text callout wrapper element. */
  private static NodeInfo textWrapper = null;

  /** The starting code point for Unicode callouts. */
  private static int unicodeStart = 0;

  /** The highest Unicode callout available before falling back to text. */
  private static int unicodeMax = 10;

  /** The Unicode callout wrapper element. */
  private static NodeInfo unicodeWrapper = null;

  /** The highest graphical callout available before falling back to text. */
  private static int graphicsMax = 10;

  /** The Graphics callout wrapper element. */
  private static NodeInfo graphicsWrapper = null;

  /**
   * <p>Constructor for Verbatim</p>
   *
   * <p>All of the methods are static, so the constructor does nothing.</p>
   */
  public Verbatim() {
  }

  /**
   * <p>Number lines in a verbatim environment</p>
   *
   * <p>The extension function expects the following variables to be
   * available in the calling context: $linenumbering.everyNth,
   * $linenumbering.width, $linenumbering.separator, and
   * $stylesheet.result.type.</p>
   *
   * <p>This method adds line numbers to a result tree fragment. Each
   * newline that occurs in a text node is assumed to start a new line.
   * The first line is always numbered, every subsequent 'everyNth' line
   * is numbered (so if everyNth=5, lines 1, 5, 10, 15, etc. will be
   * numbered. If there are fewer than everyNth lines in the environment,
   * every line is numbered.</p>
   *
   * <p>Every line number will be right justified in a string 'width'
   * characters long. If the line number of the last line in the
   * environment is too long to fit in the specified width, the width
   * is automatically increased to the smallest value that can hold the
   * number of the last line. (In other words, if you specify the value 2
   * and attempt to enumerate the lines of an environment that is 100 lines
   * long, the value 3 will automatically be used for every line in the
   * environment.)</p>
   *
   * <p>The 'separator' string is inserted between the line
   * number and the original program listing. Lines that aren't numbered
   * are preceded by a 'width' blank string and the separator.</p>
   *
   * <p>If inline markup extends across line breaks, markup changes are
   * required. All the open elements are closed before the line break and
   * "reopened" afterwards. The reopened elements will have the same
   * attributes as the originals, except that 'name' and 'id' attributes
   * are not duplicated if the stylesheet.result.type is "html" and
   * 'id' attributes will not be duplicated if the result type is "fo".</p>
   *
   * @param rtf The result tree fragment of the verbatim environment.
   *
   * @return The modified result tree fragment.
   */
  public static DocumentInfo numberLines (XPathContext context,
					  SequenceIterator ns,
					  SequenceIterator wrapperns,
					  int lnLineNumber,
					  int lnEveryNth,
					  int lnWidth,
					  String lnSeparator)
    throws TransformerException {

    Controller controller = context.getController();
    Configuration config = controller.getConfiguration();
    NamePool pool = config.getNamePool();
    TinyBuilder builder = new TinyBuilder();

    NamespaceReducer reducer = new NamespaceReducer();
    reducer.setUnderlyingReceiver(builder);
    Receiver tree = reducer;

    tree.setConfiguration(config);
    builder.setConfiguration(config);
    tree.open();
    tree.startDocument(0);

    wrapper = findWrapper(wrapperns);

    // We're really counting newlines so there's a tendency to be short
    // by one. That's why we start at 1 and not zero.
    lineCount = 1;
    SequenceIterator nscount = ns.getAnother();
    NodeInfo verbatim = (NodeInfo) nscount.next();
    if (verbatim.getNodeKind() != Type.ELEMENT) {
      System.err.println("Error!!!: " + verbatim);
    } else {
      NodeInfo node = (NodeInfo) verbatim;
      if (node.hasChildNodes()) {
	AxisIterator children = node.iterateAxis(Axis.CHILD);
	NodeInfo child = (NodeInfo) children.next();
	while (child != null) {
	  countLines(child);
	  child = (NodeInfo) children.next();
	}
      }
    }

    // Start with initialLineNumber
    lineNumber = lnLineNumber;

    // Number every line if we have fewer than 'modulus' lines
    modulus = (lineCount < lnEveryNth || lnEveryNth < 1) ? 1 : lnEveryNth;

    // Make the width at least wide enough for the longest number we may need
    double log10numLines = Math.log(lineCount) / Math.log(10);
    width = lnWidth < log10numLines+1
      ? (int) Math.floor(log10numLines + 1)
      : lnWidth;

    // Use the specified separator
    separator = lnSeparator;

    // Now walk through the content and number those lines
    openElements = new Vector();
    verbatim = (NodeInfo) ns.next();
    if (verbatim.getNodeKind() != Type.ELEMENT) {
      System.err.println("Error!!!: " + verbatim);
    } else {
      NodeInfo node = (NodeInfo) verbatim;
      if (node.hasChildNodes()) {
	formatLineNumber(pool, tree);
	AxisIterator children = node.iterateAxis(Axis.CHILD);
	NodeInfo child = (NodeInfo) children.next();
	while (child != null) {
	  number(child, pool, tree);
	  child = (NodeInfo) children.next();
	}
      }
    }

    tree.endDocument();
    tree.close();
    return builder.getCurrentDocument();
  }

  private static NodeInfo findWrapper (SequenceIterator wrapperns)
    throws TransformerException {
    NodeInfo wrapper = (NodeInfo) wrapperns.next();
    if (wrapper.getNodeKind() == Type.DOCUMENT) {
      if (wrapper.hasChildNodes()) {
	AxisIterator children = wrapper.iterateAxis(Axis.CHILD);
	NodeInfo child = (NodeInfo) children.next();
	while (child != null && child.getNodeKind() != Type.ELEMENT) {
	  child = (NodeInfo) children.next();
	}
	if (child != null) {
	  wrapper = child;
	}
      } else {
	wrapper = null;
      }
    } else if (wrapper.getNodeKind() != Type.ELEMENT) {
      wrapper = null;
    }

    return wrapper;
  }

  public static void countLines (NodeInfo node)
    throws TransformerException {

    // we have to recurse through elements
    if (node.getNodeKind() == Type.ELEMENT) {
      if (node.hasChildNodes()) {
	AxisIterator children = node.iterateAxis(Axis.CHILD);
	NodeInfo child = (NodeInfo) children.next();
	while (child != null) {
	  countLines(child);
	  child = (NodeInfo) children.next();
	}
      }
    }

    // we have to look for newlines in text nodes
    if (node.getNodeKind() == Type.TEXT) {
      String text = node.getStringValue();
      int pos = text.indexOf('\n');
      while (pos >= 0) {
	lineCount++;
	text = text.substring(pos+1);
	pos = text.indexOf('\n');
      }
    }
  }

  public static void number (NodeInfo node,
			     NamePool pool,
			     Receiver tree)
    throws TransformerException {

    // Maybe node is an element, a text node, a comment, or a PI
    switch (node.getNodeKind()) {
    case Type.ELEMENT:
      tree.startElement(node.getNameCode(), 0, 0, 0);

      {
	AxisIterator attrIter = node.iterateAxis(Axis.ATTRIBUTE);
	NodeInfo attr = (NodeInfo) attrIter.next();
	while (attr != null) {
	  tree.attribute(attr.getNameCode(), 0, attr.getStringValue(), 0, 0);
	  attr = (NodeInfo) attrIter.next();
	}
      }

      openElements.add(node);

      tree.startContent();
      if (node.hasChildNodes()) {
	AxisIterator children = node.iterateAxis(Axis.CHILD);
	NodeInfo child = (NodeInfo) children.next();
	while (child != null) {
	  number(child, pool, tree);
	  child = (NodeInfo) children.next();
	}
      }

      openElements.remove(openElements.size()-1);

      tree.endElement();
      break;
    case Type.TEXT:
      String text = node.getStringValue();
      int pos = text.indexOf('\n');
      while (pos >= 0) {
	tree.characters(text.substring(0, pos), 0, 0);

	// Close any open elements
	for (int openpos = 0; openpos < openElements.size(); openpos++) {
	  tree.endElement();
	}

	// Output the line number
	tree.characters("\n", 0, 0);
	lineNumber++;
	formatLineNumber(pool, tree);

	// Now re-open the elements
	for (int openpos = 0; openpos < openElements.size(); openpos++) {
	  NodeInfo onode = (NodeInfo) openElements.get(openpos);

	  tree.startElement(onode.getNameCode(), 0, 0, 0);

	  AxisIterator oattrIter = onode.iterateAxis(Axis.ATTRIBUTE);
	  NodeInfo attr = (NodeInfo) oattrIter.next();
	  while (attr != null) {
	    // Don't output {xml:}id attributes again
	    if (!"id".equals(attr.getLocalPart())) {
	      tree.attribute(attr.getNameCode(), 0, attr.getStringValue(), 0, 0);
	    }
	    attr = (NodeInfo) oattrIter.next();
	  }

	  tree.startContent();
	}

	text = text.substring(pos+1);
	pos = text.indexOf('\n');
      }
      tree.characters(text, 0, 0);
      break;
    case Type.COMMENT:
      tree.comment(node.getStringValue(), 0, 0);
      break;
    case Type.PROCESSING_INSTRUCTION:
      tree.processingInstruction(node.getDisplayName(),
				 node.getStringValue(), 0, 0);
      break;
    default:
      System.err.println("Error!");
      break;
    }
  }

  public static void formatLineNumber(NamePool pool, Receiver tree)
    throws TransformerException {

    char ch = 160; // &nbsp;

    String lno = "";
    if (lineNumber == 1
	|| (modulus >= 1 && (lineNumber % modulus == 0))) {
      lno = "" + lineNumber;
    }

    while (lno.length() < width) {
      lno = ch + lno;
    }

    lno += separator;

    if (wrapper != null) {
      tree.startElement(wrapper.getNameCode(), 0, 0, 0);
      AxisIterator attrIter = wrapper.iterateAxis(Axis.ATTRIBUTE);
      NodeInfo attr = (NodeInfo) attrIter.next();
      while (attr != null) {
	tree.attribute(attr.getNameCode(), 0, attr.getStringValue(), 0, 0);
	attr = (NodeInfo) attrIter.next();
      }
      tree.startContent();
    }

    tree.characters(lno, 0, 0);

    if (wrapper != null) {
      tree.endElement();
    }
  }

  public static DocumentInfo insertCallouts (XPathContext context,
					     SequenceIterator ns,
					     SequenceIterator areaspecns,
					     String prefix, String suffix,
					     boolean useText,
					     SequenceIterator twrapperns,
					     int uStart, int uMax,
					     boolean useUnicode,
					     SequenceIterator uwrapperns,
					     int gMax, boolean useGraphics,
					     SequenceIterator gwrapperns)
    throws TransformerException {

    // Take care of parameter passing
    textPrefix = prefix;
    textSuffix = suffix;
    if (useText) {
      calloutStyle = CALLOUT_TEXT;
    }

    unicodeStart = uStart;
    unicodeMax = uMax;
    if (useUnicode) {
      calloutStyle = CALLOUT_UNICODE;
    }

    graphicsMax = gMax;
    if (useGraphics) {
      calloutStyle = CALLOUT_GRAPHICS;
    }

    textWrapper = findWrapper(twrapperns);
    unicodeWrapper = findWrapper(uwrapperns);
    graphicsWrapper = findWrapper(gwrapperns);

    setupCallouts(areaspecns);

    Controller controller = context.getController();
    Configuration config = controller.getConfiguration();
    NamePool pool = config.getNamePool();
    TinyBuilder builder = new TinyBuilder();

    NamespaceReducer reducer = new NamespaceReducer();
    reducer.setUnderlyingReceiver(builder);
    Receiver tree = reducer;

    tree.setConfiguration(config);
    builder.setConfiguration(config);
    tree.open();
    tree.startDocument(0);

    // Start at (1,1)
    lineNumber = 1;
    colNumber = 1;

    // Now walk through the content and number those lines
    openElements = new Vector();
    NodeInfo verbatim = (NodeInfo) ns.next();
    if (verbatim.getNodeKind() != Type.ELEMENT) {
      System.err.println("Error!!!: " + verbatim);
    } else {
      NodeInfo node = (NodeInfo) verbatim;
      if (node.hasChildNodes()) {
	formatLineNumber(pool, tree);
	AxisIterator children = node.iterateAxis(Axis.CHILD);
	NodeInfo child = (NodeInfo) children.next();
	while (child != null) {
	  insert(child, pool, tree);
	  child = (NodeInfo) children.next();
	}
      }
    }

    tree.endDocument();
    tree.close();
    return builder.getCurrentDocument();
  }

  public static void insert (NodeInfo node,
			     NamePool pool,
			     Receiver tree)
    throws TransformerException {

    // Maybe node is an element, a text node, a comment, or a PI
    switch (node.getNodeKind()) {
    case Type.ELEMENT:
      tree.startElement(node.getNameCode(), 0, 0, 0);

      {
	AxisIterator attrIter = node.iterateAxis(Axis.ATTRIBUTE);
	NodeInfo attr = (NodeInfo) attrIter.next();
	while (attr != null) {
	  tree.attribute(attr.getNameCode(), 0, attr.getStringValue(), 0, 0);
	  attr = (NodeInfo) attrIter.next();
	}
      }

      openElements.add(node);

      tree.startContent();
      if (node.hasChildNodes()) {
	AxisIterator children = node.iterateAxis(Axis.CHILD);
	NodeInfo child = (NodeInfo) children.next();
	while (child != null) {
	  insert(child, pool, tree);
	  child = (NodeInfo) children.next();
	}
      }

      openElements.remove(openElements.size()-1);

      tree.endElement();
      break;
    case Type.TEXT:
      String text = node.getStringValue();

      // Are we ready to do a callout?
      if (calloutList.size() > 0) {
	Callout callout = (Callout) calloutList.get(0);
	System.err.println(lineNumber + ", " + callout.getLine());

	for (int count = 0; count < text.length(); count++) {
	  boolean done = (callout == null);
	  while (!done) {
	    done = true;
	    if (lineNumber == callout.getLine()
		&& colNumber == callout.getColumn()) {
	      insertCallout(callout, node, pool, tree);
	      callout = null;
	      calloutList.remove(0);
	      if (calloutList.size() > 0) {
		callout = (Callout) calloutList.get(0);
		done = false;
	      }
	    }
	  }

	  String ch = text.substring(count, count+1);

	  while (callout != null && "\n".equals(ch)
		 && lineNumber == callout.getLine()) {
	    tree.characters(" ", 0, 0);
	    colNumber++;

	    done = false;
	    while (!done) {
	      done = true;
	      if (lineNumber == callout.getLine()
		  && colNumber == callout.getColumn()) {
		insertCallout(callout, node, pool, tree);
		callout = null;
		calloutList.remove(0);
		if (calloutList.size() > 0) {
		  callout = (Callout) calloutList.get(0);
		  done = false;
		}
	      }
	    }
	  }

	  tree.characters(ch, 0, 0);

	  if ("\n".equals(ch)) {
	    lineNumber++;
	    colNumber = 1;
	  } else {
	    colNumber++;
	  }
	}
      } else {
	tree.characters(text, 0, 0);
      }
      break;
    case Type.COMMENT:
      tree.comment(node.getStringValue(), 0, 0);
      break;
    case Type.PROCESSING_INSTRUCTION:
      tree.processingInstruction(node.getDisplayName(),
				 node.getStringValue(), 0, 0);
      break;
    default:
      System.err.println("Error!");
      break;
    }
  }

  public static void insertCallout (Callout callout,
				    NodeInfo node,
				    NamePool pool,
				    Receiver tree)
    throws TransformerException {

    int style = calloutStyle;

    if ((style == CALLOUT_UNICODE
	 && callout.getCallout() > unicodeMax)
	|| (style == CALLOUT_GRAPHICS
	    && callout.getCallout() > graphicsMax)) {
      style = CALLOUT_TEXT;
    }

    // Close any open elements
    for (int openpos = 0; openpos < openElements.size(); openpos++) {
      tree.endElement();
    }

    switch (style) {
    case CALLOUT_TEXT:
      startCalloutWrapper(callout, textWrapper, tree);
      tree.characters(textPrefix + callout.getCallout() + textSuffix, 0, 0);
      endCalloutWrapper(textWrapper, tree);
      break;
    case CALLOUT_UNICODE:
      startCalloutWrapper(callout, unicodeWrapper, tree);
      int codepoint = unicodeStart + callout.getCallout() - 1;
      char chars[] = new char[1];
      chars[0] = (char) codepoint;
      String unicodeCh = new String(chars);
      tree.characters(unicodeCh, 0, 0);
      endCalloutWrapper(unicodeWrapper, tree);
      break;
    case CALLOUT_GRAPHICS:
      startCalloutWrapper(callout, graphicsWrapper, tree);
      endCalloutWrapper(graphicsWrapper, tree);
      break;
    default:
    }

    // Now re-open the elements
    for (int openpos = 0; openpos < openElements.size(); openpos++) {
      NodeInfo onode = (NodeInfo) openElements.get(openpos);

      tree.startElement(onode.getNameCode(), 0, 0, 0);

      AxisIterator oattrIter = onode.iterateAxis(Axis.ATTRIBUTE);
      NodeInfo attr = (NodeInfo) oattrIter.next();
      while (attr != null) {
	// Don't output {xml:}id attributes again
	if (!"id".equals(attr.getLocalPart())) {
	  tree.attribute(attr.getNameCode(), 0, attr.getStringValue(), 0, 0);
	}
	attr = (NodeInfo) oattrIter.next();
      }

      tree.startContent();
    }
  }

  private static void startCalloutWrapper (Callout callout,
					   NodeInfo wrapper,
					   Receiver tree)
    throws TransformerException {
    if (wrapper != null) {
      tree.startElement(wrapper.getNameCode(), 0, 0, 0);
      AxisIterator attrIter = wrapper.iterateAxis(Axis.ATTRIBUTE);
      NodeInfo attr = (NodeInfo) attrIter.next();
      while (attr != null) {
	String value = attr.getStringValue().replaceAll("\\{CALLOUT\\}",
							""+callout.getCallout());
	tree.attribute(attr.getNameCode(), 0, value, 0, 0);
	attr = (NodeInfo) attrIter.next();
      }
      tree.startContent();
    }
  }

  private static void endCalloutWrapper (NodeInfo wrapper, Receiver tree)
    throws TransformerException {
    if (wrapper != null) {
      tree.endElement();
    }
  }

  /**
   * <p>Examine the areaspec and determine the number and position of 
   * callouts.</p>
   *
   * <p>The <code><a href="http://docbook.org/tdg/html/areaspec.html">areaspecNodeSet</a></code>
   * is examined and a sorted list of the callouts is constructed.</p>
   *
   * <p>This data structure is used to augment the tree
   * with callout bullets.</p>
   *
   * @param areaspecNodeSet The source document &lt;areaspec&gt; element.
   *
   */
  public static void setupCallouts (SequenceIterator areaspecns)
    throws TransformerException {

    // First we walk through the areaspec to calculate the position
    // of the callouts
    //  <areaspec>
    //  <areaset id="ex.plco.const" coords="">
    //    <area id="ex.plco.c1" coords="4"/>
    //    <area id="ex.plco.c2" coords="8"/>
    //  </areaset>
    //  <area id="ex.plco.ret" coords="12"/>
    //  <area id="ex.plco.dest" coords="12"/>
    //  </areaspec>

    calloutList = new Vector();

    int pos = 0;
    int coNum = 0;
    boolean inAreaSet = false;
    NodeInfo areaspec = (NodeInfo) areaspecns.next();

    if (!areaspec.hasChildNodes()) {
      return;
    }

    AxisIterator children = areaspec.iterateAxis(Axis.CHILD);
    NodeInfo child = (NodeInfo) children.next();
    while (child != null) {
      if (child.getNodeKind() == Type.ELEMENT) {
	if ("areaset".equals(child.getLocalPart())) {
	  coNum++;

	  AxisIterator areas = child.iterateAxis(Axis.CHILD);
	  NodeInfo area = (NodeInfo) areas.next();
	  while (area != null) {
	    if (area.getNodeKind() == Type.ELEMENT) {
	      if ("area".equals(area.getLocalPart())) {
		addCallout(coNum, area, defaultColumn);
	      } else {
		System.out.println("Unexpected element in areaset: "
				   + area.getDisplayName());
	      }
	    }
	    area = (NodeInfo) areas.next();
	  }
	} else if ("area".equals(child.getLocalPart())) {
	  coNum++;
	  addCallout(coNum, child, defaultColumn);
	} else {
	  System.out.println("Unexpected element in areaspec: "
			     + child.getLocalPart());
	}
      }
      child = (NodeInfo) children.next();
    }

    // Now sort them
    Collections.sort(calloutList);
  }

  /**
   * <p>Add a callout to the global callout array</p>
   *
   * <p>This method examines a callout <tt>area</tt> and adds it to
   * the global callout array if it can be interpreted.</p>
   *
   * <p>Only the <tt>linecolumn</tt> and <tt>linerange</tt> units are
   * supported. If no unit is specifed, <tt>linecolumn</tt> is assumed.
   * If only a line is specified, the callout decoration appears in
   * the <tt>defaultColumn</tt>.</p>
   *
   * @param coNum The callout number.
   * @param node The <tt>area</tt>.
   * @param defaultColumn The default column for callouts.
   */
  protected static void addCallout (int coNum,
			     NodeInfo area,
			     int defaultColumn) {
    String units  = null;
    String coords = null;

    AxisIterator attrIter = area.iterateAxis(Axis.ATTRIBUTE);
    NodeInfo attr = (NodeInfo) attrIter.next();
    while (attr != null) {
      if ("".equals(attr.getURI())) {
	if ("units".equals(attr.getLocalPart())) {
	  units = attr.getStringValue();
	}
	if ("coords".equals(attr.getLocalPart())) {
	  coords = attr.getStringValue();
	}
      }
      attr = (NodeInfo) attrIter.next();
    }

    if (units != null
	&& !units.equals("linecolumn")
	&& !units.equals("linerange")) {
      System.out.println("Only linecolumn and linerange units are supported");
      return;
    }

    if (coords == null) {
      System.out.println("Coords must be specified");
      return;
    }

    // Now let's see if we can interpret the coordinates...
    StringTokenizer st = new StringTokenizer(coords);
    int tokenCount = 0;
    int c1 = 0;
    int c2 = 0;
    while (st.hasMoreTokens()) {
      tokenCount++;
      if (tokenCount > 2) {
	System.out.println("Unparseable coordinates");
	return;
      }
      try {
	String token = st.nextToken();
	int coord = Integer.parseInt(token);
	c2 = coord;
	if (tokenCount == 1) {
	  c1 = coord;
	}
      } catch (NumberFormatException e) {
	System.out.println("Unparseable coordinate");
	return;
      }
    }

    // Ok, add the callout
    if (tokenCount == 2) {
      if (units != null && units.equals("linerange")) {
	for (int count = c1; count <= c2; count++) {
	  calloutList.add(new Callout(coNum, area, count, defaultColumn));
	}
      } else {
	// assume linecolumn
	calloutList.add(new Callout(coNum, area, c1, c2));
      }
    } else {
      // if there's only one number, assume it's the line
      calloutList.add(new Callout(coNum, area, c1, defaultColumn));
    }
  }
}
