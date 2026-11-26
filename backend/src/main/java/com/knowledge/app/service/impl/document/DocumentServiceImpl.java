package com.knowledge.app.service.impl.document;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.knowledge.app.common.exception.ApiException;
import com.knowledge.app.common.response.PageResponse;
import com.knowledge.app.common.util.TimeUtils;
import com.knowledge.app.model.dto.request.document.EditDocumentRequest;
import com.knowledge.app.model.dto.request.document.UploadDocumentRequest;
import com.knowledge.app.model.dto.response.document.DocumentDetail;
import com.knowledge.app.model.dto.response.document.DocumentSummary;
import com.knowledge.app.model.dto.response.document.LibraryResponses.CatalogueStats;
import com.knowledge.app.model.dto.response.document.LibraryResponses.CategoryInfo;
import com.knowledge.app.model.dto.response.document.LibraryResponses.DownloadInfo;
import com.knowledge.app.model.dto.response.document.LibraryResponses.PersonalStats;
import com.knowledge.app.model.dto.response.document.ManagedDocument;
import com.knowledge.app.model.dto.response.user.OrganizationInfo;
import com.knowledge.app.model.entity.Category;
import com.knowledge.app.model.entity.Document;
import com.knowledge.app.model.entity.DocumentAccess;
import com.knowledge.app.model.entity.DocumentBookmark;
import com.knowledge.app.model.entity.DocumentDownload;
import com.knowledge.app.model.entity.DocumentFile;
import com.knowledge.app.model.entity.Tag;
import com.knowledge.app.model.entity.User;
import com.knowledge.app.model.entity.UserRole;
import com.knowledge.app.model.enums.AccessType;
import com.knowledge.app.model.enums.DocumentStatus;
import com.knowledge.app.model.enums.InstitutionType;
import com.knowledge.app.model.enums.ReviewAction;
import com.knowledge.app.model.enums.RoleName;
import com.knowledge.app.model.enums.UserStatus;
import com.knowledge.app.model.mapper.DocumentMapper;
import com.knowledge.app.repository.CategoryRepository;
import com.knowledge.app.repository.DocumentAccessRepository;
import com.knowledge.app.repository.DocumentBookmarkRepository;
import com.knowledge.app.repository.DocumentDownloadRepository;
import com.knowledge.app.repository.DocumentFileRepository;
import com.knowledge.app.repository.DocumentRepository;
import com.knowledge.app.repository.SubjectRepository;
import com.knowledge.app.repository.TagRepository;
import com.knowledge.app.repository.UserRepository;
import com.knowledge.app.repository.UserRoleRepository;
import com.knowledge.app.repository.custom.DocumentSearchRepository;
import com.knowledge.app.service.document.DocumentSamplePreview;
import com.knowledge.app.service.document.DocumentService;
import com.knowledge.app.service.document.PrivateDocumentStorage;
import com.knowledge.app.service.organization.OrganizationService;
import com.knowledge.app.service.user.ProfileService;

import jakarta.persistence.criteria.Predicate;

/**
 * Service xử lý toàn bộ nghiệp vụ liên quan đến TÀI LIỆU:
 * - Xem danh sách, xem chi tiết, tải tài liệu
 * - Lưu (bookmark) tài liệu, xem lịch sử tải
 * - Đăng tải, chỉnh sửa, xoá tài liệu
 * - Quản trị viên duyệt / từ chối / ẩn / khôi phục tài liệu
 *
 * Mặc định mọi hàm chỉ ĐỌC dữ liệu (readOnly = true).
 * Hàm nào cần GHI dữ liệu sẽ có thêm @Transactional riêng.
 */
@Service
@Transactional(readOnly = true)
public class DocumentServiceImpl implements DocumentService {

    /** Trình độ mặc định khi người dùng không chọn. */
    private static final String DEFAULT_LEVEL = "BEGINNER";

    // ===================== CÁC DEPENDENCY =====================

    private final DocumentRepository documentRepository;
    private final DocumentFileRepository documentFileRepository;
    private final DocumentAccessRepository documentAccessRepository;
    private final DocumentDownloadRepository documentDownloadRepository;
    private final DocumentBookmarkRepository documentBookmarkRepository;
    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;
    private final CategoryRepository categoryRepository;
    private final SubjectRepository subjectRepository;
    private final TagRepository tagRepository;

    private final OrganizationService organizationService;
    private final ProfileService profileService;
    private final PrivateDocumentStorage documentStorage;   // Lưu / đọc file trên ổ đĩa
    private final DocumentSamplePreview samplePreview;      // Tạo bản xem thử
    private final DocumentMapper documentMapper;            // Chuyển Document -> DTO
    private final DocumentSearchRepository documentSearchRepository; // Tìm kiếm tài liệu bằng SQL

