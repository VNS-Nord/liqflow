package com.vnsnord.liqflow.domain.entity;

import com.vnsnord.liqflow.domain.enums.LocationType;
import jakarta.persistence.*;

import java.util.UUID;

/**
 * Represents a physical location where products are stored, such as a central
 * warehouse, regional hub, or store.
 *
 * <p>Each location is identified by a unique {@code code}, which is trimmed and
 * upper-cased on creation. It also carries a human-readable {@code name}, a
 * {@link LocationType}, and an optional {@code address}.</p>
 *
 * @see LocationType
 * @see Inventory
 */
@Entity
@Table(
        name = "locations",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_location_code",
                columnNames = {"code"}
        )
)
public class Location
{
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 50)
    private String code;

    @Column(nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private LocationType type;

    @Column(length = 255)
    private String address;

    protected Location()
    {
    }

    /**
     * Creates a new location.
     *
     * @param code    the unique location code, trimmed and upper-cased (must not be blank)
     * @param name    the display name of the location (must not be blank)
     * @param type    the type of the location (must not be null)
     * @param address an optional address, or null/blank to leave it unset
     * @throws IllegalArgumentException if {@code code}, {@code name}, or {@code type} is missing
     */
    public Location(String code, String name, LocationType type, String address)
    {
        if (code == null || code.isBlank())
        {
            throw new IllegalArgumentException("Location code is required");
        }
        if (name == null || name.isBlank())
        {
            throw new IllegalArgumentException("Location name is required");
        }
        if (type == null)
        {
            throw new IllegalArgumentException("Location type is required");
        }
        this.code = code.trim().toUpperCase();
        this.name = name;
        this.type = type;
        this.address = (address == null || address.isBlank()) ? null : address.trim();
    }

    /**
     * Updates the display name and optionally the address of this location.
     *
     * @param name    the new display name (must not be blank)
     * @param address the new address, or null/blank to clear the current address
     * @throws IllegalArgumentException if {@code name} is blank
     */
    public void updateDetails(String name, String address)
    {
        if (name == null || name.isBlank())
        {
            throw new IllegalArgumentException("Location name is required");
        }
        this.name = name;
        this.address = (address == null || address.isBlank()) ? null : address.trim();
    }

    /**
     * @return the location identifier
     */
    public UUID getId()
    {
        return id;
    }

    /**
     * @return the unique, upper-cased location code
     */
    public String getCode()
    {
        return code;
    }

    /**
     * @return the display name of the location
     */
    public String getName()
    {
        return name;
    }

    /**
     * @return the type of the location
     */
    public LocationType getType()
    {
        return type;
    }

    /**
     * @return the optional address, or null
     */
    public String getAddress()
    {
        return address;
    }
}
