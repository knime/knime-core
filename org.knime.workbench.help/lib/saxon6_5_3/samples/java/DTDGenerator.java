import com.icl.saxon.Controller;
import com.icl.saxon.Context;
import com.icl.saxon.NodeHandler;
import com.icl.saxon.RuleManager;
import com.icl.saxon.ExtendedInputSource;
import com.icl.saxon.om.NodeInfo;
import com.icl.saxon.om.DocumentInfo;
import com.icl.saxon.om.Axis;
import com.icl.saxon.om.AxisEnumeration;
import com.icl.saxon.tinytree.TinyBuilder;
import com.icl.saxon.pattern.AnyNodeTest;
import com.icl.saxon.pattern.NodeTypeTest;
import com.icl.saxon.sort.BinaryTree;

import org.xml.sax.InputSource;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.TransformerException;

import java.net.URL;
import java.util.Vector;
import java.util.Enumeration;
import java.io.File;

/**
* DTDGenerator<BR>
* Generates a possible DTD from an XML document instance.
* Uses SAXON to process the document contents.
* @author M.H.Kay
*/

public class DTDGenerator extends Controller {

  BinaryTree elementList;   // alphabetical list of element types appearing in the document;
                            // each has the element name as a key and an ElementDetails object
                            // as the value

  /**
    * Entry point
    * Usage:  java DTDGenerator input-file >output-file
    */

    public static void main (String args[]) throws java.lang.Exception
    {
				// Check the command-line arguments.
        if (args.length != 1) {
            System.err.println("Usage: java DTDGenerator input-file >output-file");
            System.exit(1);
        } 

                // Instantiate and run the application
        DTDGenerator app = new DTDGenerator();
        DocumentInfo doc = app.prepare(args[0]);

        app.run(doc);
        app.printDTD();
    }

    public DTDGenerator () 
    {
        elementList = new BinaryTree();
    }

    /**
    * Set up
    */

    private DocumentInfo prepare (String filename) throws TransformerException 
    {
    	// Set the element handler for all elements
    	
        RuleManager rm = new RuleManager(getNamePool());
        setRuleManager(rm);
        rm.setHandler( "*", new ElemHandler() );
        rm.setHandler( "text()", new CharHandler() );

		// build the document

        TinyBuilder builder = new TinyBuilder();
        builder.setNamePool(getNamePool());
        InputSource is = new ExtendedInputSource(new File(filename));
        SAXSource ss = new SAXSource(is);
        return builder.build(ss);

    }

    /**
    * Test whether a string is an XML name.
    * This is currently an incomplete test, in that it treats all non-ASCII characters
    * as being valid in names.
    */

    private boolean isValidName(String s) {
        if (!isValidNMTOKEN(s)) return false;
        int c = s.charAt(0);
        return ! ((c>=0x30 && c<=0x39) || c=='.' || c=='-' );
    }

    /**
    * Test whether a string is an XML NMTOKEN.
    * This is currently an incomplete test, in that it treats all non-ASCII characters
    * as being valid in NMTOKENs.
    */

    private boolean isValidNMTOKEN(String s) {
        if (s.length()==0) return false;
        for (int i=0; i<s.length(); i++) {
            int c = s.charAt(i);
            if (!( (c>=0x41 && c<=0x5a) ||
                   (c>=0x61 && c<=0x7a) ||
                   (c>=0x30 && c<=0x39) ||
                    c=='.' ||
                    c=='_' ||
                    c=='-' ||
                    c==':' ||
                    c>128 ))
                return false;
        }
        return true;
    }
  
    /**
    * When the whole document has been analysed, construct the DTD
    */
    
