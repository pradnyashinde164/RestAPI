package com.example.demo.model;

import jakarta.validation.constraints.Min;
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
public class PlanCostShare {
    @NotNull
    @Min(0)
    private Integer deductible;

    @NotNull
    @NotBlank
    private String _org;

    @NotNull
    @Min(0)
    private Integer copay;

    @NotNull
    @NotBlank
    private String objectId;

    @NotNull
    @NotBlank
    private String objectType;
}
