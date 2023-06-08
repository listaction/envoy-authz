package org.example.authserver.entity;

import authserver.acl.Acl;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.java.Log;

@Log
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AclsRequestDTO {

  @NotNull private List<Acl> acls;
}
