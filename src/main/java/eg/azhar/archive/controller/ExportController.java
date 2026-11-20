package eg.azhar.archive.controller;

import eg.azhar.archive.dto.SearchCriteria;
import eg.azhar.archive.service.ExportService;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/export")
public class ExportController {
    private static final MediaType XLSX =
            MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

    private final ExportService export;

    public ExportController(ExportService export) {
        this.export = export;
    }

    /** @param type all | files | registers */
    @GetMapping("/inventory")
    public ResponseEntity<byte[]> inventory(SearchCriteria criteria, @RequestParam(defaultValue = "all") String type) {
        byte[] xlsx = export.inventoryReport(criteria, !"registers".equals(type), !"files".equals(type));
        return file(xlsx, "نتائج-البحث-قائمة-الحصر.xlsx");
    }

    @GetMapping("/subjects/{id}")
    public ResponseEntity<byte[]> subject(@PathVariable Long id, SearchCriteria criteria) {
        return file(export.subjectReport(id, criteria), "نتائج-البحث-بالموضوع.xlsx");
    }

    private static ResponseEntity<byte[]> file(byte[] body, String name) {
        return ResponseEntity.ok()
                .contentType(XLSX)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(name, StandardCharsets.UTF_8).build().toString())
                .body(body);
    }
}
