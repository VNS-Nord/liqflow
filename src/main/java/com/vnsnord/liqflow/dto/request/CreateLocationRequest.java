package com.vnsnord.liqflow.dto.request;

import com.vnsnord.liqflow.domain.enums.LocationType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Request payload for creating a new location.
 *
 * @param code    the unique location code, trimmed and upper-cased on creation
 *                (must not be blank, max 30 characters)
 * @param name    the display name of the location (must not be blank, max 100 characters)
 * @param type    the type of the location (must not be null)
 * @param address an optional address, or null
 */
public record CreateLocationRequest(@NotBlank(message = "Location code mandatory")
                                    @Size(max = 30, message = "Code cannot exceed 30 characters")
                                    String code,

                                    @NotBlank(message = "Location name mandatory")
                                    @Size(max = 100, message = "Name cannot exceed 100 characters")
                                    String name,

                                    @NotNull(message = "Location type mandatory")
                                    LocationType type,

                                    String address)
{
}