package com.example.sample.mapper;

import java.util.Collection;
import java.util.List;

/**
 * Contract for converting between API DTOs and JPA entities.
 *
 * @param <REQ> request DTO type
 * @param <CTX> extra input needed to build the entity (use {@link Void} when none)
 * @param <E>   entity type
 * @param <RES> response DTO type
 */
public interface DtoMapper<REQ, CTX, E, RES> {

    E toEntity(REQ request, CTX context);

    RES toResponse(E entity);

    default List<RES> toResponseList(Collection<E> entities) {
        return entities.stream().map(this::toResponse).toList();
    }
}
