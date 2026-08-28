package com.museum.ticket.admin;

public record AdminResponse(
        String workerId,
        String account,
        String name,
        String status,
        String roleId,
        String roleName,
        String permissionDescription
) {
}
