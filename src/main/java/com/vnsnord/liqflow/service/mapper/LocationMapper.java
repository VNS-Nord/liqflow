package com.vnsnord.liqflow.service.mapper;

import com.vnsnord.liqflow.domain.entity.Location;
import com.vnsnord.liqflow.dto.CreateLocationRequest;
import com.vnsnord.liqflow.dto.LocationResponse;
import org.springframework.stereotype.Component;

/**
 * Maps between {@link Location} entities and their DTOs.
 */
@Component
public class LocationMapper
{
    /**
     * Converts a location entity into a response DTO.
     *
     * @param location the location entity
     * @return the response DTO
     */
    public LocationResponse toResponse(Location location)
    {
        return new LocationResponse(location.getId(),
                location.getCode(),
                location.getName(),
                location.getType(),
                location.getAddress());
    }

    /**
     * Converts a create request into a new location entity.
     *
     * @param request the creation request
     * @return a new location entity
     */
    public Location toRequest(CreateLocationRequest request)
    {
        return new Location(request.code(),
                request.name(),
                request.type(),
                request.address()
        );
    }
}