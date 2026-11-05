package com.knowledge.app.model.dto.response.document;
import java.util.List;
public record DocumentDetail(DocumentSummary document,String description,String author,boolean saved,boolean canDownload,List<FileInfo> files) {
 public record FileInfo(Long id,String name,String contentType,long sizeBytes) {}
}
