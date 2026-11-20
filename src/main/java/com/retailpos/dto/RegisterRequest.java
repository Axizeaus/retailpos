
package com.retailpos.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(

        @NotBlank
        @Size(min = 2, max = 150)
        String name,

        @Email
        @Size(max = 254)
        String email,

        @Pattern(regexp = "^\\+[1-9]\\d{1,14}$")
        String phoneNumber,

        @NotBlank
        @Size(min = 8, max = 72)
        String password,

        @NotBlank
        @Size(min = 2, max = 150)
        String shopName

) {

        @AssertTrue(message = "At least one of email or phone number must be provided.")
        public boolean isLoginIdentifierProvided() {
                return (email != null && !email.isBlank())
                        || (phoneNumber != null && !phoneNumber.isBlank());
        }
}
