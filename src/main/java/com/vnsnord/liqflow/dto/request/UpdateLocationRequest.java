package com.vnsnord.liqflow.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request payload for updating an existing location's mutable fields.
 *
 * <p>The {@code code} and {@code type} are immutable and are not part of
 * this payload.</p>
 *
 * @param name    the new display name of the location (must not be blank, max 100 characters)
 * @param address the new address, or null/blank to clear it
 */
public record UpdateLocationRequest(@NotBlank(message = "Location name mandatory")
                                    @Size(max = 100, message = "Name cannot exceed 100 characters")
                                    String name,

                                    String address)
{
}