package eg.azhar.archive.controller;

import eg.azhar.archive.dto.StatsDto;
import eg.azhar.archive.repository.SubjectRepository;
import eg.azhar.archive.service.InventoryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/stats")
public class StatsController {
    private final InventoryService inventory;
    private final SubjectRepository subjects;

    public StatsController(InventoryService inventory, SubjectRepository subjects) {
        this.inventory = inventory;
        this.subjects = subjects;
    }

    @GetMapping
    public StatsDto stats() {
        var all = subjects.findAll();
        return new StatsDto(inventory.fileCount(), inventory.registerCount(),
                all.stream().filter(s -> s.getParentId() == null).count(), all.size());
    }
}