    private void printDTD ()
    {
        // process the element types encountered, in turn

        Enumeration e=elementList.getKeys().elements();
        while ( e.hasMoreElements() )
        {
            String elementname = (String) e.nextElement();
            ElementDetails ed = (ElementDetails) elementList.get(elementname); 
            BinaryTree children = ed.children;
            Vector childKeys = children.getKeys();
            Vector childValues = children.getValues();

            //EMPTY content
            if (childKeys.size()==0 && !ed.hasCharacterContent) 
                System.out.print("<!ELEMENT " + elementname + " EMPTY >\n");

            //CHARACTER content
            if (childKeys.size()==0 && ed.hasCharacterContent)
                System.out.print("<!ELEMENT " + elementname + " ( #PCDATA ) >\n");

            //ELEMENT content
            if (childKeys.size()>0 && !ed.hasCharacterContent) {
                System.out.print("<!ELEMENT " + elementname + " ( ");

                if (ed.sequenced) {
                    
                    // all elements of this type have the same child elements
                    // in the same sequence, retained in the childseq vector
                    
                    Enumeration c = ed.childseq.elements();
                    while (true) {
                        ChildDetails ch = (ChildDetails)c.nextElement();
                        System.out.print(ch.name);
                        if (ch.repeatable && !ch.optional) 
                            System.out.print("+");
                        if (ch.repeatable && ch.optional) 
                            System.out.print("*");
                        if (ch.optional && !ch.repeatable) 
                            System.out.print("?");
                        if (c.hasMoreElements())
                            System.out.print(", ");
                        else
                            break;
                    }
                    System.out.print(" ) >\n");
                }
                else {
                    
                    // the children don't always appear in the same sequence; so
                    // list them alphabetically and allow them to be in any order
                    
                    for (int c1=0; c1<childKeys.size(); c1++) {
                        if (c1>0) System.out.print(" | ");
                        System.out.print((String)childKeys.elementAt(c1));
                    };
                    System.out.print(" )* >\n");
                }
            };

            //MIXED content
            if (childKeys.size()>0 && ed.hasCharacterContent) {
                System.out.print("<!ELEMENT " + elementname + " ( #PCDATA");
                for (int c2=0; c2<childKeys.size(); c2++) {
                    System.out.print(" | " + (String)childKeys.elementAt(c2));
                };
                System.out.print(" )* >\n");
            };

            //Now examine the attributes encountered for this element type

            BinaryTree attlist = ed.attributes;
            boolean doneID = false;                   // to ensure we have at most one ID attribute per element
            Enumeration a=attlist.getKeys().elements();
            while ( a.hasMoreElements() )
            {
                String attname = (String) a.nextElement();
                AttributeDetails ad = (AttributeDetails) attlist.get(attname);

                // if the attribute is present on every instance of the element, treat it as required
                boolean required = (ad.occurrences==ed.occurrences);

                // if every value of the attribute is distinct, and there are >10, treat it as an ID
                //     (!!this may give the wrong answer, we should really check whether the value sets of two
                //        candidate-ID attributes overlap, in which case they can't both be IDs !!)
                boolean isid = ad.allNames &&           // ID values must be Names
                                (!doneID) &&            // Only allowed one ID attribute per element type
                                (ad.values.size()==ad.occurrences) &&
                                (ad.occurrences>10);

                // if there is only one attribute value, and 4 or more occurrences of it, treat it as FIXED 
                boolean isfixed = required && ad.values.size()==1 && ad.occurrences>4;

                // if the number of distinct values is small compared with the number of occurrences,
                // treat it as an enumeration
                boolean isenum = ad.allNMTOKENs &&      // Enumeration values must be NMTOKENs
                                (ad.occurrences>10) && 
                                (ad.values.size()<=ad.occurrences/3) &&
                                (ad.values.size()<10);

                System.out.print("<!ATTLIST " + elementname + " " + attname + " ");
                String tokentype = (ad.allNMTOKENs ? "NMTOKEN" : "CDATA");
                
                if (isid) { 
                    System.out.print("ID");
                    doneID = true;
                }
                else if (isfixed) {
                    String val = (String) ad.values.getKeys().elementAt(0);                    
                    System.out.print(tokentype + " #FIXED \"" + escape(val) + "\" >\n");
                }
                else if (isenum) {
                    System.out.print("( ");
                    Vector v=ad.values.getKeys();
                    for (int v1=0; v1<v.size(); v1++) {
                        if (v1!=0) System.out.print(" | ");
                        System.out.print((String) v.elementAt(v1));
                    };
                    System.out.print(" )");
                }
                else
                    System.out.print(tokentype);

                if (!isfixed) {
                    if (required)
                        System.out.print(" #REQUIRED >\n");
                    else
                        System.out.print(" #IMPLIED >\n");
                }
            };
            System.out.print("\n");
        };
   
    }
    
    /**
    * Determine whether an element is the first is a group of consecutive
    * elements with the same name
    */
    
