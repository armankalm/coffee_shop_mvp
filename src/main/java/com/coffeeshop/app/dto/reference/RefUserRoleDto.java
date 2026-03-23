package com.coffeeshop.app.dto.reference;

import com.coffeeshop.app.domain.RefUserRole;

public class RefUserRoleDto {
    private Long id;
    private String code;
    private String nameRu;
    private String nameEn;
    private String permissions;

    public static RefUserRoleDto from(RefUserRole ref) {
        RefUserRoleDto dto = new RefUserRoleDto();
        dto.id = ref.getId();
        dto.code = ref.getCode();
        dto.nameRu = ref.getNameRu();
        dto.nameEn = ref.getNameEn();
        dto.permissions = ref.getPermissions();
        return dto;
    }

    public Long getId() { return id; }
    public String getCode() { return code; }
    public String getNameRu() { return nameRu; }
    public String getNameEn() { return nameEn; }
    public String getPermissions() { return permissions; }
}
