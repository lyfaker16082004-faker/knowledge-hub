package com.knowledge.app.service.impl.document;
import java.time.*;
import java.util.*;
import java.io.IOException;
import com.knowledge.app.common.exception.ApiException;
import com.knowledge.app.common.response.PageResponse;
import com.knowledge.app.model.entity.*;
import com.knowledge.app.model.dto.response.document.*;
import com.knowledge.app.model.dto.response.document.LibraryResponses.*;
import com.knowledge.app.repository.*;
import com.knowledge.app.service.document.DocumentService;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Service @Transactional(readOnly=true)
public class DocumentServiceImpl implements DocumentService {
 private final DocumentRepository docs;private final DocumentFileRepository files;private final DocumentAccessRepository access;private final DocumentDownloadRepository downloads;private final DocumentBookmarkRepository bookmarks;private final UserRepository users;private final CategoryRepository categories;private final com.knowledge.app.service.organization.OrganizationService organizations;private final com.knowledge.app.service.user.ProfileService profiles;private final com.knowledge.app.service.document.PrivateDocumentStorage storage;
 public DocumentServiceImpl(DocumentRepository docs,DocumentFileRepository files,DocumentAccessRepository access,DocumentDownloadRepository downloads,DocumentBookmarkRepository bookmarks,UserRepository users,CategoryRepository categories,com.knowledge.app.service.organization.OrganizationService organizations,com.knowledge.app.service.user.ProfileService profiles,com.knowledge.app.service.document.PrivateDocumentStorage storage){this.categories=categories;this.organizations=organizations;this.profiles=profiles;this.storage=storage;this.docs=docs;this.files=files;this.access=access;this.downloads=downloads;this.bookmarks=bookmarks;this.users=users;}
 @Override public PageResponse<DocumentSummary> catalogue(String q,Long category,String type,int page,int size,String order,Long institutionId,Long facultyId,boolean global,Long userId){
  if(global&&(institutionId!=null||facultyId!=null))throw new ApiException(HttpStatus.BAD_REQUEST,"Không kết hợp bộ lọc toàn cầu với trường/khoa cụ thể");
  if(!global&&institutionId==null&&userId!=null){var profile=profiles.organization(userId);if(profile!=null&&!"OTHER".equals(profile.institutionType()))institutionId=profile.institutionId();}
  var selection=organizations.resolve(institutionId,facultyId);
  Long school=selection.institution()==null||"OTHER".equals(selection.institution().getType())?null:selection.institution().getId();
  Sort sort="title".equals(order)?Sort.by("title").ascending():Sort.by("featured".equals(order)?"featured":"publishedAt").descending().and(Sort.by("id").descending());
  var result="popular".equals(order)?docs.popular(q.trim(),category,type,school,facultyId,LocalDateTime.now(ZoneOffset.UTC).minusDays(7),PageRequest.of(page,size)):docs.catalogue(q.trim(),category,type,school,facultyId,PageRequest.of(page,size,sort));
  return new PageResponse<>(result.getContent().stream().map(this::summary).toList(),result.getTotalElements(),result.getTotalPages(),page,size);
 }
 @Override public List<CategoryInfo> categories(){return docs.categoryCounts().stream().map(row->new CategoryInfo((Long)row[0],(String)row[1],(String)row[2],(Long)row[3])).toList();}
 @Override public CatalogueStats stats(){return new CatalogueStats(docs.countPublished(),docs.countFree(),categories().size(),downloads.count());}
 @Override public DocumentDetail detail(Long id,Long userId){
  var doc=published(id);var list=files.findByDocumentIdAndActiveTrueOrderByIdAsc(id).stream().map(f->new DocumentDetail.FileInfo(f.getId(),f.getOriginalName(),f.getContentType(),f.getSizeBytes())).toList();
  return new DocumentDetail(summary(doc),doc.getDescription(),doc.getAuthor()==null?"Ban biên tập Knowledge Hub":doc.getAuthor().getUsername(),userId!=null&&bookmarks.existsByUserIdAndDocumentId(userId,id),userId!=null&&canRead(doc,userId),list);
 }
 @Override @Transactional public DownloadPayload download(Long documentId,Long fileId,Long userId){
  var doc=published(documentId);if(!canRead(doc,userId))throw new ApiException(HttpStatus.FORBIDDEN,"Tài liệu có phí. Bạn chưa được cấp quyền tải tài liệu này.");
  var file=files.findByIdAndDocumentIdAndActiveTrue(fileId,documentId).orElseThrow(()->new ApiException(HttpStatus.NOT_FOUND,"Không tìm thấy tệp tài liệu"));
  byte[] bytes=storage.read(file.getStorageKey());
  var record=new DocumentDownload();record.setUser(activeUser(userId));record.setDocument(doc);record.setFile(file);record.setDocumentTitle(doc.getTitle());record.setFileName(file.getOriginalName());downloads.saveAndFlush(record);
  return new DownloadPayload(bytes,file.getOriginalName(),file.getContentType());
 }
 @Override public PersonalStats personalStats(Long userId){return new PersonalStats(downloads.distinctDocuments(userId),downloads.countByUserId(userId),bookmarks.countByUserIdAndDocumentStatus(userId,"PUBLISHED"));}
 @Override public PageResponse<DownloadInfo> history(Long userId,int page,int size){
  var result=downloads.findByUserId(userId,PageRequest.of(page,size,Sort.by("id").descending()));
  return new PageResponse<>(result.getContent().stream().map(d->new DownloadInfo(d.getId(),d.getDocument().getId(),d.getDocumentTitle(),d.getDocument().getCategory().getName(),d.getFileName(),d.getCreatedAt(),"PUBLISHED".equals(d.getDocument().getStatus())&&Boolean.TRUE.equals(d.getDocument().getCategory().getActive()))).toList(),result.getTotalElements(),result.getTotalPages(),page,size);
 }
 @Override public PageResponse<DocumentSummary> saved(Long userId,int page,int size){var result=bookmarks.findByUserIdAndDocumentStatus(userId,"PUBLISHED",PageRequest.of(page,size,Sort.by("id").descending()));return new PageResponse<>(result.getContent().stream().map(b->summary(b.getDocument())).toList(),result.getTotalElements(),result.getTotalPages(),page,size);}
 @Override @Transactional public void bookmark(Long userId,Long documentId,boolean saved){var doc=published(documentId);var existing=bookmarks.findByUserIdAndDocumentId(userId,documentId);if(saved&&existing.isEmpty()){var bookmark=new DocumentBookmark();bookmark.setUser(activeUser(userId));bookmark.setDocument(doc);bookmarks.saveAndFlush(bookmark);}else if(!saved)existing.ifPresent(bookmarks::delete);}
 @Override @Transactional public void grant(Long documentId,Long userId){var doc=published(documentId);var user=activeUser(userId);var grant=access.findByUserIdAndDocumentId(userId,documentId).orElseGet(DocumentAccess::new);grant.setUser(user);grant.setDocument(doc);grant.setGrantType("MANUAL");grant.setExpiresAt(null);grant.setRevokedAt(null);access.save(grant);}

