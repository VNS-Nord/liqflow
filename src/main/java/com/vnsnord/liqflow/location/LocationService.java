package com.vnsnord.liqflow.location;

import com.vnsnord.liqflow.common.exception.ConflictException;
import com.vnsnord.liqflow.common.exception.LocationNotFoundException;
import com.vnsnord.liqflow.inventory.InventoryRepository;
import com.vnsnord.liqflow.transfer.TransferOrderRepository;
import com.vnsnord.liqflow.transfer.TransferOrderReservationRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Application service handling location read, creation, update, and deletion.
 */
@Service
@Transactional(readOnly = true)
public class LocationService
{
    private final LocationRepository locationRepository;
    private final InventoryRepository inventoryRepository;
    private final TransferOrderRepository transferOrderRepository;
    private final TransferOrderReservationRepository reservationRepository;
    private final LocationMapper locationMapper;

    /**
     * Creates a new location service with the given dependencies.
     *
     * @param locationRepository     the location repository
     * @param inventoryRepository    the inventory repository
     * @param transferOrderRepository the transfer order repository
     * @param reservationRepository  the transfer order reservation repository
     * @param locationMapper         the location mapper
     */
    public LocationService(LocationRepository locationRepository,
                           InventoryRepository inventoryRepository,
                           TransferOrderRepository transferOrderRepository,
                           TransferOrderReservationRepository reservationRepository,
                           LocationMapper locationMapper)
    {
        this.locationRepository = locationRepository;
        this.inventoryRepository = inventoryRepository;
        this.transferOrderRepository = transferOrderRepository;
        this.reservationRepository = reservationRepository;
        this.locationMapper = locationMapper;
    }

    /**
     * Finds a location by its identifier.
     *
     * @param id the location identifier
     * @return the corresponding response DTO
     * @throws LocationNotFoundException if no location exists with the given id
     */
    public LocationResponse getLocationById(UUID id)
    {
        return locationRepository.findById(id).map(locationMapper::toResponse).orElseThrow(() -> new LocationNotFoundException("id", id));
    }

    /**
     * Finds a location by its code.
     *
     * @param code the unique location code
     * @return the corresponding response DTO
     * @throws LocationNotFoundException if no location exists with the given code
     */
    public LocationResponse getLocationByCode(String code)
    {
        String normalizedCode = code.trim().toUpperCase();
        return locationRepository.findByCode(normalizedCode)
                .map(locationMapper::toResponse)
                .orElseThrow(() -> new LocationNotFoundException("code", normalizedCode));
    }

    /**
     * Returns a page of all locations.
     *
     * @param pageable pagination and sorting information
     * @return a page of response DTOs
     */
    public Page<LocationResponse> getAllLocations(Pageable pageable)
    {
        return locationRepository.findAll(pageable).map(locationMapper::toResponse);
    }

    /**
     * Creates a new location.
     *
     * @param request the creation request
     * @return the response DTO of the saved location
     * @throws ConflictException if a location with the same code already exists
     */
    @Transactional
    public LocationResponse createLocation(CreateLocationRequest request)
    {
        String normalizedCode = request.code().trim().toUpperCase();
        if (locationRepository.existsByCode(normalizedCode))
        {
            throw new ConflictException("Location with code '" + normalizedCode + "' already exists");
        }
        Location location = locationMapper.toRequest(request);
        Location savedLocation = locationRepository.save(location);
        return locationMapper.toResponse(savedLocation);
    }

    /**
     * Updates the mutable details of an existing location.
     *
     * @param id      the location identifier
     * @param request the update request
     * @return the response DTO of the updated location
     * @throws LocationNotFoundException if no location exists with the given id
     */
    @Transactional
    public LocationResponse updateLocation(UUID id, UpdateLocationRequest request)
    {
        Location location = getLocation(id);
        location.updateDetails(request.name(), request.address());
        return locationMapper.toResponse(locationRepository.save(location));
    }

    /**
     * Deletes a location.
     *
     * @param id the location identifier
     * @throws LocationNotFoundException if no location exists with the given id
     * @throws IllegalStateException     if the location has inventory records, or is referenced by
     *                                   transfer orders or transfer order reservations
     */
    @Transactional
    public void deleteLocation(UUID id)
    {
        Location location = getLocation(id);
        if (inventoryRepository.existsByLocationId(id))
        {
            throw new IllegalStateException("Location cannot be deleted because it has inventory records");
        }
        if (transferOrderRepository.existsByLocationId(id))
        {
            throw new IllegalStateException("Location cannot be deleted because it is referenced by transfer orders");
        }
        if (reservationRepository.existsByLocationId(id))
        {
            throw new IllegalStateException("Location cannot be deleted because it is referenced by transfer order reservations");
        }
        locationRepository.delete(location);
    }

    /**
     * Loads a location by identifier or throws when it does not exist.
     *
     * @param id the location identifier
     * @return the location entity
     * @throws LocationNotFoundException if no location exists with the given id
     */
    private Location getLocation(UUID id)
    {
        return locationRepository.findById(id)
                .orElseThrow(() -> new LocationNotFoundException("id", id));
    }
}