    private boolean isFirstInGroup(NodeInfo node) {
        AxisEnumeration prev = node.getEnumeration(Axis.PRECEDING_SIBLING,
                                                   new NodeTypeTest(NodeInfo.ELEMENT)); 
        if (prev.hasMoreElements()) {
            NodeInfo prevNode = prev.nextElement();
            return (!prevNode.getDisplayName().equals(node.getDisplayName()));
        } else {
            return true;
        }
    }       


    /**
    * Escape special characters for display.
    * @param ch The character array containing the string
    * @param start The start position of the input string within the character array
    * @param length The length of the input string within the character array
    * @return The XML/HTML representation of the string<br>
    * This static method converts a Unicode string to a string containing
    * only ASCII characters, in which non-ASCII characters are represented
    * by the usual XML/HTML escape conventions (for example, "&lt;" becomes "&amp;lt;").
    * Note: if the input consists solely of ASCII or Latin-1 characters,
    * the output will be equally valid in XML and HTML. Otherwise it will be valid
    * only in XML.
    * The escaped characters are written to the dest array starting at position 0; the
    * number of positions used is returned as the result
    */
    
    private static int escape(char ch[], int start, int length, char[] out)
    {        
        int o = 0;
        for (int i = start; i < start+length; i++) {
            if (ch[i]=='<') {("&lt;").getChars(0,4,out,o); o+=4;}
            else if (ch[i]=='>') {("&gt;").getChars(0,4,out,o); o+=4;}
            else if (ch[i]=='&') {("&amp;").getChars(0,5,out,o); o+=5;}
            else if (ch[i]=='\"') {("&#34;").getChars(0,5,out,o); o+=5;}
            else if (ch[i]=='\'') {("&#39;").getChars(0,5,out,o); o+=5;}
            else if (ch[i]<=0x7f) {out[o++]=ch[i];}
            else {
                String dec = "&#" + Integer.toString((int)ch[i]) + ';';
                dec.getChars(0, dec.length(), out, o);
                o+=dec.length();
            }            
        }
        return o;
    }

    /**
    * Escape special characters in a String value.
    * @param in The input string
    * @return The XML representation of the string<br>
    * This static method converts a Unicode string to a string containing
    * only ASCII characters, in which non-ASCII characters are represented
    * by the usual XML/HTML escape conventions (for example, "&lt;" becomes
    * "&amp;lt;").<br>
    * Note: if the input consists solely of ASCII or Latin-1 characters,
    * the output will be equally valid in XML and HTML. Otherwise it will be valid
    * only in XML.
    */
    
    private static String escape(String in)
    {
        char[] dest = new char[in.length()*8];
        int newlen = escape( in.toCharArray(), 0, in.length(), dest);
        return new String(dest, 0, newlen);
    }

    /////////////////////////
    // inner classes       //
    /////////////////////////

    /**
    * Element handler processes each element in turn
    */

    private class ElemHandler implements NodeHandler {

    public boolean needsStackFrame() { return false; }
        
