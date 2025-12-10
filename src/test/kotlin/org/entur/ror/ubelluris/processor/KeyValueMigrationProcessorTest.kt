package org.entur.ror.ubelluris.processor

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import javax.xml.parsers.DocumentBuilderFactory

class KeyValueMigrationProcessorTest {

    private val processor = KeyValueMigrationProcessor()

    @Test
    fun `should zero-pad owner value when it is numeric`(@TempDir tempDir: Path) {
        val inputXml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <PublicationDelivery xmlns="http://www.netex.org.uk/netex">
                <dataObjects>
                    <StopPlace id="SAM:StopPlace:1">
                        <keyList>
                            <KeyValue>
                                <Key>owner</Key>
                                <Value>42</Value>
                            </KeyValue>
                        </keyList>
                    </StopPlace>
                </dataObjects>
            </PublicationDelivery>
        """.trimIndent()

        val xmlFile = tempDir.resolve("test.xml").toFile()
        xmlFile.writeText(inputXml)

        processor.process(xmlFile)

        val result = xmlFile.readText()
        assertThat(result).contains("<Value>042</Value>")
    }

    @Test
    fun `should zero-pad data-from value when it is numeric`(@TempDir tempDir: Path) {
        val inputXml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <PublicationDelivery xmlns="http://www.netex.org.uk/netex">
                <dataObjects>
                    <StopPlace id="SAM:StopPlace:1">
                        <keyList>
                            <KeyValue>
                                <Key>data-from</Key>
                                <Value>7</Value>
                            </KeyValue>
                        </keyList>
                    </StopPlace>
                </dataObjects>
            </PublicationDelivery>
        """.trimIndent()

        val xmlFile = tempDir.resolve("test.xml").toFile()
        xmlFile.writeText(inputXml)

        processor.process(xmlFile)

        val result = xmlFile.readText()
        assertThat(result).contains("<Value>007</Value>")
    }

    @Test
    fun `should not zero-pad non-numeric values`(@TempDir tempDir: Path) {
        val inputXml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <PublicationDelivery xmlns="http://www.netex.org.uk/netex">
                <dataObjects>
                    <StopPlace id="SAM:StopPlace:1">
                        <keyList>
                            <KeyValue>
                                <Key>owner</Key>
                                <Value>ABC123</Value>
                            </KeyValue>
                        </keyList>
                    </StopPlace>
                </dataObjects>
            </PublicationDelivery>
        """.trimIndent()

        val xmlFile = tempDir.resolve("test.xml").toFile()
        xmlFile.writeText(inputXml)

        processor.process(xmlFile)

        val result = xmlFile.readText()
        assertThat(result).contains("<Value>ABC123</Value>")
    }

    @Test
    fun `should not zero-pad values for keys not in the zero-pad list`(@TempDir tempDir: Path) {
        val inputXml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <PublicationDelivery xmlns="http://www.netex.org.uk/netex">
                <dataObjects>
                    <StopPlace id="SAM:StopPlace:1">
                        <keyList>
                            <KeyValue>
                                <Key>other-key</Key>
                                <Value>42</Value>
                            </KeyValue>
                        </keyList>
                    </StopPlace>
                </dataObjects>
            </PublicationDelivery>
        """.trimIndent()

        val xmlFile = tempDir.resolve("test.xml").toFile()
        xmlFile.writeText(inputXml)

        processor.process(xmlFile)

        val result = xmlFile.readText()
        assertThat(result).contains("<Value>42</Value>")
        assertThat(result).doesNotContain("<Value>042</Value>")
    }

    @Test
    fun `should remove blacklisted KeyValue elements`(@TempDir tempDir: Path) {
        val inputXml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <PublicationDelivery xmlns="http://www.netex.org.uk/netex">
                <dataObjects>
                    <StopPlace id="SAM:StopPlace:1">
                        <keyList>
                            <KeyValue>
                                <Key>trafikverket-name</Key>
                                <Value>SomeValue</Value>
                            </KeyValue>
                            <KeyValue>
                                <Key>owner</Key>
                                <Value>42</Value>
                            </KeyValue>
                        </keyList>
                    </StopPlace>
                </dataObjects>
            </PublicationDelivery>
        """.trimIndent()

        val xmlFile = tempDir.resolve("test.xml").toFile()
        xmlFile.writeText(inputXml)

        processor.process(xmlFile)

        val result = xmlFile.readText()
        assertThat(result).doesNotContain("trafikverket-name")
        assertThat(result).contains("<Key>owner</Key>")
        assertThat(result).contains("<Value>042</Value>")
    }

    @Test
    fun `should remove all blacklisted keys`(@TempDir tempDir: Path) {
        val inputXml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <PublicationDelivery xmlns="http://www.netex.org.uk/netex">
                <dataObjects>
                    <StopPlace id="SAM:StopPlace:1">
                        <keyList>
                            <KeyValue>
                                <Key>local-name</Key>
                                <Value>Test1</Value>
                            </KeyValue>
                            <KeyValue>
                                <Key>sellable</Key>
                                <Value>Test2</Value>
                            </KeyValue>
                            <KeyValue>
                                <Key>preliminary</Key>
                                <Value>Test3</Value>
                            </KeyValue>
                            <KeyValue>
                                <Key>valid-key</Key>
                                <Value>Keep</Value>
                            </KeyValue>
                        </keyList>
                    </StopPlace>
                </dataObjects>
            </PublicationDelivery>
        """.trimIndent()

        val xmlFile = tempDir.resolve("test.xml").toFile()
        xmlFile.writeText(inputXml)

        processor.process(xmlFile)

        val result = xmlFile.readText()
        assertThat(result).doesNotContain("local-name")
        assertThat(result).doesNotContain("sellable")
        assertThat(result).doesNotContain("preliminary")
        assertThat(result).contains("<Key>valid-key</Key>")
        assertThat(result).contains("<Value>Keep</Value>")
    }

