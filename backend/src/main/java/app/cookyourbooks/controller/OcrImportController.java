package app.cookyourbooks.controller;

import app.cookyourbooks.dto.ocr.OcrImportResponse;
import app.cookyourbooks.security.SecurityUtils;
import app.cookyourbooks.service.OcrImportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/ocr")
@RequiredArgsConstructor
public class OcrImportController {

    private final OcrImportService ocrImportService;

    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<OcrImportResponse> importFromImage(
            @RequestParam("image") MultipartFile image) {
        OcrImportResponse response = ocrImportService.importFromImage(
            SecurityUtils.currentUserId(), image);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