    // Spring sẽ tự động truyền (inject) các đối tượng vào constructor này
    public DocumentServiceImpl(DocumentRepository documentRepository,
            DocumentFileRepository documentFileRepository,
            DocumentAccessRepository documentAccessRepository,
            DocumentDownloadRepository documentDownloadRepository,
            DocumentBookmarkRepository documentBookmarkRepository,
            UserRepository userRepository,
            UserRoleRepository userRoleRepository,
            CategoryRepository categoryRepository,
            SubjectRepository subjectRepository,
            TagRepository tagRepository,
            OrganizationService organizationService,
            ProfileService profileService,
            PrivateDocumentStorage documentStorage,
            DocumentSamplePreview samplePreview,
            DocumentSearchRepository documentSearchRepository,
            DocumentMapper documentMapper) {
        this.documentRepository = documentRepository;
        this.documentFileRepository = documentFileRepository;
        this.documentAccessRepository = documentAccessRepository;
        this.documentDownloadRepository = documentDownloadRepository;
        this.documentBookmarkRepository = documentBookmarkRepository;
        this.userRepository = userRepository;
        this.userRoleRepository = userRoleRepository;
        this.categoryRepository = categoryRepository;
        this.subjectRepository = subjectRepository;
        this.tagRepository = tagRepository;
        this.organizationService = organizationService;
        this.profileService = profileService;
        this.documentStorage = documentStorage;
        this.samplePreview = samplePreview;
        this.documentSearchRepository = documentSearchRepository;
        this.documentMapper = documentMapper;
    }

    // =========================================================
    // PHẦN 1: XEM TÀI LIỆU (dành cho mọi người dùng)
    // =========================================================

    /**
     * Lấy danh sách tài liệu theo bộ lọc (từ khoá, chủ đề, trường, khoa, môn học, nhãn...).
     */
    @Override
    public PageResponse<DocumentSummary> catalogue(String keyword, Long categoryId, String accessType,
            int page, int size, String order, Long institutionId, Long facultyId, boolean global,
            Long userId, Long subjectId, String tag) {

        // Bước 1: Không cho phép vừa chọn "toàn cầu" vừa chọn trường/khoa cụ thể
        if (global && (institutionId != null || facultyId != null)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Không kết hợp bộ lọc toàn cầu với trường/khoa cụ thể");
        }

        // Bước 2: Nếu người dùng không chọn trường -> tự động lấy trường trong hồ sơ của họ
        if (!global && institutionId == null && userId != null) {
            OrganizationInfo profile = profileService.organization(userId);
            if (profile != null && !InstitutionType.OTHER.is(profile.institutionType())) {
                institutionId = profile.institutionId();
            }
        }

        // Bước 3: Kiểm tra trường/khoa có hợp lệ không
        OrganizationService.Selection selection = organizationService.resolve(institutionId, facultyId);

        // Bước 4: Trường loại "OTHER" thì coi như không lọc theo trường
        Long schoolId = null;
        if (selection.institution() != null && !InstitutionType.OTHER.is(selection.institution().getType())) {
            schoolId = selection.institution().getId();
        }

        // Bước 5: Tìm kiếm và chuyển kết quả sang DTO
        Page<Document> result = documentSearchRepository.search(keyword.trim(), categoryId, accessType, schoolId,
                facultyId, subjectId, tag, order, page, size);

        List<DocumentSummary> items = new ArrayList<>();
        for (Document document : result.getContent()) {
            items.add(documentMapper.toSummary(document));
        }
        return new PageResponse<>(items, result.getTotalElements(), result.getTotalPages(), page, size);
    }

    /**
     * Lấy danh sách chủ đề kèm số lượng tài liệu của mỗi chủ đề.
     */
    @Override
    public List<CategoryInfo> categories() {
        List<CategoryInfo> list = new ArrayList<>();
        // Mỗi dòng trả về từ query gồm: [id, tên, slug, số lượng]
        for (Object[] row : documentRepository.categoryCounts()) {
            Long id = (Long) row[0];
            String name = (String) row[1];
            String slug = (String) row[2];
            Long count = (Long) row[3];
            list.add(new CategoryInfo(id, name, slug, count));
        }
        return list;
    }

    /**
     * Thống kê chung của thư viện.
     */
    @Override
    public CatalogueStats stats() {
        long totalDocuments = documentRepository.countPublished();
        long freeDocuments = documentRepository.countFree();
        long totalCategories = categories().size();
        long totalDownloads = documentDownloadRepository.count();
        return new CatalogueStats(totalDocuments, freeDocuments, totalCategories, totalDownloads);
    }

