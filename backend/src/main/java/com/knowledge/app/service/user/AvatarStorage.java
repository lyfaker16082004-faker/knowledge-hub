package com.knowledge.app.service.user;
import java.nio.file.*;
import java.io.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.UUID;
import javax.imageio.ImageIO;
import com.knowledge.app.common.exception.ApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.transaction.support.*;
@Service
public class AvatarStorage {
 private final Path directory;
 public AvatarStorage(@Value("${app.avatars.storage-dir:./config/avatar-storage}") String directory){this.directory=Path.of(directory).toAbsolutePath().normalize();}
 public String save(MultipartFile file){
  if(file==null||file.isEmpty())throw invalid("Chọn ảnh đại diện PNG hoặc JPEG");
  if(file.getSize()>=2*1024*1024)throw new ApiException(HttpStatus.PAYLOAD_TOO_LARGE,"Ảnh đại diện phải nhỏ hơn 2 MB");
  String key=UUID.randomUUID()+".png";Path target=directory.resolve(key).normalize();
  try(var input=ImageIO.createImageInputStream(new ByteArrayInputStream(file.getBytes()))){
   var readers=ImageIO.getImageReaders(input);if(!readers.hasNext())throw invalid("Nội dung không phải ảnh PNG/JPEG hợp lệ");var reader=readers.next();BufferedImage image;
   try{String format=reader.getFormatName();if(!format.equalsIgnoreCase("png")&&!format.equalsIgnoreCase("jpeg"))throw invalid("Chỉ hỗ trợ ảnh PNG và JPEG");reader.setInput(input,true,true);int width=reader.getWidth(0),height=reader.getHeight(0);if(width<1||height<1||width>4096||height>4096||(long)width*height>16000000)throw invalid("Ảnh quá lớn; tối đa 4096 px và 16 triệu điểm ảnh");image=reader.read(0);}finally{reader.dispose();}
   if(image==null)throw invalid("Không đọc được ảnh đại diện");
   double scale=Math.min(1.0,512.0/Math.max(image.getWidth(),image.getHeight()));var normalized=new BufferedImage(Math.max(1,(int)(image.getWidth()*scale)),Math.max(1,(int)(image.getHeight()*scale)),BufferedImage.TYPE_INT_ARGB);var graphics=normalized.createGraphics();try{graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BICUBIC);graphics.drawImage(image,0,0,normalized.getWidth(),normalized.getHeight(),null);}finally{graphics.dispose();}
   Files.createDirectories(directory);try(var output=Files.newOutputStream(target,StandardOpenOption.CREATE_NEW)){if(!ImageIO.write(normalized,"png",output))throw invalid("Không chuyển đổi được ảnh");}
   if(!TransactionSynchronizationManager.isSynchronizationActive())throw new IllegalStateException("Avatar writes require transaction");
   TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization(){@Override public void afterCompletion(int status){if(status!=STATUS_COMMITTED)remove(target);}});
   return "/api/avatars/"+key;
  }catch(ApiException e){remove(target);throw e;}catch(IOException|RuntimeException e){remove(target);throw invalid("Không thể xử lý ảnh PNG/JPEG. Hãy thử một ảnh khác.");}
 }
 public byte[] read(String key){if(!key.matches("[a-f0-9-]{36}[.]png"))throw new ApiException(HttpStatus.NOT_FOUND,"Không tìm thấy ảnh");Path file=directory.resolve(key).normalize();try{if(!file.startsWith(directory)||!Files.isRegularFile(file))throw new IOException();return Files.readAllBytes(file);}catch(IOException e){throw new ApiException(HttpStatus.NOT_FOUND,"Không tìm thấy ảnh");}}
 public void deleteAfterCommit(String url){if(url==null||!url.matches("/api/avatars/[a-f0-9-]{36}[.]png"))return;Path old=directory.resolve(url.substring(url.lastIndexOf('/')+1)).normalize();if(!old.startsWith(directory))return;TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization(){@Override public void afterCommit(){remove(old);}});}
 private void remove(Path file){try{Files.deleteIfExists(file);}catch(IOException ignored){}}
 private ApiException invalid(String message){return new ApiException(HttpStatus.BAD_REQUEST,message);}
}
