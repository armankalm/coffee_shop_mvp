package com.coffeeshop.app.dto.reference;

import com.coffeeshop.app.domain.RefProductCategory;

public class RefProductCategoryDto {
    private Long id;
    private String code;
    private String nameRu;
    private String nameEn;
    private String icon;

    public static RefProductCategoryDto from(RefProductCategory ref) {
        RefProductCategoryDto dto = new RefProductCategoryDto();
        dto.id = ref.getId();
        dto.code = ref.getCode();
        dto.nameRu = ref.getNameRu();
        dto.nameEn = ref.getNameEn();
        dto.icon = ref.getIcon();
        return dto;
    }

    public Long getId() { return id; }
    public String getCode() { return code; }
    public String getNameRu() { return nameRu; }
    public String getNameEn() { return nameEn; }
    public String getIcon() { return icon; }
}
