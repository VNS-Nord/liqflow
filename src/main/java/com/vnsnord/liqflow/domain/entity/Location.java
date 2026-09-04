package com.vnsnord.liqflow.domain.entity;

import com.vnsnord.liqflow.domain.enums.LocationType;
import jakarta.persistence.*;

import java.util.UUID;

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

    @Column(nullable = false, unique = true, length = 50)
    private String code;

    @Column(nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private LocationType type;

    @Column()
    private String address;

    protected Location()
    {
    }

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
        this.code = code;
        this.name = name;
        this.type = type;
        this.address = address;
    }

    public void updateDetails(String name, String address)
    {
        if (name == null || name.isBlank())
        {
            throw new IllegalArgumentException("Location name is required");
        }
        this.name = name;
        this.address = address;
    }

    public UUID getId()
    {
        return id;
    }

    public String getCode()
    {
        return code;
    }

    public String getName()
    {
        return name;
    }

    public LocationType getType()
    {
        return type;
    }

    public String getAddress()
    {
        return address;
    }
}
