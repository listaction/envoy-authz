package org.example.authserver.entity;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ResourceDto {
    @NotBlank
    private String namespace;
    
    @NotBlank
    private String resourceId;
}