 @Override @Transactional public DocumentSummary upload(Long userId,com.knowledge.app.model.dto.request.document.UploadDocumentRequest request,org.springframework.web.multipart.MultipartFile file){
  var user=activeUser(userId);var selection=organizations.resolve(request.institutionId(),request.facultyId());
  var category=categories.findById(request.categoryId()).filter(c->Boolean.TRUE.equals(c.getActive())).orElseThrow(()->new ApiException(HttpStatus.BAD_REQUEST,"Chủ đề không hợp lệ"));
  if(("FREE".equals(request.accessType())&&request.price().signum()!=0)||("PAID".equals(request.accessType())&&request.price().signum()<=0))throw new ApiException(HttpStatus.BAD_REQUEST,"Tài liệu miễn phí có giá 0; tài liệu có phí cần giá lớn hơn 0");
  var stored=storage.store(file);var doc=new Document();doc.setAuthor(user);doc.setCategory(category);doc.setTitle(request.title().trim());doc.setSlug("uploaded-"+UUID.randomUUID());doc.setSummary(request.summary().trim());doc.setDescription(request.description()==null?request.summary():request.description());doc.setAccessType(request.accessType());doc.setPrice(request.price());doc.setCurrency("VND");doc.setLevel(request.level()==null?"BEGINNER":request.level());doc.setLanguage("vi");doc.setStatus("PUBLISHED");doc.setFeatured(false);doc.setPublishedAt(LocalDateTime.now(ZoneOffset.UTC));doc.setCoverStyle("violet");doc.setInstitution(selection.institution());doc.setFaculty(selection.faculty());docs.saveAndFlush(doc);
  var attachment=new DocumentFile();attachment.setDocument(doc);attachment.setOriginalName(stored.name());attachment.setStorageKey(stored.key());attachment.setContentType(stored.contentType());attachment.setSizeBytes(stored.size());attachment.setActive(true);files.saveAndFlush(attachment);return summary(doc);
 }
 private User activeUser(Long id){return users.findById(id).filter(u->"ACTIVE".equals(u.getStatus())).orElseThrow(()->new ApiException(HttpStatus.UNAUTHORIZED,"Tài khoản không hoạt động"));}
 private boolean canRead(Document doc,Long userId){return "FREE".equals(doc.getAccessType())||(doc.getAuthor()!=null&&doc.getAuthor().getId().equals(userId))||access.validGrant(userId,doc.getId(),LocalDateTime.now(ZoneOffset.UTC))>0;}
 private Document published(Long id){return docs.findByIdAndStatus(id,"PUBLISHED").filter(d->Boolean.TRUE.equals(d.getCategory().getActive())).orElseThrow(()->new ApiException(HttpStatus.NOT_FOUND,"Không tìm thấy tài liệu hoặc tài liệu chưa được công bố"));}
 private String format(Document doc){return files.findByDocumentIdAndActiveTrueOrderByIdAsc(doc.getId()).stream().findFirst().map(f->f.getOriginalName().substring(f.getOriginalName().lastIndexOf('.')+1).toUpperCase(Locale.ROOT)).orElse("FILE");}
 private DocumentSummary summary(Document doc){return new DocumentSummary(doc.getId(),doc.getSlug(),doc.getTitle(),doc.getSummary(),doc.getCategory().getId(),doc.getCategory().getName(),doc.getAccessType(),doc.getPrice(),doc.getCurrency(),doc.getLevel(),doc.getLanguage(),doc.getCoverStyle(),Boolean.TRUE.equals(doc.getFeatured()),doc.getPublishedAt(),downloads.countByDocumentId(doc.getId()),doc.getInstitution()==null?null:doc.getInstitution().getId(),doc.getInstitution()==null?null:doc.getInstitution().getName(),doc.getFaculty()==null?null:doc.getFaculty().getId(),doc.getFaculty()==null?null:doc.getFaculty().getName(),format(doc));}
}