    @Test
    fun `should handle multiple KeyValue elements correctly`(@TempDir tempDir: Path) {
        val inputXml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <PublicationDelivery xmlns="http://www.netex.org.uk/netex">
                <dataObjects>
                    <StopPlace id="SAM:StopPlace:1">
                        <keyList>
                            <KeyValue>
                                <Key>owner</Key>
                                <Value>5</Value>
                            </KeyValue>
                            <KeyValue>
                                <Key>data-from</Key>
                                <Value>99</Value>
                            </KeyValue>
                            <KeyValue>
                                <Key>trafikverket-name</Key>
                                <Value>Remove</Value>
                            </KeyValue>
                            <KeyValue>
                                <Key>keep-me</Key>
                                <Value>123</Value>
                            </KeyValue>
                        </keyList>
                    </StopPlace>
                </dataObjects>
            </PublicationDelivery>
        """.trimIndent()

        val xmlFile = tempDir.resolve("test.xml").toFile()
        xmlFile.writeText(inputXml)

        processor.process(xmlFile)

        val result = xmlFile.readText()

        // Zero-padded values
        assertThat(result).contains("<Value>005</Value>")
        assertThat(result).contains("<Value>099</Value>")

        // Removed blacklisted key
        assertThat(result).doesNotContain("trafikverket-name")

        // Preserved non-targeted key
        assertThat(result).contains("<Key>keep-me</Key>")
        assertThat(result).contains("<Value>123</Value>")
    }

    @Test
    fun `should preserve XML structure and other elements`(@TempDir tempDir: Path) {
        val inputXml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <PublicationDelivery xmlns="http://www.netex.org.uk/netex">
                <dataObjects>
                    <StopPlace id="SAM:StopPlace:1" version="1">
                        <Name>Test Stop</Name>
                        <keyList>
                            <KeyValue>
                                <Key>owner</Key>
                                <Value>8</Value>
                            </KeyValue>
                        </keyList>
                        <Centroid>
                            <Location>
                                <Longitude>12.345</Longitude>
                                <Latitude>67.890</Latitude>
                            </Location>
                        </Centroid>
                    </StopPlace>
                </dataObjects>
            </PublicationDelivery>
        """.trimIndent()

        val xmlFile = tempDir.resolve("test.xml").toFile()
        xmlFile.writeText(inputXml)

        processor.process(xmlFile)

        val result = xmlFile.readText()

        // Check that KeyValue was processed
        assertThat(result).contains("<Value>008</Value>")

        // Check that other elements are preserved
        assertThat(result).contains("<Name>Test Stop</Name>")
        assertThat(result).contains("<Longitude>12.345</Longitude>")
        assertThat(result).contains("<Latitude>67.890</Latitude>")
        assertThat(result).contains("id=\"SAM:StopPlace:1\"")
    }

    @Test
    fun `should handle empty keyList`(@TempDir tempDir: Path) {
        val inputXml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <PublicationDelivery xmlns="http://www.netex.org.uk/netex">
                <dataObjects>
                    <StopPlace id="SAM:StopPlace:1">
                        <keyList>
                        </keyList>
                    </StopPlace>
                </dataObjects>
            </PublicationDelivery>
        """.trimIndent()

        val xmlFile = tempDir.resolve("test.xml").toFile()
        xmlFile.writeText(inputXml)

        processor.process(xmlFile)

        val result = xmlFile.readText()
        assertThat(result).contains("<keyList>")
    }

    @Test
    fun `should produce valid XML output`(@TempDir tempDir: Path) {
        val inputXml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <PublicationDelivery xmlns="http://www.netex.org.uk/netex">
                <dataObjects>
                    <StopPlace id="SAM:StopPlace:1">
                        <keyList>
                            <KeyValue>
                                <Key>owner</Key>
                                <Value>9</Value>
                            </KeyValue>
                        </keyList>
                    </StopPlace>
                </dataObjects>
            </PublicationDelivery>
        """.trimIndent()

        val xmlFile = tempDir.resolve("test.xml").toFile()
        xmlFile.writeText(inputXml)

        processor.process(xmlFile)

        // Verify the output is valid XML by parsing it
        val factory = DocumentBuilderFactory.newInstance()
        val builder = factory.newDocumentBuilder()
        val doc = builder.parse(xmlFile)

        assertThat(doc).isNotNull
        assertThat(doc.documentElement.tagName).isEqualTo("PublicationDelivery")
    }
}