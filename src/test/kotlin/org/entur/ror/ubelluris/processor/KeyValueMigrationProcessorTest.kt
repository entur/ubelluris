package org.entur.ror.ubelluris.processor

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

class KeyValueMigrationProcessorTest {

    private val processor = KeyValueMigrationProcessor()

    @Test
    fun shouldZeroPadOwnerValueWhenNumeric(@TempDir tempDir: Path) {
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
    fun shouldZeroPadDataFromValueWhenNumeric(@TempDir tempDir: Path) {
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
    fun shouldNotZeroPadNonNumericValues(@TempDir tempDir: Path) {
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
    fun shouldNotZeroPadValuesForKeysNotPresentInZeroPadList(@TempDir tempDir: Path) {
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
    fun shouldRemoveBlacklistedKeyValueElements(@TempDir tempDir: Path) {
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
    fun shouldRemoveAllBlacklistedKeys(@TempDir tempDir: Path) {
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
    fun shouldHandleMultipleKeyValueElementsCorrectly(@TempDir tempDir: Path) {
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

        assertThat(result).contains("<Value>005</Value>")
        assertThat(result).contains("<Value>099</Value>")

        assertThat(result).doesNotContain("trafikverket-name")

        assertThat(result).contains("<Key>keep-me</Key>")
        assertThat(result).contains("<Value>123</Value>")
    }

    @Test
    fun shouldHandleEmptyKeyList(@TempDir tempDir: Path) {
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

}