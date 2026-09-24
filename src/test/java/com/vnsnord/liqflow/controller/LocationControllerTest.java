package com.vnsnord.liqflow.controller;

import com.vnsnord.liqflow.domain.enums.LocationType;
import com.vnsnord.liqflow.dto.request.CreateLocationRequest;
import com.vnsnord.liqflow.dto.response.LocationResponse;
import com.vnsnord.liqflow.dto.request.UpdateLocationRequest;
import com.vnsnord.liqflow.exception.LocationNotFoundException;
import com.vnsnord.liqflow.service.LocationService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(LocationController.class)
public class LocationControllerTest
{
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private LocationService locationService;

    @Test
    void getById_ShouldReturn200AndLocationDto_WhenExists() throws Exception
    {
        // Given
        UUID id = UUID.randomUUID();
        LocationResponse dto = new LocationResponse(
                id,
                "WH-MAIN",
                "Central Warehouse",
                LocationType.CENTRAL_WAREHOUSE,
                "Main Street 1"
        );

        Mockito.when(locationService.getLocationById(id)).thenReturn(dto);

        // When & Then
        mockMvc.perform(get("/api/v1/locations/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.code").value("WH-MAIN"))
                .andExpect(jsonPath("$.name").value("Central Warehouse"))
                .andExpect(jsonPath("$.type").value("CENTRAL_WAREHOUSE"))
                .andExpect(jsonPath("$.address").value("Main Street 1"));
    }

    @Test
    void getByCode_ShouldReturn200AndLocationDto_WhenExists() throws Exception
    {
        // Given
        String code = "WH-MAIN";
        UUID id = UUID.randomUUID();
        LocationResponse dto = new LocationResponse(
                id,
                code,
                "Central Warehouse",
                LocationType.CENTRAL_WAREHOUSE,
                "Main Street 1"
        );

        Mockito.when(locationService.getLocationByCode(code)).thenReturn(dto);

        // When & Then
        mockMvc.perform(get("/api/v1/locations/code/{code}", code)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(code))
                .andExpect(jsonPath("$.name").value("Central Warehouse"));
    }

    @Test
    void getById_ShouldReturn404_WhenNotFound() throws Exception
    {
        // Given
        UUID id = UUID.randomUUID();
        Mockito.when(locationService.getLocationById(id))
                .thenThrow(new LocationNotFoundException("id", id));

        // When & Then
        mockMvc.perform(get("/api/v1/locations/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.path").value("/api/v1/locations/" + id));
    }

    @Test
    void getAll_ShouldReturn200AndPagedLocations() throws Exception
    {
        // Given
        LocationResponse dto = new LocationResponse(
                UUID.randomUUID(),
                "WH-MAIN",
                "Central Warehouse",
                LocationType.CENTRAL_WAREHOUSE,
                "Main Street 1"
        );

        Mockito.when(locationService.getAllLocations(any()))
                .thenReturn(new PageImpl<>(List.of(dto), PageRequest.of(0, 10), 1));

        // When & Then
        mockMvc.perform(get("/api/v1/locations")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].code").value("WH-MAIN"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void createLocation_ShouldReturn201CreatedAndLocationHeader() throws Exception
    {
        // Given
        UUID id = UUID.randomUUID();
        LocationResponse response = new LocationResponse(id, "LOC-01", "Main Warehouse", LocationType.CENTRAL_WAREHOUSE, "Street 1");

        Mockito.when(locationService.createLocation(any(CreateLocationRequest.class))).thenReturn(response);

        // When & Then
        mockMvc.perform(post("/api/v1/locations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "code": "LOC-01",
                                    "name": "Main Warehouse",
                                    "type": "CENTRAL_WAREHOUSE",
                                    "address": "Street 1"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", org.hamcrest.Matchers.endsWith("/api/v1/locations/" + id)))
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.code").value("LOC-01"))
                .andExpect(jsonPath("$.name").value("Main Warehouse"));
    }

    @Test
    void createLocation_ShouldReturn400BadRequest_WhenValidationFails() throws Exception
    {
        // When & Then (tuščias kodas, pavadinimas bei trūkstamas tipas)
        mockMvc.perform(post("/api/v1/locations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "code": "",
                                    "name": "",
                                    "type": null
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateLocation_ShouldReturn200AndUpdatedDto() throws Exception
    {
        // Given
        UUID id = UUID.randomUUID();
        LocationResponse response = new LocationResponse(id, "WH-MAIN", "Renovated Warehouse",
                LocationType.CENTRAL_WAREHOUSE, "New street 2");

        Mockito.when(locationService.updateLocation(eq(id), any(UpdateLocationRequest.class))).thenReturn(response);

        // When & Then
        mockMvc.perform(put("/api/v1/locations/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "name": "Renovated Warehouse",
                                    "address": "New street 2"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.name").value("Renovated Warehouse"))
                .andExpect(jsonPath("$.address").value("New street 2"));
    }

    @Test
    void updateLocation_ShouldReturn400_WhenValidationFails() throws Exception
    {
        // Given
        UUID id = UUID.randomUUID();

        // When & Then
        mockMvc.perform(put("/api/v1/locations/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "name": ""
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deleteLocation_ShouldReturn204NoContent() throws Exception
    {
        // Given
        UUID id = UUID.randomUUID();

        // When & Then
        mockMvc.perform(delete("/api/v1/locations/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());
    }
}
