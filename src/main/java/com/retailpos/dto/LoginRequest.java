
package com.retailpos.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(

        @NotBlank
        @Size(max = 254)
        String identifier,

        @NotBlank
        String password

) {
}
