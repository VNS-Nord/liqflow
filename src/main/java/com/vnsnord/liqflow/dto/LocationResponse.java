package com.vnsnord.liqflow.dto;

import com.vnsnord.liqflow.domain.enums.LocationType;

import java.util.UUID;

/**
 * Response payload describing a location.
 *
 * @param id      the location identifier
 * @param code    the unique location code
 * @param name    the display name of the location
 * @param type    the type of the location
 * @param address an optional address, or null
 */
public record LocationResponse(UUID id, String code, String name, LocationType type, String address)
{
}