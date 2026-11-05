package com.knowledge.app.model.dto.response.document;
import java.time.LocalDateTime;
import java.math.BigDecimal;
public record DocumentSummary(Long id,String slug,String title,String summary,Long categoryId,String category,
 String accessType,BigDecimal price,String currency,String level,String language,String coverStyle,
 boolean featured,LocalDateTime publishedAt,long downloads,Long institutionId,String institutionName,Long facultyId,String facultyName,String fileFormat) {}
