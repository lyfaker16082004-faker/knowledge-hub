package com.knowledge.app.model.dto.response.document;
import java.time.LocalDateTime;
public final class LibraryResponses {
 private LibraryResponses() {}
 public record CategoryInfo(Long id,String name,String slug,long count) {}
 public record CatalogueStats(long documents,long freeDocuments,long categories,long downloads) {}
 public record PersonalStats(long downloadedDocuments,long downloads,long savedDocuments) {}
 public record DownloadInfo(Long id,Long documentId,String title,String category,String fileName,LocalDateTime downloadedAt,boolean available) {}
}
