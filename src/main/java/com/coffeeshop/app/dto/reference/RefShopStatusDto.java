package com.coffeeshop.app.dto.reference;

import com.coffeeshop.app.domain.RefShopStatus;

public class RefShopStatusDto {
    private Long id;
    private String code;
    private String nameRu;
    private String nameEn;

    public static RefShopStatusDto from(RefShopStatus ref) {
        RefShopStatusDto dto = new RefShopStatusDto();
        dto.id = ref.getId();
        dto.code = ref.getCode();
        dto.nameRu = ref.getNameRu();
        dto.nameEn = ref.getNameEn();
        return dto;
    }

    public Long getId() { return id; }
    public String getCode() { return code; }
    public String getNameRu() { return nameRu; }
    public String getNameEn() { return nameEn; }
}
