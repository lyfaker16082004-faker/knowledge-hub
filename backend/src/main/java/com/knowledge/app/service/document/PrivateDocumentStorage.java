package com.knowledge.app.service.document;
import java.nio.file.*;
import java.nio.*;
import java.nio.charset.*;
import java.util.*;
import java.util.zip.ZipFile;
import java.io.IOException;
import com.knowledge.app.common.exception.ApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.transaction.support.*;
@Service
public class PrivateDocumentStorage {
 private final Path directory;
 public PrivateDocumentStorage(@Value("${app.documents.storage-dir:./config/document-storage}") String directory){this.directory=Path.of(directory).toAbsolutePath().normalize();}
 public StoredFile store(MultipartFile file){
  if(file==null||file.isEmpty()||file.getSize()>20*1024*1024)throw invalid("Chọn tệp không rỗng, dung lượng tối đa 20 MB");
  String raw=Optional.ofNullable(file.getOriginalFilename()).orElse("").replace((char)92,(char)47);String baseName=raw.substring(raw.lastIndexOf(47)+1);String name=baseName.codePoints().filter(c->!Character.isISOControl(c)).collect(StringBuilder::new,StringBuilder::appendCodePoint,StringBuilder::append).toString();
  if(name.isBlank()||name.length()>255)throw invalid("Tên tệp không hợp lệ");
  String extension=name.contains(".")?name.substring(name.lastIndexOf('.')+1).toLowerCase(Locale.ROOT):"";
  String type=switch(extension){case "pdf"->"application/pdf";case "ppt"->"application/vnd.ms-powerpoint";case "pptx"->"application/vnd.openxmlformats-officedocument.presentationml.presentation";case "docx"->"application/vnd.openxmlformats-officedocument.wordprocessingml.document";case "md"->"text/markdown; charset=UTF-8";case "txt"->"text/plain; charset=UTF-8";default->throw invalid("Chỉ hỗ trợ PDF, PPT/PPTX, DOCX, Markdown và TXT");};
  String key="upload-"+UUID.randomUUID()+"."+extension;Path target=directory.resolve(key).normalize();
  if(!target.startsWith(directory))throw invalid("Đường dẫn tệp không hợp lệ");
  try{
   byte[] bytes=file.getBytes();
   if(extension.equals("pdf")&&(bytes.length<5||!new String(bytes,0,5,StandardCharsets.US_ASCII).equals("%PDF-")))throw invalid("Nội dung không phải tệp PDF");
   if(extension.equals("md")||extension.equals("txt"))try{StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT).onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(bytes));}catch(CharacterCodingException e){throw invalid("Tệp văn bản cần sử dụng UTF-8 để giữ tiếng Việt");}
   Files.createDirectories(directory);Files.write(target,bytes,StandardOpenOption.CREATE_NEW);
   if(extension.equals("docx")||extension.equals("pptx"))validateOffice(target,extension);
   if(extension.equals("ppt"))validatePpt(target);
   if(!TransactionSynchronizationManager.isSynchronizationActive())throw new IllegalStateException("Uploads require a transaction");
   TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization(){@Override public void afterCompletion(int status){if(status!=STATUS_COMMITTED)remove(target);}});
   return new StoredFile(key,name,type,bytes.length);
  }catch(ApiException e){remove(target);throw e;}catch(IOException e){remove(target);throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE,"Chưa thể lưu tệp. Vui lòng thử lại sau.");}catch(RuntimeException e){remove(target);throw e;}
 }
 public byte[] read(String key){
  try{
   if(key.matches("upload-[a-f0-9-]{36}[.](pdf|ppt|pptx|docx|md|txt)")){Path target=directory.resolve(key).normalize();if(!target.startsWith(directory)||!Files.isRegularFile(target)||Files.size(target)>20*1024*1024)throw invalid("Tệp không khả dụng");return Files.readAllBytes(target);}
   if(key.matches("[a-z0-9-]+[.]md"))try(var stream=new ClassPathResource("library/"+key).getInputStream()){return stream.readAllBytes();}
  }catch(IOException e){throw new ApiException(HttpStatus.NOT_FOUND,"Tệp tài liệu tạm thời không khả dụng");}
  throw new ApiException(HttpStatus.NOT_FOUND,"Tệp không khả dụng");
 }
 private void validateOffice(Path file,String extension){try(var zip=new ZipFile(file.toFile())){String required=extension.equals("pptx")?"ppt/presentation.xml":"word/document.xml";if(zip.getEntry("[Content_Types].xml")==null||zip.getEntry(required)==null)throw invalid("Tệp Office không hợp lệ");long total=0;int count=0;var names=new HashSet<String>();var entries=zip.entries();while(entries.hasMoreElements()){var entry=entries.nextElement();String name=entry.getName();if(!names.add(name)||name.contains("..")||name.startsWith("/")||name.toLowerCase(Locale.ROOT).contains("vbaproject")||++count>5000||entry.getSize()<0||(total+=entry.getSize())>100*1024*1024)throw invalid("Tệp Office có macro, cấu trúc sai hoặc vượt giới hạn giải nén");try(var input=zip.getInputStream(entry)){long actual=0;byte[] buffer=new byte[8192];int n;while((n=input.read(buffer))!=-1)if((actual+=n)>100*1024*1024||actual>entry.getSize())throw invalid("Nội dung Office vượt giới hạn");}}}catch(IOException e){throw invalid("Không đọc được cấu trúc Office");}}
 private void validatePpt(Path file){try(var input=Files.newInputStream(file);var fs=new org.apache.poi.poifs.filesystem.POIFSFileSystem(input)){var root=fs.getRoot();if(!root.hasEntry("PowerPoint Document")||!root.hasEntry("Current User")||root.hasEntry("VBA")||root.hasEntry("_VBA_PROJECT_CUR"))throw invalid("Tệp PPT không hợp lệ hoặc có macro");}catch(IOException|IllegalArgumentException e){throw invalid("Không đọc được tệp PPT");}}
 private void remove(Path path){try{Files.deleteIfExists(path);}catch(IOException ignored){/* Best-effort cleanup of a failed private upload. */}}
 private ApiException invalid(String message){return new ApiException(HttpStatus.BAD_REQUEST,message);}
 public record StoredFile(String key,String name,String contentType,long size){}
}
