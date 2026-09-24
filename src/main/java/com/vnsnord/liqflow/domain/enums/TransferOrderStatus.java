package com.vnsnord.liqflow.domain.enums;

import com.vnsnord.liqflow.domain.entity.TransferOrder;

/**
 * Lifecycle states of a {@link TransferOrder}.
 *
 * <p>An order progresses through {@link #DRAFT}, {@link #SUBMITTED}, and
 * {@link #IN_TRANSIT} before reaching {@link #COMPLETED}. A {@link #CANCELLED}
 * order may be reached from any state except a completed one.</p>
 *
 * @see TransferOrder
 */
public enum TransferOrderStatus
{
    DRAFT,
    SUBMITTED,
    IN_TRANSIT,
    COMPLETED,
    CANCELLED
}