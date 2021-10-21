package org.example.authserver.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.java.Log;
import org.hibernate.annotations.Type;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import java.io.Serializable;
import java.util.List;

@Log
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity(name = "userrelations")
public class UserRelationEntity implements Serializable {

    @Id
    @Column(name = "usr")
    private String user;

    @Type(type = "jsonb")
    @Column(columnDefinition = "jsonb")
    private List<String> relations;
}
