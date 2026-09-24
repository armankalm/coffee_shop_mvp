package com.coffeeshop.app.dto.reference;

import com.coffeeshop.app.domain.RefOrderStatus;

public class RefOrderStatusDto {
    private Long id;
    private String code;
    private String nameRu;
    private String nameEn;
    private String description;

    public static RefOrderStatusDto from(RefOrderStatus ref) {
        RefOrderStatusDto dto = new RefOrderStatusDto();
        dto.id = ref.getId();
        dto.code = ref.getCode();
        dto.nameRu = ref.getNameRu();
        dto.nameEn = ref.getNameEn();
        dto.description = ref.getDescription();
        return dto;
    }

    public Long getId() { return id; }
    public String getCode() { return code; }
    public String getNameRu() { return nameRu; }
    public String getNameEn() { return nameEn; }
    public String getDescription() { return description; }
}
