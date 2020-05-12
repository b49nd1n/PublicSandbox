package ru.mgvk;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;


public class XXEtest {


    public static void main(String[] args) throws JAXBException, IOException, XMLStreamException {
        new XXEtest().xmlExternalEntity();
    }

    public void xmlExternalEntity() throws IOException,
            JAXBException, XMLStreamException {


        // Создаём временный файл и вписываем туда секрет


        Path secretfile = Paths.get("secretfile");


        System.out.println("TMP File:");
        Files.readAllLines(secretfile).forEach(System.out::println);

        String xmlString2 = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                            + "<!DOCTYPE myobject[<!ENTITY xxe SYSTEM \"file:///"
                            + secretfile.toAbsolutePath() // XXE
                            + "\">]>"
                            + "<myobject>"
                            + "<field1>"
                            + "&xxe;"
                            + "</field1>"
                            + "</myobject>";


        System.out.println("XML:");
        System.out.println(xmlString2);
        JAXBContext  jaxbContext      = JAXBContext.newInstance(MyObject.class);
        MyObject     myObject;
        Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();


        System.out.println("INSECURE VARIANT:");


        XMLInputFactory xif = XMLInputFactory.newFactory();

        /*
         *      включать поддержку внешних сущностей нет необходимости - она по умолчанию включена
         *      в дефолтных значениях такой XMLInputFactory
         */

//        xif.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, true);
//        xif.setProperty(XMLInputFactory.SUPPORT_DTD, true);


        try (StringReader stringReader = new StringReader(xmlString2)) {
            XMLStreamReader xsr = xif.createXMLStreamReader(stringReader);
            myObject = (MyObject) jaxbUnmarshaller.unmarshal(xsr);
        }
        System.out.println("XXE success: " + myObject.getField1());


        System.out.println("SECURE VARIANT:");

        /*      А вот без создания своей  кривой фабрики все достаточно безопасно - выбрасывает exception,
         *      мол. "access is not allowed"
         */

        try {
            myObject = (MyObject) jaxbUnmarshaller.unmarshal(new StringReader(xmlString2));

            System.out.println(myObject.getField1());
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("cannot XXE :(");
        }


    }

    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlRootElement(name = "myobject")
    static class MyObject {

        @XmlElement(name = "field1")
        private String field1;

        public String getField1() {
            return field1;
        }

        public void setField1(String field) {
            this.field1 = field;
        }

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder();
            builder.append("MyObject [field1=");
            builder.append(field1);
            builder.append("]");
            return builder.toString();
        }

    }
}