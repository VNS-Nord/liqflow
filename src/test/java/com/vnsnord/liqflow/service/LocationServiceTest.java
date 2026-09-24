package com.vnsnord.liqflow.service;

import com.vnsnord.liqflow.domain.entity.Location;
import com.vnsnord.liqflow.domain.enums.LocationType;
import com.vnsnord.liqflow.dto.CreateLocationRequest;
import com.vnsnord.liqflow.dto.LocationResponse;
import com.vnsnord.liqflow.dto.UpdateLocationRequest;
import com.vnsnord.liqflow.exception.ConflictException;
import com.vnsnord.liqflow.exception.LocationNotFoundException;
import com.vnsnord.liqflow.infrastructure.persistence.InventoryRepository;
import com.vnsnord.liqflow.infrastructure.persistence.LocationRepository;
import com.vnsnord.liqflow.infrastructure.persistence.TransferOrderRepository;
import com.vnsnord.liqflow.service.mapper.LocationMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class LocationServiceTest
{
    @Mock
    private LocationRepository locationRepository;
    @Mock
    private InventoryRepository inventoryRepository;
    @Mock
    private TransferOrderRepository transferOrderRepository;
    @Mock
    private LocationResponse locationResponse;

    @Mock
    private LocationMapper locationMapper;

    @InjectMocks
    private LocationService locationService;

    @Test
    void getLocationById_WhenLocationExists_ReturnsLocationDto()
    {
        Location location = createLocation();
        UUID locationId = location.getId();

        LocationResponse expectedDto = new LocationResponse(locationId,
                "HUB-ANYKSCIAI",
                "Anyksciu sandelis",
                LocationType.REGIONAL_HUB,
                "Ramybes g. 1-50, Anyksciai");

        when(locationRepository.findById(locationId)).thenReturn(Optional.of(location));
        when(locationMapper.toResponse(location)).thenReturn(expectedDto);

        LocationResponse actualDto = locationService.getLocationById(locationId);

        assertNotNull(actualDto);
        assertEquals(expectedDto.id(), actualDto.id());
        assertEquals(expectedDto.code(), actualDto.code());

        verify(locationRepository).findById(locationId);
        verify(locationMapper).toResponse(location);
    }

    @Test
    void getLocationById_WhenNotFound_ThrowsLocationNotFoundException()
    {

        UUID locationId = UUID.randomUUID();
        when(locationRepository.findById(locationId)).thenReturn(Optional.empty());

        LocationNotFoundException exception = assertThrows(
                LocationNotFoundException.class,
                () -> locationService.getLocationById(locationId)
        );

        assertEquals("Location not found with id: '" + locationId + "'", exception.getMessage());

        verify(locationRepository).findById(locationId);
        Mockito.verifyNoInteractions(locationMapper);
    }

    @Test
    void createLocation_ShouldSaveAndReturnLocationResponse_WhenCodeIsUnique()
    {
        // Given
        CreateLocationRequest request = new CreateLocationRequest("LOC-01", "Main Warehouse", LocationType.CENTRAL_WAREHOUSE, "Street 1");
        Location location = new Location("LOC-01", "Main Warehouse", LocationType.CENTRAL_WAREHOUSE, "Street 1");
        UUID generatedId = UUID.randomUUID();
        Location savedLocation = new Location("LOC-01", "Main Warehouse", LocationType.CENTRAL_WAREHOUSE, "Street 1");
        LocationResponse expectedResponse = new LocationResponse(generatedId, "LOC-01", "Main Warehouse", LocationType.CENTRAL_WAREHOUSE, "Street 1");

        when(locationRepository.existsByCode("LOC-01")).thenReturn(false);
        when(locationMapper.toRequest(request)).thenReturn(location);
        when(locationRepository.save(location)).thenReturn(savedLocation);
        when(locationMapper.toResponse(savedLocation)).thenReturn(expectedResponse);

        // When
        LocationResponse result = locationService.createLocation(request);

        // Then
        assertNotNull(result);
        assertEquals(generatedId, result.id());
        assertEquals("LOC-01", result.code());
        verify(locationRepository).save(location);
    }

    @Test
    void createLocation_ShouldThrowException_WhenCodeAlreadyExists()
    {
        // Given
        CreateLocationRequest request = new CreateLocationRequest("LOC-01", "Main Warehouse", LocationType.CENTRAL_WAREHOUSE, "Street 1");
        when(locationRepository.existsByCode("LOC-01")).thenReturn(true);

        // When & Then
        ConflictException exception = assertThrows(
                ConflictException.class,
                () -> locationService.createLocation(request)
        );

        assertEquals("Location with code 'LOC-01' already exists", exception.getMessage());
        verify(locationRepository, never()).save(any());
    }

    @Test
    void updateLocation_ShouldUpdateDetailsAndReturnResponse()
    {
        // Given
        Location location = createLocation();
        UUID locationId = location.getId();
        UpdateLocationRequest request = new UpdateLocationRequest("Updated Hub", "New address 1");

        Location saved = new Location("HUB-ANYKSCIAI", "Updated Hub", LocationType.REGIONAL_HUB, "New address 1");
        ReflectionTestUtils.setField(saved, "id", locationId);
        LocationResponse expectedResponse = new LocationResponse(
                locationId, "HUB-ANYKSCIAI", "Updated Hub", LocationType.REGIONAL_HUB, "New address 1");

        when(locationRepository.findById(locationId)).thenReturn(Optional.of(location));
        when(locationRepository.save(location)).thenReturn(saved);
        when(locationMapper.toResponse(saved)).thenReturn(expectedResponse);

        // When
        LocationResponse result = locationService.updateLocation(locationId, request);

        // Then
        assertNotNull(result);
        assertEquals("Updated Hub", result.name());
        assertEquals("New address 1", result.address());
        assertEquals("Updated Hub", location.getName());
        verify(locationRepository).save(location);
    }

    @Test
    void updateLocation_ShouldThrowLocationNotFoundException_WhenNotFound()
    {
        // Given
        UUID locationId = UUID.randomUUID();
        UpdateLocationRequest request = new UpdateLocationRequest("Updated Hub", "New address 1");
        when(locationRepository.findById(locationId)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(LocationNotFoundException.class, () -> locationService.updateLocation(locationId, request));
        verify(locationRepository, never()).save(any());
    }

    @Test
    void deleteLocation_ShouldDelete_WhenNotReferenced()
    {
        // Given
        Location location = createLocation();
        UUID locationId = location.getId();

        when(locationRepository.findById(locationId)).thenReturn(Optional.of(location));
        when(inventoryRepository.existsByLocationId(locationId)).thenReturn(false);
        when(transferOrderRepository.existsByLocationId(locationId)).thenReturn(false);

        // When
        locationService.deleteLocation(locationId);

        // Then
        verify(locationRepository).delete(location);
    }

    @Test
    void deleteLocation_ShouldThrow_WhenReferencedByInventory()
    {
        // Given
        Location location = createLocation();
        UUID locationId = location.getId();

        when(locationRepository.findById(locationId)).thenReturn(Optional.of(location));
        when(inventoryRepository.existsByLocationId(locationId)).thenReturn(true);

        // When & Then
        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> locationService.deleteLocation(locationId)
        );
        assertEquals("Location cannot be deleted because it has inventory records", exception.getMessage());
        verify(locationRepository, never()).delete(any());
    }

    @Test
    void deleteLocation_ShouldThrow_WhenReferencedByTransferOrder()
    {
        // Given
        Location location = createLocation();
        UUID locationId = location.getId();

        when(locationRepository.findById(locationId)).thenReturn(Optional.of(location));
        when(inventoryRepository.existsByLocationId(locationId)).thenReturn(false);
        when(transferOrderRepository.existsByLocationId(locationId)).thenReturn(true);

        // When & Then
        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> locationService.deleteLocation(locationId)
        );
        assertEquals("Location cannot be deleted because it is referenced by transfer orders", exception.getMessage());
        verify(locationRepository, never()).delete(any());
    }


    private Location createLocation()
    {
        Location location = new Location("HUB-ANYKSCIAI",
                "Anyksciu sandelis",
                LocationType.REGIONAL_HUB,
                "Ramybes g. 1-50, Anyksciai");
        UUID locationId = UUID.randomUUID();
        ReflectionTestUtils.setField(location, "id", locationId);
        return location;
    }

}
