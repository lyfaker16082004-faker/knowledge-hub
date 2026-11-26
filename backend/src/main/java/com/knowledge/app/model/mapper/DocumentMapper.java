package com.knowledge.app.model.mapper;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import com.knowledge.app.common.util.TextUtils;
import com.knowledge.app.model.dto.response.document.DocumentSummary;
import com.knowledge.app.model.dto.response.document.ManagedDocument;
import com.knowledge.app.model.entity.Document;
import com.knowledge.app.model.entity.DocumentFile;
import com.knowledge.app.model.entity.Tag;
import com.knowledge.app.repository.DocumentDownloadRepository;
import com.knowledge.app.repository.DocumentFileRepository;

/**
 * Chuyển entity Document sang các DTO trả về cho frontend.
 *
 * Là một @Component (không phải class static) vì cần truy vấn thêm
 * số lượt tải và định dạng file từ database.
 */
@Component
public class DocumentMapper {

    private final DocumentFileRepository documentFileRepository;
    private final DocumentDownloadRepository documentDownloadRepository;

    public DocumentMapper(DocumentFileRepository documentFileRepository,
            DocumentDownloadRepository documentDownloadRepository) {
        this.documentFileRepository = documentFileRepository;
        this.documentDownloadRepository = documentDownloadRepository;
    }

    /** Document -> DocumentSummary (thông tin tóm tắt). */
    public DocumentSummary toSummary(Document document) {
        // Thông tin trường
        Long institutionId = null;
        String institutionName = null;
        if (document.getInstitution() != null) {
            institutionId = document.getInstitution().getId();
            institutionName = document.getInstitution().getName();
        }

        // Thông tin khoa
        Long facultyId = null;
        String facultyName = null;
        if (document.getFaculty() != null) {
            facultyId = document.getFaculty().getId();
            facultyName = document.getFaculty().getName();
        }

        // Thông tin môn học
        Long subjectId = null;
        String subjectName = null;
        if (document.getSubject() != null) {
            subjectId = document.getSubject().getId();
            subjectName = document.getSubject().getName();
        }

        // Danh sách tên nhãn, sắp xếp theo bảng chữ cái
        List<String> tagNames = new ArrayList<>();
        for (Tag tag : document.getTags()) {
            tagNames.add(tag.getName());
        }
        tagNames.sort(null);

        long downloadCount = documentDownloadRepository.countByDocumentId(document.getId());

        return new DocumentSummary(
                document.getId(),
                document.getSlug(),
                document.getTitle(),
                document.getSummary(),
                document.getCategory().getId(),
                document.getCategory().getName(),
                document.getAccessType(),
                document.getPrice(),
                document.getCurrency(),
                document.getLevel(),
                document.getLanguage(),
                document.getCoverStyle(),
                Boolean.TRUE.equals(document.getFeatured()),
                document.getPublishedAt(),
                downloadCount,
                institutionId,
                institutionName,
                facultyId,
                facultyName,
                getFileFormat(document),
                subjectId,
                subjectName,
                tagNames);
    }

    /** Document -> ManagedDocument (thông tin cho trang quản lý). */
    public ManagedDocument toManagedDocument(Document document) {
        Long authorId = null;
        String authorName = "Ban biên tập";
        if (document.getAuthor() != null) {
            authorId = document.getAuthor().getId();
            authorName = document.getAuthor().getUsername();
        }

        return new ManagedDocument(
                toSummary(document),
                document.getDescription(),
                authorId,
                authorName,
                document.getStatus(),
                document.getReviewNote(),
                document.getCreatedAt(),
                document.getUpdatedAt(),
                document.getVersion());
    }

    /** Định dạng của file đầu tiên. VD: "bai-giang.pdf" -> "PDF". Không có file thì trả về "FILE". */
    private String getFileFormat(Document document) {
        List<DocumentFile> files = documentFileRepository.findByDocumentIdAndActiveTrueOrderByIdAsc(document.getId());
        if (files.isEmpty()) {
            return "FILE";
        }
        return TextUtils.fileExtension(files.get(0).getOriginalName());
    }
}
