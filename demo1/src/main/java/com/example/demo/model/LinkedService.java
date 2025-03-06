package com.example.demo.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class LinkedService {
    @NotBlank(message = "org cannot be empty")
    private String _org;

    @NotNull(message = "objectId cannot be null")
    @NotBlank(message = "objectId cannot be empty")
    private String objectId;

    @NotNull(message = "objectType cannot be null")
    @NotBlank(message = "objectType cannot be empty")
    private String objectType;

    @NotBlank(message = "name cannot be empty")
    private String name;
}
