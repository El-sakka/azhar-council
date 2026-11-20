package eg.azhar.archive.service;

import eg.azhar.archive.dto.FileDto;
import eg.azhar.archive.dto.PageDto;
import eg.azhar.archive.dto.RegisterDto;
import eg.azhar.archive.dto.SearchCriteria;
import eg.azhar.archive.repository.ArchiveFileRepository;
import eg.azhar.archive.repository.ArchiveRegisterRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Search over the full inventory lists ("قائمة الحصر ككل"): by title, subject, or date. */
@Service
@Transactional(readOnly = true)
public class InventoryService {
    public static final int MAX_PAGE_SIZE = 1000;

    private final ArchiveFileRepository files;
    private final ArchiveRegisterRepository registers;

    public InventoryService(ArchiveFileRepository files, ArchiveRegisterRepository registers) {
        this.files = files;
        this.registers = registers;
    }

    public PageDto<FileDto> searchFiles(SearchCriteria c, int page, int size) {
        return PageDto.of(files.search(c.likeKeyword(), c.safeField(), c.keywordYear(), c.from(), c.to(),
                pageRequest(page, size)), FileDto::from);
    }

    public PageDto<RegisterDto> searchRegisters(SearchCriteria c, int page, int size) {
        return PageDto.of(registers.search(c.likeKeyword(), c.safeField(), c.keywordYear(), c.from(), c.to(),
                pageRequest(page, size)), RegisterDto::from);
    }

    public FileDto file(Long id) {
        return files.findById(id).map(FileDto::from).orElseThrow(() -> new NotFoundException("الملف غير موجود"));
    }

    public RegisterDto register(Long id) {
        return registers.findById(id).map(RegisterDto::from)
                .orElseThrow(() -> new NotFoundException("السجل غير موجود"));
    }

    public long fileCount() {
        return files.count();
    }

    public long registerCount() {
        return registers.count();
    }

    private static PageRequest pageRequest(int page, int size) {
        return PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), MAX_PAGE_SIZE));
    }
}