    /**
     * Xem chi tiết một tài liệu.
     */
    @Override
    public DocumentDetail detail(Long documentId, Long userId) {
        Document document = findVisibleDocument(documentId, userId);

        // Người dùng có quyền quản lý nếu là tác giả hoặc là admin
        boolean canManage = userId != null && (isOwner(document, userId) || isAdmin(userId));

        // Danh sách file đính kèm
        List<DocumentDetail.FileInfo> fileInfos = new ArrayList<>();
        for (DocumentFile file : documentFileRepository.findByDocumentIdAndActiveTrueOrderByIdAsc(documentId)) {
            fileInfos.add(new DocumentDetail.FileInfo(
                    file.getId(), file.getOriginalName(), file.getContentType(), file.getSizeBytes()));
        }

        // Tên tác giả (nếu không có thì hiển thị "Ban biên tập")
        String authorName = "Ban biên tập Knowledge Hub";
        if (document.getAuthor() != null) {
            authorName = document.getAuthor().getUsername();
        }

        // Người dùng đã lưu tài liệu này chưa?
        boolean saved = userId != null && documentBookmarkRepository.existsByUserIdAndDocumentId(userId, documentId);

        // Người dùng có được tải không? (phải đăng nhập + tài liệu đã công bố + có quyền đọc)
        boolean canDownload = userId != null
                && DocumentStatus.PUBLISHED.is(document.getStatus())
                && canRead(document, userId);

        // Ghi chú duyệt và version chỉ hiện cho người quản lý
        String reviewNote = null;
        Long version = null;
        if (canManage) {
            reviewNote = document.getReviewNote();
            version = document.getVersion();
        }

        return new DocumentDetail(documentMapper.toSummary(document), document.getDescription(), authorName, saved,
                canDownload, fileInfos, document.getStatus(), reviewNote, version, canManage, canManage);
    }

    /**
     * Gợi ý tài liệu liên quan.
     *
     * Bước 1: Lấy tối đa 30 ứng viên cùng chủ đề hoặc cùng môn học.
     * Bước 2: Chấm điểm từng ứng viên:
     *   - Cùng môn học: +3 điểm
     *   - Mỗi nhãn trùng: +2 điểm
     *   - Cùng chủ đề:  +1 điểm
     *   - Cùng khoa:    +1 điểm
     * Bước 3: Sắp xếp theo điểm giảm dần (bằng điểm thì giữ thứ tự mới nhất) và lấy "limit" tài liệu.
     * Bước 4: Nếu vẫn chưa đủ thì bù bằng tài liệu mới nhất ở chủ đề khác.
     */
    @Override
    public List<DocumentSummary> related(Long documentId, Long viewerId, int limit) {
        Document current = findVisibleDocument(documentId, viewerId);
        Long subjectId = current.getSubject() != null ? current.getSubject().getId() : null;

        // Bước 1: Lấy ứng viên
        List<Document> candidates = documentRepository.relatedCandidates(documentId,
                current.getCategory().getId(), subjectId, PageRequest.of(0, 30));

        // Bước 2: Chấm điểm
        Set<String> currentTags = new HashSet<>();
        for (Tag tag : current.getTags()) {
            currentTags.add(tag.getName().toLowerCase(Locale.ROOT));
        }

        List<ScoredDocument> scored = new ArrayList<>();
        for (Document candidate : candidates) {
            scored.add(new ScoredDocument(candidate, relatedScore(current, candidate, currentTags)));
        }

        // Bước 3: Sắp xếp và cắt danh sách (List.sort là sắp xếp ổn định nên giữ thứ tự mới nhất khi bằng điểm)
        scored.sort((a, b) -> Integer.compare(b.score(), a.score()));

        List<DocumentSummary> result = new ArrayList<>();
        Set<Long> addedIds = new HashSet<>();
        for (ScoredDocument item : scored) {
            if (result.size() >= limit) {
                break;
            }
            result.add(documentMapper.toSummary(item.document()));
            addedIds.add(item.document().getId());
        }

        // Bước 4: Bù thêm tài liệu mới nhất cho đủ số lượng
        if (result.size() < limit) {
            List<Document> latest = documentRepository.latestPublishedExcept(documentId, PageRequest.of(0, limit * 2));
            for (Document document : latest) {
                if (result.size() >= limit) {
                    break;
                }
                if (addedIds.add(document.getId())) { // add() trả về false nếu id đã có trong danh sách
                    result.add(documentMapper.toSummary(document));
                }
            }
        }
        return result;
    }

    /** Một ứng viên kèm điểm liên quan (chỉ dùng trong hàm related). */
    private record ScoredDocument(Document document, int score) {
    }

    private int relatedScore(Document current, Document candidate, Set<String> currentTags) {
        int score = 0;

        if (current.getSubject() != null && candidate.getSubject() != null
                && current.getSubject().getId().equals(candidate.getSubject().getId())) {
            score += 3;
        }

        for (Tag tag : candidate.getTags()) {
            if (currentTags.contains(tag.getName().toLowerCase(Locale.ROOT))) {
                score += 2;
            }
        }

        if (current.getCategory().getId().equals(candidate.getCategory().getId())) {
            score += 1;
        }

        if (current.getFaculty() != null && candidate.getFaculty() != null
                && current.getFaculty().getId().equals(candidate.getFaculty().getId())) {
            score += 1;
        }

        return score;
    }

