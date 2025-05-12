package org.example.authserver.service;

import authserver.acl.Acl;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.example.authserver.config.AppProperties;
import org.example.authserver.entity.AclEntity;
import org.example.authserver.entity.RoleDto;
import org.example.authserver.entity.RoleEntity;
import org.example.authserver.repo.RoleRepository;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class RoleService {

    private final AclService aclService;
    private final RoleRepository roleRepository;

    public RoleService(AclService aclService, RoleRepository roleRepository) {
        this.aclService = aclService;
        this.roleRepository = roleRepository;
    }

    public Set<RoleDto> findAll() {
        return roleRepository.findAll().stream()
                .map(m->{
                    RoleDto roleDto = new RoleDto();
                    roleDto.setId(m.getId());
                    roleDto.setPermissions(m.getPermissions());
                    return roleDto;
                })
                .collect(Collectors.toSet());
    }

    public void save(@Valid RoleDto dto) {
        log.info("Saving role: {}", dto);
        RoleEntity role = null;
        if (dto.getId() != null){
            role = roleRepository.findById(dto.getId()).orElse(null);
        }
        if (role == null){
            role = RoleEntity.builder()
                    .id(dto.getId())
                    .created(System.currentTimeMillis())
                    .build();
        }
        role.setPermissions(dto.getPermissions());
        role.setUpdated(System.currentTimeMillis());
        roleRepository.save(role);
    }

    public Set<Acl> deleteById(String roleId) {
        Set<Acl> deletedAcls = new HashSet<>();
        RoleEntity role = roleRepository.findById(roleId).orElse(null);
        if (role != null) {
            Set<Acl> acls = aclService.findAllByNsObjectIn(List.of("_rbac:_users"));
            for (Acl acl : acls) {
                if (!roleId.equals(acl.getRelation())) {
                    continue;
                }
                aclService.delete(acl);
                deletedAcls.add(acl);
            }
            roleRepository.delete(role);
        }

        return deletedAcls;
    }
}
