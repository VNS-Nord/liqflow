package com.vnsnord.liqflow.domain.enums;

import com.vnsnord.liqflow.domain.entity.TransferOrderReservation;

/**
 * Lifecycle states of a {@link TransferOrderReservation}.
 *
 * <p>A reservation is created in {@link #HELD} when its transfer order is
 * submitted, and then moves exactly once to either {@link #CONSUMED} (the
 * transfer completed and the stock physically left the source location) or
 * {@link #RELEASED} (the order was cancelled and the stock became available
 * again). Terminal states are kept as history.</p>
 *
 * @see TransferOrderReservation
 */
public enum ReservationStatus
{
    HELD, CONSUMED, RELEASED
}