    /**
     * Tải file của tài liệu và ghi lại lịch sử tải.
     */
    @Override
    @Transactional
    public DownloadPayload download(Long documentId, Long fileId, Long userId) {
        Document document = findPublishedDocument(documentId);

        // Kiểm tra quyền tải
        if (!canRead(document, userId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Tài liệu có phí. Bạn chưa được cấp quyền tải tài liệu này.");
        }

        DocumentFile file = findActiveFile(fileId, documentId, "Không tìm thấy tệp tài liệu");
        byte[] bytes = documentStorage.read(file.getStorageKey());

        // Ghi lại lịch sử tải
        DocumentDownload downloadRecord = new DocumentDownload();
        downloadRecord.setUser(findActiveUser(userId));
        downloadRecord.setDocument(document);
        downloadRecord.setFile(file);
        downloadRecord.setDocumentTitle(document.getTitle());
        downloadRecord.setFileName(file.getOriginalName());
        documentDownloadRepository.saveAndFlush(downloadRecord);

        return new DownloadPayload(bytes, file.getOriginalName(), file.getContentType());
    }

    /**
     * Tải bản xem thử (chỉ một phần nội dung) của tài liệu đã công bố.
     */
    @Override
    public DownloadPayload sample(Long viewerId, Long documentId, Long fileId) {
        findPublishedDocument(documentId);
        DocumentFile file = findActiveFile(fileId, documentId, "Không tìm thấy tệp tài liệu");

        byte[] originalBytes = documentStorage.read(file.getStorageKey());
        DocumentSamplePreview.Sample sample = samplePreview.create(originalBytes, file.getOriginalName());

        return new DownloadPayload(sample.bytes(), sample.fileName(), sample.contentType());
    }

    /**
     * Xem bản đầy đủ để kiểm duyệt (chỉ tác giả hoặc admin).
     */
    @Override
    public DownloadPayload preview(Long viewerId, Long documentId, Long fileId) {
        Document document = findDocumentNotDeleted(documentId);

        if (!isOwner(document, viewerId) && !isAdmin(viewerId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Chỉ tác giả hoặc quản trị viên được xem bản kiểm duyệt");
        }

        DocumentFile file = findActiveFile(fileId, document.getId(), "Không tìm thấy tệp");
        byte[] bytes = documentStorage.read(file.getStorageKey());
        return new DownloadPayload(bytes, file.getOriginalName(), file.getContentType());
    }

    // =========================================================
    // PHẦN 2: THƯ VIỆN CÁ NHÂN (lịch sử tải, tài liệu đã lưu)
    // =========================================================

    /**
     * Thống kê cá nhân: số tài liệu đã tải, số lượt tải, số tài liệu đã lưu.
     */
    @Override
    public PersonalStats personalStats(Long userId) {
        long downloadedDocuments = documentDownloadRepository.distinctDocuments(userId);
        long totalDownloads = documentDownloadRepository.countByUserId(userId);
        long savedDocuments = documentBookmarkRepository.countByUserIdAndDocumentStatus(userId, DocumentStatus.PUBLISHED.name());
        return new PersonalStats(downloadedDocuments, totalDownloads, savedDocuments);
    }

    /**
     * Lịch sử tải tài liệu của người dùng (mới nhất lên đầu).
     */
    @Override
    public PageResponse<DownloadInfo> history(Long userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());
        Page<DocumentDownload> result = documentDownloadRepository.findByUserId(userId, pageable);

        List<DownloadInfo> items = new ArrayList<>();
        for (DocumentDownload download : result.getContent()) {
            Document document = download.getDocument();

            // Tài liệu còn khả dụng nếu vẫn đang công bố và chủ đề còn hoạt động
            boolean available = DocumentStatus.PUBLISHED.is(document.getStatus())
                    && Boolean.TRUE.equals(document.getCategory().getActive());

            items.add(new DownloadInfo(
                    download.getId(),
                    document.getId(),
                    download.getDocumentTitle(),
                    document.getCategory().getName(),
                    download.getFileName(),
                    download.getCreatedAt(),
                    available));
        }
        return new PageResponse<>(items, result.getTotalElements(), result.getTotalPages(), page, size);
    }

    /**
     * Danh sách tài liệu người dùng đã lưu.
     */
    @Override
    public PageResponse<DocumentSummary> saved(Long userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());
        Page<DocumentBookmark> result = documentBookmarkRepository
                .findByUserIdAndDocumentStatus(userId, DocumentStatus.PUBLISHED.name(), pageable);

        List<DocumentSummary> items = new ArrayList<>();
        for (DocumentBookmark bookmark : result.getContent()) {
            items.add(documentMapper.toSummary(bookmark.getDocument()));
        }
        return new PageResponse<>(items, result.getTotalElements(), result.getTotalPages(), page, size);
    }

    /**
     * Lưu hoặc bỏ lưu một tài liệu.
     *
     * @param saved true = lưu, false = bỏ lưu
     */
    @Override
    @Transactional
    public void bookmark(Long userId, Long documentId, boolean saved) {
        Document document = findPublishedDocument(documentId);
        Optional<DocumentBookmark> existing = documentBookmarkRepository.findByUserIdAndDocumentId(userId, documentId);

        if (saved) {
            // Chỉ tạo mới nếu chưa lưu trước đó
            if (existing.isEmpty()) {
                DocumentBookmark bookmark = new DocumentBookmark();
                bookmark.setUser(findActiveUser(userId));
                bookmark.setDocument(document);
                documentBookmarkRepository.saveAndFlush(bookmark);
            }
        } else {
            // Bỏ lưu: xoá nếu có
            if (existing.isPresent()) {
                documentBookmarkRepository.delete(existing.get());
            }
        }
    }

