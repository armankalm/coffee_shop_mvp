package com.coffeeshop.app.dto.reference;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class RefUserRoleRequest {

    @NotBlank
    @Size(max = 30)
    private String code;

    @NotBlank
    @Size(max = 100)
    private String nameRu;

    @NotBlank
    @Size(max = 100)
    private String nameEn;

    @Size(max = 500)
    private String permissions;

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getNameRu() { return nameRu; }
    public void setNameRu(String nameRu) { this.nameRu = nameRu; }

    public String getNameEn() { return nameEn; }
    public void setNameEn(String nameEn) { this.nameEn = nameEn; }

    public String getPermissions() { return permissions; }
    public void setPermissions(String permissions) { this.permissions = permissions; }
}
