package com.example.sample.mapper;

import static com.example.sample.support.TestData.CREATED_AT;
import static com.example.sample.support.TestData.MODIFIED_AT;
import static com.example.sample.support.TestData.department;
import static org.assertj.core.api.Assertions.assertThat;

import com.example.sample.dto.request.CreateDepartmentRequest;
import com.example.sample.dto.response.DepartmentResponse;
import com.example.sample.entity.DepartmentEntity;
import java.util.List;
import org.junit.jupiter.api.Test;

class DepartmentMapperTest {

    private final DepartmentMapper mapper = new DepartmentMapper();

    @Test
    void toEntityCopiesAndTrimsFields() {
        DepartmentEntity entity = mapper.toEntity(new CreateDepartmentRequest("  Engineering ", " Toronto "), null);

        assertThat(entity.getId()).isNull();
        assertThat(entity.getName()).isEqualTo("Engineering");
        assertThat(entity.getLocation()).isEqualTo("Toronto");
        assertThat(entity.getCreatedAt()).isNull();
        assertThat(entity.getModifiedAt()).isNull();
    }

    @Test
    void toEntityTurnsBlankOrMissingLocationIntoNull() {
        assertThat(mapper.toEntity(new CreateDepartmentRequest("Eng", "   "), null).getLocation()).isNull();
        assertThat(mapper.toEntity(new CreateDepartmentRequest("Eng", null), null).getLocation()).isNull();
    }

    @Test
    void toResponseCopiesAllFields() {
        DepartmentResponse response = mapper.toResponse(department(5L, "Engineering"));

        assertThat(response).isEqualTo(new DepartmentResponse(5L, "Engineering", "Toronto", CREATED_AT, MODIFIED_AT));
    }

    @Test
    void toResponseListMapsEachEntityInOrder() {
        List<DepartmentResponse> responses = mapper.toResponseList(List.of(department(1L, "A"), department(2L, "B")));

        assertThat(responses).extracting(DepartmentResponse::id).containsExactly(1L, 2L);
        assertThat(mapper.toResponseList(List.of())).isEmpty();
    }
}
