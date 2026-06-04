package com.ada.proj.enums;

public enum AttachmentType {
    IMAGE, VIDEO, HWP, PDF, DOCUMENT, OTHER;

    public static AttachmentType fromFileName(String fileName) {
        if (fileName == null) return OTHER;
        String lower = fileName.toLowerCase();
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".png")
                || lower.endsWith(".gif") || lower.endsWith(".webp") || lower.endsWith(".svg")) return IMAGE;
        if (lower.endsWith(".mp4") || lower.endsWith(".avi") || lower.endsWith(".mov")
                || lower.endsWith(".wmv") || lower.endsWith(".mkv")) return VIDEO;
        if (lower.endsWith(".hwp") || lower.endsWith(".hwpx")) return HWP;
        if (lower.endsWith(".pdf")) return PDF;
        if (lower.endsWith(".doc") || lower.endsWith(".docx") || lower.endsWith(".xls")
                || lower.endsWith(".xlsx") || lower.endsWith(".ppt") || lower.endsWith(".pptx")
                || lower.endsWith(".txt") || lower.endsWith(".csv")) return DOCUMENT;
        return OTHER;
    }
}
