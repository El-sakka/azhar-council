package eg.azhar.archive.service;

import eg.azhar.archive.dto.*;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/** Builds the Excel report of search results ("تقرير بنتائج البحث في مخرج إكسيل شيت"). */
@Service
public class ExportService {
    private final InventoryService inventory;
    private final SubjectService subjects;

    public ExportService(InventoryService inventory, SubjectService subjects) {
        this.inventory = inventory;
        this.subjects = subjects;
    }

    public byte[] inventoryReport(SearchCriteria c, boolean includeFiles, boolean includeRegisters) {
        try (Workbook wb = new XSSFWorkbook()) {
            Styles st = new Styles(wb);
            if (includeFiles) {
                List<FileDto> rows = inventory.searchFiles(c, 0, InventoryService.MAX_PAGE_SIZE).items();
                sheet(wb, st, "الملفات", List.of(
                        col("م", r -> null), col("رقم الملف", FileDto::fileNumber),
                        col("رمز الإرجاع", FileDto::referenceCode), col("عنوان الملف", FileDto::title),
                        col("لون الغلاف", FileDto::coverColor), col("مقاس الملف", FileDto::fileSize),
                        col("تاريخ بداية الملف", FileDto::startDate), col("تاريخ نهاية الملف", FileDto::endDate),
                        col("عدد الأوراق", FileDto::sheetCount), col("لغة الكتابة", FileDto::language),
                        col("المحتوى الموضوعي", FileDto::subjectContent),
                        col("الحالة المادية", FileDto::physicalCondition), col("ملاحظات", FileDto::notes)), rows);
            }
            if (includeRegisters) {
                List<RegisterDto> rows = inventory.searchRegisters(c, 0, InventoryService.MAX_PAGE_SIZE).items();
                sheet(wb, st, "السجلات", List.of(
                        col("م", r -> null), col("رقم السجل", RegisterDto::registerNumber),
                        col("رمز الإرجاع", RegisterDto::referenceCode), col("عنوان السجل", RegisterDto::title),
                        col("اسم المتكاملة الفرعية", RegisterDto::subUnitName),
                        col("صادر / وارد", RegisterDto::direction),
                        col("تاريخ بداية السجل", RegisterDto::startDate),
                        col("تاريخ نهاية السجل", RegisterDto::endDate),
                        col("موضوعات السجل", RegisterDto::subjectsText),
                        col("الأوراق المكتوبة", RegisterDto::writtenSheets),
                        col("الأوراق الفارغة", RegisterDto::blankSheets), col("لغة الكتابة", RegisterDto::language),
                        col("نوع الخط", RegisterDto::scriptType), col("الطول (سم)", RegisterDto::lengthCm),
                        col("العرض (سم)", RegisterDto::widthCm),
                        col("الحالة المادية", RegisterDto::physicalCondition)), rows);
            }
            return toBytes(wb);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public byte[] subjectReport(Long subjectId, SearchCriteria c) {
        SubjectResultDto result = subjects.search(subjectId, c);
        List<EntryRow> files = new ArrayList<>();
        List<EntryRow> registers = new ArrayList<>();
        result.groups().forEach(g -> {
            String name = g.code() + " - " + g.name();
            g.files().forEach(e -> files.add(new EntryRow(name, e)));
            g.registers().forEach(e -> registers.add(new EntryRow(name, e)));
        });
        try (Workbook wb = new XSSFWorkbook()) {
            Styles st = new Styles(wb);
            subjectSheet(wb, st, "الملفات", "رقم الملف", "عنوان الملف", files);
            subjectSheet(wb, st, "السجلات", "رقم السجل", "عنوان السجل", registers);
            return toBytes(wb);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private record EntryRow(String subject, SubjectEntryDto e) {
    }

    private static void subjectSheet(Workbook wb, Styles st, String name, String numberHeader, String titleHeader,
                                     List<EntryRow> rows) {
        sheet(wb, st, name, List.of(
                col("م", r -> null), col("الموضوع", EntryRow::subject),
                col(numberHeader, r -> r.e().itemNumber()), col("رمز الإرجاع", r -> r.e().referenceCode()),
                col(titleHeader, r -> r.e().title()), col("التاريخ", r -> r.e().dateText()),
                col("المحتوى الموضوعي", r -> r.e().subjectContent())), rows);
    }

    private record Column<T>(String header, Function<T, Object> value) {
    }

    private static <T> Column<T> col(String header, Function<T, Object> value) {
        return new Column<>(header, value);
    }

    private static <T> void sheet(Workbook wb, Styles st, String name, List<Column<T>> cols, List<T> rows) {
        Sheet sheet = wb.createSheet(name);
        sheet.setRightToLeft(true);
        Row header = sheet.createRow(0);
        for (int i = 0; i < cols.size(); i++) {
            Cell cell = header.createCell(i);
            cell.setCellValue(cols.get(i).header());
            cell.setCellStyle(st.header);
        }
        for (int r = 0; r < rows.size(); r++) {
            Row row = sheet.createRow(r + 1);
            for (int i = 0; i < cols.size(); i++) {
                Object v = i == 0 ? Integer.valueOf(r + 1) : cols.get(i).value().apply(rows.get(r));
                Cell cell = row.createCell(i);
                cell.setCellStyle(st.body);
                if (v instanceof Number n) cell.setCellValue(n.doubleValue());
                else if (v != null) cell.setCellValue(v.toString());
            }
        }
        for (int i = 0; i < cols.size(); i++) {
            sheet.setColumnWidth(i, Math.min(60, Math.max(8, cols.get(i).header().length() + 6)) * 256);
        }
        sheet.createFreezePane(0, 1);
    }

    private static byte[] toBytes(Workbook wb) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        wb.write(out);
        return out.toByteArray();
    }

    private static final class Styles {
        final CellStyle header;
        final CellStyle body;

        Styles(Workbook wb) {
            Font bold = wb.createFont();
            bold.setBold(true);
            header = wb.createCellStyle();
            header.setFont(bold);
            header.setFillForegroundColor(IndexedColors.LIGHT_GREEN.getIndex());
            header.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            header.setAlignment(HorizontalAlignment.CENTER);
            header.setWrapText(true);
            body = wb.createCellStyle();
            body.setWrapText(true);
            body.setVerticalAlignment(VerticalAlignment.TOP);
        }
    }
}
