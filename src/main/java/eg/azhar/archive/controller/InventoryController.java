package eg.azhar.archive.controller;

import eg.azhar.archive.dto.*;
import eg.azhar.archive.service.InventoryService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class InventoryController {
    private final InventoryService inventory;

    public InventoryController(InventoryService inventory) {
        this.inventory = inventory;
    }

    @GetMapping("/files")
    public PageDto<FileDto> files(SearchCriteria criteria,
                                  @RequestParam(defaultValue = "0") int page,
                                  @RequestParam(defaultValue = "25") int size) {
        return inventory.searchFiles(criteria, page, size);
    }

    @GetMapping("/files/{id}")
    public FileDto file(@PathVariable Long id) {
        return inventory.file(id);
    }

    @GetMapping("/registers")
    public PageDto<RegisterDto> registers(SearchCriteria criteria,
                                          @RequestParam(defaultValue = "0") int page,
                                          @RequestParam(defaultValue = "25") int size) {
        return inventory.searchRegisters(criteria, page, size);
    }

    @GetMapping("/registers/{id}")
    public RegisterDto register(@PathVariable Long id) {
        return inventory.register(id);
    }
}
