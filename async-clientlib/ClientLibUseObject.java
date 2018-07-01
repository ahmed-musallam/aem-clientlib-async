package apps.async_clientlib;

import java.io.*;
import java.util.*;

import javax.script.Bindings;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.commons.lang.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.scripting.SlingBindings;
import org.apache.sling.api.scripting.SlingScriptHelper;
import org.apache.sling.api.resource.Resource;

import com.adobe.granite.ui.clientlibs.HtmlLibraryManager;
import com.adobe.granite.ui.clientlibs.LibraryType;

import org.apache.sling.xss.XSSAPI;
import org.slf4j.Logger;

import org.apache.sling.scripting.sightly.pojo.Use;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * HTL Clientlibs that can accept expression options for 'defer', 'async'
 * 'onload' and 'crossorigin'.
 *
 * Heavily based on nate yolles aem-clientlib-async: https://github.com/nateyolles/aem-clientlib-async
 *
 * This class is mostly code from /libs/granite/sightly/templates/ClientLibUseObjec.java,
 * found in your local AEM instance. The differences are that this class gets
 * the 'loading' and 'onload' attributes, gets the categories retrieved from
 * {@link com.adobe.granite.ui.clientlibs.HtmlLibraryManager#writeIncludes(SlingHttpServletRequest, Writer, String...)}
 * The result is then parsed into XML then the additional attributes ('defer', 'async'
 * 'onload' and 'crossorigin') are added.
 *
 * Nate's aem-clientlib-async writes the HTML for clientlibs using string templatse instead of 
 * letting the HtmlLibraryManager write the HTML 
 *
 * @see       libs.granite.sightly.templates.ClientLibUseObject
 * @see       com.adobe.granite.ui.clientlibs.HtmlLibraryManager
 */
public class ClientLibUseObject implements Use {


    private static final String BINDINGS_CATEGORIES = "categories";
    private static final String BINDINGS_MODE = "mode";
    private static final String TEMPLATE_PATH = "/apps/async-clientlib";


    /**
     * Sightly parameter that becomes the script element void attribute such as
     * 'defer' and 'async'. Valid values are listed in {@link #VALID_JS_ATTRIBUTES}.
     */
    private static final String BINDINGS_LOADING = "loading";

    /**
     * Sightly parameter that becomes the javascript function value in the
     * script element's 'onload' attribute.
     */
    private static final String BINDINGS_ONLOAD = "onload";

    /**
     * Sightly parameter that becomes the value in the script and link elements'
     * 'crossorigin' attribute.
     */
    private static final String BINDINGS_CROSS_ORIGIN = "crossorigin";


    /**
     * Valid void attributes for HTML markup of script element.
     */
    private static final List<String> VALID_JS_ATTRIBUTES = new ArrayList<String>(){{
        add("async");
        add("defer");
    }};

    /**
     * Valid values for crossorigin attribute for HTML markup of script and link
     * elements.
     */
    private static final List<String> VALID_CROSS_ORIGIN_VALUES = new ArrayList<String>(){{
        add("anonymous");
        add("use-credentials");
    }};

    private HtmlLibraryManager htmlLibraryManager = null;
    private String[] categories;
    private String mode;
    private String loadingAttribute;
    private String onloadAttribute;
    private String crossoriginAttribute;
    private SlingHttpServletRequest request;
    private PrintWriter out;
    private Logger log;
    private Resource resource;
    private XSSAPI xssAPI;

    /**
     * Same as AEM provided method with the addition of getting the XSSAPI
     * service and the two additional bindings for loading and onload.
     *
     * @see libs.granite.sightly.templates.ClientLibUseObject#init(Bindings)
     */
    public void init(Bindings bindings) {
        loadingAttribute = (String) bindings.get(BINDINGS_LOADING);
        onloadAttribute = (String) bindings.get(BINDINGS_ONLOAD);
        crossoriginAttribute = (String) bindings.get(BINDINGS_CROSS_ORIGIN);
        resource = (Resource) bindings.get(SlingBindings.RESOURCE);
        categories = getCategories(bindings);


        if (categories != null && categories.length > 0) {
            mode = (String) bindings.get(BINDINGS_MODE);
            request = (SlingHttpServletRequest) bindings.get(SlingBindings.REQUEST);
            log = (Logger) bindings.get(SlingBindings.LOG);
            SlingScriptHelper sling = (SlingScriptHelper) bindings.get(SlingBindings.SLING);
            htmlLibraryManager = sling.getService(HtmlLibraryManager.class);
            xssAPI = sling.getService(XSSAPI.class);
        }
    }

    /**
     * @return the <script>'s/<link>'s for the clientlibs
     */
    public String include() {

        if (categories == null || categories.length == 0)  {
            log.error("'categories' option might be missing from the invocation of the " + TEMPLATE_PATH +
                    "client libraries template library. Please provide a CSV list or an array of categories to include.");
            return "";
        }

        try {

            StringWriter sw = new StringWriter(); // create writer
            writeIncludes(sw); // write JS/CSS includes to writer

            // create an input stream from a well-formatter XML document.
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setValidating(false);
            DocumentBuilder db = dbf.newDocumentBuilder();
            ByteArrayInputStream in = new ByteArrayInputStream(getValidXMLDocumentString(sw.toString()).getBytes());
            Document doc = db.parse(in);


            Map<String, String> otherAttributes = getOtherAttributes();
            Map<String, String> jsAttributes = getJSAttributes();
            jsAttributes.putAll(otherAttributes);

            // find elements and add respective attributes
            setAttributes(doc.getElementsByTagName("script"), jsAttributes);
            setAttributes(doc.getElementsByTagName("link"), otherAttributes);

            return nodeListToString(doc.getDocumentElement().getChildNodes());

        } catch (ParserConfigurationException e) {
            log.error("Error creating a new document builder", e);
        } catch (IOException e) {
            log.error("Error while parsing XML from InputStream", e);
        } catch (SAXException e) {
            log.error("Error while parsing XML", e);
        }
        // return an error comment to show the issue immediately
        return "<!-- could not include CSS/JS for categories: $libs -->".replace("$libs", StringUtils.join(categories, ", "));
    }

    /**
     * Get categories from bindings and transform into array
     *
     * @param bindings Sling bindings
     * @return an array of categories
     */
    private String[] getCategories(Bindings bindings){
        String[] categories = null;
        Object categoriesObject = bindings.get(BINDINGS_CATEGORIES);
        if (categoriesObject != null) {
            if (categoriesObject instanceof Object[]) {
                Object[] categoriesArray = (Object[]) categoriesObject;
                categories = new String[categoriesArray.length];
                int i = 0;
                for (Object o : categoriesArray) {
                    if (o instanceof String) {
                        categories[i++] = ((String) o).trim();
                    }
                }
            } else if (categoriesObject instanceof String) {
                categories = ((String) categoriesObject).split(",");
                int i = 0;
                for (String c : categories) {
                    categories[i++] = c.trim();
                }
            }
        }
        return categories;
    }

    /**
     * Set every attribute entry in attrs to each element in the nodeList
     *
     * @param nodeList The node list to set attributes for
     * @param attrs The attributes to set on each node of the list
     */
    private void setAttributes(NodeList nodeList, Map<String, String> attrs){
        if(nodeList == null || attrs == null || attrs.isEmpty()) return;
        for (int i = 0; i < nodeList.getLength(); i++) {
            Element e = (Element) nodeList.item(i);
            for(Map.Entry<String, String> entry : attrs.entrySet()){
                e.setAttribute(entry.getKey(), entry.getValue());
            }
        }
    }


    /**
     * Surrounds an XML fragment with an XML declaration and root node.
     * Essentially to create a "well-formatted" XML document
     *
     * @param XMLFragmentString An XML partial
     * @return A "well-formatted" XML document
     */
    private String getValidXMLDocumentString(String XMLFragmentString ){
        return "<?xml version=\"1.0\" ?><root>$content</root>".replace("$content", XMLFragmentString);
    }

    /**
     * Write clientlib includes to the writer
     *
     * @param writer The Writer to write includes (script and link elements) into
     * @throws IOException in case of errors while performing writing
     */
    private void writeIncludes(Writer writer) throws IOException{
        if ("js".equalsIgnoreCase(mode)) {
            htmlLibraryManager.writeJsInclude(request, writer, categories);
        } else if ("css".equalsIgnoreCase(mode)) {
            htmlLibraryManager.writeCssInclude(request, writer, categories);
        } else {
            htmlLibraryManager.writeIncludes(request, writer, categories);
        }
    }

    /**
     * Get `<script>` specific attributes
     *
     * @return A map where key = attribute name  and value = attribute value
     */
    private Map<String, String> getJSAttributes(){
        Map<String, String> attrMap = new HashMap<String, String>();
        if (StringUtils.isNotBlank(loadingAttribute) && VALID_JS_ATTRIBUTES.contains(loadingAttribute.toLowerCase())) {
            attrMap.put(loadingAttribute, "true");
        }

        if (StringUtils.isNotBlank(onloadAttribute)) {
            String safeOnload = xssAPI.encodeForHTMLAttr(onloadAttribute);
            attrMap.put("onload", safeOnload);
        }
        return attrMap;
    }

    /**
     * Get Other attributes that apply for both js/css (`<script>`/`<link>`)
     *
     * @return A map where key = attribute name  and value = attribute value
     */
    private Map<String, String> getOtherAttributes(){
        Map<String, String> attrMap = new HashMap<String, String>();

        if (StringUtils.isNotBlank(crossoriginAttribute) && VALID_CROSS_ORIGIN_VALUES.contains(crossoriginAttribute.toLowerCase())) {
            attrMap.put("crossorigin", crossoriginAttribute.toLowerCase());
        }

        return attrMap;
    }

    /**
     * Convert NodeList to a string
     *
     * @param nodes The nodes to convert to String
     * @return A string representation of the nodelist
     */
    private String nodeListToString(NodeList nodes) {
        try {
            // create the transformer factory and set transform options
            TransformerFactory tf = TransformerFactory.newInstance();
            Transformer transformer = tf.newTransformer();
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes"); // no need for xml declaration
            transformer.setOutputProperty(OutputKeys.METHOD, "html");
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");

            // each node will be appended to this StringBuilder
            StringBuilder stringBuilder = new StringBuilder();

            for (int i = 0; i < nodes.getLength(); i++) {

                StringWriter sw = new StringWriter();
                transformer.transform(new DOMSource(nodes.item(i)), new StreamResult(sw));
                stringBuilder.append(sw.toString());
                sw.close(); // close StringWriter as it is not needed anymore

            }

            return stringBuilder.toString();

        } catch (TransformerException e) {
            log.error("Error converting NodeList to String", e);
        } catch ( IOException e) {
            log.error("Failed to close StringWriter", e);
        }
        return StringUtils.EMPTY;
    }
}
