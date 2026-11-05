package com.knowledge.app.service.document;
import java.io.*;
import java.util.zip.*;
import javax.xml.stream.*;
import org.springframework.stereotype.Service;
import com.knowledge.app.common.exception.ApiException;
import org.springframework.http.HttpStatus;
@Service public class OfficeTextPreview {
 public String extract(byte[] bytes){
  StringBuilder text=new StringBuilder();var factory=XMLInputFactory.newFactory();factory.setProperty(XMLInputFactory.SUPPORT_DTD,false);factory.setProperty("javax.xml.stream.isSupportingExternalEntities",false);
  try(var zip=new ZipInputStream(new ByteArrayInputStream(bytes))){ZipEntry entry;int sections=0;
   while((entry=zip.getNextEntry())!=null){if(!entry.getName().matches("word/document[.]xml|ppt/slides/slide[0-9]+[.]xml"))continue;
    if(++sections>300)break;byte[] xml=zip.readNBytes(10*1024*1024+1);if(xml.length>10*1024*1024)throw new IOException("XML too large");text.append("\n── ").append(entry.getName().startsWith("ppt/")?"Slide "+sections:"Nội dung tài liệu").append(" ──\n");
    var reader=factory.createXMLStreamReader(new ByteArrayInputStream(xml));try{boolean inside=false;while(reader.hasNext()){int event=reader.next();if(event==XMLStreamConstants.DTD)throw new XMLStreamException("DTD not allowed");if(event==XMLStreamConstants.START_ELEMENT&&reader.getLocalName().equals("t"))inside=true;else if(event==XMLStreamConstants.END_ELEMENT){if(reader.getLocalName().equals("t")){inside=false;text.append(' ');}if(reader.getLocalName().equals("p"))text.append('\n');}else if(event==XMLStreamConstants.CHARACTERS&&inside)text.append(reader.getText());if(text.length()>100000)return text.substring(0,100000)+"\n… (bản xem trước đã rút gọn)";}}finally{reader.close();}
   }
  }catch(IOException|XMLStreamException e){throw new ApiException(HttpStatus.BAD_REQUEST,"Không thể trích nội dung xem trước. Bạn có thể tải bản gốc để kiểm tra");}
  return text.isEmpty()?"Không có văn bản để xem trước; hãy tải bản gốc để kiểm tra hình ảnh/nội dung.":text.toString();
 }
}
