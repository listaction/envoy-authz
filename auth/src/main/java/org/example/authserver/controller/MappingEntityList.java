package org.example.authserver.controller;

import lombok.Data;
import org.example.authserver.entity.MappingEntity;

import javax.validation.constraints.NotNull;
import java.util.List;

@Data
public class MappingEntityList {
    @NotNull
    private List<MappingEntity> mappings;
}
