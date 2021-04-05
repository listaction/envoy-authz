package org.example.authserver.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExpectedRelationMapping {

    private String defaultMapping;
    private String post;
    private String get;
    private String put;
    private String delete;

}
