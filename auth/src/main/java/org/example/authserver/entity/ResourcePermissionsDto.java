package org.example.authserver.entity;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResourcePermissionsDto {
    @NotBlank
    private String namespace;
    
    @NotBlank
    private String resourceId;
    
    @NotNull
    private Map<String, List<String>> permissions;
}