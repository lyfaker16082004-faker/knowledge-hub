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
 private final com.knowledge.app.service.document.DocumentSearch search;private final SubjectRepository subjects;private final TagRepository tags;private final UserRoleRepository roles;
 private final DocumentRepository docs;private final DocumentFileRepository files;private final DocumentAccessRepository access;private final DocumentDownloadRepository downloads;private final DocumentBookmarkRepository bookmarks;private final UserRepository users;private final CategoryRepository categories;private final com.knowledge.app.service.organization.OrganizationService organizations;private final com.knowledge.app.service.user.ProfileService profiles;private final com.knowledge.app.service.document.PrivateDocumentStorage storage;private final com.knowledge.app.service.document.DocumentSamplePreview samplePreview;
 public DocumentServiceImpl(DocumentRepository docs,DocumentFileRepository files,DocumentAccessRepository access,DocumentDownloadRepository downloads,DocumentBookmarkRepository bookmarks,UserRepository users,CategoryRepository categories,com.knowledge.app.service.organization.OrganizationService organizations,com.knowledge.app.service.user.ProfileService profiles,com.knowledge.app.service.document.PrivateDocumentStorage storage,com.knowledge.app.service.document.DocumentSamplePreview samplePreview,com.knowledge.app.service.document.DocumentSearch search,SubjectRepository subjects,TagRepository tags,UserRoleRepository roles){this.search=search;this.subjects=subjects;this.tags=tags;this.roles=roles;this.categories=categories;this.organizations=organizations;this.profiles=profiles;this.storage=storage;this.samplePreview=samplePreview;this.docs=docs;this.files=files;this.access=access;this.downloads=downloads;this.bookmarks=bookmarks;this.users=users;}
 @Override public PageResponse<DocumentSummary> catalogue(String q,Long category,String type,int page,int size,String order,Long institutionId,Long facultyId,boolean global,Long userId,Long subjectId,String tag){
  if(global&&(institutionId!=null||facultyId!=null))throw new ApiException(HttpStatus.BAD_REQUEST,"Không kết hợp bộ lọc toàn cầu với trường/khoa cụ thể");
  if(!global&&institutionId==null&&userId!=null){var profile=profiles.organization(userId);if(profile!=null&&!"OTHER".equals(profile.institutionType()))institutionId=profile.institutionId();}
  var selection=organizations.resolve(institutionId,facultyId);
  Long school=selection.institution()==null||"OTHER".equals(selection.institution().getType())?null:selection.institution().getId();
  var result=search.search(q.trim(),category,type,school,facultyId,subjectId,tag,order,page,size);
  return new PageResponse<>(result.getContent().stream().map(this::summary).toList(),result.getTotalElements(),result.getTotalPages(),page,size);
 }
 @Override public List<CategoryInfo> categories(){return docs.categoryCounts().stream().map(row->new CategoryInfo((Long)row[0],(String)row[1],(String)row[2],(Long)row[3])).toList();}
 @Override public CatalogueStats stats(){return new CatalogueStats(docs.countPublished(),docs.countFree(),categories().size(),downloads.count());}
 @Override public DocumentDetail detail(Long id,Long userId){
  var doc=visible(id,userId);boolean manager=userId!=null&&(owns(doc,userId)||isAdmin(userId));var list=files.findByDocumentIdAndActiveTrueOrderByIdAsc(id).stream().map(f->new DocumentDetail.FileInfo(f.getId(),f.getOriginalName(),f.getContentType(),f.getSizeBytes())).toList();
  return new DocumentDetail(summary(doc),doc.getDescription(),doc.getAuthor()==null?"Ban biên tập Knowledge Hub":doc.getAuthor().getUsername(),userId!=null&&bookmarks.existsByUserIdAndDocumentId(userId,id),userId!=null&&"PUBLISHED".equals(doc.getStatus())&&canRead(doc,userId),list,doc.getStatus(),manager?doc.getReviewNote():null,manager?doc.getVersion():null,manager,manager);
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
  var user=activeUser(userId);var selection=uploadOrganization(userId,request);
  var category=categories.findById(request.categoryId()).filter(c->Boolean.TRUE.equals(c.getActive())).orElseThrow(()->new ApiException(HttpStatus.BAD_REQUEST,"Chủ đề không hợp lệ"));
  if(("FREE".equals(request.accessType())&&request.price().signum()!=0)||("PAID".equals(request.accessType())&&request.price().signum()<=0))throw new ApiException(HttpStatus.BAD_REQUEST,"Tài liệu miễn phí có giá 0; tài liệu có phí cần giá lớn hơn 0");
  var stored=storage.store(file);var doc=new Document();doc.setAuthor(user);doc.setCategory(category);doc.setTitle(request.title().trim());doc.setSlug("uploaded-"+UUID.randomUUID());doc.setSummary(request.summary().trim());doc.setDescription(request.description()==null?request.summary():request.description());doc.setAccessType(request.accessType());doc.setPrice(request.price());doc.setCurrency("VND");doc.setLevel(request.level()==null?"BEGINNER":request.level());doc.setLanguage("vi");doc.setStatus(isAdmin(userId)?"PUBLISHED":"PENDING");doc.setFeatured(false);doc.setPublishedAt(isAdmin(userId)?LocalDateTime.now(ZoneOffset.UTC):null);doc.setCoverStyle("violet");doc.setInstitution(selection.institution());doc.setFaculty(selection.faculty());applySubjectTags(doc,request);docs.saveAndFlush(doc);
  var attachment=new DocumentFile();attachment.setDocument(doc);attachment.setOriginalName(stored.name());attachment.setStorageKey(stored.key());attachment.setContentType(stored.contentType());attachment.setSizeBytes(stored.size());attachment.setActive(true);files.saveAndFlush(attachment);return summary(doc);
 }
 private boolean isAdmin(Long id){return id!=null&&roles.findWithRoles(id).stream().anyMatch(r->"ADMIN".equals(r.getRole().getName()));}
 private boolean owns(Document doc,Long id){return doc.getAuthor()!=null&&doc.getAuthor().getId().equals(id);}
 private void admin(Long id){activeUser(id);if(!isAdmin(id))throw new ApiException(HttpStatus.FORBIDDEN,"Chức năng dành cho quản trị viên");}
 private Document visible(Long id,Long viewer){var doc=required(id);if(("PUBLISHED".equals(doc.getStatus())&&Boolean.TRUE.equals(doc.getCategory().getActive()))||(viewer!=null&&(owns(doc,viewer)||isAdmin(viewer))))return doc;throw new ApiException(HttpStatus.NOT_FOUND,"Tài liệu chưa được công bố");}
 private Document required(Long id){return docs.findById(id).filter(d->!"DELETED".equals(d.getStatus())).orElseThrow(()->new ApiException(HttpStatus.NOT_FOUND,"Không tìm thấy tài liệu"));}
 private Document editable(Long viewer,boolean adminMode,Long id){if(adminMode)admin(viewer);var doc=required(id);if(!adminMode&&!owns(doc,viewer))throw new ApiException(HttpStatus.FORBIDDEN,"Bạn chỉ được quản lý tài liệu của mình");return doc;}
 private void version(Document doc,Long expected){if(expected==null||!expected.equals(doc.getVersion()))throw new ApiException(HttpStatus.CONFLICT,"Tài liệu đã thay đổi. Hãy tải lại trước khi thao tác");}
 private com.knowledge.app.service.organization.OrganizationService.Selection uploadOrganization(Long viewer,com.knowledge.app.model.dto.request.document.UploadDocumentRequest r){
  if(isAdmin(viewer))return organizations.resolve(r.institutionId(),r.facultyId());
  var profile=profiles.organization(viewer);Long school=profile==null?null:profile.institutionId(),faculty=profile==null?null:profile.facultyId();
  if((r.institutionId()!=null&&!r.institutionId().equals(school))||(r.facultyId()!=null&&!r.facultyId().equals(faculty)))throw new ApiException(HttpStatus.BAD_REQUEST,"Trường/khoa được lấy từ hồ sơ. Hãy cập nhật hồ sơ nếu cần thay đổi");
  return organizations.resolve(school,faculty);
 }
 private void applySubjectTags(Document doc,com.knowledge.app.model.dto.request.document.UploadDocumentRequest r){
  doc.setSubject(r.subjectId()==null?null:subjects.findById(r.subjectId()).orElseThrow(()->new ApiException(HttpStatus.BAD_REQUEST,"Môn học không hợp lệ")));
  var selected=new LinkedHashSet<Tag>();if(r.tags()!=null)for(String raw:r.tags()){String name=raw.trim().replaceFirst("^#+","").trim();if(name.isBlank()||name.length()>80)throw new ApiException(HttpStatus.BAD_REQUEST,"Nhãn cần từ 1 đến 80 ký tự");var tag=tags.findByNameIgnoreCase(name).orElseGet(()->{var t=new Tag();t.setName(name);t.setSlug("document-"+UUID.randomUUID());return tags.saveAndFlush(t);});selected.add(tag);}doc.getTags().clear();doc.getTags().addAll(selected);
 }
 private ManagedDocument managed(Document d){return new ManagedDocument(summary(d),d.getDescription(),d.getAuthor()==null?null:d.getAuthor().getId(),d.getAuthor()==null?"Ban biên tập":d.getAuthor().getUsername(),d.getStatus(),d.getReviewNote(),d.getCreatedAt(),d.getUpdatedAt(),d.getVersion());}
 @Override public PageResponse<ManagedDocument> managed(Long viewer,boolean adminMode,String q,String status,int page,int size){
  if(adminMode)admin(viewer);else activeUser(viewer);
  org.springframework.data.jpa.domain.Specification<Document> spec=(root,query,cb)->{var conditions=new ArrayList<jakarta.persistence.criteria.Predicate>();conditions.add(cb.notEqual(root.get("status"),"DELETED"));if(!adminMode)conditions.add(cb.equal(root.get("author").get("id"),viewer));if(!status.isBlank())conditions.add(cb.equal(root.get("status"),status));if(!q.isBlank()){String pattern="%"+q.trim().toLowerCase(Locale.ROOT)+"%";conditions.add(cb.or(cb.like(cb.lower(root.get("title")),pattern),cb.like(cb.lower(root.get("summary")),pattern)));}return cb.and(conditions.toArray(jakarta.persistence.criteria.Predicate[]::new));};
  var result=docs.findAll(spec,PageRequest.of(page,size,Sort.by("updatedAt").descending().and(Sort.by("id").descending())));return new PageResponse<>(result.getContent().stream().map(this::managed).toList(),result.getTotalElements(),result.getTotalPages(),page,size);
 }
 @Override @Transactional public ManagedDocument update(Long viewer,boolean adminMode,Long id,com.knowledge.app.model.dto.request.document.EditDocumentRequest edit){var d=editable(viewer,adminMode,id);version(d,edit.version());var r=edit.metadata();var selection=adminMode?organizations.resolve(r.institutionId(),r.facultyId()):organizations.resolve(d.getInstitution()==null?null:d.getInstitution().getId(),d.getFaculty()==null?null:d.getFaculty().getId());
  var category=categories.findById(r.categoryId()).filter(c->Boolean.TRUE.equals(c.getActive())).orElseThrow(()->new ApiException(HttpStatus.BAD_REQUEST,"Chủ đề không hợp lệ"));
  if(("FREE".equals(r.accessType())&&r.price().signum()!=0)||("PAID".equals(r.accessType())&&r.price().signum()<=0))throw new ApiException(HttpStatus.BAD_REQUEST,"Giá tài liệu không hợp lệ");
  d.setTitle(r.title().trim());d.setSummary(r.summary().trim());d.setDescription(r.description()==null?r.summary():r.description());d.setCategory(category);d.setLevel(r.level()==null?"BEGINNER":r.level());d.setAccessType(r.accessType());d.setPrice(r.price());d.setInstitution(selection.institution());d.setFaculty(selection.faculty());applySubjectTags(d,r);
  if(!adminMode){d.setStatus("PENDING");d.setPublishedAt(null);d.setReviewNote(null);d.setReviewedAt(null);d.setReviewedBy(null);}docs.flush();return managed(d);
 }
 @Override @Transactional public void delete(Long viewer,boolean adminMode,Long id,Long expected){var d=editable(viewer,adminMode,id);version(d,expected);d.setStatus("DELETED");docs.flush();}
 @Override @Transactional public ManagedDocument review(Long viewer,Long id,String action,String note,Long expected){admin(viewer);var d=required(id);version(d,expected);String state=d.getStatus();String next=switch(action){case "APPROVE"->{if(!"PENDING".equals(state))throw transition();yield "PUBLISHED";}case "REJECT"->{if(!"PENDING".equals(state))throw transition();if(note==null||note.isBlank())throw new ApiException(HttpStatus.BAD_REQUEST,"Nhập lý do từ chối");yield "REJECTED";}case "HIDE"->{if(!"PUBLISHED".equals(state))throw transition();yield "ARCHIVED";}case "RESTORE"->{if(!"ARCHIVED".equals(state))throw transition();yield "PUBLISHED";}default->throw transition();};d.setStatus(next);d.setReviewNote(note==null?null:note.trim());d.setReviewedAt(LocalDateTime.now(ZoneOffset.UTC));d.setReviewedBy(activeUser(viewer));if("PUBLISHED".equals(next))d.setPublishedAt(LocalDateTime.now(ZoneOffset.UTC));docs.flush();return managed(d);}
 private ApiException transition(){return new ApiException(HttpStatus.CONFLICT,"Trạng thái tài liệu không cho phép thao tác này. Hãy tải lại danh sách");}
 @Override public DownloadPayload sample(Long viewer,Long id,Long fileId){var doc=published(id);var file=files.findByIdAndDocumentIdAndActiveTrue(fileId,id).orElseThrow(()->new ApiException(HttpStatus.NOT_FOUND,"Không tìm thấy tệp tài liệu"));var sample=samplePreview.create(storage.read(file.getStorageKey()),file.getOriginalName());return new DownloadPayload(sample.bytes(),sample.fileName(),sample.contentType());}
 @Override public DownloadPayload preview(Long viewer,Long id,Long fileId){var d=required(id);if(!owns(d,viewer)&&!isAdmin(viewer))throw new ApiException(HttpStatus.FORBIDDEN,"Chỉ tác giả hoặc quản trị viên được xem bản kiểm duyệt");var file=files.findByIdAndDocumentIdAndActiveTrue(fileId,d.getId()).orElseThrow(()->new ApiException(HttpStatus.NOT_FOUND,"Không tìm thấy tệp"));return new DownloadPayload(storage.read(file.getStorageKey()),file.getOriginalName(),file.getContentType());}
 private User activeUser(Long id){return users.findById(id).filter(u->"ACTIVE".equals(u.getStatus())).orElseThrow(()->new ApiException(HttpStatus.UNAUTHORIZED,"Tài khoản không hoạt động"));}
 private boolean canRead(Document doc,Long userId){return "FREE".equals(doc.getAccessType())||(doc.getAuthor()!=null&&doc.getAuthor().getId().equals(userId))||access.validGrant(userId,doc.getId(),LocalDateTime.now(ZoneOffset.UTC))>0;}
 private Document published(Long id){return docs.findByIdAndStatus(id,"PUBLISHED").filter(d->Boolean.TRUE.equals(d.getCategory().getActive())).orElseThrow(()->new ApiException(HttpStatus.NOT_FOUND,"Không tìm thấy tài liệu hoặc tài liệu chưa được công bố"));}
 private String format(Document doc){return files.findByDocumentIdAndActiveTrueOrderByIdAsc(doc.getId()).stream().findFirst().map(f->f.getOriginalName().substring(f.getOriginalName().lastIndexOf('.')+1).toUpperCase(Locale.ROOT)).orElse("FILE");}
 private DocumentSummary summary(Document doc){return new DocumentSummary(doc.getId(),doc.getSlug(),doc.getTitle(),doc.getSummary(),doc.getCategory().getId(),doc.getCategory().getName(),doc.getAccessType(),doc.getPrice(),doc.getCurrency(),doc.getLevel(),doc.getLanguage(),doc.getCoverStyle(),Boolean.TRUE.equals(doc.getFeatured()),doc.getPublishedAt(),downloads.countByDocumentId(doc.getId()),doc.getInstitution()==null?null:doc.getInstitution().getId(),doc.getInstitution()==null?null:doc.getInstitution().getName(),doc.getFaculty()==null?null:doc.getFaculty().getId(),doc.getFaculty()==null?null:doc.getFaculty().getName(),format(doc),doc.getSubject()==null?null:doc.getSubject().getId(),doc.getSubject()==null?null:doc.getSubject().getName(),doc.getTags().stream().map(Tag::getName).sorted().toList());}
}

