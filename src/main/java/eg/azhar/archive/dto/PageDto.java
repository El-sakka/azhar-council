package eg.azhar.archive.dto;

import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;

public record PageDto<T>(List<T> items, long total, int page, int size, int totalPages) {
    public static <E, T> PageDto<T> of(Page<E> page, Function<E, T> mapper) {
        return new PageDto<>(page.getContent().stream().map(mapper).toList(), page.getTotalElements(),
                page.getNumber(), page.getSize(), page.getTotalPages());
    }
}
