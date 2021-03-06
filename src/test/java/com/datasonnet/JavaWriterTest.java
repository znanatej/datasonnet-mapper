package com.datasonnet;

import com.datasonnet.document.DefaultDocument;
import com.datasonnet.document.Document;
import com.datasonnet.document.MediaTypes;
import com.datasonnet.javatest.Gizmo;
import com.datasonnet.javatest.WsdlGeneratedObj;
import com.datasonnet.util.TestResourceReader;
import org.junit.jupiter.api.Test;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;


public class JavaWriterTest {

    @Test
    void testJavaWriter() throws Exception {
        //Test with output as Gizmo class
        String json = TestResourceReader.readFileAsString("javaTest.json");
        String mapping = TestResourceReader.readFileAsString("writeJavaTest.ds");

        Document<String> data = new DefaultDocument<>(json, MediaTypes.APPLICATION_JSON);

        Mapper mapper = new Mapper(mapping);


        Document<Gizmo> mapped = mapper.transform(data, new HashMap<>(), MediaTypes.APPLICATION_JAVA, Gizmo.class);

        Object result = mapped.getContent();
        assertTrue(result instanceof Gizmo);

        Gizmo gizmo = (Gizmo)result;
        assertEquals("gizmo", gizmo.getName());
        assertEquals(123, gizmo.getQuantity());
        assertEquals(true, gizmo.isInStock());
        assertEquals(Arrays.asList("red","white","blue"), gizmo.getColors());
        assertEquals("ACME Corp.", gizmo.getManufacturer().getManufacturerName());
        assertEquals("ACME123", gizmo.getManufacturer().getManufacturerCode());

        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");
        assertEquals("2020-01-06", df.format(gizmo.getDate()));

        //Test with default output, i.e. java.util.HashMap
        mapping = mapping.substring(mapping.lastIndexOf("*/") + 2);

        mapper = new Mapper(mapping);


        Document<Map> mappedMap = mapper.transform(data, new HashMap<>(), MediaTypes.APPLICATION_JAVA, Map.class);

        result = mappedMap.getContent();
        assertTrue(result instanceof java.util.HashMap);

        Map gizmoMap = (Map)result;
        assertTrue(gizmoMap.get("colors") instanceof java.util.ArrayList);
        assertTrue(gizmoMap.get("manufacturer") instanceof java.util.HashMap);


    }

    @Test
    void testJavaWriteFunction() throws Exception {
        String json = TestResourceReader.readFileAsString("javaTest.json");
        Document<String> data = new DefaultDocument<>(json, MediaTypes.APPLICATION_JSON);

        //Test calling write() function
        String mapping = TestResourceReader.readFileAsString("writeJavaFunctionTest.ds");
        Mapper mapper = new Mapper(mapping);


        try {
            mapper.transform(data, new HashMap<>(), MediaTypes.APPLICATION_JAVA);
            fail("Should not succeed");
        } catch(Exception e) {
            // this error is now thrown by jackson as it _will_ try to write a String...
            assertTrue(e.getMessage().contains("Unable to convert to target type"), "Failed with wrong message: " + e.getMessage());
        }
    }

    @Test
    void testJAXBElementMapping() throws Exception {
        Document<String> data = new DefaultDocument<>("{}", MediaTypes.APPLICATION_JSON);
        String mapping = TestResourceReader.readFileAsString("writeJAXBElement.ds");
        Mapper mapper = new Mapper(mapping);

        Document<WsdlGeneratedObj> mapped = mapper.transform(data, new HashMap<>(), MediaTypes.APPLICATION_JAVA, WsdlGeneratedObj.class);
        Object result = mapped.getContent();
        assertTrue(result instanceof WsdlGeneratedObj);

        JAXBContext jaxbContext = JAXBContext.newInstance(WsdlGeneratedObj.class );
        Marshaller jaxbMarshaller = jaxbContext.createMarshaller();
        jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);

        StringWriter sw = new StringWriter();
        jaxbMarshaller.marshal(result, sw);
        assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
                "<WsdlGeneratedObj xmlns:ns2=\"http://com.datasonnet.test\">\n" +
                "    <ns2:testField>\n" +
                "        <test>Hello World</test>\n" +
                "    </ns2:testField>\n" +
                "</WsdlGeneratedObj>\n", sw.toString());
    }
}
