package com.pavan.salesforceswitch.dto;

public class ValidationRuleDto {

    private String id;
    private String validationName;
    private Boolean active;
    private String objectName;

    public ValidationRuleDto() {
    }

    public ValidationRuleDto(String id, String validationName, Boolean active, String objectName) {
        this.id = id;
        this.validationName = validationName;
        this.active = active;
        this.objectName = objectName;
    }

    public String getId() {
        return id;
    }

    public String getValidationName() {
        return validationName;
    }

    public Boolean getActive() {
        return active;
    }

    public String getObjectName() {
        return objectName;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setValidationName(String validationName) {
        this.validationName = validationName;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public void setObjectName(String objectName) {
        this.objectName = objectName;
    }
}