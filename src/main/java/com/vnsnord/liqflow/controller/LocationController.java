package com.vnsnord.liqflow.controller;

import com.vnsnord.liqflow.dto.request.CreateLocationRequest;
import com.vnsnord.liqflow.dto.response.LocationResponse;
import com.vnsnord.liqflow.dto.request.UpdateLocationRequest;
import com.vnsnord.liqflow.service.LocationService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.UUID;

/**
 * REST endpoints for managing locations under {@code /api/v1/locations}.
 */
@RestController
@RequestMapping("/api/v1/locations")
public class LocationController
{
    private final LocationService locationService;

    /**
     * Creates a new location controller backed by the given service.
     *
     * @param locationService the location application service
     */
    public LocationController(LocationService locationService)
    {
        this.locationService = locationService;
    }

    /**
     * Returns a single location by its identifier.
     *
     * @param id the location identifier
     * @return the location with HTTP 200
     */
    @GetMapping("/{id}")
    public ResponseEntity<LocationResponse> getById(@PathVariable UUID id)
    {
        return ResponseEntity.ok(locationService.getLocationById(id));
    }

    /**
     * Returns a single location by its code.
     *
     * @param code the unique location code
     * @return the location with HTTP 200
     */
    @GetMapping("/code/{code}")
    public ResponseEntity<LocationResponse> getByCode(@PathVariable String code)
    {
        return ResponseEntity.ok(locationService.getLocationByCode(code));
    }

    /**
     * Returns a page of all locations.
     *
     * @param pageable pagination and sorting information
     * @return the page of locations with HTTP 200
     */
    @GetMapping
    public ResponseEntity<Page<LocationResponse>> getAll(Pageable pageable)
    {
        return ResponseEntity.ok(locationService.getAllLocations(pageable));
    }

    /**
     * Creates a new location.
     *
     * @param request the creation request
     * @return the created location with a {@code Location} header and HTTP 201
     */
    @PostMapping
    public ResponseEntity<LocationResponse> createLocation(@Valid @RequestBody CreateLocationRequest request)
    {
        LocationResponse response = locationService.createLocation(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.id())
                .toUri();
        return ResponseEntity.created(location).body(response);
    }

    /**
     * Updates the mutable details of an existing location.
     *
     * @param id      the location identifier
     * @param request the update request
     * @return the updated location with HTTP 200
     */
    @PutMapping("/{id}")
    public ResponseEntity<LocationResponse> updateLocation(@PathVariable UUID id,
                                                           @Valid @RequestBody UpdateLocationRequest request)
    {
        return ResponseEntity.ok(locationService.updateLocation(id, request));
    }

    /**
     * Deletes a location.
     *
     * @param id the location identifier
     * @return HTTP 204
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteLocation(@PathVariable UUID id)
    {
        locationService.deleteLocation(id);
        return ResponseEntity.noContent().build();
    }
}