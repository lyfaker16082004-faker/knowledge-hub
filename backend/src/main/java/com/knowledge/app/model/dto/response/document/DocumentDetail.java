package com.knowledge.app.model.dto.response.document;
import java.util.List;
public record DocumentDetail(DocumentSummary document,String description,String author,boolean saved,boolean canDownload,List<FileInfo> files,String status,String reviewNote,Long version,boolean canManage,boolean canPreview) {
 public record FileInfo(Long id,String name,String contentType,long sizeBytes) {}
}
