package org.example.authserver.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "resources")
@Entity
public class ResourceEntity {
    
    @Id
    private String id;
    
    @Column(nullable = false)
    private String namespace;
    
    @Column(nullable = false, name = "resource_id")
    private String resourceId;

    @Builder.Default private Long created = System.currentTimeMillis();
    @Builder.Default private Long updated = System.currentTimeMillis();

}