    /**
    * Handle the start of an element 
    */
    public void start(NodeInfo node, Context context) throws TransformerException
    {
        AxisEnumeration atts = node.getEnumeration(Axis.ATTRIBUTE, AnyNodeTest.getInstance());
        String name = node.getDisplayName();
        Controller ctrl = context.getController();

        // create an entry in the Element List, or locate the existing entry        
        ElementDetails ed = (ElementDetails) elementList.get(name);
        if (ed==null)  { 
            ed = new ElementDetails(name);
            elementList.put(name,ed);
        };

        // retain the associated element details object
        ctrl.setUserData(node, "ed", ed);

        // initialise sequence numbering of child element types
        ctrl.setUserData(node, "seq", new Integer(-1));
        
        // count occurrences of this element type
        ed.occurrences++;

        // Handle the attributes accumulated for this element.
        // Merge the new attribute list into the existing list for the element

        while (atts.hasMoreElements()) {
            NodeInfo att = atts.nextElement();
            String attName = att.getDisplayName();
            String val = att.getStringValue();
 
            AttributeDetails ad = (AttributeDetails) ed.attributes.get(attName);
            if (ad==null) {
               ad=new AttributeDetails(attName);
               ed.attributes.put(attName, ad);
            };
 
            ad.values.put(val, Boolean.TRUE);               //this is a dummy value to indicate presence
            if (!isValidName(val)) ad.allNames = false;     //check if attribute value is a valid name
            if (!isValidNMTOKEN(val)) ad.allNMTOKENs = false;
            ad.occurrences++;
        };

        // now keep track of the nesting and sequencing of child elements
        NodeInfo parent = (NodeInfo)node.getParent();
        if (parent.getNodeType()==NodeInfo.ELEMENT) {
            ElementDetails parentDetails = (ElementDetails)ctrl.getUserData(parent, "ed");
            int seq = ((Integer)ctrl.getUserData(parent, "seq")).intValue();

            // for sequencing, we're interested in consecutive groups of the same child element type
            if (isFirstInGroup(node)) {
                seq++;
                ctrl.setUserData(parent, "seq", new Integer(seq));
            }

            // if we've seen this child of this parent before, get the details
            BinaryTree children = parentDetails.children;
            ChildDetails c = (ChildDetails)children.get(name);
            if (c==null) {
                // this is the first time we've seen this child belonging to this parent
                c = new ChildDetails();
                c.name = name;
                c.position = seq;
                c.repeatable = false;
                c.optional = false;
                children.put(name, c);
                parentDetails.childseq.addElement(c);

                // if the first time we see this child is not on the first instance of the parent,
                // then we allow it as an optional element
                if (parentDetails.occurrences!=1) {
                    c.optional = true;
                }

            } else {

                // if it's the first occurrence of the parent element, and we've seen this
                // child before, and it's the first of a new group, then the child occurrences are
                // not consecutive
                if (parentDetails.occurrences==1 && isFirstInGroup(node)) {
                    parentDetails.sequenced = false;
                }
                
                // check whether the position of this group of children in this parent element is
                // the same as its position in previous instances of the parent.
                if (parentDetails.childseq.size()<=seq ||
                        !((ChildDetails)parentDetails.childseq.elementAt(seq)).name.equals(name))
                {
                    parentDetails.sequenced = false;
                }
            }

            // if there's more than one child element, mark it as repeatable
            if (!isFirstInGroup(node)) {
                c.repeatable = true;
            }
        };

        ctrl.applyTemplates(context, null, null, null);

        //
        // End of element. If sequenced, check that all expected children are accounted for.
        //


        // if the number of child element groups in this parent element is less than the
        // number in previous elements, then the absent children are marked as optional
        if (ed.sequenced) {
            int seq = ((Integer)ctrl.getUserData(node, "seq")).intValue();
            for (int i=seq+1; i<ed.childseq.size(); i++) {
                ((ChildDetails)ed.childseq.elementAt(i)).optional = true;
            }
        }
    }

    } // end of inner class ElemHandler

    private class CharHandler implements NodeHandler {

        /**
        * Handle character data.
        * Make a note whether significant character data is found in the element
        */
        public void start (NodeInfo c, Context context) throws TransformerException 
        {
            String s = c.getStringValue();
            NodeInfo parent = c.getParent();
            if (s.trim().length()>0) {
                ElementDetails ed = (ElementDetails)context.getController().getUserData(parent, "ed");
                ed.hasCharacterContent = true;
            }
        }
        
        public boolean needsStackFrame() { return false; }
    }

    /**
    * ElementDetails is a data structure to keep information about element types
    */

    private class ElementDetails {
        String name;
        int occurrences;
        boolean hasCharacterContent;
        boolean sequenced;
        BinaryTree children;
        Vector childseq;
        BinaryTree attributes;

        public ElementDetails ( String name ) {
            this.name = name;
            this.occurrences = 0;
            this.hasCharacterContent = false;
            this.sequenced = true;
            this.children = new BinaryTree();
            this.childseq = new Vector();
            this.attributes = new BinaryTree();
        }
    }

    /**
    * ChildDetails records information about the presence of a child element within its
    * parent element. If the parent element is sequenced, then the child elements always
    * occur in sequence with the given frequency.
    */

    private class ChildDetails {
        String name;
        int position;
        boolean repeatable;
        boolean optional;
    }
    

    /**
    * AttributeDetails is a data structure to keep information about attribute types
    */

    private class AttributeDetails {
        String name;
        int occurrences;
        BinaryTree values;      //used as a set
        boolean allNames;       //true if all the attribute values are valid names
        boolean allNMTOKENs;    //true if all the attribute values are valid NMTOKENs

        public AttributeDetails ( String name ) {
            this.name = name;
            this.occurrences = 0;
            this.values = new BinaryTree();
            this.allNames = true;
            this.allNMTOKENs = true;
        }
    }


} // end of outer class DTDGenerator