    /**
     * Cấp quyền đọc tài liệu (có phí) cho một người dùng.
     */
    @Override
    @Transactional
    public void grant(Long documentId, Long userId) {
        Document document = findPublishedDocument(documentId);
        User user = findActiveUser(userId);

        // Nếu đã có bản ghi quyền thì cập nhật lại, chưa có thì tạo mới
        DocumentAccess accessGrant = documentAccessRepository
                .findByUserIdAndDocumentId(userId, documentId)
                .orElse(new DocumentAccess());

        accessGrant.setUser(user);
        accessGrant.setDocument(document);
        accessGrant.setGrantType("MANUAL");
        accessGrant.setExpiresAt(null); // Không hết hạn
        accessGrant.setRevokedAt(null); // Không bị thu hồi
        documentAccessRepository.save(accessGrant);
    }

    // =========================================================
    // PHẦN 3: ĐĂNG TẢI VÀ QUẢN LÝ TÀI LIỆU
    // =========================================================

    /**
     * Đăng tải tài liệu mới.
     * - Admin đăng: công bố ngay.
     * - Người dùng thường đăng: chờ duyệt.
     */
    @Override
    @Transactional
    public DocumentSummary upload(Long userId, UploadDocumentRequest request, MultipartFile file) {
        // Bước 1: Kiểm tra dữ liệu đầu vào
        User author = findActiveUser(userId);
        OrganizationService.Selection organization = resolveUploadOrganization(userId, request);
        Category category = findActiveCategory(request.categoryId());
        if (!isValidPrice(request)) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "Tài liệu miễn phí có giá 0; tài liệu có phí cần giá lớn hơn 0");
        }

        // Bước 2: Lưu file lên ổ đĩa
        PrivateDocumentStorage.StoredFile storedFile = documentStorage.store(file);

        // Bước 3: Tạo đối tượng Document
        boolean uploadedByAdmin = isAdmin(userId);

        Document document = new Document();
        document.setAuthor(author);
        document.setCategory(category);
        document.setTitle(request.title().trim());
        document.setSlug("uploaded-" + UUID.randomUUID());
        document.setSummary(request.summary().trim());
        document.setDescription(request.description() != null ? request.description() : request.summary());
        document.setAccessType(request.accessType());
        document.setPrice(request.price());
        document.setCurrency("VND");
        document.setLevel(request.level() != null ? request.level() : DEFAULT_LEVEL);
        document.setLanguage("vi");
        document.setFeatured(false);
        document.setCoverStyle("violet");
        document.setInstitution(organization.institution());
        document.setFaculty(organization.faculty());

        if (uploadedByAdmin) {
            document.setStatus(DocumentStatus.PUBLISHED.name());
            document.setPublishedAt(TimeUtils.now());
        } else {
            document.setStatus(DocumentStatus.PENDING.name());
            document.setPublishedAt(null);
        }

        applySubjectAndTags(document, request);
        documentRepository.saveAndFlush(document);

        // Bước 4: Lưu thông tin file đính kèm
        DocumentFile attachment = new DocumentFile();
        attachment.setDocument(document);
        attachment.setOriginalName(storedFile.name());
        attachment.setStorageKey(storedFile.key());
        attachment.setContentType(storedFile.contentType());
        attachment.setSizeBytes(storedFile.size());
        attachment.setActive(true);
        documentFileRepository.saveAndFlush(attachment);

        return documentMapper.toSummary(document);
    }

    /**
     * Danh sách tài liệu để quản lý.
     * - adminMode = true: admin xem tất cả tài liệu
     * - adminMode = false: người dùng chỉ xem tài liệu của mình
     */
    @Override
    public PageResponse<ManagedDocument> managed(Long viewerId, boolean adminMode, String keyword,
            String status, int page, int size) {
        if (adminMode) {
            requireAdmin(viewerId);
        } else {
            findActiveUser(viewerId);
        }

        // Tạo điều kiện truy vấn động (giống mệnh đề WHERE trong SQL)
        Specification<Document> condition = (root, query, cb) -> {
            List<Predicate> conditions = new ArrayList<>();

            // WHERE status != 'DELETED'
            conditions.add(cb.notEqual(root.get("status"), DocumentStatus.DELETED.name()));

            // AND author_id = viewerId (nếu không phải admin)
            if (!adminMode) {
                conditions.add(cb.equal(root.get("author").get("id"), viewerId));
            }

            // AND status = ? (nếu có lọc theo trạng thái)
            if (!status.isBlank()) {
                conditions.add(cb.equal(root.get("status"), status));
            }

            // AND (title LIKE ? OR summary LIKE ?) (nếu có từ khoá)
            if (!keyword.isBlank()) {
                String pattern = "%" + keyword.trim().toLowerCase(Locale.ROOT) + "%";
                Predicate titleMatches = cb.like(cb.lower(root.get("title")), pattern);
                Predicate summaryMatches = cb.like(cb.lower(root.get("summary")), pattern);
                conditions.add(cb.or(titleMatches, summaryMatches));
            }

            return cb.and(conditions.toArray(new Predicate[0]));
        };

        // Sắp xếp: mới cập nhật lên đầu
        Sort sort = Sort.by("updatedAt").descending().and(Sort.by("id").descending());
        Page<Document> result = documentRepository.findAll(condition, PageRequest.of(page, size, sort));

        List<ManagedDocument> items = new ArrayList<>();
        for (Document document : result.getContent()) {
            items.add(documentMapper.toManagedDocument(document));
        }
        return new PageResponse<>(items, result.getTotalElements(), result.getTotalPages(), page, size);
    }

    /**
     * Chỉnh sửa thông tin tài liệu.
     * Nếu người dùng thường sửa, tài liệu sẽ quay về trạng thái chờ duyệt.
     */
    @Override
    @Transactional
    public ManagedDocument update(Long viewerId, boolean adminMode, Long documentId, EditDocumentRequest editRequest) {
        Document document = findEditableDocument(viewerId, adminMode, documentId);
        checkVersion(document, editRequest.version());

        UploadDocumentRequest data = editRequest.metadata();

        // Admin được đổi trường/khoa; người dùng thường giữ nguyên trường/khoa cũ
        OrganizationService.Selection organization;
        if (adminMode) {
            organization = organizationService.resolve(data.institutionId(), data.facultyId());
        } else {
            Long oldInstitutionId = document.getInstitution() != null ? document.getInstitution().getId() : null;
            Long oldFacultyId = document.getFaculty() != null ? document.getFaculty().getId() : null;
            organization = organizationService.resolve(oldInstitutionId, oldFacultyId);
        }

        Category category = findActiveCategory(data.categoryId());
        if (!isValidPrice(data)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Giá tài liệu không hợp lệ");
        }

        // Cập nhật thông tin
        document.setTitle(data.title().trim());
        document.setSummary(data.summary().trim());
        document.setDescription(data.description() != null ? data.description() : data.summary());
        document.setCategory(category);
        document.setLevel(data.level() != null ? data.level() : DEFAULT_LEVEL);
        document.setAccessType(data.accessType());
        document.setPrice(data.price());
        document.setInstitution(organization.institution());
        document.setFaculty(organization.faculty());
        applySubjectAndTags(document, data);

        // Người dùng thường sửa -> phải duyệt lại từ đầu
        if (!adminMode) {
            document.setStatus(DocumentStatus.PENDING.name());
            document.setPublishedAt(null);
            document.setReviewNote(null);
            document.setReviewedAt(null);
            document.setReviewedBy(null);
        }

        documentRepository.flush();
        return documentMapper.toManagedDocument(document);
    }

    /**
     * Xoá tài liệu (xoá mềm: chỉ đổi trạng thái sang DELETED, không xoá khỏi database).
     */
    @Override
    @Transactional
    public void delete(Long viewerId, boolean adminMode, Long documentId, Long expectedVersion) {
        Document document = findEditableDocument(viewerId, adminMode, documentId);
        checkVersion(document, expectedVersion);
        document.setStatus(DocumentStatus.DELETED.name());
        documentRepository.flush();
    }

    /**
     * Admin duyệt tài liệu.
     *
     * Các thao tác hợp lệ:
     *   APPROVE : PENDING   -> PUBLISHED (duyệt)
     *   REJECT  : PENDING   -> REJECTED  (từ chối, bắt buộc có lý do)
     *   HIDE    : PUBLISHED -> ARCHIVED  (ẩn)
     *   RESTORE : ARCHIVED  -> PUBLISHED (khôi phục)
     */
    @Override
    @Transactional
    public ManagedDocument review(Long adminId, Long documentId, String action, String note, Long expectedVersion) {
        requireAdmin(adminId);
        Document document = findDocumentNotDeleted(documentId);
        checkVersion(document, expectedVersion);

        String currentStatus = document.getStatus();
        String newStatus;

        ReviewAction reviewAction = parseReviewAction(action);
        switch (reviewAction) {
            case APPROVE:
                requireStatus(currentStatus, DocumentStatus.PENDING);
                newStatus = DocumentStatus.PUBLISHED.name();
                break;

            case REJECT:
                requireStatus(currentStatus, DocumentStatus.PENDING);
                if (note == null || note.isBlank()) {
                    throw new ApiException(HttpStatus.BAD_REQUEST, "Nhập lý do từ chối");
                }
                newStatus = DocumentStatus.REJECTED.name();
                break;

            case HIDE:
                requireStatus(currentStatus, DocumentStatus.PUBLISHED);
                newStatus = DocumentStatus.ARCHIVED.name();
                break;

            case RESTORE:
                requireStatus(currentStatus, DocumentStatus.ARCHIVED);
                newStatus = DocumentStatus.PUBLISHED.name();
                break;

            default:
                throw invalidTransitionError();
        }

        // Cập nhật trạng thái và thông tin duyệt
        document.setStatus(newStatus);
        document.setReviewNote(note != null ? note.trim() : null);
        document.setReviewedAt(TimeUtils.now());
        document.setReviewedBy(findActiveUser(adminId));
        if (DocumentStatus.PUBLISHED.is(newStatus)) {
            document.setPublishedAt(TimeUtils.now());
        }

        documentRepository.flush();
        return documentMapper.toManagedDocument(document);
    }

    // =========================================================
    // PHẦN 4: CÁC HÀM HỖ TRỢ - KIỂM TRA QUYỀN
    // =========================================================

    /** Kiểm tra người dùng có phải admin không. */
    private boolean isAdmin(Long userId) {
        if (userId == null) {
            return false;
        }
        for (UserRole userRole : userRoleRepository.findWithRoles(userId)) {
            if (RoleName.ADMIN.is(userRole.getRole().getName())) {
                return true;
            }
        }
        return false;
    }

    /** Bắt buộc phải là admin, nếu không thì báo lỗi 403. */
    private void requireAdmin(Long userId) {
        findActiveUser(userId);
        if (!isAdmin(userId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Chức năng dành cho quản trị viên");
        }
    }

    /** Kiểm tra người dùng có phải tác giả của tài liệu không. */
    private boolean isOwner(Document document, Long userId) {
        return document.getAuthor() != null && document.getAuthor().getId().equals(userId);
    }

    /**
     * Người dùng có quyền đọc (tải) tài liệu không?
     * Được đọc nếu: tài liệu miễn phí, HOẶC là tác giả, HOẶC đã được cấp quyền còn hiệu lực.
     */
    private boolean canRead(Document document, Long userId) {
        if (AccessType.FREE.is(document.getAccessType())) {
            return true;
        }
        if (isOwner(document, userId)) {
            return true;
        }
        long validGrants = documentAccessRepository.validGrant(userId, document.getId(), TimeUtils.now());
        return validGrants > 0;
    }

    /**
     * Kiểm tra giá có hợp lệ không:
     * - Tài liệu FREE thì giá phải bằng 0
     * - Tài liệu PAID thì giá phải lớn hơn 0
     */
    private boolean isValidPrice(UploadDocumentRequest request) {
        int priceSign = request.price().signum(); // -1: âm, 0: bằng 0, 1: dương
        if (AccessType.FREE.is(request.accessType()) && priceSign != 0) {
            return false;
        }
        if (AccessType.PAID.is(request.accessType()) && priceSign <= 0) {
            return false;
        }
        return true;
    }

    /**
     * Kiểm tra version để tránh 2 người sửa cùng lúc ghi đè lên nhau.
     * Nếu version người dùng gửi lên khác version hiện tại -> có người khác đã sửa trước.
     */
    private void checkVersion(Document document, Long expectedVersion) {
        if (expectedVersion == null || !expectedVersion.equals(document.getVersion())) {
            throw new ApiException(HttpStatus.CONFLICT, "Tài liệu đã thay đổi. Hãy tải lại trước khi thao tác");
        }
    }

    /** Chuyển chuỗi action (VD: "APPROVE") sang enum; giá trị lạ thì báo lỗi chuyển trạng thái. */
    private ReviewAction parseReviewAction(String action) {
        try {
            return ReviewAction.valueOf(action);
        } catch (IllegalArgumentException | NullPointerException e) {
            throw invalidTransitionError();
        }
    }

    /** Dùng trong review(): trạng thái hiện tại phải đúng như yêu cầu. */
    private void requireStatus(String currentStatus, DocumentStatus requiredStatus) {
        if (!requiredStatus.is(currentStatus)) {
            throw invalidTransitionError();
        }
    }

    private ApiException invalidTransitionError() {
        return new ApiException(HttpStatus.CONFLICT,
                "Trạng thái tài liệu không cho phép thao tác này. Hãy tải lại danh sách");
    }

    // =========================================================
    // PHẦN 5: CÁC HÀM HỖ TRỢ - TÌM DỮ LIỆU
    // =========================================================

    /** Tìm người dùng đang hoạt động, nếu không có thì báo lỗi 401. */
    private User findActiveUser(Long userId) {
        return userRepository.findById(userId)
                .filter(user -> UserStatus.ACTIVE.is(user.getStatus()))
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Tài khoản không hoạt động"));
    }

    /** Tìm chủ đề đang hoạt động. */
    private Category findActiveCategory(Long categoryId) {
        return categoryRepository.findById(categoryId)
                .filter(category -> Boolean.TRUE.equals(category.getActive()))
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "Chủ đề không hợp lệ"));
    }

    /** Tìm file đang hoạt động của tài liệu. */
    private DocumentFile findActiveFile(Long fileId, Long documentId, String notFoundMessage) {
        return documentFileRepository.findByIdAndDocumentIdAndActiveTrue(fileId, documentId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, notFoundMessage));
    }

    /** Tìm tài liệu chưa bị xoá. */
    private Document findDocumentNotDeleted(Long documentId) {
        return documentRepository.findById(documentId)
                .filter(document -> !DocumentStatus.DELETED.is(document.getStatus()))
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Không tìm thấy tài liệu"));
    }

    /** Tìm tài liệu đã công bố và chủ đề của nó còn hoạt động. */
    private Document findPublishedDocument(Long documentId) {
        return documentRepository.findByIdAndStatus(documentId, DocumentStatus.PUBLISHED.name())
                .filter(document -> Boolean.TRUE.equals(document.getCategory().getActive()))
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND,
                        "Không tìm thấy tài liệu hoặc tài liệu chưa được công bố"));
    }

    /**
     * Tìm tài liệu mà người dùng được phép XEM:
     * - Tài liệu đã công bố (và chủ đề còn hoạt động): ai cũng xem được
     * - Tài liệu chưa công bố: chỉ tác giả hoặc admin xem được
     */
    private Document findVisibleDocument(Long documentId, Long viewerId) {
        Document document = findDocumentNotDeleted(documentId);

        boolean isPublic = DocumentStatus.PUBLISHED.is(document.getStatus())
                && Boolean.TRUE.equals(document.getCategory().getActive());
        if (isPublic) {
            return document;
        }

        boolean isOwnerOrAdmin = viewerId != null && (isOwner(document, viewerId) || isAdmin(viewerId));
        if (isOwnerOrAdmin) {
            return document;
        }

        throw new ApiException(HttpStatus.NOT_FOUND, "Tài liệu chưa được công bố");
    }

    /**
     * Tìm tài liệu mà người dùng được phép SỬA / XOÁ:
     * - adminMode: phải là admin
     * - không phải adminMode: phải là tác giả
     */
    private Document findEditableDocument(Long viewerId, boolean adminMode, Long documentId) {
        if (adminMode) {
            requireAdmin(viewerId);
        }
        Document document = findDocumentNotDeleted(documentId);
        if (!adminMode && !isOwner(document, viewerId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Bạn chỉ được quản lý tài liệu của mình");
        }
        return document;
    }

    /**
     * Xác định trường/khoa cho tài liệu khi đăng tải:
     * - Admin: dùng trường/khoa được chọn trong form
     * - Người dùng thường: bắt buộc lấy từ hồ sơ cá nhân
     */
    private OrganizationService.Selection resolveUploadOrganization(Long userId, UploadDocumentRequest request) {
        if (isAdmin(userId)) {
            return organizationService.resolve(request.institutionId(), request.facultyId());
        }

        // Lấy trường/khoa trong hồ sơ
        OrganizationInfo profile = profileService.organization(userId);
        Long profileInstitutionId = null;
        Long profileFacultyId = null;
        if (profile != null) {
            profileInstitutionId = profile.institutionId();
            profileFacultyId = profile.facultyId();
        }

        // Nếu người dùng gửi trường/khoa khác với hồ sơ -> báo lỗi
        boolean differentInstitution = request.institutionId() != null
                && !request.institutionId().equals(profileInstitutionId);
        boolean differentFaculty = request.facultyId() != null
                && !request.facultyId().equals(profileFacultyId);
        if (differentInstitution || differentFaculty) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "Trường/khoa được lấy từ hồ sơ. Hãy cập nhật hồ sơ nếu cần thay đổi");
        }

        return organizationService.resolve(profileInstitutionId, profileFacultyId);
    }

    /**
     * Gán môn học và danh sách nhãn (tag) cho tài liệu.
     * Nhãn chưa tồn tại trong database sẽ được tạo mới.
     */
    private void applySubjectAndTags(Document document, UploadDocumentRequest request) {
        // Gán môn học
        if (request.subjectId() == null) {
            document.setSubject(null);
        } else {
            document.setSubject(subjectRepository.findById(request.subjectId())
                    .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "Môn học không hợp lệ")));
        }

        // Gán nhãn (dùng LinkedHashSet để không bị trùng và giữ đúng thứ tự)
        Set<Tag> selectedTags = new LinkedHashSet<>();
        if (request.tags() != null) {
            for (String rawTag : request.tags()) {
                // Bỏ khoảng trắng và dấu # ở đầu. Ví dụ: "  #java " -> "java"
                String tagName = rawTag.trim().replaceFirst("^#+", "").trim();

                if (tagName.isBlank() || tagName.length() > 80) {
                    throw new ApiException(HttpStatus.BAD_REQUEST, "Nhãn cần từ 1 đến 80 ký tự");
                }

                selectedTags.add(findOrCreateTag(tagName));
            }
        }

        document.getTags().clear();
        document.getTags().addAll(selectedTags);
    }

    /** Tìm nhãn theo tên (không phân biệt hoa thường), chưa có thì tạo mới. */
    private Tag findOrCreateTag(String tagName) {
        Optional<Tag> existingTag = tagRepository.findByNameIgnoreCase(tagName);
        if (existingTag.isPresent()) {
            return existingTag.get();
        }

        Tag newTag = new Tag();
        newTag.setName(tagName);
        newTag.setSlug("document-" + UUID.randomUUID());
        return tagRepository.saveAndFlush(newTag);
    }
}
