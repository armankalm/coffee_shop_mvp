package com.coffeeshop.app.dto.reference;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class RefShopStatusRequest {

    @NotBlank
    @Size(max = 30)
    private String code;

    @NotBlank
    @Size(max = 100)
    private String nameRu;

    @NotBlank
    @Size(max = 100)
    private String nameEn;

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getNameRu() { return nameRu; }
    public void setNameRu(String nameRu) { this.nameRu = nameRu; }

    public String getNameEn() { return nameEn; }
    public void setNameEn(String nameEn) { this.nameEn = nameEn; }
}
