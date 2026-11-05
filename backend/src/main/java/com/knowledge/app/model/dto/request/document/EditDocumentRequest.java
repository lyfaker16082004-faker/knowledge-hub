package com.knowledge.app.model.dto.request.document;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
public record EditDocumentRequest(@NotNull @PositiveOrZero Long version,@NotNull @Valid UploadDocumentRequest metadata){}
