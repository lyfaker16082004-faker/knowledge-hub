package com.knowledge.app.service.document;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.knowledge.app.common.exception.ApiException;

/**
 * Trích văn bản từ file DOCX / PPTX để hiển thị bản xem trước.
 *
 * File DOCX/PPTX thực chất là file ZIP chứa các file XML.
 * Chữ trong văn bản nằm trong các thẻ <w:t> hoặc <a:t> (tên thẻ là "t"),
 * mỗi đoạn văn nằm trong thẻ "p".
 *
 * Kết quả có dạng:
 *   ── Slide 1 ──            (PPTX: mỗi slide một phần, đúng thứ tự slide)
 *   ── Nội dung tài liệu ──  (DOCX: một phần duy nhất)
 * Giao diện dựa vào dòng "── ... ──" để tách thành từng slide.
 */
@Service
public class OfficeTextPreview {

    private static final int MAX_SECTIONS = 300;                 // Tối đa 300 slide / phần
    private static final int MAX_XML_SIZE = 10 * 1024 * 1024;    // Mỗi file XML tối đa 10 MB
    private static final int MAX_TEXT_LENGTH = 100_000;          // Văn bản xem trước tối đa 100.000 ký tự

    private static final Pattern WORD_ENTRY = Pattern.compile("word/document[.]xml");
    private static final Pattern SLIDE_ENTRY = Pattern.compile("ppt/slides/slide([0-9]+)[.]xml");

    public String extract(byte[] bytes) {
        XMLInputFactory factory = createSafeXmlFactory();

        // Trong file ZIP, slide10 có thể nằm trước slide2 -> gom lại theo số slide rồi mới ghép
        Map<Integer, String> slides = new TreeMap<>();
        String wordText = null;

        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(bytes))) {
            ZipEntry entry;
            int totalLength = 0;

            while ((entry = zip.getNextEntry()) != null) {
                Matcher slide = SLIDE_ENTRY.matcher(entry.getName());
                boolean isWord = WORD_ENTRY.matcher(entry.getName()).matches();
                boolean isSlide = slide.matches();

                // Bỏ qua các file không chứa nội dung
                if (!isWord && !isSlide) {
                    continue;
                }
                if (slides.size() >= MAX_SECTIONS) {
                    break;
                }

                // Đọc nội dung XML (đọc dư 1 byte để biết có vượt giới hạn không)
                byte[] xml = zip.readNBytes(MAX_XML_SIZE + 1);
                if (xml.length > MAX_XML_SIZE) {
                    throw new IOException("XML too large");
                }

                String text = cleanUp(readTextFromXml(factory, xml));
                totalLength += text.length();

                if (isWord) {
                    wordText = text;
                } else {
                    slides.put(Integer.parseInt(slide.group(1)), text);
                }

                if (totalLength > MAX_TEXT_LENGTH) {
                    break;
                }
            }
        } catch (IOException | XMLStreamException | NumberFormatException e) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "Không thể trích nội dung xem trước. Bạn có thể tải bản gốc để kiểm tra");
        }

        // Ghép kết quả
        StringBuilder result = new StringBuilder();
        if (wordText != null) {
            result.append("── Nội dung tài liệu ──\n").append(wordText).append('\n');
        }
        for (Map.Entry<Integer, String> slide : slides.entrySet()) {
            result.append("\n── Slide ").append(slide.getKey()).append(" ──\n").append(slide.getValue()).append('\n');
        }

        if (result.toString().isBlank()) {
            return "Không có văn bản để xem trước; hãy tải bản gốc để kiểm tra hình ảnh/nội dung.";
        }
        if (result.length() > MAX_TEXT_LENGTH) {
            return result.substring(0, MAX_TEXT_LENGTH) + "\n… (bản xem trước đã rút gọn)";
        }
        return result.toString().strip();
    }

    /**
     * Dọn văn bản cho dễ đọc:
     * - Gộp nhiều dấu cách liền nhau thành một
     * - Bỏ dãy dấu chấm dẫn trong mục lục (". . . . . 12")
     * - Gộp nhiều dòng trống thành một dòng trống
     */
    static String cleanUp(String text) {
        String result = text.replace(' ', ' ');
        result = result.replaceAll("(\\s*\\.){4,}\\s*", " … ");
        result = result.replaceAll("[ \\t]+", " ");
        result = result.replaceAll(" *\\n *", "\n");
        result = result.replaceAll("\\n{3,}", "\n\n");
        return result.strip();
    }

    /**
     * Tạo bộ đọc XML an toàn: tắt DTD và entity bên ngoài để chống tấn công XXE.
     */
    private XMLInputFactory createSafeXmlFactory() {
        XMLInputFactory factory = XMLInputFactory.newFactory();
        factory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
        factory.setProperty("javax.xml.stream.isSupportingExternalEntities", false);
        return factory;
    }

    /**
     * Đọc chữ trong một file XML.
     * Các mảnh chữ <t> trong cùng đoạn được nối liền (Word hay cắt một từ thành nhiều mảnh),
     * hết đoạn </p> hoặc gặp <br/> thì xuống dòng, gặp <tab/> thì thêm dấu cách.
     */
    private String readTextFromXml(XMLInputFactory factory, byte[] xml) throws XMLStreamException {
        StringBuilder text = new StringBuilder();
        XMLStreamReader reader = factory.createXMLStreamReader(new ByteArrayInputStream(xml));
        try {
            boolean insideTextTag = false; // Đang ở bên trong thẻ <t> hay không

            while (reader.hasNext()) {
                int event = reader.next();

                if (event == XMLStreamConstants.DTD) {
                    throw new XMLStreamException("DTD not allowed");
                }

                if (event == XMLStreamConstants.START_ELEMENT) {
                    String name = reader.getLocalName();
                    if (name.equals("t")) {
                        insideTextTag = true;
                    } else if (name.equals("tab")) {
                        text.append(' ');
                    } else if (name.equals("br")) {
                        text.append('\n');
                    }
                } else if (event == XMLStreamConstants.END_ELEMENT) {
                    String name = reader.getLocalName();
                    if (name.equals("t")) {
                        insideTextTag = false;
                    } else if (name.equals("p")) {
                        text.append('\n');
                    }
                } else if (event == XMLStreamConstants.CHARACTERS && insideTextTag) {
                    text.append(reader.getText());
                }

                if (text.length() > MAX_TEXT_LENGTH) {
                    break;
                }
            }
            return text.toString();
        } finally {
            reader.close();
        }
    }
}
