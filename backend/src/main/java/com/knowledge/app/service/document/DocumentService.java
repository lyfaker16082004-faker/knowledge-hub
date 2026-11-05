package com.knowledge.app.service.document;
import com.knowledge.app.common.response.PageResponse;
import com.knowledge.app.model.dto.response.document.*;
import com.knowledge.app.model.dto.response.document.LibraryResponses.*;
import java.util.List;
public interface DocumentService {
 PageResponse<DocumentSummary> catalogue(String q,Long category,String access,int page,int size,String sort,Long institutionId,Long facultyId,boolean global,Long userId);
 DocumentSummary upload(Long userId,com.knowledge.app.model.dto.request.document.UploadDocumentRequest request,org.springframework.web.multipart.MultipartFile file);
 List<CategoryInfo> categories();CatalogueStats stats();DocumentDetail detail(Long id,Long userId);
 DownloadPayload download(Long documentId,Long fileId,Long userId);
 PersonalStats personalStats(Long userId);PageResponse<DownloadInfo> history(Long userId,int page,int size);
 PageResponse<DocumentSummary> saved(Long userId,int page,int size);
 void bookmark(Long userId,Long documentId,boolean saved);void grant(Long documentId,Long userId);
 record DownloadPayload(byte[] bytes,String fileName,String contentType) {}
}
