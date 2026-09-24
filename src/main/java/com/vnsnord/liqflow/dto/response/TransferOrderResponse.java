package com.vnsnord.liqflow.dto.response;

import com.vnsnord.liqflow.domain.enums.TransferOrderStatus;

import java.time.Instant;
import java.util.UUID;

/**
 * Response payload describing a transfer order.
 *
 * <p>This summary omits the order's line items; use {@link TransferOrderDetailResponse}
 * to obtain the full contents of an order.</p>
 *
 * @param id                 the order identifier
 * @param orderNumber        the unique business order number
 * @param sourceLocationId   the identifier of the source location
 * @param sourceLocationCode the code of the source location
 * @param targetLocationId   the identifier of the target location
 * @param targetLocationCode the code of the target location
 * @param status             the current lifecycle status
 * @param createdAt          the creation timestamp
 */
public record TransferOrderResponse(UUID id,
                                    String orderNumber,
                                    UUID sourceLocationId,
                                    String sourceLocationCode,
                                    UUID targetLocationId,
                                    String targetLocationCode,
                                    TransferOrderStatus status,
                                    Instant createdAt)
{
}