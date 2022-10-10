package org.example.authserver.entity;

import authserver.acl.Acl;
import java.util.List;
import javax.validation.constraints.NotNull;
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
