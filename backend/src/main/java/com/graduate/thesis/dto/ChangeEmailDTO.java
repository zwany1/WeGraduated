package com.graduate.thesis.dto;

import lombok.Data;

@Data
public class ChangeEmailDTO {
    private String newEmail;
    private String code;
    private String currentPassword;
}
