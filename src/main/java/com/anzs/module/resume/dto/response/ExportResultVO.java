package com.anzs.module.resume.dto.response;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

@Data
public class ExportResultVO {
    @JsonSerialize(using = ToStringSerializer.class)
    private Long exportId;
    private String status;
    private String pdfUrl;
    private Long fileSize;
}
