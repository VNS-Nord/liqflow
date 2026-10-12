package com.vnsnord.liqflow.location;

/**
 * Classifies the role of a {@link Location} in the distribution network.
 *
 * <p>A location can act as a {@link #CENTRAL_WAREHOUSE}, a {@link #REGIONAL_HUB},
 * or a {@link #STORE}, which determines how stock flows between locations.</p>
 *
 * @see Location
 */
public enum LocationType
{
    CENTRAL_WAREHOUSE, REGIONAL_HUB, STORE
}