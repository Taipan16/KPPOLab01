package com.example.vmserver.mapper;


import java.util.stream.Collectors;

import com.example.vmserver.dto.VMUserDTO;
import com.example.vmserver.dto.VMUserLoggedDTO;
import com.example.vmserver.model.Permission;
import com.example.vmserver.model.VMUser;

public class VMUserMapper {
    public static VMUserDTO userToUserDTO(VMUser user){
        return new VMUserDTO(user.getId(),
        user.getUsername(),
        user.getPassword(),
        user.getRole().getAuthority(),
        user.getRole().getPermissions().stream().map(Permission::getAuthority).collect(Collectors.toSet()));
    }

    public static VMUserLoggedDTO userToUserLoggedDto (VMUser user) {
        return new VMUserLoggedDTO(user.getUsername(),
        user.getRole().getAuthority(),
        user.getRole().getPermissions().stream().map(Permission::getAuthority).collect(Collectors.toSet()));
    }

}
